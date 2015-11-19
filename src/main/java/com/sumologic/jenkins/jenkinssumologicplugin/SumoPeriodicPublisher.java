package com.sumologic.jenkins.jenkinssumologicplugin;

import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by deven on 8/6/15.
 *
 * Periodically publish jenkins system metadata to sumo
 */
@Extension
public class SumoPeriodicPublisher extends AsyncPeriodicWork {
  protected static final long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);
  private static final Logger LOGGER = Logger.getLogger(SumoPeriodicPublisher.class.getName());

  public SumoPeriodicPublisher() {
    super("Sumologic Periodic Data Publisher");
    LOGGER.log(Level.FINE, "Sumologic status publishing period is {0}ms", recurrencePeriod);
  }

  @Override
  protected void execute(TaskListener listener) throws IOException, InterruptedException {
    new SumoLogic().push(ModelFactory.generateJenkinsModelFor(Jenkins.getInstance()).toJson());

  }

  @Override
  public long getRecurrencePeriod() {
    return recurrencePeriod;
  }
}
