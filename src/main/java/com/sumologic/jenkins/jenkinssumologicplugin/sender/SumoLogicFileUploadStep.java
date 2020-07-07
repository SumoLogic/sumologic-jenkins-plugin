package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.google.common.base.Preconditions;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SumoLogicFileUploadStep extends Step {

    private String file;
    private String includePathPattern;
    private String excludePathPattern;
    private String workingDir;
    private String text;
    private HashMap<String, String> fields = new HashMap<>();

    @DataBoundConstructor
    public SumoLogicFileUploadStep() {
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

    public String getText() {
        return text;
    }

    @DataBoundSetter
    public void setText(String text) {
        this.text = text;
    }

    public HashMap<String, String> getFields() {
        return fields;
    }

    @DataBoundSetter
    public void setFields(HashMap<String, String> fields) {
        this.fields = fields;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SumoLogicFileUploadStep.Execution(this, context);
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
            return "Upload files or Text to Sumo Logic HTTP source as provided in Sumo Logic Publisher Configuration.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<String> {

        protected static final long serialVersionUID = 1L;

        protected final transient SumoLogicFileUploadStep step;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

        public Execution(SumoLogicFileUploadStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        public void sendTextData(String text, String url, String sourceCategory, String jobName, int number, HashMap<String, String> fields, String host) {
            List<String> lines = new ArrayList<>();
            if (StringUtils.isNotEmpty(text)) {
                lines.add(text);
                logSenderHelper.sendFilesData(lines, jobName + "#" + number, url, sourceCategory, fields, host);
            }
        }

        @Override
        protected String run() throws Exception {
            final String file = this.step.getFile();
            final String includePathPattern = this.step.getIncludePathPattern();
            final String excludePathPattern = this.step.getExcludePathPattern();
            final String workingDir = this.step.getWorkingDir();
            final String text = this.step.getText();
            final HashMap<String, String> fields = this.step.getFields();

            Run run = this.getContext().get(Run.class);
            boolean omitSourcePath = false;

            if (includePathPattern == null && file == null && text == null){
                throw new Exception("File or IncludePathPattern or Text must not be null");
            }

            if ((includePathPattern != null && file != null) || (file != null && text != null) || (text != null && includePathPattern != null)){
                throw new Exception("File and IncludePathPattern and Text cannot be use together");
            }

            final List<FilePath> files = new ArrayList<>();
            final FilePath directory;

            if (workingDir != null && !"".equals(workingDir.trim())) {
                directory = Objects.requireNonNull(this.getContext().get(FilePath.class)).child(workingDir);
            } else {
                directory = this.getContext().get(FilePath.class);
            }

            TaskListener listener = Execution.this.getContext().get(TaskListener.class);

            if (directory != null) {
                PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

                if (text != null) {
                    listener.getLogger().println(String.format("Sending Text %s to Sumo Logic with Fields as %s", text, fields));
                    sendTextData(text, pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(), run.getParent().getFullName(), run.getNumber(), fields, pluginDescriptor.getMetricDataPrefix());
                } else if (file != null) {
                    files.add(directory.child(file));
                    omitSourcePath = true;
                } else if (excludePathPattern != null && !excludePathPattern.trim().isEmpty()) {
                    files.addAll(Arrays.asList(directory.list(includePathPattern, excludePathPattern, true)));
                } else {
                    files.addAll(Arrays.asList(directory.list(includePathPattern, null, true)));
                }

                if (omitSourcePath) {
                    FilePath file_name = files.get(0);
                    listener.getLogger().println("Uploading to Sumo Logic with File as " + file_name.toURI());
                    if (!file_name.exists()) {
                        listener.getLogger().println("Upload failed due to missing source file " + file_name.toURI().toString());
                    } else {
                        file_name.act(new FileUploader(pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(), run.getParent().getFullName(), run.getNumber(), fields, pluginDescriptor.getMetricDataPrefix()));
                        listener.getLogger().println("Upload complete");
                    }
                } else {
                    List<File> fileList = new ArrayList<>();
                    listener.getLogger().println("Uploading to Sumo Logic with Include Path Pattern as " + includePathPattern);
                    for (FilePath child : files) {
                        fileList.add(child.act(new Find_File_On_Slave()));
                    }
                    directory.act(new FileListUploader(fileList, pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(), run.getParent().getFullName(), run.getNumber(), fields, pluginDescriptor.getMetricDataPrefix()));
                    listener.getLogger().println("Upload complete for files " + Arrays.toString(fileList.toArray()));
                }
                return "Uploaded to Sumo Logic";
            } else {
                return null;
            }
        }
    }

    private static class FileUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
        private final String url;
        private final String sourceCategory;
        private final String jobName;
        private final int buildNumber;
        private HashMap<String, String> fields;
        private String hostName;

        FileUploader(String url, String sourceCategory, String jobName, int buildNumber, HashMap<String, String> fields, String hostName) {
            this.url = url;
            this.sourceCategory = sourceCategory;
            this.jobName = jobName;
            this.buildNumber = buildNumber;
            this.fields = fields;
            this.hostName = hostName;
        }

        @Override
        public Void invoke(File localFile, VirtualChannel channel) throws IOException, InterruptedException {
            if (localFile.isFile()) {
                sendFilesData(localFile);
            } else if (localFile.isDirectory()) {
                File[] files = localFile.listFiles();
                if (ArrayUtils.isNotEmpty(files)) {
                    for (File dataFile : files) {
                        sendFilesData(dataFile);
                    }
                }
            }
            return null;
        }

        public void sendFilesData(File localFile) throws IOException {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(localFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                logSenderHelper.sendFilesData(lines, this.jobName + "#" + this.buildNumber + "#" + localFile.toURI().toString(), url, sourceCategory, fields, hostName);
            }
        }
    }

    private static class FileListUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;

        private final List<File> fileList;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
        private final String url;
        private final String sourceCategory;
        private final String jobName;
        private final int buildNumber;
        private HashMap<String, String> fields;
        private String hostName;

        FileListUploader(List<File> fileList, String url, String sourceCategory, String jobName, int buildNumber, HashMap<String, String> fields, String hostName) {
            this.fileList = fileList;
            this.url = url;
            this.sourceCategory = sourceCategory;
            this.jobName = jobName;
            this.buildNumber = buildNumber;
            this.fields = fields;
            this.hostName = hostName;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            if (CollectionUtils.isNotEmpty(this.fileList)) {
                for (File dataFile : fileList) {
                    sendFilesData(dataFile);
                }
            }
            return null;
        }

        public void sendFilesData(File localFile) throws IOException {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(localFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                logSenderHelper.sendFilesData(lines, this.jobName + "#" + this.buildNumber + "#" + localFile.toURI().toString(), url, sourceCategory, fields, hostName);
            }
        }
    }

    private static class Find_File_On_Slave extends MasterToSlaveFileCallable<File> {
        protected static final long serialVersionUID = 1L;

        @Override
        public File invoke(File localFile, VirtualChannel channel) throws IOException, InterruptedException {
            return localFile;
        }
    }
}
