package com.io7m.jxdeltapoc;

import java.io.IOException;
import java.net.URI;

public interface PatcherURIStreamProviderType
{
  PatcherRemoteContent openURI(URI uri)
    throws IOException;
}
