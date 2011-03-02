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

import flex2.compiler.mxml.Token;
import flex2.compiler.util.QName;

import java.util.*;

/**
 * @author Clement Wong
 */
public class XMLNode extends Node
{
	// static final Set<QName> attributes;
	public static final Set attributes;

	static
	{
		// attributes = new HashSet<QName>();
		attributes = new HashSet();
		attributes.add(new QName("", "id"));
		attributes.add(new QName("", "source"));
        attributes.add(new QName("", "format")); //for WebService Operation
	}

	XMLNode(String uri, String localName)
	{
		this(uri, localName, 0);
	}

	XMLNode(String uri, String localName, int size)
	{
		super(uri, localName, size);
	}

	private Node[] sourceFile;
	
	public void analyze(Analyzer analyzer)
	{
		analyzer.prepare(this);
		analyzer.analyze(this);
	}

	public void setSourceFile(Node[] nodes)
	{
		sourceFile = nodes;
	}

	public Node[] getSourceFile()
	{
		return sourceFile;
	}

	public int getChildCount()
	{
		return sourceFile != null ? sourceFile.length : super.getChildCount();
	}

	public Token getChildAt(int index)
	{
		return sourceFile != null ? sourceFile[index] : super.getChildAt(index);
	}

	public Collection getChildren()
	{
		return sourceFile != null ? Collections.unmodifiableCollection(Arrays.asList(sourceFile)) : super.getChildren();
	}

	/**
	 *
	 */
	public boolean isE4X()
	{
		String formatAttr = (String)getAttribute("format");
		return formatAttr == null || "E4X".equalsIgnoreCase(formatAttr);
	}
}
