package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.google.gson.Gson;
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

import javax.annotation.Nonnull;
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
    private HashMap<String, Object> keyValueMap = new HashMap<>();
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

    public HashMap<String, Object> getKeyValueMap() {
        return keyValueMap;
    }

    @DataBoundSetter
    public void setKeyValueMap(HashMap<String, Object> keyValueMap) {
        this.keyValueMap = keyValueMap;
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

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Upload files or Text to Sumo Logic HTTP source as provided in Sumo Logic Publisher Configuration.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<String> {

        protected static final long serialVersionUID = 1L;

        protected final transient SumoLogicFileUploadStep step;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
        private static final PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

        public Execution(SumoLogicFileUploadStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        public void sendTextData(String text, String sourceName, HashMap<String, String> fields) {
            List<String> lines = new ArrayList<>();
            if (StringUtils.isNotEmpty(text)) {
                lines.add(text);
                logSenderHelper.sendFilesData(lines, sourceName, pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(),
                        fields, pluginDescriptor.getMetricDataPrefix());
            }
        }

        @Override
        protected String run() throws Exception {
            final String file = this.step.getFile();
            final String includePathPattern = this.step.getIncludePathPattern();
            final String excludePathPattern = this.step.getExcludePathPattern();
            final String workingDir = this.step.getWorkingDir();
            final String text = this.step.getText();
            final HashMap<String, Object> keyValueMap = this.step.getKeyValueMap();
            final HashMap<String, String> fields = this.step.getFields();

            if (includePathPattern == null && file == null && text == null && keyValueMap.isEmpty()) {
                throw new Exception("File or IncludePathPattern or Text must not be null");
            }

            if ((includePathPattern != null && file != null) || (includePathPattern != null && text != null)
                    || (includePathPattern != null && !keyValueMap.isEmpty()) || (file != null && text != null)
                    || (file != null && !keyValueMap.isEmpty()) || (text != null && !keyValueMap.isEmpty())) {
                throw new Exception("File, IncludePathPattern, Text and KeyValueMap cannot be use together");
            }

            // Generate Directory Path
            final FilePath directory;
            if (workingDir != null && !"".equals(workingDir.trim())) {
                directory = Objects.requireNonNull(this.getContext().get(FilePath.class)).child(workingDir);
            } else {
                directory = this.getContext().get(FilePath.class);
            }

            if (directory != null) {
                // Get Listener for Logging
                TaskListener listener = Execution.this.getContext().get(TaskListener.class);
                // Get Run object for Job Information
                Run run = this.getContext().get(Run.class);
                // Get Build Number and Full Name
                String sourceName = run.getParent().getFullName() + "#" + run.getNumber();

                boolean omitSourcePath = false;
                final List<FilePath> files = new ArrayList<>();

                // Decide if text, KeyValueMap, File Path, Directory or File list
                if (text != null) {
                    sendTextData(text, sourceName, fields);
                    listener.getLogger().println(String.format("Uploaded Text %s to Sumo Logic.", text));
                } else if (!keyValueMap.isEmpty()) {
                    Gson gson = new Gson();
                    String mapString = gson.toJson(keyValueMap);
                    sendTextData(mapString, sourceName, fields);
                    listener.getLogger().println(String.format("Uploaded KeyValueMap String %s to Sumo Logic.", mapString));
                } else if (file != null) {
                    files.add(directory.child(file));
                    omitSourcePath = true;
                } else if (excludePathPattern != null && !excludePathPattern.trim().isEmpty()) {
                    files.addAll(Arrays.asList(directory.list(includePathPattern, excludePathPattern, true)));
                } else {
                    files.addAll(Arrays.asList(directory.list(includePathPattern, null, true)));
                }

                if (!files.isEmpty()) {
                    if (omitSourcePath) {
                        FilePath file_name = files.get(0);
                        listener.getLogger().println("Uploading to Sumo Logic with File as " + file_name.toURI());
                        if (!file_name.exists()) {
                            listener.getLogger().println("Upload failed due to missing source file " + file_name.toURI().toString());
                        } else {
                            file_name.act(new FileUploader(pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(),
                                    pluginDescriptor.getMetricDataPrefix(), sourceName, fields));
                            listener.getLogger().println("Upload complete");
                        }
                    } else {
                        List<File> fileList = new ArrayList<>();
                        listener.getLogger().println("Uploading to Sumo Logic with Include Path Pattern as " + includePathPattern);
                        for (FilePath child : files) {
                            fileList.add(child.act(new Find_File_On_Slave()));
                        }
                        directory.act(new FileListUploader(fileList, pluginDescriptor.getUrl(), pluginDescriptor.getSourceCategory(),
                                pluginDescriptor.getMetricDataPrefix(), sourceName, fields));
                        listener.getLogger().println("Upload complete for files " + Arrays.toString(fileList.toArray()));
                    }
                    return "Uploaded to Sumo Logic.";
                }
            }
            return null;
        }
    }

    private static class FileUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
        private final String url;
        private final String sourceCategory;
        private final String hostName;
        private final String sourceName;
        private final HashMap<String, String> fields;

        FileUploader(String url, String sourceCategory, String hostName, String sourceName, HashMap<String, String> fields) {
            this.url = url;
            this.sourceCategory = sourceCategory;
            this.hostName = hostName;
            this.sourceName = sourceName;
            this.fields = fields;
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
                logSenderHelper.sendFilesData(lines, this.sourceName + "#" + localFile.toURI().toString(),
                        url, sourceCategory, fields, hostName);
            }
        }
    }

    private static class FileListUploader extends MasterToSlaveFileCallable<Void> {
        protected static final long serialVersionUID = 1L;

        private final List<File> fileList;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
        private final String url;
        private final String sourceCategory;
        private final String hostName;
        private final String sourceName;
        private final HashMap<String, String> fields;

        FileListUploader(List<File> fileList, String url, String sourceCategory, String hostName, String sourceName, HashMap<String, String> fields) {
            this.fileList = fileList;
            this.url = url;
            this.sourceCategory = sourceCategory;
            this.hostName = hostName;
            this.sourceName = sourceName;
            this.fields = fields;
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
                logSenderHelper.sendFilesData(lines, this.sourceName + "#" + localFile.toURI().toString(),
                        url, sourceCategory, fields, hostName);
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
