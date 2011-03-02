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
 * Thrown when an attempt is made to set the child of a node 
 * that does not exist
 */
public class UnknownOperationException extends Exception
{
	public UnknownOperationException() { super(); }
	public UnknownOperationException(Operator op) { super(op.toString()); }

	public String getLocalizedMessage()
	{
		String err = ASTBuilder.getLocalizationManager().getLocalizedTextString("key5"); //$NON-NLS-1$
		String message = getMessage();
		if (message != null && message.length() > 0)
		{
			err = err + ": " + message; //$NON-NLS-1$
		}
		return err;
	}
}
