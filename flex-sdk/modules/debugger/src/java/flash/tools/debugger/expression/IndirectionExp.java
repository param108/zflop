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
 * De-reference a variable producing a string which contains a list of memebers
 * and their values.
 */
public class IndirectionExp extends NonTerminalExp implements SingleArgumentExp
{
	/**
	 * We need to evaluate our left child then request a list of 
	 * members from it
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		// should eval to a variable id
		Object l = m_right.evaluate(context);

		// now request a lookup on this variable
		Object result = context.lookupMembers(l);

		return result;
	}
}
