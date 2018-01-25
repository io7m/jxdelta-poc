package com.io7m.jxdeltapoc;

import com.google.auto.value.AutoValue;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@AutoValue
public abstract class PatcherRemoteContent implements Closeable
{
  public static PatcherRemoteContent create(
    final URI uri,
    final long size,
    final InputStream stream)
  {
    return new AutoValue_PatcherRemoteContent(uri, size, stream);
  }

  public abstract URI uri();

  public abstract long size();

  public abstract InputStream stream();

  @Override
  public final void close()
    throws IOException
  {
    this.stream().close();
  }
}
