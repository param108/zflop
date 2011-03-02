////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.dom;

import java.io.StringWriter;

/**
 * @author Clement Wong
 */
public class CDATANode extends Node
{
	public CDATANode()
	{
		super("", "", 0);
		inCDATA = false;
	}

	public boolean inCDATA;

	public void analyze(Analyzer analyzer)
	{
		analyzer.prepare(this);
		analyzer.analyze(this);
	}

	public void toStartElement(StringWriter w)
	{
		if (inCDATA)
		{
			w.write("<![CDATA[");
			w.write(image);
			w.write("]]>");
		}
		else
		{
			w.write(image);
		}
	}

	public void toEndElement(StringWriter w)
	{
	}

	public String toString()
	{
		String cdata = image.replace('\r', ' ').replace('\n', ' ').trim();
		cdata = (cdata.length() > 10) ? cdata.substring(0, 10) + "..." : cdata;
		return "<![[ " + cdata + " ]]>";
	}
}
