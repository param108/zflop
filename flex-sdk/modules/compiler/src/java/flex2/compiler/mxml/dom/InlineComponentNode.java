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

import flex2.compiler.util.QName;

import java.util.Set;
import java.util.HashSet;

/**
 * @author Paul Reilly
 */
public class InlineComponentNode extends Node
{
	public static final String CLASS_NAME_ATTR = "className";

	public static final Set attributes;
	static
	{
		attributes = new HashSet();
		attributes.add(new QName("", "id"));
		attributes.add(new QName("", CLASS_NAME_ATTR));
	}

	private QName classQName;

	InlineComponentNode(String uri, String localName)
	{
		this(uri, localName, 0);
	}

	InlineComponentNode(String uri, String localName, int size)
	{
		super(uri, localName, size);
	}

	public void analyze(Analyzer analyzer)
	{
		analyzer.prepare(this);
		analyzer.analyze(this);
	}

	public void setClassQName(QName classQName)
	{
		assert this.classQName == null;
		this.classQName = classQName;
	}

	public QName getClassQName()
	{
		assert this.classQName != null;
		return classQName;
	}
}
