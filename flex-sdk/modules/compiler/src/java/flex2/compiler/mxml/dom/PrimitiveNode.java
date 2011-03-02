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

package flex2.compiler.mxml.dom;

import flex2.compiler.mxml.Token;
import flex2.compiler.util.QName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Clement Wong
 */
public abstract class PrimitiveNode extends Node
{
	// static final Set<QName> attributes;
	public static final Set attributes;

	static
	{
		// attributes = new HashSet<QName>();
		attributes = new HashSet();
		attributes.add(new QName("", "id"));
		attributes.add(new QName("", "source"));
	}

	PrimitiveNode(String uri, String localName)
	{
		this(uri, localName, 0);
	}

	PrimitiveNode(String uri, String localName, int size)
	{
		super(uri, localName, size);
	}

	private CDATANode sourceFile;

	public void setSourceFile(CDATANode cdata)
	{
		sourceFile = cdata;
	}

	public CDATANode getSourceFile()
	{
		return sourceFile;
	}

	public int getChildCount()
	{
		return sourceFile != null ? 1 : super.getChildCount();
	}

	public Token getChildAt(int index)
	{
		return sourceFile != null && index == 0 ? sourceFile : super.getChildAt(index);
	}

	public Collection getChildren()
	{
		return sourceFile != null ? Collections.singletonList(sourceFile) : super.getChildren();
	}
}
