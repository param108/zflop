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

import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.gen.TextGen;

/**
 * initializer for a named lvalue - e.g. property or style, but not array element or visual child
 */
public abstract class NamedInitializer extends ValueInitializer
{
	NamedInitializer(Object value, int line)
	{
		super(value, line);
	}

	/**
	 *
	 */
	public abstract String getName();

	/**
	 *
	 */
	public String getAssignExpr(String lvalueBase)
	{
		String name = getName();
		
		String lvalue = TextParser.isValidIdentifier(name) ?
				lvalueBase + '.' + name :
				lvalueBase + "[" + TextGen.quoteWord(name) + "]";

		return lvalue + " = " + getValueExpr();
	}

}
