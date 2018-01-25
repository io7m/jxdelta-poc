package com.io7m.jxdeltapoc;

import com.google.auto.value.AutoValue;

import java.net.URI;

import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_FAILED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_FINISHED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_PROGRESS;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_STARTED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_UPDATE_FINISHED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_UPDATE_STARTED;

public abstract class PatcherEvent
{
  public abstract Kind kind();

  public enum Kind
  {
    PATCHER_UPDATE_STARTED,
    PATCHER_DOWNLOAD_STARTED,
    PATCHER_DOWNLOAD_PROGRESS,
    PATCHER_DOWNLOAD_FAILED,
    PATCHER_DOWNLOAD_FINISHED,
    PATCHER_UPDATE_FINISHED
  }

  @AutoValue
  public abstract static class PatcherEventUpdateStarted extends PatcherEvent
  {
    public static PatcherEventUpdateStarted create()
    {
      return new AutoValue_PatcherEvent_PatcherEventUpdateStarted();
    }

    public Kind kind()
    {
      return PATCHER_UPDATE_STARTED;
    }
  }

  @AutoValue
  public abstract static class PatcherEventUpdateFinished extends PatcherEvent
  {
    public static PatcherEventUpdateFinished create()
    {
      return new AutoValue_PatcherEvent_PatcherEventUpdateFinished();
    }

    public Kind kind()
    {
      return PATCHER_UPDATE_FINISHED;
    }
  }

  public abstract static class PatcherEventDownload extends PatcherEvent
  {
    /**
     * @return The URI of the remote file
     */

    public abstract URI uri();

    /**
     * @return The index of the current download
     */

    public abstract int index();

    /**
     * @return The total number of downloads that will be performed
     */

    public abstract int count();
  }

  @AutoValue
  public abstract static class PatcherEventDownloadStarted extends
    PatcherEventDownload
  {
    public static PatcherEventDownloadStarted create(
      final URI uri,
      final int index,
      final int count)
    {
      return new AutoValue_PatcherEvent_PatcherEventDownloadStarted(
        uri, index, count);
    }

    public final Kind kind()
    {
      return PATCHER_DOWNLOAD_STARTED;
    }
  }

  @AutoValue
  public abstract static class PatcherEventDownloadFailed extends
    PatcherEventDownload
  {
    public static PatcherEventDownloadFailed create(
      final URI uri,
      final int index,
      final int count,
      final Exception error)
    {
      return new AutoValue_PatcherEvent_PatcherEventDownloadFailed(
        uri, index, count, error);
    }

    public final Kind kind()
    {
      return PATCHER_DOWNLOAD_FAILED;
    }

    public abstract Exception error();
  }

  @AutoValue
  public abstract static class PatcherEventDownloadProgress extends
    PatcherEventDownload
  {
    public static PatcherEventDownloadProgress create(
      final URI uri,
      final int index,
      final int count,
      final long expected,
      final long received)
    {
      return new AutoValue_PatcherEvent_PatcherEventDownloadProgress(
        uri, index, count, expected, received);
    }

    public final Kind kind()
    {
      return PATCHER_DOWNLOAD_PROGRESS;
    }

    public abstract long expected();

    public abstract long received();

    public final double progress()
    {
      return (double) this.received() / (double) this.expected();
    }

    public final double progressPercent()
    {
      return this.progress() * 100.0;
    }
  }

  @AutoValue
  public abstract static class PatcherEventDownloadFinished extends
    PatcherEventDownload
  {
    public static PatcherEventDownloadFinished create(
      final URI uri,
      final int index,
      final int count)
    {
      return new AutoValue_PatcherEvent_PatcherEventDownloadFinished(
        uri, index, count);
    }

    public final Kind kind()
    {
      return PATCHER_DOWNLOAD_FINISHED;
    }
  }
}
