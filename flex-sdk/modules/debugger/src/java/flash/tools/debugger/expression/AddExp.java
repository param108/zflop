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
 * Arithmetic add
 */
public class AddExp extends ArithmeticExp
{
	public long operateOn(long a, long b)
	{
		return a + b;
	}

	/* String concatenation */
	public String operateOn(String a, String b) 
	{ 
		// we may be getting double values coming through with trailing ".0", so trim them 
		if (a.endsWith(".0")) //$NON-NLS-1$
			a = a.substring(0, a.length() - 2);
		if (b.endsWith(".0")) //$NON-NLS-1$
			b = b.substring(0, b.length() - 2);

		return a + b;
	}
}
