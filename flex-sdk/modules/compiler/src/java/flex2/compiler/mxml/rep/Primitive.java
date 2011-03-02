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

import flex2.compiler.mxml.reflect.Type;

public class Primitive extends Model
{
	private Object value;

	public Primitive(MxmlDocument document, Type type, int line)
	{
		super(document, type, line);
	}

	public Primitive(MxmlDocument document, Type type, Object value, int line)
	{
		super(document, type, line);
		setValue(value.toString());
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	/**
	 * override hasBindings to check value
	 */
	public boolean hasBindings()
	{
		return value instanceof BindingExpression;
	}

}
