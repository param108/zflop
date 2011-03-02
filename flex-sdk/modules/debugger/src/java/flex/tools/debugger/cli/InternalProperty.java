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

import flash.tools.debugger.expression.NoSuchVariableException;

public class InternalProperty
{
	String m_key;
	Object m_value;

	public InternalProperty(String key, Object value)
	{
		m_key = key;
		m_value = value;
	}

	/* getters */
	public String getName()		{ return m_key; }
	public String toString()	{ return (m_value == null) ? "null" : m_value.toString(); } //$NON-NLS-1$

	public String valueOf() throws NoSuchVariableException 
	{ 
		if (m_value == null) 
			throw new NoSuchVariableException(m_key);

		return toString();
	}

}
