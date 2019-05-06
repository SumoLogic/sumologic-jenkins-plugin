package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogListener;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Custom pipeline step that can be used without a node and build wrapper.
 *
 * Created by - Sourabh Jain 5/2019
 */
public class SumoPipelineLogCollection extends Step {

    private final static Logger LOG = Logger.getLogger(SumoPipelineLogCollection.class.getName());

    @DataBoundConstructor
    public SumoPipelineLogCollection() {}

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(context);
    }

    /**
     * Execution for {@link SumoPipelineLogCollection}.
     */
    public static class Execution extends StepExecution {

        protected Execution(StepContext context) {
            super(context);
        }

        @Override
        public void onResume() {}

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean start() throws Exception {
            StepContext context = getContext();
            context.newBodyInvoker().
                    withContext(createConsoleLogFilter(context)).
                    withCallback(BodyExecutionCallback.wrap(context)).
                    start();
            return false;
        }

        private ConsoleLogFilter createConsoleLogFilter(StepContext context)
                throws IOException, InterruptedException {
            ConsoleLogFilter original = context.get(ConsoleLogFilter.class);
            Run build = context.get(Run.class);
            ConsoleLogFilter subsequent = new LogListener(build);
            return BodyInvoker.mergeConsoleLogFilters(original, subsequent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stop(@Nonnull Throwable cause) {
            getContext().onFailure(cause);
        }
    }

    /**
     * Descriptor for {@link SumoPipelineLogCollection}.
     */
    @Extension(optional = true)
    public static class StepDescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "SumoPipelineLogCollection";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFunctionName() {
            return "SumoPipelineLogCollection";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<Class<?>>();
        }

    }

}
