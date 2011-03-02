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
 * Arithmetic type of expression, e.g. Add, Div, etc.  This class was 
 * created in order to categorize the types of non-terminals.
 * 
 * Additionally it understand how to convert the children
 * results into long and then perform operate() within
 * the subclass.
 */
public abstract class ArithmeticExp extends NonTerminalExp
{
	/* Sub-classes use this method to perform their specific operation */
	public abstract long operateOn(long a, long b);

	/* Sub-classes use this method to perform their specific operation (String manipulation is not supported by default)  */
	public String operateOn(String a, String b) throws UnknownOperationException { throw new UnknownOperationException(); }

	/**
	 * We override this in order to catch and convert the values returned 
	 * by our childrens evaluate method.
	 * 
	 * If we only take a single argument then don't play with the m_left.
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException
	{
		Object l = (this instanceof SingleArgumentExp) ? null : m_left.evaluate(context);
		Object r = m_right.evaluate(context);
		Object result = null;

		/**
		 * Now convert each to a long and perform the operation 
		 */
		try
		{
			long lVal = (this instanceof SingleArgumentExp) ? 0 : toLong(l);
			long rVal = toLong(r);

			result = new Long( operateOn(lVal, rVal) );
		}
		catch(NumberFormatException nfe)
		{
			// we could not perform arithmetic operation on these guys, 
			// so convert to a string and request operateOn to do its stuff.
			try
			{
				result = operateOn( (this instanceof SingleArgumentExp) ? null : l.toString(), r.toString() );
			} 
			catch(UnknownOperationException uoe)
			{
				// this form of operateOn is not supported, so throw the original exception
				throw nfe;
			}
		}

		return result;
	}

	/**
	 * The magic conversion function which provides a mapping
	 * from any object into a long!
	 */
	public static final long toLong(Object o) throws NumberFormatException
	{
		long value = 0;

		try
		{
			if (o instanceof Number)
				value = ((Number)o).longValue();
			else if (o instanceof Boolean)
				value = ( ((Boolean)o).booleanValue() ) ? 1 : 0;
			else
				value = (long)Double.parseDouble(o.toString());
		}
		catch(NumberFormatException n) { throw n; }

		return value;
	}
}
