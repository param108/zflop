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

package flash.tools.debugger.concrete;

import flash.tools.debugger.SuspendReason;

/**
 * The suspend information object returns information about the
 * current halted state of the Player.
 */
public class DSuspendInfo
{
	int m_reason;
	int m_actionIndex;  // which script caused the halt
	int m_offset;		// offset into the actions that the player has halted
	int m_previousOffset;  // previous offset, if any, which lies on the same source line (-1 means unknown)
	int m_nextOffset;  // next offset, if any, which lies on the same source line (-1 means unknown)

	public DSuspendInfo()
	{
		m_reason = SuspendReason.Unknown;
		m_actionIndex =	-1;
		m_offset = -1;	
		m_previousOffset = -1;
		m_nextOffset = -1;
	}

	public DSuspendInfo(int reason, int actionIndex, int offset, int previousOffset, int nextOffset)
	{
		m_reason = reason;
		m_actionIndex =	actionIndex;
		m_offset = offset;	
		m_previousOffset = previousOffset;
		m_nextOffset = nextOffset;
	}

    public int getReason()			{ return m_reason; }
	public int getActionIndex()		{ return m_actionIndex; }
    public int getOffset()			{ return m_offset; }
	public int getPreviousOffset()	{ return m_previousOffset; }
	public int getNextOffset()		{ return m_nextOffset; }
}
