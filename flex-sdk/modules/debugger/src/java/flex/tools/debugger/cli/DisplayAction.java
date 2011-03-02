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

package flex.tools.debugger.cli;

import flash.tools.debugger.expression.ValueExp;

/**
 * An object that relates a CLI debugger 'display' command
 * with the contents of the display 
 */
public class DisplayAction
{
	private static int s_uniqueIdentifier  = 1;

	boolean		m_enabled;
	int			m_id;
	ValueExp	m_expression;
	String		m_content;

	public DisplayAction(ValueExp expr, String content)
	{
		init();
		m_expression = expr;
		m_content = content;
	}

	void init()
	{
		m_enabled = true;
		m_id = s_uniqueIdentifier++;
	}

	/* getters */
	public String		getContent()					{ return m_content; }
	public int			getId()							{ return m_id; }
	public boolean		isEnabled()						{ return m_enabled; }
	public ValueExp		getExpression()					{ return m_expression; }

	/* setters */
	public void setEnabled(boolean enable)				{ m_enabled = enable; }
}
