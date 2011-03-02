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

import flash.tools.debugger.Watch;

/**
 * Holder of Watchpoint information
 */
public class DWatch implements Watch
{
	int			m_parentValueId;
	String		m_rawMemberName; // corresponds to Variable.getRawName()
	int			m_kind;
	int			m_tag;

	public DWatch(int id, String name, int kind, int tag)
	{
		m_parentValueId = id;
		m_rawMemberName = name;
		m_kind = kind;
		m_tag = tag;
	}

    public int			getValueId()	{ return m_parentValueId; }
	public String		getMemberName()	{ return m_rawMemberName; }
    public int			getKind()		{ return m_kind; }
    public int			getTag()		{ return m_tag; }
}
