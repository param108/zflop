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
 * Boolean type of expression, e.g. And, Not, Or.  This class was 
 * created in order to categorize the types of non-terminals.
 * 
 * Additionally it understand how to convert the children
 * results into booleans and then perform operate() within
 * the subclass.
 */
public abstract class BooleanExp extends NonTerminalExp
{
	/* Sub-classes use this method to perform their specific operation */
	public abstract boolean operateOn(boolean a, boolean b);

	/**
	 * We override this in order to catch and convert the values returned 
	 * by our childrens evaluate method.
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		Object l = (this instanceof SingleArgumentExp) ? null : m_left.evaluate(context);
		Object r = m_right.evaluate(context);

		/**
		 * Now convert each to a long and perform the operation 
		 */
		boolean lVal = (this instanceof SingleArgumentExp) ? false : toBoolean(l);
		boolean rVal = toBoolean(r);

		boolean result = operateOn(lVal, rVal);

		return new Boolean(result);
	}

	/**
	 * The magic conversion function which provides a mapping
	 * from any object into a boolean!
	 */
	public static boolean toBoolean(Object o) throws NumberFormatException
	{
		boolean value = false;

		try
		{
			if (o instanceof Boolean)
				value = ((Boolean)o).booleanValue();
			else if (o instanceof Number)
				value = ( ((Number)o).longValue() != 0 ) ? true : false;
			else
			{
				String v = o.toString().toLowerCase();
				value = "true".equals(v); //$NON-NLS-1$
			}
		}
		catch(NumberFormatException n) { throw n; }

		return value;
	}
}

