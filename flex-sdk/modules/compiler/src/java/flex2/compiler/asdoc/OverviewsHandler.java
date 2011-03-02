////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.asdoc;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

import flex2.tools.ASDocConfiguration;

/**
 * SAX Handler for parsing Overviews_Base.xml and writing out overviews.xml
 *
 * @author Brian Deitte
 */
public class OverviewsHandler extends DefaultHandler
{
	private BufferedWriter writer;
	private List packages;

	public OverviewsHandler(BufferedWriter w, ASDocConfiguration config)
	{
		writer = w;
		packages = config.getPackagesConfiguration().getPackages();
	}

	public void startElement (String uri, String localName,
	                          String qName, Attributes attributes)
			throws SAXException
	{
		try
		{
			if (qName.equals("packages") && packages.size() > 0)
			{
				throw new RuntimeException("packages can not be specified in ASDoc_Config.xml and as a Flex parameter");
			}
			// if this element was added as a parameter, we use the parameter value
			writer.newLine();
			writer.write("<" + qName);
			for (int i = 0; i < attributes.getLength(); i++)
			{
				writer.write(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
			}
			writer.write(">");
		}
		catch(IOException ioe)
		{
			throw new SAXException(ioe);
		}
	}


	public void endElement (String uri, String localName, String qName)
			throws SAXException
	{
		try
		{
			if (qName.equals("overviews"))
			{
				if (packages.size() > 0)
				{
					writer.newLine();
					writer.write("<packages>");
					for (Iterator iterator = packages.iterator(); iterator.hasNext();)
					{
						PackageInfo info = (PackageInfo)iterator.next();
						writer.newLine();
						writer.write("<package name=\"" + info.name + "\">");
						writer.newLine();
						writer.write("<shortDescription>" + info.description + "</shortDescription>");
						writer.write("<longDescription>" + info.description + "</longDescription>");

						writer.newLine();
						writer.write("</package>");
					}
					writer.newLine();
					writer.write("</packages>");
				}
			}
			writer.write("</" + qName + ">");
		}
		catch(IOException ioe)
		{
			throw new SAXException(ioe);
		}
	}

	public void characters (char ch[], int start, int length)
			throws SAXException
	{
		try
		{
			writer.write(new String(ch, start, length));
		}
		catch(IOException ioe)
		{
			throw new SAXException(ioe);
		}
	}
}
