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

import java.util.HashMap;
import java.util.Map;

/**
 * Thrown when a variable name cannot be resolved in the current scope
 */
public class NoSuchVariableException extends Exception
{
	public NoSuchVariableException(String s)	{ super(s); }
	public NoSuchVariableException(Object o)	{ super(o.toString()); }

	public String getLocalizedMessage()
	{
		Map args = new HashMap();
		args.put("arg2", getMessage() ); //$NON-NLS-1$
		return ASTBuilder.getLocalizationManager().getLocalizedTextString("key4", args); //$NON-NLS-1$
	}
}
