////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex.tools.debugger.cli;

import java.util.HashMap;
import java.util.Set;

public class IntProperties
{
	HashMap m_map = new HashMap();

	/* getters */
	public Integer		getInteger(String s)	{ return (Integer)m_map.get(s); }
	public int			get(String s)			{ return getInteger(s).intValue(); }
	public Set			keySet()				{ return m_map.keySet(); }
	public HashMap		map()					{ return m_map; }

	/* setters */
	public void put(String s, int value)		{ m_map.put(s, new Integer(value)); }

}
