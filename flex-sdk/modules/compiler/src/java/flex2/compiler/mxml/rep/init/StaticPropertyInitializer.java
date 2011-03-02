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

import flex2.compiler.mxml.reflect.Property;
import flex2.compiler.mxml.reflect.Type;

/**
 *
 */
public class StaticPropertyInitializer extends NamedInitializer
{
	final Property property;

	public StaticPropertyInitializer(Property property, Object value, int line)
	{
		super(value, line);
		this.property = property;
	}

	public String getName()
	{
		return property.getName();
	}

	public Type getLValueType()
	{
		return property.getType();
	}
}
