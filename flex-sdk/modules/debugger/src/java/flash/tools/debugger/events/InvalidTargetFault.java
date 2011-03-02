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
 * Signals that a bad target name was provided while executing 
 * a ActionSetTarget instruction.
 */
public class InvalidTargetFault extends FaultEvent
{
	public final static String name = "invalid_target";  //$NON-NLS-1$

	public InvalidTargetFault(String target) { super(target); }

	public String name() { return name; }	
}
