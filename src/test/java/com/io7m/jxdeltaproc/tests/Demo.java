package com.io7m.jxdeltaproc.tests;

import com.io7m.jxdeltapoc.Patcher;
import com.io7m.jxdeltapoc.PatcherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public final class Demo
{
  private static final Logger LOG = LoggerFactory.getLogger(Demo.class);

  private Demo()
  {
  }

  public static void main(final String[] args)
    throws Exception
  {
    final File directory =
      new File("/tmp/jxdelta-poc");
    final File file =
      new File("/tmp/jxdelta-poc/file.zip");

    Patcher.updateFromRemote(
      URI.create("http://ataxia.io7m.com/jxdelta-poc/simple/manifest.xml"),
      file,
      directory,
      Patcher::remoteContentForURI,
      Demo::onEvent);
  }

  private static void onEvent(final PatcherEvent event)
  {
    LOG.debug("event: {}", event);
  }

}
