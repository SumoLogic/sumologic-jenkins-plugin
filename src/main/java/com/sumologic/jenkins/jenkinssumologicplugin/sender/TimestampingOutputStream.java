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

/**
 * OutputStream decorator that for each new line in the stream additionally adds a consistent timestamp
 * and forwards the rest of the stream line without modifications.
 *
 * Created by lukasz on 3/21/17.
 */
public class TimestampingOutputStream extends LineTransformationOutputStream {

  private static final Logger LOGGER = Logger.getLogger(TimestampingOutputStream.class.getName());
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS ZZZZ");

  private OutputStream wrappedStream;

  public TimestampingOutputStream(OutputStream stream) {
    super();
    wrappedStream = stream;
  }

  public static byte[] getTimestampAsByteArray() {
    String timeStampStr = "[" + DATE_FORMAT.format(new Date()) + "] ";
    byte[] timestampBytes = timeStampStr.getBytes();

    return timestampBytes;
  }

  public static boolean shouldPutTimestamp(byte[] bytes, int i) {
    String prefix = new String(bytes, 0, i < 4 ? i : 4, Charset.forName("UTF-8"));

    if (prefix.length() <= 0 || Character.isWhitespace(prefix.charAt(0))) {
      return false;
    }

    return true;
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
