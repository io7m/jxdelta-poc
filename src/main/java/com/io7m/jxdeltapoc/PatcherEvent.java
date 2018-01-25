package com.io7m.jxdeltapoc;

import com.google.auto.value.AutoValue;

import java.net.URI;

import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DELTA_FAILED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DELTA_PROGRESS;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DELTA_SUCCEEDED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_FAILED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_PROGRESS;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_STARTED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_DOWNLOAD_SUCCEEDED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_UPDATE_FAILED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_UPDATE_STARTED;
import static com.io7m.jxdeltapoc.PatcherEvent.Kind.PATCHER_UPDATE_SUCCEEDED;

public abstract class PatcherEvent
{
  public abstract Kind kind();

  public enum Kind
  {
    PATCHER_UPDATE_STARTED,
    PATCHER_DOWNLOAD_STARTED,
    PATCHER_DOWNLOAD_PROGRESS,
    PATCHER_DOWNLOAD_FAILED,
    PATCHER_DOWNLOAD_SUCCEEDED,
    PATCHER_DELTA_PROGRESS,
    PATCHER_DELTA_FAILED,
    PATCHER_DELTA_SUCCEEDED,
    PATCHER_UPDATE_FAILED,
    PATCHER_UPDATE_SUCCEEDED
  }

  @AutoValue
  public abstract static class PatcherEventUpdateStarted extends PatcherEvent
  {
    public static PatcherEventUpdateStarted create()
    {
      return new AutoValue_PatcherEvent_PatcherEventUpdateStarted();
    }

    public final Kind kind()
    {
      return PATCHER_UPDATE_STARTED;
    }
  }

  @AutoValue
  public abstract static class PatcherEventUpdateSucceeded extends PatcherEvent
  {
    public static PatcherEventUpdateSucceeded create()
    {
      return new AutoValue_PatcherEvent_PatcherEventUpdateSucceeded();
    }

    public final Kind kind()
    {
      return PATCHER_UPDATE_SUCCEEDED;
    }
  }

  @AutoValue
  public abstract static class PatcherEventUpdateFailed extends PatcherEvent
  {
    public static PatcherEventUpdateFailed create(final Exception error)
    {
      return new AutoValue_PatcherEvent_PatcherEventUpdateFailed(error);
    }

    public final Kind kind()
    {
      return PATCHER_UPDATE_FAILED;
    }

    public abstract Exception error();
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
  public abstract static class PatcherEventDownloadSucceeded extends
    PatcherEventDownload
  {
    public static PatcherEventDownloadSucceeded create(
      final URI uri,
      final int index,
      final int count)
    {
      return new AutoValue_PatcherEvent_PatcherEventDownloadSucceeded(
        uri, index, count);
    }

    public final Kind kind()
    {
      return PATCHER_DOWNLOAD_SUCCEEDED;
    }
  }

  public abstract static class PatcherEventDelta extends PatcherEvent
  {
    /**
     * @return The index of the current patch
     */

    public abstract int index();

    /**
     * @return The total number of patches that will be applied
     */

    public abstract int count();
  }


  @AutoValue
  public abstract static class PatcherEventDeltaFailed extends PatcherEventDelta
  {
    public static PatcherEventDeltaFailed create(
      final int index,
      final int count,
      final Exception error)
    {
      return new AutoValue_PatcherEvent_PatcherEventDeltaFailed(
        index, count, error);
    }

    public final Kind kind()
    {
      return PATCHER_DELTA_FAILED;
    }

    public abstract Exception error();
  }

  @AutoValue
  public abstract static class PatcherEventDeltaProgress extends PatcherEventDelta
  {
    public static PatcherEventDeltaProgress create(
      final int index,
      final int count)
    {
      return new AutoValue_PatcherEvent_PatcherEventDeltaProgress(
        index, count);
    }

    public final Kind kind()
    {
      return PATCHER_DELTA_PROGRESS;
    }
  }

  @AutoValue
  public abstract static class PatcherEventDeltaSucceeded extends PatcherEventDelta
  {
    public static PatcherEventDeltaSucceeded create(
      final int index,
      final int count)
    {
      return new AutoValue_PatcherEvent_PatcherEventDeltaSucceeded(
        index, count);
    }

    public final Kind kind()
    {
      return PATCHER_DELTA_SUCCEEDED;
    }
  }
}
