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
 * NotConnectedException is thrown when the Session
 * is no longer connnected to the Player
 */
public class NotConnectedException extends PlayerDebugException
{
	public String getMessage()
	{
		return Bootstrap.getLocalizationManager().getLocalizedTextString("key3"); //$NON-NLS-1$
	}
}
