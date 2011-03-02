////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.concrete;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import flash.tools.debugger.NoResponseException;
import flash.tools.debugger.NotConnectedException;
import flash.tools.debugger.NotSuspendedException;
import flash.tools.debugger.Session;
import flash.tools.debugger.Value;
import flash.tools.debugger.ValueAttribute;
import flash.tools.debugger.Variable;
import flash.tools.debugger.VariableType;
import flash.tools.debugger.expression.Context;
import flash.util.ArrayUtil;

/**
 * @author mmorearty
 */
public class DValue implements Value
{
	/** @see VariableType */
	private int			m_type;

	/** @see Variable#getTypeName() */
	private String		m_typeName;

	/** @see Variable#getClassName() */
	private String		m_className;

	/** @see ValueAttribute */
	private int			m_attribs;

	/** Maps "varname" (without its namespace) to a Variable */
	private Map			m_members;

	/**
	 * Either my own ID, or else my parent's ID if I am <code>__proto__</code>.
	 */
	int					m_nonProtoId;

	/**
	 * <code>m_value</code> can have one of several possible meanings:
	 *
	 * <ul>
	 * <li> If this variable's value is an <code>Object</code> or a <code>MovieClip</code>,
	 *      then <code>m_value</code> contains the ID of the <code>Object</code> or
	 *      <code>MovieClip</code>, stored as a <code>Long</code>. </li>
	 * <li> If this variable refers to a Getter which has not yet been invoked, then
	 *      <code>m_value</code> contains the ID of the Getter, stored as a
	 *      <code>Long</code>. </li>
	 * <li> Otherwise, this variable's value is a simple type such as <code>int</code> or
	 *      <code>String</code>, in which case <code>m_value</code> holds the actual value.
	 * </ul>
	 */
	private Object		m_value;

	/**
	 * The list of classes that contributed members to this object, from
	 * the class itself all the way down to Object.
	 */
	private String[] m_classHierarchy;

	/**
	 * How many members of <code>m_classHierarchy</code> actually contributed
	 * members to this object.
	 */
	private int m_levelsWithMembers;

	/**
	 * Create a top-level variable which has no parent.  This may be used for
	 * _global, _root, stack frames, etc.
	 *
	 * @param id the ID of the variable
	 */
	public DValue(long id)
	{
		init(VariableType.UNKNOWN, null, null, 0, new Long(id));
	}

	/**
	 * Create a value.
	 *
	 * @param type see <code>VariableType</code>
	 * @param typeName
	 * @param className
	 * @param attribs
	 *            the attributes of this value; see <code>ValueAttribute</code>
	 * @param value
	 *            for an Object or MovieClip, this should be a Long which contains the
	 *            ID of this variable.  For a variable of any other type, such as integer
	 *            or string, this should be the value of the variable.
	 */
	public DValue(int type, String typeName, String className, int attribs, Object value)
	{
		init(type, typeName, className, attribs, value);
	}

	/**
	 * Initialize a variable.
	 *
	 * For the meanings of the arguments, see the DVariable constructor.
	 */
	private void init(int type, String typeName, String className, int attribs, Object value)
	{
		m_type = type;
		m_typeName = typeName;
		m_className = className;
		m_attribs = attribs;
		m_value = value;
		m_members = null;
		m_nonProtoId = getId();
	}

	/*
	 * @see flash.tools.debugger.Value#getAttributes()
	 */
	public int getAttributes()
	{
		return m_attribs;
	}

	/*
	 * @see flash.tools.debugger.Value#getClassName()
	 */
	public String getClassName()
	{
		return m_className;
	}

	/*
	 * @see flash.tools.debugger.Value#getId()
	 */
	public int getId()
	{
		// see if we support an id concept
		if (m_value instanceof Long)
			return ((Long)m_value).intValue();
		else
			return Value.UNKNOWN_ID;
	}

	/*
	 * @see flash.tools.debugger.Value#getMemberCount(flash.tools.debugger.Session)
	 */
	public int getMemberCount(Session s) throws NotSuspendedException,
			NoResponseException, NotConnectedException
	{
		obtainMembers(s);
		return (m_members == null) ? 0 : m_members.size();
	}

	/*
	 * @see flash.tools.debugger.Value#getMemberNamed(flash.tools.debugger.Session, java.lang.String)
	 */
	public Variable getMemberNamed(Session s, String name)
			throws NotSuspendedException, NoResponseException,
			NotConnectedException
	{
		obtainMembers(s);
		return findMember(name);
	}

	/*
	 * @see flash.tools.debugger.Value#getClassHierarchy(boolean)
	 */
	public String[] getClassHierarchy(boolean allLevels) {
		if (allLevels) {
			return m_classHierarchy;
		} else {
			String[] partialClassHierarchy;

			if (m_classHierarchy != null)
			{
				partialClassHierarchy = new String[m_levelsWithMembers];
				System.arraycopy(m_classHierarchy, 0, partialClassHierarchy, 0, m_levelsWithMembers);
			}
			else
			{
				partialClassHierarchy = new String[0];
			}
			return partialClassHierarchy;
		}
	}

	/* TODO should this really be public? */
	public DVariable findMember(String named)
	{
		if (m_members == null)
			return null;
		else
			return (DVariable) m_members.get(named);
	}

	/*
	 * @see flash.tools.debugger.Value#getMembers(flash.tools.debugger.Session)
	 */
	public Variable[] getMembers(Session s) throws NotSuspendedException,
			NoResponseException, NotConnectedException
	{
		obtainMembers(s);

		/* find out the size of the array */
		int count = getMemberCount(s);
		DVariable[] ar = new DVariable[count];

		if (count > 0)
		{
			count = 0;
			Iterator itr = m_members.values().iterator();
			while(itr.hasNext())
			{
				DVariable  sf = (DVariable) itr.next();
				ar[count++] = sf;
			}

			// sort the member list by name
			ArrayUtil.sort(ar);
		}

		return ar;
	}

	/**
	 * WARNING: this call will initiate a call to the session to obtain the members
	 * the first time around.
	 * @throws NotConnectedException
	 * @throws NoResponseException
	 * @throws NotSuspendedException
	 */
	private void obtainMembers(Session s) throws NotSuspendedException, NoResponseException, NotConnectedException
	{
		if (m_members == null && s != null)
		{
			// performing a get on this variable obtains all its members
			int id = getId();
			if (id != Value.UNKNOWN_ID)
			{
				if (((PlayerSession)s).getRawValue(id) == this)
					((PlayerSession)s).obtainMembers(id);
				if (m_members != null)
				{
					Iterator iter = m_members.values().iterator();
					while (iter.hasNext())
					{
						Object next = iter.next();
						if (next instanceof DVariable)
						{
							((DVariable)next).setSession(s);
						}
					}
				}
			}
		}
	}

	public boolean membersObtained()
	{
		return (getId() == UNKNOWN_ID || m_members != null);
	}

	public void setMembersObtained(boolean obtained)
	{
		if (obtained)
		{
			if (m_members == null)
				m_members = Collections.EMPTY_MAP;
		}
		else
		{
			m_members = null;
		}
	}

	public void addMember(DVariable v)
	{
		if (m_members == null)
			m_members = new HashMap();

		// if we are a proto member house away our original parent id
		String name = v.getName();
		DValue val = (DValue) v.getValue();
		val.m_nonProtoId = (name != null && name.equals("__proto__")) ? m_nonProtoId : val.getId(); //$NON-NLS-1$ // TODO is this right?
		v.m_nonProtoParentId = m_nonProtoId;

		m_members.put(name, v);
	}

	public void removeAllMembers()
	{
		m_members = null;
	}

	/*
	 * @see flash.tools.debugger.Value#getType()
	 */
	public int getType()
	{
		return m_type;
	}

	/*
	 * @see flash.tools.debugger.Value#getTypeName()
	 */
	public String getTypeName()
	{
		return m_typeName;
	}

	/*
	 * @see flash.tools.debugger.Value#getValueAsObject()
	 */
	public Object getValueAsObject()
	{
		return m_value;
	}

	/*
	 * @see flash.tools.debugger.Value#getValueAsString()
	 */
	public String getValueAsString()
	{
		if (m_value == null)
			return "undefined"; //$NON-NLS-1$

		if (m_value instanceof Double)
		{
			// Java often formats whole numbers in ugly ways.  For example,
			// the number 3 might be formatted as "3.0" and, even worse,
			// the number 12345678 might be formatted as "1.2345678E7" !
			// So, if the number has no fractional part, then we override
			// the default display behavior.
			double value = ((Double)m_value).doubleValue();
			long longValue = (long) value;
			if (value == longValue)
				return Long.toString(longValue);
		}

		return m_value.toString();
	}

	/*
	 * @see flash.tools.debugger.Value#isAttributeSet(int)
	 */
	public boolean isAttributeSet(int variableAttribute)
	{
		return (m_attribs & variableAttribute) != 0;
	}

	public void	setTypeName(String s)	{ m_typeName = s; }
	public void	setClassName(String s)	{ m_className = s; }
	public void setType(int t)			{ m_type = t; }
	public void setValue(Object o)		{ m_value = o; }
	public void setAttributes(int f)	{ m_attribs = f; }

	public void setClassHierarchy(String[] classHierarchy, int levelsWithMembers)
	{
		m_classHierarchy = classHierarchy;
		m_levelsWithMembers = levelsWithMembers;
	}

	public String membersToString()
	{
		StringBuffer sb = new StringBuffer();

		/* find out the size of the array */
		if (m_members == null)
			sb.append(PlayerSessionManager.getLocalizationManager().getLocalizedTextString("empty")); //$NON-NLS-1$
		else
		{
			Iterator itr = m_members.values().iterator();
			while(itr.hasNext())
			{
				DVariable  sf = (DVariable) itr.next();
				sb.append(sf);
				sb.append(",\n"); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * Necessary for expression evaluation.
	 * @see Context#lookup(Object)
	 */
	public String toString() { return getValueAsString(); }
}
