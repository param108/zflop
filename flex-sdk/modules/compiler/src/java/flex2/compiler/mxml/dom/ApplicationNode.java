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

/**
 * @author Clement Wong
 */
public class ApplicationNode extends Node
{
	private String outerDocumentClassName;

	ApplicationNode(String uri, String localName)
	{
		this(uri, localName, 0);
	}

	ApplicationNode(String uri, String localName, int size)
	{
		super(uri, localName, size);
	}

	public String getOuterDocumentClassName()
	{
		return outerDocumentClassName;
	}

	public boolean isInlineComponent()
	{
		return outerDocumentClassName != null;
	}

	public static ApplicationNode inlineApplicationNode(String uri, String localName, String outerDocumentClassName)
	{
		ApplicationNode node = new ApplicationNode(uri, localName);
		node.setOuterDocumentClassName(outerDocumentClassName);
		return node;
	}

	private void setOuterDocumentClassName(String outerDocumentClassName)
	{
		this.outerDocumentClassName = outerDocumentClassName;
	}
}
