////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
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
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import flex2.tools.ASDocConfiguration;

/**
 * SAX Handler for parsing ASDoc_Config_Base.xml and writing out ASDoc_Config.xml
 *
 * @author Brian Deitte
 */
public class ASDocConfigHandler extends DefaultHandler
{
	private BufferedWriter writer;
	private Map configMap = new HashMap();
	private boolean skipCharacters;

	public ASDocConfigHandler(BufferedWriter w, ASDocConfiguration config)
	{
		writer = w;

		// store all config values but packages in a map, with the key as the ASDoc_Config.xml element
		if (config.getMainTitle() != null) configMap.put("title", config.getMainTitle());
		if (config.getWindowTitle() != null) configMap.put("windowTitle", config.getWindowTitle());
		if (config.getFooter() != null) configMap.put("footer", config.getFooter());
		if (config.getExamplesPath() != null) configMap.put("includeExamplesDirectory", config.getExamplesPath());
	}

	public void startElement (String uri, String localName,
	                          String qName, Attributes attributes)
			throws SAXException
	{
		try
		{
			// if this element was added as a parameter, we use the parameter value
			if (! configMap.containsKey(qName))
			{
				skipCharacters = false;
				writer.newLine();
				writer.write("<" + qName);
				for (int i = 0; i < attributes.getLength(); i++)
				{
					writer.write(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
				}
				writer.write(">");
			}
			else
			{
				skipCharacters = true;
			}
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
			if (qName.equals("asDocConfig"))
			{
				// if we're at the end of the document, write out all the config values
				for (Iterator iterator = configMap.entrySet().iterator(); iterator.hasNext();)
				{
					Map.Entry entry = (Map.Entry)iterator.next();
					writer.newLine();
					writer.write("<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">");
				}
			}
			if (configMap.get(qName) == null)
			{
				writer.write("</" + qName + ">");
			}
		}
		catch(IOException ioe)
		{
			throw new SAXException(ioe);
		}
	}

	public void characters (char ch[], int start, int length)
			throws SAXException
	{
		if (! skipCharacters)
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
}
