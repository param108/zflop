////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.expression;

import flash.tools.debugger.events.FaultEvent;

/**
 * Thrown when the player generates a fault.  For example, if
 * an attempt to assign a value to a variable results in the player
 * generating a fault because that value has no setter, or because
 * the setter throws an exception for any other reason, then this
 * exception will be generated.
 */
public class PlayerFaultException extends Exception {
	private FaultEvent m_event;

	public PlayerFaultException(FaultEvent event)
	{
		m_event = event;
	}
	
	public FaultEvent getFaultEvent()
	{
		return m_event;
	}
	
	public String getMessage()
	{
		return m_event.information;
	}
}
