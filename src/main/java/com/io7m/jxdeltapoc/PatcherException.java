package com.io7m.jxdeltapoc;

public abstract class PatcherException extends Exception
{
  public PatcherException(final String message)
  {
    super(message);
  }

  public PatcherException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  public PatcherException(final Throwable cause)
  {
    super(cause);
  }
}
