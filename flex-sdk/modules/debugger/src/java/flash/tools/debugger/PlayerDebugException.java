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
 * PlayerDebugException is the base class for all
 * exceptions thrown by the playerdebug API
 */
public class PlayerDebugException extends Exception
{
	public PlayerDebugException()				{ super(); }
	public PlayerDebugException(String s)		{ super(s); }
}
