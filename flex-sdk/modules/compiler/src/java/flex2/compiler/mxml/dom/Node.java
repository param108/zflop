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

package flex2.compiler.mxml.dom;

import flex2.compiler.mxml.Element;

/**
 * @author Clement Wong
 */
public class Node extends Element
{
	Node(String uri, String localName)
	{
		this(uri, localName, 0);
	}

	Node(String uri, String localName, int size)
	{
		super(uri, localName, size);
		index = 0;
	}

	private int index;

	public void setIndex(int index)
	{
		this.index = index;
	}

	public int getIndex()
	{
		return index;
	}

	public void analyze(Analyzer analyzer)
	{
		analyzer.prepare(this);
		analyzer.analyze(this);
	}

	public String toString()
	{
		return image + " " + beginLine;
	}
}
