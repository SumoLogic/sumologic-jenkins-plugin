package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.captureItemAuditEvent;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Sumo Item Listener for any event in Jenkins
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class SumoItemListener extends ItemListener {

    @Override
    public void onCreated(Item item) {
        captureItemAuditEvent(AuditEventTypeEnum.CREATED, getItemName(item), null);
    }

    @Override
    public void onCopied(Item src, Item item) {
        captureItemAuditEvent(AuditEventTypeEnum.UPDATED, getItemName(item), getItemName(src));
    }

    @Override
    public void onDeleted(Item item) {
        captureItemAuditEvent(AuditEventTypeEnum.DELETED, getItemName(item), null);
    }

    @Override
    public void onUpdated(Item item) {
        captureItemAuditEvent(AuditEventTypeEnum.UPDATED, getItemName(item), null);
    }

    @Override
    public void onLocationChanged(Item item, String oldFullName, String newFullName) {
        captureItemAuditEvent(AuditEventTypeEnum.UPDATED, oldFullName, newFullName);
    }

    @Override
    public void onBeforeShutdown() {
        //Can stop any process like sending logs, Metrics sending
        PluginDescriptorImpl.getInstance().getSumoMetricDataPublisher().stopReporter();
    }


    private String getItemName(Item item) {
        if (item == null) {
            return "unknown";
        }
        return item.getName();
    }
}
