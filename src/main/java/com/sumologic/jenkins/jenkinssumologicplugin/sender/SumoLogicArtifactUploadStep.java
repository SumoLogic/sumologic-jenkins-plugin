package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.google.common.base.Preconditions;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SumoLogicArtifactUploadStep extends Step {

    private String file;
    private String includePathPattern;
    private String excludePathPattern;
    private String workingDir;

    @DataBoundConstructor
    public SumoLogicArtifactUploadStep() {
    }

    public String getFile() {
        return file;
    }

    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    public String getIncludePathPattern() {
        return includePathPattern;
    }

    @DataBoundSetter
    public void setIncludePathPattern(String includePathPattern) {
        this.includePathPattern = includePathPattern;
    }

    public String getExcludePathPattern() {
        return excludePathPattern;
    }

    @DataBoundSetter
    public void setExcludePathPattern(String excludePathPattern) {
        this.excludePathPattern = excludePathPattern;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    @DataBoundSetter
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SumoLogicArtifactUploadStep.Execution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<Class<?>>();
        }

        @Override
        public String getFunctionName() {
            return "SumoUpload";
        }

        @Override
        public String getDisplayName() {
            return "Upload Artifacts or files to Sumo Logic HTTP source as in Sumo Logic Publisher Configuration.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<String> {

        protected static final long serialVersionUID = 1L;

        protected final transient SumoLogicArtifactUploadStep step;

        public Execution(SumoLogicArtifactUploadStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            final String file = this.step.getFile();
            final String includePathPattern = this.step.getIncludePathPattern();
            final String excludePathPattern = this.step.getExcludePathPattern();
            final String workingDir = this.step.getWorkingDir();

            boolean omitSourcePath = false;

            Preconditions.checkArgument(file != null || includePathPattern != null, "File or IncludePathPattern must not be null");
            Preconditions.checkArgument(includePathPattern == null || file == null, "File and IncludePathPattern cannot be use together");

            final List<FilePath> artifacts = new ArrayList<>();
            final FilePath directory;

            if (workingDir != null && !"".equals(workingDir.trim())) {
                directory = Objects.requireNonNull(this.getContext().get(FilePath.class)).child(workingDir);
            } else {
                directory = this.getContext().get(FilePath.class);
            }

            TaskListener listener = Execution.this.getContext().get(TaskListener.class);

            if (directory != null) {
                if (file != null) {
                    artifacts.add(directory.child(file));
                    omitSourcePath = true;
                } else if (excludePathPattern != null && !excludePathPattern.trim().isEmpty()) {
                    artifacts.addAll(Arrays.asList(directory.list(includePathPattern, excludePathPattern, true)));
                } else {
                    artifacts.addAll(Arrays.asList(directory.list(includePathPattern, null, true)));

                }

                if (artifacts.isEmpty()) {
                    listener.getLogger().println("No Artifacts to upload.");
                    return null;
                } else if (omitSourcePath) {
                    FilePath artifact = artifacts.get(0);
                    listener.getLogger().println("Upload to Sumo Logic with File as " + artifact.toURI());
                    if (!artifact.exists()) {
                        listener.getLogger().println("Upload failed due to missing source file " + artifact.toURI().toString());
                    }
                    artifact.act(new FileUploader(listener));
                    listener.getLogger().println("Upload complete");
                    return "Uploaded to Sumo Logic";
                } else {
                    List<File> fileList = new ArrayList<>();
                    listener.getLogger().println("Upload to Sumo Logic with Include Path Pattern as " + includePathPattern);
                    for (FilePath child : artifacts) {
                        fileList.add(child.act(FIND_FILE_ON_SLAVE));
                    }
                    directory.act(new FileListUploader(listener, fileList));
                    listener.getLogger().println("Upload complete for files " + Arrays.toString(fileList.toArray()));
                    return "Uploaded to Sumo Logic";
                }
            } else {
                return null;
            }
        }
    }

    private static class FileUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;
        private final TaskListener taskListener;
        private final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

        FileUploader(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        @Override
        public Void invoke(File localFile, VirtualChannel channel) throws IOException, InterruptedException {
            if (localFile.isFile()) {
                logSenderHelper.sendFilesData(localFile);
            } else if (localFile.isDirectory()) {
                File[] files = localFile.listFiles();
                if (ArrayUtils.isNotEmpty(files)) {
                    for (File dataFile : files) {
                        CompletableFuture.runAsync(() -> logSenderHelper.sendFilesData(dataFile));
                    }
                }
            }
            return null;
        }
    }

    private static class FileListUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;

        private final TaskListener taskListener;
        private final List<File> fileList;
        private final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

        FileListUploader(TaskListener taskListener, List<File> fileList) {
            this.taskListener = taskListener;
            this.fileList = fileList;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            if (CollectionUtils.isNotEmpty(this.fileList)) {
                fileList.forEach(dataFile -> CompletableFuture.runAsync(() -> logSenderHelper.sendFilesData(dataFile)));
            }
            return null;
        }
    }

    private static MasterToSlaveFileCallable<File> FIND_FILE_ON_SLAVE = new MasterToSlaveFileCallable<File>() {
        @Override
        public File invoke(File localFile, VirtualChannel channel) throws IOException, InterruptedException {
            return localFile;
        }
    };
}
