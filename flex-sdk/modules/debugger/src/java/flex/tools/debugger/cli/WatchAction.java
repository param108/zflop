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

package flex.tools.debugger.cli;

import flash.tools.debugger.Watch;

/**
 * An object that relates a CLI debugger watchpoint with the
 * actual Watch obtained from the Session
 */
public class WatchAction
{
	Watch		m_watch;
	int			m_id;             

	public WatchAction(Watch w) 
	{
		init(w);
	}

	void init(Watch w)
	{
		m_watch = w;
		m_id = BreakIdentifier.next();
	}

	/* getters */
	public int			getId()					{ return m_id; }
	public int			getVariableId()			{ return m_watch.getValueId(); }
	public int			getKind()				{ return m_watch.getKind(); }
	public Watch		getWatch()				{ return m_watch; }

	public String		getExpr()
	{
		String memberName = m_watch.getMemberName();
		int namespaceSeparator = memberName.indexOf("::"); //$NON-NLS-1$
		if (namespaceSeparator != -1)
			memberName = memberName.substring(namespaceSeparator + 2);
		return "#"+getVariableId()+"."+memberName; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* setters */
	public void			resetWatch(Watch w)		{ m_watch = w; }
}
