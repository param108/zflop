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
 * This event is fired when the player has unloaded a swf
 */
public class SwfUnloadedEvent extends DebugEvent
{
	/** unique identifier for the SWF */
	public long			id;

	/** index of SWF in Session.getSwfs() array */
	public int			index;

	/** full path name for the SWF */
	public String		path;

	public SwfUnloadedEvent(long sId, String sPath, int sIndex)
	{
		id = sId;
		index = sIndex;
		path = sPath;
	}
}
