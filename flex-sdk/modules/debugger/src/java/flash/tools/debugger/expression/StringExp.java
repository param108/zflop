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

package flash.tools.debugger.expression;

/**
 * A class which contains a fixed string.
 */
public class StringExp extends TerminalExp
{
	String m_text;

	StringExp(String text) { m_text = text; } /* use static to create nodes */

	public Object evaluate(Context context) throws NumberFormatException
	{
		return m_text;
	}

	/* we put this here in case we later want to refine object construction */
	public static final StringExp create(String text)	{ return new StringExp(text); }
}
