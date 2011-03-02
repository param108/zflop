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
public class DynamicPropertyInitializer extends NamedInitializer
{
	protected final Type type;
	protected final String name;

	public DynamicPropertyInitializer(Type type, String name, Object value, int line)
	{
		super(value, line);
		this.type = type;
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Type getLValueType()
	{
		return type;
	}
}
