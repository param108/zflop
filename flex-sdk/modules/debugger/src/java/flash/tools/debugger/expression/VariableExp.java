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
 * A class which contains a variable that needs to be
 * resolved in an appropriate context in order for 
 * its value to be determined.
 */
public class VariableExp extends TerminalExp
{
	String m_name;

	VariableExp(String name) { m_name = name; } /* use static to create nodes */

	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		/* do a lookup in the current context for this variable */
		Object result =  context.lookup(m_name); 
		return result;
	}

	/* we put this here in case we later want to refine object construction */
	public static VariableExp create(String name)	{ return new VariableExp(name); }
}
