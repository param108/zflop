////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.events;

/**
 * Signals that an ActionScript error has caused a fault
 */
public class ConsoleErrorFault extends FaultEvent
{
	public final static String name = "console_error";  //$NON-NLS-1$

	public ConsoleErrorFault(String s) { super(s); }

	public String name() { return name; }
}
