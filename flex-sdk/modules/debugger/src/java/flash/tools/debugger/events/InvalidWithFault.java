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
 * Signals that a ActionWith instruction could not be executed becuase
 * the target of the operation is not an object. 
 */
public class InvalidWithFault extends FaultEvent
{
	public final static String name = "invalid_with";  //$NON-NLS-1$

	public String name() { return name; }	
}
