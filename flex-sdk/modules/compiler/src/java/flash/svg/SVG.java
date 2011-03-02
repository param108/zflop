////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.svg;

import java.io.StringWriter;
import java.io.Writer;

/**
 * @author Peter Farland
 */
public class SVG
{
	public SVG(StringWriter writer)
	{
		this.writer = writer;
	}

    public String getLiteralXML()
	{
        return writer.getBuffer().toString();
    }

	public Writer getWriter()
	{
		return writer;
	}

	private StringWriter writer;
}
