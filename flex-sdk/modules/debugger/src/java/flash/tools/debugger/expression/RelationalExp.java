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
 * Relational type of expression, e.g. Eq, GTEq, Neq, etc.  This class was 
 * created in order to categorize the types of non-terminals.
 * 
 * Additionally it understand how to convert the children
 * results into long and then perform operate() within
 * the subclass.
 */
public abstract class RelationalExp extends NonTerminalExp
{
	/* Sub-classes use these method to perform their specific operation */
	public abstract boolean operateOn(long a, long b);
	public abstract boolean operateOn(String a, String b);

	/**
	 * We override this in order to catch and convert the values returned 
	 * by our childrens evaluate method.
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		Object l = m_left.evaluate(context);
		Object r = m_right.evaluate(context);

		boolean result = false;

		/**
		 * Now if either are strings force them both to strigs and compute 
		 */
		if ( (l instanceof String) || (r instanceof String) )
		{
		    String lhs = l.toString();
			String rhs = r.toString();
			
			result = operateOn(lhs, rhs);
		}
		else
		{
			/* we are some form of number or boolean */
			long lVal = ArithmeticExp.toLong(l);
			long rVal = ArithmeticExp.toLong(r);

			result = operateOn(lVal, rVal);
		}
		return new Boolean(result);
	}
}
