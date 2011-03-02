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
 * Break event is received when the player halts execution
 */
public class BreakEvent extends DebugEvent
{
	/** unique identifier for the source file where the Player has suspened. */
	public int fileId;

	/** line number in the source file where the Player has suspended. */
	public int line;

	public BreakEvent(int fId, int l)
	{
		fileId = fId;
		line = l;
	}
}
