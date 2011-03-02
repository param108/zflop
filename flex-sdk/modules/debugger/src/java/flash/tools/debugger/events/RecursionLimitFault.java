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

package flash.tools.debugger.events;

/**
 * Signals that a recursion limit has been reached 
 */
public class RecursionLimitFault extends FaultEvent
{
	public final static String name = "recursion_limit";  //$NON-NLS-1$

	public String name() { return name; }	
}
