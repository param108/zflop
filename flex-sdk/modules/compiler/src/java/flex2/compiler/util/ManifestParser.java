////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import flex2.compiler.io.VirtualFile;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parses a manifest into a NameMappings
 *
 * @author Brian Deitte
 * @author Clement Wong
 */
public class ManifestParser
{
    public synchronized static void parse(String namespaceURI, VirtualFile file, NameMappings mappings)
    {
        if (file == null)
        {
            return;
        }

        InputStream in = null;

        try
        {
            in = new BufferedInputStream(file.getInputStream());
        }
        catch (FileNotFoundException ex)
        {
            // manifest is not found.
            return;
        }
        catch (IOException ex)
        {
            ThreadLocalToolkit.logError(file.getNameForReporting(), ex.getMessage());
            return;
        }

        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, new Parser(file.getName(), mappings, namespaceURI));
        }
        catch (Exception ex) // ParserConfigurationException, SAXException, IOException
        {
            ThreadLocalToolkit.logError(file.getNameForReporting(), ex.getMessage());
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }

    private static class Parser extends DefaultHandler
    {
        Parser(String fileName, NameMappings mappings, String namespaceURI)
        {
            this.fileName = fileName;
            this.mappings = mappings;
            this.namespaceURI = namespaceURI;
        }

        private String fileName;
        private NameMappings mappings;
        private String namespaceURI;
        private Locator locator;

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException
        {
            if (localName.equals("component"))
            {
                String id = attributes.getValue("id");
                String className = attributes.getValue("class");
                if (className == null)
                {
	                ThreadLocalToolkit.log(new UndefinedClass(fileName, locator.getLineNumber(), id));
                    return;
                }
                else if ("*".equals(className))
                {
	                ThreadLocalToolkit.log(new InvalidClassName(fileName, locator.getLineNumber(), id));
                }
	            else
                {
	                assert className.indexOf(':') == -1 && className.indexOf('/') == -1 : fileName + ": " + className;
	                className = NameFormatter.toColon(className);
                }

                if (id == null)
                {
                    id = NameFormatter.retrieveClassName(className);
                }

                String lookupOnlyStr = attributes.getValue("lookupOnly");
                boolean lookupOnly = lookupOnlyStr == null ? false : Boolean.valueOf(lookupOnlyStr).booleanValue();

                boolean added = mappings.addClass(namespaceURI, id, className);
                if (! added)
                {
	                ThreadLocalToolkit.log(new DuplicateComponentDefinition(fileName, locator.getLineNumber(), id));
                    return;
                }

                if (lookupOnly)
                {
                    mappings.addLookupOnly(className);
                }
            }
        }

        public void warning(SAXParseException e)
        {
	        ThreadLocalToolkit.log(new ManifestError(fileName, e.getLineNumber(), e.getMessage()));
        }

        public void error(SAXParseException e)
        {
	        ThreadLocalToolkit.log(new ManifestError(fileName, e.getLineNumber(), e.getMessage()));
        }

        public void fatalError(SAXParseException e)
                throws SAXParseException
        {
	        ThreadLocalToolkit.log(new ManifestError(fileName, e.getLineNumber(), e.getMessage()));
            throw e;
        }

        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }
    }

	// error messages

	public static class UndefinedClass extends CompilerMessage.CompilerError
	{
		public UndefinedClass(String fileName, int line, String tag)
		{
			super();
			this.fileName = fileName;
			this.line = line;
			this.tag = tag;
		}

		public final String fileName;
		public final int line;
		public final String tag;
	}

	public static class InvalidClassName extends CompilerMessage.CompilerError
	{
		public InvalidClassName(String fileName, int line, String tag)
		{
			super();
			this.fileName = fileName;
			this.line = line;
			this.tag = tag;
		}

		public final String fileName;
		public final int line;
		public final String tag;
	}

	public static class DuplicateComponentDefinition extends CompilerMessage.CompilerError
	{
		public DuplicateComponentDefinition(String fileName, int line, String tag)
		{
			super();
			this.fileName = fileName;
			this.line = line;
			this.tag = tag;
		}

		public final String fileName;
		public final int line;
		public final String tag;
	}

	public static class ManifestError extends CompilerMessage.CompilerError
	{
		public ManifestError(String fileName, int line, String message)
		{
			super();
			this.fileName = fileName;
			this.line = line;
			this.message = message;
		}

		public final String fileName;
		public final int line;
		public final String message;
	}
}
