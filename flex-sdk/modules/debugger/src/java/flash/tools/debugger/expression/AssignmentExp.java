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
 * Provides the ability to do assignment of a rhs to a lhs
 * The expectation is that the rhs can resolve to a variable.
 */
public class AssignmentExp extends NonTerminalExp
{
	/**
	 * We need to evaluate both left and right children and then request an assignment
	 * @throws PlayerFaultException 
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		// should eval to a variable, but in order to create some that 
		// may not exist we enable dummies to be made during this process
		context.createPseudoVariables(true);

		Object l = m_left.evaluate(context);

		context.createPseudoVariables(false);

		// should eval to a scalar (string)
		Object r = m_right.evaluate(context);

		// now request a assignment on this variable 
		Object result = context.assign(l, r);

		return result;
	}
}
