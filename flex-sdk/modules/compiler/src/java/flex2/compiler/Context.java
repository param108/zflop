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

package flex2.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Clement Wong
 * @author Cathy Murphy
 */
public final class Context
{
	public static final String BINDING_EXPRESSIONS = "BindingExpressions";
	public static final String RENAMED_VARIABLE_MAP = "RenamedVariableMap";
	public static final String CSS_ARCHIVE_FILES = "CSSArchiveFiles";
	public static final String L10N_ARCHIVE_FILES = "L10NArchiveFiles";

	// C: This class will eventually be phased out.
	public Context()
	{
		attributes = new HashMap();
	}

	private Map attributes; // Map<String, Object>

	// C: check to see if some of this usage can be replaced by removeAttribute.
	public Object getAttribute(String name)
	{
		return attributes.get(name);
	}

	public void setAttribute(String name, Object value)
	{
		attributes.put(name, value);
	}

	public Object removeAttribute(String name)
	{
		return attributes.remove(name);
	}

	public void clear()
	{
		attributes.clear();
	}

	public void setAttributes(Context context)
	{
		attributes.putAll(context.attributes);
	}
}
