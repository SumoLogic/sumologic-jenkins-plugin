package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Identify the Pipeline stages information
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public abstract class SumoPipelineJobIdentifier<R extends Run> implements ExtensionPoint {
    private final Class<R> targetType;

    SumoPipelineJobIdentifier() {
        Type type = Types.getBaseClass(getClass(), SumoPipelineJobIdentifier.class);
        if (type instanceof ParameterizedType)
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        else
            throw new IllegalStateException(getClass() + " uses the raw type for extending SumoPipelineJobIdentifier");
    }

    public abstract void sendPipelineStagesAndConsoleLogs(R r, BuildModel buildModel, PluginDescriptorImpl pluginDescriptor, boolean isSpecificJobFlagEnabled);

    /**
     * @return Returns all the registered {@link SumoPipelineJobIdentifier}
     */
    private static ExtensionList<SumoPipelineJobIdentifier> all() {
        return ExtensionList.lookup(SumoPipelineJobIdentifier.class);
    }

    static List<SumoPipelineJobIdentifier> canApply(Run run) {
        List<SumoPipelineJobIdentifier> extensions = new ArrayList<>();
        for (SumoPipelineJobIdentifier extendListener : SumoPipelineJobIdentifier.all()) {
            if (extendListener.targetType.isInstance(run)) {
                extensions.add(extendListener);
            }
        }
        return extensions;
    }
}