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

package flash.tools.debugger;

/**
 * SuspendedException is thrown when the Player 
 * is in a state for which the action cannot be performed.
 */
public class SuspendedException extends PlayerDebugException
{
	public String getMessage()
	{
		return Bootstrap.getLocalizationManager().getLocalizedTextString("key5"); //$NON-NLS-1$
	}
}
