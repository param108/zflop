////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.swc.catalog;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler for reading in catalog.xml
 *
 * @author Brian Deitte
 */
public class CatalogHandler extends DefaultHandler
{
    private ReadContext readContext = new ReadContext();
    private CatalogReader reader;

    public CatalogHandler(CatalogReader reader)
    {
        this.reader = reader;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        readContext.setCurrent(qName, attributes);

        CatalogReadElement current = readContext.getCurrentParent();
        if (current == null)
        {
            current = reader.defaultReadElement;
        }
        current = current.readElement(readContext);
        if (current != null)
        {
            readContext.setCurrentParent(current, localName);
        }
    }

    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        readContext.setCurrent(qName, null);        
        CatalogReadElement current = readContext.getCurrentParent();
        if (current != null)
        {
            current.endElement(readContext);
        }
        readContext.clearCurrentParent(qName);
    }

    public void clear()
    {
        readContext.clear();
    }

    // TODO: use below?

    /*public void warning (SAXParseException e)
	throws SAXException
    {
        System.err.println("WARNING: " + e);
        e.printStackTrace();
    }

    public void deprecated(SAXParseException e)
	{
        System.err.println("DEPRECATED: " + e);
        e.printStackTrace();
	}

    public void error(SAXParseException e)
    {
        System.err.println("ERROR: " + e);
        e.printStackTrace();
    }

    public void fatalError(SAXParseException e)
            throws SAXParseException
    {
        System.err.println("FATAL ERROR: " + e);
        e.printStackTrace();
        throw e;
    }*/
}
