package com.io7m.jxdeltaproc.tests;

import com.io7m.jxdeltapoc.Manifest;
import com.io7m.jxdeltapoc.ManifestXML;
import com.io7m.jxdeltapoc.Patcher;
import com.io7m.jxdeltapoc.PatcherEvent;
import com.io7m.jxdeltapoc.PatcherHardenedSAXParsers;
import com.io7m.jxdeltapoc.PatcherRemoteContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public final class Demo
{
  private static final Logger LOG = LoggerFactory.getLogger(Demo.class);

  private Demo()
  {
  }

  public static void main(final String[] args)
    throws Exception
  {
    final URL url = new URL("http://ataxia.io7m.com/jxdelta-poc/simple/manifest.xml");

    final PatcherHardenedSAXParsers parsers = PatcherHardenedSAXParsers.create();
    try (InputStream stream = url.openStream()) {
      final Manifest manifest = ManifestXML.parse(parsers, stream);

      final File directory = new File("/tmp/jxdelta-poc");
      final File file = new File("/tmp/jxdelta-poc/file.zip");
      Patcher.updateFromManifest(
        manifest,
        file,
        directory,
        Demo::fetchRemoteContent,
        Demo::onEvent);
    }
  }

  private static void onEvent(final PatcherEvent event)
  {
    LOG.debug("event: {}", event);
  }

  private static PatcherRemoteContent fetchRemoteContent(
    final URI url)
    throws IOException
  {
    final HttpURLConnection connection =
      (HttpURLConnection) url.toURL().openConnection();

    final int code = connection.getResponseCode();
    if (code >= 200 && code < 400) {
      return PatcherRemoteContent.create(
        url, connection.getContentLengthLong(), connection.getInputStream());
    }
    throw new IOException("HTTP error: " + code);
  }
}
