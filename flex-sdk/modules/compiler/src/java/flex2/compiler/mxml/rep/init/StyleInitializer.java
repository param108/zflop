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

import flex2.compiler.mxml.reflect.Style;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.gen.TextGen;

public class StyleInitializer extends NamedInitializer
{
	protected final Style style;

	public StyleInitializer(Style style, Object value, int line)
	{
		super(value, line);
		this.style = style;
	}

	public Style getStyle()
	{
		return style;
	}

	public Type getLValueType()
	{
		return style.getType();
	}

	public String getName()
	{
		return style.getName();
	}

	public String getAssignExpr(String lvalueBase)
	{
		return lvalueBase + ".setStyle(" + TextGen.quoteWord(getName()) + ", " + getValueExpr() + ")";
	}
}
