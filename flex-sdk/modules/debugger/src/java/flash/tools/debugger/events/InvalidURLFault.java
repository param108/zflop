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
 * Signals that a request to open a URL failed. 
 */
public class InvalidURLFault extends FaultEvent
{
	public final static String name = "invalid_url";  //$NON-NLS-1$

	public InvalidURLFault(String url) { super(url); }

	public String name() { return name; }	
}
