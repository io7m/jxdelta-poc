package com.io7m.jxdeltapoc;

import com.google.common.collect.ImmutableList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public final class ManifestXML
{
  private ManifestXML()
  {

  }

  public static Manifest parse(
    final PatcherHardenedSAXParsers parsers,
    final InputStream stream)
    throws IOException
  {
    try {
      final XMLReader parser = parsers.createXMLReaderNonValidating(false);
      final ManifestContentHandler handler = new ManifestContentHandler();
      parser.setContentHandler(handler);
      parser.parse(new InputSource(stream));
      return handler.manifest();
    } catch (final ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }

  private static final class ManifestContentHandler extends DefaultHandler2
  {
    private Locator locator;
    private URI manifest_uri;
    private String manifest_hash;
    private ArrayList<Manifest.Delta> deltas;
    private boolean failed;

    ManifestContentHandler()
    {
      this.deltas = new ArrayList<>(32);
    }

    @Override
    public void setDocumentLocator(final Locator locator)
    {
      this.locator = locator;
    }

    @Override
    public void startDocument()
    {

    }

    @Override
    public void endDocument()
    {

    }

    @Override
    public void startPrefixMapping(
      final String prefix,
      final String uri)
    {

    }

    @Override
    public void endPrefixMapping(
      final String prefix)
    {

    }

    @Override
    public void startElement(
      final String uri,
      final String localName,
      final String qName,
      final Attributes atts)
      throws SAXParseException
    {
      try {
        switch (localName) {
          case "manifest": {
            this.parseManifest(localName, atts);
            break;
          }
          case "deltas": {
            break;
          }
          case "delta": {
            this.deltas.add(this.parseDelta(localName, atts));
            break;
          }
          default: {
            throw new SAXParseException(
              "Unrecognized element name: " + localName, this.locator);
          }
        }
      } catch (final URISyntaxException e) {
        throw new SAXParseException(
          "Unparseable URI: " + e.getMessage(), this.locator, e);
      }
    }

    private Manifest.Delta parseDelta(
      final String localName,
      final Attributes atts)
      throws URISyntaxException, SAXParseException
    {
      final Map<String, String> attrs =
        AttributeUtilities.attributeMap(atts);

      final URI uri;
      if (attrs.containsKey("file")) {
        uri = new URI(attrs.get("file"));
      } else {
        throw new SAXParseException(
          "Missing URI attribute: " + localName, this.locator);
      }
      final String delta_hash;
      if (attrs.containsKey("deltaHash")) {
        delta_hash = attrs.get("deltaHash");
      } else {
        throw new SAXParseException(
          "Missing deltaHash attribute: " + localName, this.locator);
      }
      final String result_hash;
      if (attrs.containsKey("resultHash")) {
        result_hash = attrs.get("resultHash");
      } else {
        throw new SAXParseException(
          "Missing resultHash attribute: " + localName, this.locator);
      }

      return Manifest.Delta.create(uri, delta_hash, result_hash);
    }

    private void parseManifest(
      final String localName,
      final Attributes atts)
      throws URISyntaxException, SAXParseException
    {
      final Map<String, String> attrs =
        AttributeUtilities.attributeMap(atts);

      if (attrs.containsKey("file")) {
        this.manifest_uri = new URI(attrs.get("file"));
      } else {
        throw new SAXParseException(
          "Missing URI attribute: " + localName, this.locator);
      }
      if (attrs.containsKey("hash")) {
        this.manifest_hash = attrs.get("hash");
      } else {
        throw new SAXParseException(
          "Missing hash attribute: " + localName, this.locator);
      }
    }

    @Override
    public void endElement(
      final String uri,
      final String localName,
      final String qName)
      throws SAXParseException
    {
      switch (localName) {
        case "manifest": {
          break;
        }
        case "deltas": {
          break;
        }
        case "delta": {
          break;
        }
        default: {
          throw new SAXParseException(
            "Unrecognized element name: " + localName, this.locator);
        }
      }
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
    {

    }

    @Override
    public void ignorableWhitespace(
      final char[] ch,
      final int start,
      final int length)
    {

    }

    @Override
    public void processingInstruction(
      final String target,
      final String data)
    {

    }

    @Override
    public void warning(final SAXParseException e)
      throws SAXException
    {
      super.warning(e);
      this.failed = true;
    }

    @Override
    public void error(final SAXParseException e)
      throws SAXException
    {
      super.error(e);
      this.failed = true;
    }

    @Override
    public void fatalError(final SAXParseException e)
      throws SAXException
    {
      this.failed = true;
      super.fatalError(e);
    }

    @Override
    public void skippedEntity(final String name)
    {

    }

    public Manifest manifest()
      throws IOException
    {
      if (this.failed) {
        throw new IOException("Parsing has failed");
      }

      return Manifest.create(
        this.manifest_uri, this.manifest_hash, ImmutableList.copyOf(this.deltas));
    }
  }
}
