package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.IGNORE_PATTERN;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.*;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * For any config related change
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class SumoJobConfigListener extends SaveableListener implements Serializable {

    private static final Logger LOG = Logger.getLogger(SumoJobConfigListener.class.getName());

    private static final Pattern IGNORED = Pattern.compile(IGNORE_PATTERN, Pattern.CASE_INSENSITIVE);
    private WeakHashMap<String, Integer> cached = new WeakHashMap<>(512);

    @Override
    public void onChange(Saveable saveable, XmlFile file) {
        try {
            String configPath = file.getFile().getAbsolutePath();
            if (saveable == null || IGNORED.matcher(configPath).find()
                    || "SYSTEM".equals(Jenkins.getAuthentication().getName())
                    || saveable instanceof User) {
                return;
            }

            String configContent = file.asString();
            String checkSum = DigestUtils.md5Hex(configPath + configContent);
            if (cached.containsKey(checkSum)) {
                return;
            }
            cached.put(checkSum, 0);

            String encodeFileToString = Base64.getEncoder().encodeToString(file.asString().getBytes());

            File oldFile = getOldFile(file);
            byte[] bytes = fileToString(oldFile);
            String oldFileAsString = "";
            if (bytes != null && bytes.length > 0) {
                oldFileAsString = Base64.getEncoder().encodeToString(bytes);
            }
            if (!encodeFileToString.equals(oldFileAsString)) {
                captureConfigChanges(encodeFileToString, oldFileAsString, AuditEventTypeEnum.CHANGES_IN_CONFIG, getRelativeJenkinsHomePath(file.getFile().getAbsolutePath()));
            }

            try {
                Files.copy(file.getFile().toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOG.warning("Can not copy the data to old file");
            }
            if (!(saveable instanceof Item)) {
                captureItemAuditEvent(AuditEventTypeEnum.UPDATED, file.getFile().getName(), null);
            }
        } catch (Exception exception) {
            LOG.warning("An error occurred while Checking the Job Configuration" + Arrays.toString(exception.getStackTrace()));
        }
    }

    private static File getOldFile(XmlFile file) {
        File oldFile = null;
        String pathForOldFile = file.getFile().getParent() + "/" + file.getFile().getName().replace(".xml", "") + "_old.xml";
        if (file.getFile().getParentFile() != null) {
            for (File fileNames : Objects.requireNonNull(file.getFile().getParentFile().listFiles())) {
                if (fileNames.getPath().matches(pathForOldFile)) {
                    oldFile = fileNames;
                    break;
                }
            }
        }

        if (oldFile == null) {
            oldFile = new File(pathForOldFile);
        }
        return oldFile;
    }

    private static byte[] fileToString(File file) {
        try {
            if (file.length() > 0) {
                int length = (int) file.length();
                BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
                byte[] bytes = new byte[length];
                reader.read(bytes, 0, length);
                reader.close();
                return bytes;
            }
        } catch (Exception e) {
            LOG.warning("Conversion to string failed for " + file.toPath());
        }
        return null;
    }

    /*private static List<Map<String, Object>> compare(File oldFile, File newFile) throws Exception {
        List<Map<String, Object>> diff = new ArrayList<>();

        Diff build = DiffBuilder.compare(newFile).withTest(oldFile)
                .ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace().build();

        Iterator<Difference> iter = build.getDifferences().iterator();
        while(iter.hasNext()){
            Difference next = iter.next();
            if(ComparisonResult.DIFFERENT.equals(next.getResult())){
                Map<String, Object> differences = new HashMap<>();
                Object currentValue = next.getComparison().getControlDetails().getValue();
                Object oldValue = next.getComparison().getTestDetails().getValue();
                String nodeName = next.getComparison().getControlDetails().getTarget().getParentNode().getNodeName();
                differences.put("nodeName", nodeName);
                differences.put("currentValue", currentValue);
                differences.put("oldValue", oldValue);
                diff.add(differences);
            }
        }
        return diff;
    }*/
}
