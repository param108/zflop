////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex.tools.debugger.cli;

import flash.tools.debugger.NoResponseException;
import flash.tools.debugger.NotConnectedException;
import flash.tools.debugger.NotSuspendedException;
import flash.tools.debugger.Session;
import flash.tools.debugger.Value;
import flash.tools.debugger.Variable;
import flash.tools.debugger.events.FaultEvent;

/**
 * A VariableFacade provides a wrapper around a Variable object
 * that provides a convenient way of storing parent information.
 * 
 * Don't ask me why we didn't just add a parent member to 
 * Variable and be done with it.
 */
public class VariableFacade implements Variable
{
	Variable	m_var;
	long		m_context;
	String		m_name;
	String		m_path;

	public VariableFacade(Variable v, long context)		{ init(context, v, null); }
	public VariableFacade(long context, String name)	{ init(context, null, name); }

	void init(long context, Variable v, String name)
	{
		m_var = v;
		m_context = context;
		m_name = name;
	}

	/**
	 * The variable interface 
	 */
	public String		getName()								{ return (m_var == null) ? m_name : m_var.getName(); }
	public String		getQualifiedName()						{ return m_var.getQualifiedName(); }
	public String		getNamespace()							{ return m_var.getNamespace(); }
	public int			getLevel()								{ return m_var.getLevel(); }
	public String		getDefiningClass()						{ return m_var.getDefiningClass(); }
	public int			getAttributes()							{ return m_var.getAttributes(); }
	public int			getScope()								{ return m_var.getScope(); }
	public boolean		isAttributeSet(int variableAttribute)	{ return m_var.isAttributeSet(variableAttribute); }
	public Value		getValue()								{ return m_var.getValue(); }
	public boolean		hasValueChanged(Session s)				{ return m_var.hasValueChanged(s); }
	public FaultEvent setValue(Session s, int type, String value) throws NotSuspendedException, NoResponseException, NotConnectedException
	{
		return m_var.setValue(s, type, value);
	}
	public String		toString()								{ return (m_var == null) ? m_name : m_var.toString(); }
	public String		getPath()								{ return m_path; }
	public void			setPath(String path)					{ m_path = path; }
	public boolean needsToInvokeGetter()						{ return m_var.needsToInvokeGetter(); }
	public void invokeGetter(Session s) throws NotSuspendedException, NoResponseException, NotConnectedException
	{
		m_var.invokeGetter(s);
	}

	/**
	 * Our lone get context (i.e. parent) interface 
	 */
	public int			getContext()									{ return (int)m_context; }
	public Variable		getVariable()									{ return m_var; }
}
