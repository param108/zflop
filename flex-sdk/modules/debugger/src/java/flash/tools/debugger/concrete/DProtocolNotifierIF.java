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

package flash.tools.debugger.concrete;

/**
 * Interface for receiving DMessages from the DProtocol object 
 */
public interface DProtocolNotifierIF
{
	/**
	 * Issused when a message has been received from the socket
	 */
	public void messageArrived(DMessage message, DProtocol which);

	/**
	 * Issued when the socket connection to the player is cut 
	 */
	public void disconnected();
}
