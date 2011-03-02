////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

/**
 * @author Edwin Smith
 */
public class Script implements LineNumberMapped
{
	private int xmlLineNumber, endXmlLineNumber;
	protected String text;
	private boolean isEmbedded;

	public Script(String text, int lineNumber)
	{
		this(text, lineNumber, lineNumber);
	}

	public Script(String text, int beginLine, int endLine)
	{
		this(text);
		setXmlLineNumber(beginLine);
		setEndXmlLineNumber(endLine);
	}

	public Script(String text)
	{
		this.text = text;
	}

	public int getXmlLineNumber()
	{
		return xmlLineNumber;
	}

	public int getEndXmlLineNumber()
	{
		return endXmlLineNumber;
	}

	public String getText()
	{
		return text;
	}

	public void setXmlLineNumber(int xmlLineNumber)
	{
		this.xmlLineNumber = xmlLineNumber;
	}

	public void setEndXmlLineNumber(int xmlLineNumber)
	{
		this.endXmlLineNumber = xmlLineNumber;
	}

	public void setXmlLineNumber(int xmlLineNumber, boolean allowXmlLineOffset)
	{
		setXmlLineNumber(xmlLineNumber);
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public void setEmbeddedScript(boolean b)
	{
		isEmbedded = b;
	}

	public boolean isEmbedded()
	{
		return isEmbedded;
	}
}
