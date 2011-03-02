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
 * Arithmetic subtraction of longs 
 */
public class SubExp extends ArithmeticExp
{
	public long operateOn(long a, long b)
	{
		return a-b;
	}
}
