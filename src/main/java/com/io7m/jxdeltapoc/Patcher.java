package com.io7m.jxdeltapoc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDeltaFailed;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDeltaProgress;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDeltaSucceeded;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDownloadFailed;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDownloadProgress;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventDownloadStarted;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventUpdateFailed;
import com.io7m.jxdeltapoc.PatcherEvent.PatcherEventUpdateStarted;
import net.dongliu.vcdiff.VcdiffDecoder;
import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import net.dongliu.vcdiff.io.FileStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class Patcher
{
  private static final Logger LOG = LoggerFactory.getLogger(Patcher.class);

  private Patcher()
  {

  }

  public static PatcherRemoteContent remoteContentForURI(
    final URI url)
    throws IOException
  {
    LOG.debug("open: {}", url);

    final HttpURLConnection connection =
      (HttpURLConnection) url.toURL().openConnection();

    final int code = connection.getResponseCode();
    if (code >= 200 && code < 400) {
      return PatcherRemoteContent.create(
        url, connection.getContentLengthLong(), connection.getInputStream());
    }
    throw new IOException("HTTP error: " + code);
  }

  public static File updateFromRemote(
    final URI manifest_uri,
    final File input,
    final File directory,
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events)
    throws PatcherException
  {
    events.onEvent(PatcherEventUpdateStarted.create());

    LOG.debug("fetching manifest: {}", manifest_uri);

    final PatcherHardenedSAXParsers parsers = PatcherHardenedSAXParsers.create();
    try (InputStream stream = manifest_uri.toURL().openStream()) {
      final PatcherManifest manifest = PatcherManifestXML.parse(parsers, stream);
      final File file = update(manifest, input, directory, streams, events);
      events.onEvent(PatcherEvent.PatcherEventUpdateSucceeded.create());
      return file;
    } catch (final IOException e) {
      events.onEvent(PatcherEventUpdateFailed.create(e));
      throw new PatcherIOException(e);
    }
  }

  public static File updateFromManifest(
    final PatcherManifest manifest,
    final File input,
    final File directory,
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events)
    throws PatcherException
  {
    events.onEvent(PatcherEventUpdateStarted.create());
    try {
      final File file = update(manifest, input, directory, streams, events);
      events.onEvent(PatcherEvent.PatcherEventUpdateSucceeded.create());
      return file;
    } catch (final Exception e) {
      events.onEvent(PatcherEventUpdateFailed.create(e));
      throw e;
    }
  }

  private static File update(
    final PatcherManifest manifest,
    final File input,
    final File directory,
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events)
    throws PatcherException
  {
    final RequiredOperations required;
    if (input.isFile()) {
      final String hash = sha256Of(input);
      required = calculateRequirements(Optional.of(hash), manifest);
    } else {
      required = calculateRequirements(Optional.absent(), manifest);
    }

    LOG.debug(
      "initial file download required: {}",
      Boolean.valueOf(required.require_initial));
    LOG.debug(
      "require {} deltas, starting at {}",
      Integer.valueOf(required.required_deltas.size()),
      !required.required_deltas.isEmpty()
        ? required.required_deltas.get(0).resultHash()
        : "(none)");

    return runDownloadsAndPatches(
      manifest, input, directory, streams, events, required);
  }

  private static File runDownloadsAndPatches(
    final PatcherManifest manifest,
    final File input,
    final File directory,
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events,
    final RequiredOperations required)
    throws PatcherException
  {
    int downloads = 0;
    if (required.require_initial) {
      downloadPublishingEvents(
        input,
        streams,
        events,
        manifest.initialFile(),
        downloads,
        required.requiredDownloads(),
        manifest.initialHash());
      ++downloads;
    }

    final ArrayList<File> patches =
      new ArrayList<>(required.required_deltas.size());

    for (final PatcherManifest.Delta delta : required.required_deltas) {
      patches.add(downloadPublishingEvents(
        new File(directory, downloads + ".patch"),
        streams,
        events,
        delta.file(),
        downloads,
        required.requiredDownloads(),
        delta.deltaHash()));
      ++downloads;
    }

    return applyPatches(input, directory, events, required, patches);
  }

  private static File applyPatches(
    final File input,
    final File directory,
    final PatcherEventConsumerType events,
    final RequiredOperations required,
    final List<File> patches)
    throws PatcherIOException
  {
    final int count = patches.size();
    assert count == required.required_deltas.size();

    File source = input;
    for (int index = 0; index < count; ++index) {
      events.onEvent(PatcherEventDeltaProgress.create(index, count));

      final File patch = patches.get(index);
      final PatcherManifest.Delta delta = required.required_deltas.get(index);
      final File out = new File(directory, index + ".data");

      try {
        applyPatch(delta, source, patch, out);
        events.onEvent(PatcherEventDeltaSucceeded.create(index, count));
        source = out;
      } catch (final Exception e) {
        events.onEvent(PatcherEventDeltaFailed.create(index, count, e));
        throw new PatcherIOException(e);
      }

      if (index + 1 == count) {
        out.renameTo(input);
      }
    }

    return source;
  }

  private static void applyPatch(
    final PatcherManifest.Delta delta,
    final File source,
    final File patch,
    final File output)
    throws PatcherException
  {
    try {
      final VcdiffDecoder decoder = new VcdiffDecoder(
        new FileStream(new RandomAccessFile(source, "r")),
        new FileInputStream(patch),
        new FileStream(new RandomAccessFile(output, "rw")));
      decoder.decode();
      checkHash(output, delta.resultHash());
    } catch (final VcdiffDecodeException e) {
      throw new PatcherDecodeException(e);
    } catch (final IOException e) {
      throw new PatcherIOException(e);
    }
  }

  private static File downloadPublishingEvents(
    final File output,
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events,
    final URI uri,
    final int index,
    final int count,
    final String hash)
    throws PatcherException
  {
    events.onEvent(PatcherEventDownloadStarted.create(uri, index, count));
    try {
      final File file = download(streams, events, uri, index, count, output);
      checkHash(file, hash);
      events.onEvent(PatcherEvent.PatcherEventDownloadSucceeded.create(
        uri,
        index,
        count));
      return file;
    } catch (final PatcherException e) {
      events.onEvent(PatcherEventDownloadFailed.create(uri, index, count, e));
      throw e;
    }
  }

  private static void checkHash(
    final File file,
    final String expected_hash)
    throws PatcherException
  {
    final String received_hash = sha256Of(file);
    if (!received_hash.equals(expected_hash)) {
      throw new PatcherBadHashException(
        new StringBuilder(128)
          .append("Downloaded file hash is incorrect\n")
          .append("  Expected: ")
          .append(expected_hash)
          .append("\n")
          .append("  Received: ")
          .append(received_hash)
          .append("\n")
          .toString());
    }
  }

  private static File download(
    final PatcherURIStreamProviderType streams,
    final PatcherEventConsumerType events,
    final URI uri,
    final int index,
    final int count,
    final File file)
    throws PatcherIOException
  {
    try (OutputStream output = new FileOutputStream(file)) {
      try (PatcherRemoteContent content = streams.openURI(uri)) {
        final InputStream stream = content.stream();
        final byte[] buffer = new byte[8192];
        long received = 0L;
        while (true) {
          final int r = stream.read(buffer);
          if (r < 0) {
            break;
          }
          output.write(buffer, 0, r);
          received += (long) r;
          events.onEvent(PatcherEventDownloadProgress.create(
            uri, index, count, content.size(), received));
        }
      }
    } catch (final IOException e) {
      throw new PatcherIOException(e);
    }
    return file;
  }

  private static RequiredOperations calculateRequirements(
    final Optional<String> hash_opt,
    final PatcherManifest manifest)
  {
    LOG.debug("calculating requirements");

    /*
     * The initial file does not exist. The initial file must be fetched along
     * with all deltas.
     */

    if (!hash_opt.isPresent()) {
      return new RequiredOperations(true, manifest.deltas());
    }

    /*
     * If the file matches the initial manifest hash, then all deltas need
     * to be applied.
     */

    final String hash = hash_opt.get();
    final ImmutableList<PatcherManifest.Delta> deltas = manifest.deltas();
    if (hash.equals(manifest.initialHash())) {
      return new RequiredOperations(false, manifest.deltas());
    }

    /*
     * Otherwise, find the first delta that has a result hash matching
     * the current hash. The list of required deltas is the list of deltas
     * following the matching delta.
     */

    for (int index = 0; index < deltas.size(); ++index) {
      final PatcherManifest.Delta delta = deltas.get(index);
      if (hash.equals(delta.resultHash())) {
        final ImmutableList<PatcherManifest.Delta> subset =
          deltas.subList(index + 1, deltas.size());
        return new RequiredOperations(false, subset);
      }
    }

    /*
     * No delta matched. The initial file must be fetched along with all
     * deltas.
     */

    return new RequiredOperations(true, manifest.deltas());
  }

  private static String sha256Of(final File file)
    throws PatcherIOException
  {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (FileInputStream stream = new FileInputStream(file)) {
        final byte[] buffer = new byte[1024];
        while (true) {
          final int r = stream.read(buffer);
          if (r < 0) {
            break;
          }
          digest.update(buffer, 0, r);
        }
        final byte[] result = digest.digest();
        final String text = BaseEncoding.base16()
          .lowerCase()
          .encode(result);

        LOG.debug("hash: {} -> {}", file, text);
        return text;
      } catch (final IOException e) {
        throw new PatcherIOException(e);
      }
    } catch (final NoSuchAlgorithmException e) {
      throw new PatcherIOException(new IOException(e));
    }
  }

  private static final class RequiredOperations
  {
    private final boolean require_initial;
    private final ImmutableList<PatcherManifest.Delta> required_deltas;

    RequiredOperations(
      final boolean require_initial,
      final ImmutableList<PatcherManifest.Delta> required_deltas)
    {
      this.require_initial = require_initial;
      this.required_deltas = required_deltas;
    }

    int requiredDownloads()
    {
      return this.required_deltas.size() + (this.require_initial ? 1 : 0);
    }
  }
}
