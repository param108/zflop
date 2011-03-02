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

import flash.tools.debugger.Location;
import flash.tools.debugger.SourceFile;

public class DLocation implements Location
{
	SourceFile	m_source;
	int			m_line;
	boolean     m_removed;

	DLocation(SourceFile src, int line)
	{
		m_source = src;
		m_line = line;
		m_removed = false;
	}

	/* getters/setters */
	public SourceFile	getFile()						{ return m_source; }
    public int		    getLine()						{ return m_line; }
	public boolean		isRemoved()						{ return m_removed; }
	public void			setRemoved(boolean removed)		{ m_removed = removed; }

	public int			getId() { return encodeId(getFile().getId(), getLine()); }

	/* encode /decode */
	public static final int encodeId(int fileId, int line)
	{
		return ( (line << 16) | fileId );
	}
	
	public static final int decodeFile(long id)
	{
		return (int)(id & 0xffff);
	}

	public static final int decodeLine(long id)
	{
		return (int)(id >> 16 & 0xffff);
	}
	
	/** for debugging */
	public String toString()
	{
		return m_source.toString() + ":" + m_line; //$NON-NLS-1$
	}
}
