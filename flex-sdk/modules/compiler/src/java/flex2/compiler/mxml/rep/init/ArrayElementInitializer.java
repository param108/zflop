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

package flex2.compiler.mxml.rep.init;

import flex2.compiler.mxml.reflect.Type;

/**
 *
 */
public class ArrayElementInitializer extends ValueInitializer
{
	final Type type;
	final int index;

	public ArrayElementInitializer(Type type, int index, Object value, int line)
	{
		super(value, line);
		this.type = type;
		this.index = index;
	}

	public Type getLValueType()
	{
		return type;
	}

	public String getAssignExpr(String lvalueBase)
	{
		return lvalueBase + "[" + index + "] = " + getValueExpr();
	}
}
