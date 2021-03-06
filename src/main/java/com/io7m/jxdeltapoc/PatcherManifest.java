package com.io7m.jxdeltapoc;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.net.URI;

@AutoValue
public abstract class PatcherManifest
{
  PatcherManifest()
  {

  }

  public static PatcherManifest create(
    final URI file,
    final String hash,
    final ImmutableList<Delta> deltas)
  {
    return new AutoValue_PatcherManifest(file, hash, deltas);
  }

  /**
   * @return The initial file URI
   */

  public abstract URI initialFile();

  /**
   * @return The hash of the initial file
   */

  public abstract String initialHash();

  /**
   * @return The list of deltas in application order
   */

  public abstract ImmutableList<Delta> deltas();

  @AutoValue
  public static abstract class Delta
  {
    public static Delta create(
      final URI file,
      final String delta_hash,
      final String result_hash)
    {
      return new AutoValue_PatcherManifest_Delta(file, delta_hash, result_hash);
    }

    /**
     * @return The delta file
     */

    public abstract URI file();

    /**
     * @return The hash of the actual delta file
     */

    public abstract String deltaHash();

    /**
     * @return The final hash of the file if all the deltas up to and including this point are applied
     */

    public abstract String resultHash();
  }
}
