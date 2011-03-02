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
 * A class which contains a constant.  Currently only
 * integers are supported. 
 */
public class ConstantExp extends TerminalExp
{
    Object m_value;

	ConstantExp(long value) { m_value = new Long(value); } /* use static to create nodes */

	public Object evaluate(Context context) throws NumberFormatException
	{
		return m_value;
	}

	/* we put this here in case we later want to refine object construction */
	public static final ConstantExp create(long value)	{ return new ConstantExp(value); }
}
