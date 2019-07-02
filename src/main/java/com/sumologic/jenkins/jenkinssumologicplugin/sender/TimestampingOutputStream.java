package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;

/**
 * OutputStream decorator that for each new line in the stream additionally adds a consistent timestamp
 * and forwards the rest of the stream line without modifications.
 *
 * Created by lukasz on 3/21/17.
 */
public class TimestampingOutputStream extends LineTransformationOutputStream {

  private static final Logger LOGGER = Logger.getLogger(TimestampingOutputStream.class.getName());

  private OutputStream wrappedStream;

  public TimestampingOutputStream(OutputStream stream) {
    super();
    wrappedStream = stream;
  }

  /**
   * Heuristic used for determining multiline log messages, e.g. stack traces.
   * For Sumo Logic purposes only lines prefixed with timestamp will be considered a beginning of new log message.
   *
   * @param bytes - byte array containing single log line
   * @param i - log line length (can be less that bytes.length)
   * @return false if line starts with whitespace, true otherwise
   */
  public static boolean shouldPutTimestamp(byte[] bytes, int i) {
    String prefix = new String(bytes, 0, i < 4 ? i : 4, Charset.forName("UTF-8"));

    if (prefix.length() <= 0 || Character.isWhitespace(prefix.charAt(0))) {
      return false;
    }

    return true;
  }

  public static byte[] getTimestampAsByteArray(String jobName, String jobNumber) {
    String timeStampStr = "[" + DATETIME_FORMATTER.format(new Date()) + "] "+" "+jobName+"#"+jobNumber+" ";
    byte[] timestampBytes = timeStampStr.getBytes();

    return timestampBytes;
  }

  public static byte[] getTimestampAsByteArray() {
    String timeStampStr = "[" + DATETIME_FORMATTER.format(new Date()) + "] ";
    byte[] timestampBytes = timeStampStr.getBytes();

    return timestampBytes;
  }

  @Override
  protected void eol(byte[] bytes, int i) throws IOException {
    if (shouldPutTimestamp(bytes, i)) {
      byte[] timestampBytes = getTimestampAsByteArray();
      wrappedStream.write(timestampBytes, 0, timestampBytes.length);
    }

    wrappedStream.write(bytes, 0, i);
  }
}
