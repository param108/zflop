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
 * A class which contains an internal variable that needs 
 * to be resolved.
 */
public class InternalVariableExp extends VariableExp
{
	InternalVariableExp(String name) { super(name); } /* use static to create nodes */

	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		/* perform a lookup on this dude */
		return context.lookup(m_name);
	}

	/* we put this here in case we later want to refine object construction */
	public static VariableExp create(String name)	{ return new InternalVariableExp(name); }
}
