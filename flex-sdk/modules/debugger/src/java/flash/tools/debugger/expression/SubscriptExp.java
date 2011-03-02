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

package flash.tools.debugger.expression;

/**
 * handles array indexing, e.g. a[k]
 * 
 * @author mmorearty
 */
public class SubscriptExp extends NonTerminalExp
{

	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		// eval the left side and right side
		Object l = m_left.evaluate(context);
		VariableExp r = VariableExp.create(m_right.evaluate(context).toString());

		// create a new context object using our left side, then ask 
		// the right to evaluate 
		Context current = context.createContext(l);
		
		Object result = r.evaluate(current);

		return result;
	}

}
