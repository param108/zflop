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

package flash.css;

import org.w3c.css.sac.LexicalUnit;
import org.apache.commons.collections.SequencedHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class StyleDeclaration
{
	private int lineNumber;
	private String path;

	public StyleDeclaration(Rule parentRule, String path, int lineNumber)
	{
		this.parentRule = parentRule;
		this.path = path;
		this.lineNumber = lineNumber;
	}

	public String getCssText()
	{
		return cssText;
	}

	public void setCssText(String cssText)
	{
		this.cssText = cssText;
	}

	public String getPath()
	{
		return path;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public Descriptor getPropertyValue(String propertyName)
	{
		return (Descriptor)properties.get(propertyName);
	}

	public Descriptor removeProperty(String propertyName)
	{
		priorities.remove(propertyName);
		return (Descriptor)properties.remove(propertyName);
	}

	public String getPropertyPriority(String propertyName)
	{
		return (String)priorities.get(propertyName);
	}

	public void setProperty(String propertyName, LexicalUnit value, String priority, int lineNumberOffset)
	{
		priorities.put(propertyName, priority);
		properties.put(propertyName, new Descriptor(propertyName, value, path, lineNumberOffset));
	}

	public int getLength()
	{
		return priorities.size();
	}

	public Descriptor item(int i)
	{
		return (Descriptor)properties.get(properties.get(i));
	}

	public Rule getParentRule()
	{
		return parentRule;
	}

	public Iterator iterator()
	{
		return properties.iterator();
	}

	public int size()
	{
		return properties.size();
	}

	private SequencedHashMap properties = new SequencedHashMap();
	private Map priorities = new HashMap();
	private String cssText;
	private Rule parentRule;
}
