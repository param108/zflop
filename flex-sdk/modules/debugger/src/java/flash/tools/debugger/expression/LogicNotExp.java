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
 *    Logical NOT of right child only.
 */
public class LogicNotExp extends BooleanExp implements SingleArgumentExp
{
	public boolean operateOn(boolean a, boolean b)
	{
		return !b;
	}
}
