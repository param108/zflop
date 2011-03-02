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
 * Implements the dot operator.
 */
public class DotExp extends NonTerminalExp
{
	/* perform your evaluation */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		// eval the left side 

		Object l = m_left.evaluate(context);

		// create a new context object using our left side then ask 
		// the right to evaluate 
		Context current = context.createContext(l);

		Object result = m_right.evaluate(current);

		return result;
	}
}
