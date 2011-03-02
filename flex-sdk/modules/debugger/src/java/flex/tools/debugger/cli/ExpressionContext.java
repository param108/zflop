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

import java.util.StringTokenizer;
import java.util.Vector;

import flash.tools.debugger.Session;
import flash.tools.debugger.SessionManager;
import flash.tools.debugger.Value;
import flash.tools.debugger.ValueAttribute;
import flash.tools.debugger.Variable;
import flash.tools.debugger.VariableType;
import flash.tools.debugger.PlayerDebugException;
import flash.tools.debugger.events.ExceptionFault;
import flash.tools.debugger.events.FaultEvent;
import flash.tools.debugger.expression.ArithmeticExp;
import flash.tools.debugger.expression.Context;
import flash.tools.debugger.expression.NoSuchVariableException;
import flash.tools.debugger.expression.PlayerFaultException;

public class ExpressionContext implements Context
{
	ExpressionCache		m_cache;
	Object				m_current;
	boolean				m_createIfMissing;  // set if we need to create a variable if it doesn't exist
	Vector				m_namedPath;
	boolean				m_nameLocked;
	String				m_newline = System.getProperty("line.separator"); //$NON-NLS-1$

	// used when evaluating an expression 
	public ExpressionContext(ExpressionCache cache)
	{
		m_cache = cache;
		m_current = null;
		m_createIfMissing = false;
		m_namedPath = new Vector();
		m_nameLocked = false;
	}

	void		setContext(Object o)	{ m_current = o; }
	Session		getSession()			{ return m_cache.getSession(); }

	void		pushName(String name)	{ if (m_nameLocked || name.length() < 1) return; m_namedPath.add(name);  }
	boolean		setName(String name)	{ if (m_nameLocked) return true; m_namedPath.clear(); pushName(name); return true; }
	void		lockName()				{ m_nameLocked = true; }

	public String getName() 
	{ 
		int size = m_namedPath.size();
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<size; i++)
		{
			String s = (String) m_namedPath.get(i);
			if (i > 0)
				sb.append('.');
			sb.append(s);
		}
		return ( sb.toString() );
	}

	String getCurrentPackageName()	
	{ 
		String s = null;
		try
		{
			Integer o = (Integer)m_cache.get(DebugCLI.LIST_MODULE);
			s = m_cache.getPackageName(o.intValue());
		}
		catch(NullPointerException npe)
		{
		}
		catch(ClassCastException cce)
		{
		}
		return s; 
	}

	//
	//
	// Start of Context API implementation 
	//
	//
	public void createPseudoVariables(boolean oui) { m_createIfMissing = oui; }
	
	// create a new context object by combinging the current one and o 
	public Context createContext(Object o)
	{
		ExpressionContext c = new ExpressionContext(m_cache);
		c.setContext(o);
		c.createPseudoVariables(m_createIfMissing);
		c.m_namedPath.addAll(m_namedPath);
		return c;
	}

	// assign the object o, the value v; returns Boolean true if worked, false if failed
	public Object assign(Object o, Object v) throws NoSuchVariableException, PlayerFaultException
	{
		boolean worked = false;
		try
		{
			// we expect that o is a variable that can be resolved or is a specially marked internal variable
			if (o instanceof InternalProperty)
				worked = assignInternal((InternalProperty)o, v);
			else
			{
				Variable var = resolveToVariable(o);

				if (var == null)
					throw new NoSuchVariableException((var == null) ? m_current : var.getName());

				// set the value, for the case of a variable that does not exist it will not have a type
				// so we try to glean one from v.
				int type = determineType(var, v);
				FaultEvent faultEvent = var.setValue(getSession(), type, v.toString());
				if (faultEvent != null)
					throw new PlayerFaultException(faultEvent);
				worked = true;
			}
		}
		catch(PlayerDebugException pde)
		{
			worked = false;
		}

		return new Boolean(worked);
	}

	/**
	 * The Context interface which goes out and gets values from the session
	 * Expressions use this interface as a means of evaluation.
	 * 
	 * We also use this to create a reference to internal variables.
	 */
	public Object lookup(Object o) throws NoSuchVariableException, PlayerFaultException
	{
		Object result = null;
		try
		{
			// first see if it is an internal property (avoids player calls)
			if (o instanceof String && ((String)o).charAt(0) == '$')
			{
				String key = (String)o;
				Object value = null;
			
				try { value = m_cache.get(key); } catch(Exception e) {}
				result = new InternalProperty(key, value);
			}

			// attempt to resolve to a player variable
			else if ( (result = resolveToVariable(o)) != null)
				;

			// or value
			else if ( (result = resolveToValue(o)) != null)
				;

			else
				throw new NoSuchVariableException(o);

			// take on the path to the variable; so 'what' command prints something nice
			if ((result != null) && result instanceof VariableFacade)
			{
				((VariableFacade)result).setPath(getName());
			}

			// if the attempt to get the variable's value threw an exception inside the
			// player (most likely because the variable is actually a getter, and the
			// getter threw something), then throw something here
			Value resultValue = null;
			if (result instanceof Variable)
				resultValue = ((Variable)result).getValue();
			else if (result instanceof Value)
				resultValue = (Value) result;
			if (resultValue != null)
			{
				if (resultValue.isAttributeSet(ValueAttribute.IS_EXCEPTION))
				{
					String value = resultValue.getValueAsString();
					throw new PlayerFaultException(new ExceptionFault(value));
				}
			}
		}
		catch(PlayerDebugException pde)
		{
			result = null; // null object
		}
		return result;
	}

	/* returns a string consisting of formatted member names and values */
	public Object lookupMembers(Object o) throws NoSuchVariableException
	{
		Variable var = null;
		Value val = null;
  		Variable[] mems = null;
		try
		{
			var = resolveToVariable(o);
			if (var != null)
				val = var.getValue();
			else
				val = resolveToValue(o);
			mems = val.getMembers(getSession());
		}
		catch(NullPointerException npe)
		{
			throw new NoSuchVariableException(o);
		}
		catch(PlayerDebugException pde)
		{
			throw new NoSuchVariableException(o); // not quite right...
		}

  		StringBuffer sb = new StringBuffer();

  		if (var != null)
  			ExpressionCache.appendVariable(sb, var);
  		else
  			ExpressionCache.appendVariableValue(sb, val);

		boolean attrs = m_cache.propertyEnabled(DebugCLI.DISPLAY_ATTRIBUTES);
		if (attrs && var != null)
			ExpressionCache.appendVariableAttributes(sb, var);

		// [mmorearty] experimenting with hierarchical display of members
		String[] classHierarchy = val.getClassHierarchy(false);
		if (classHierarchy != null && getSession().getPreference(SessionManager.PREF_HIERARCHICAL_VARIABLES) != 0)
		{
			for (int c=0; c<classHierarchy.length; ++c)
			{
				String classname = classHierarchy[c];
				sb.append(m_newline + "(Members of " + classname + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				for (int i=0; i<mems.length; ++i)
				{
					if (classname.equals(mems[i].getDefiningClass()))
					{
			  			sb.append(m_newline + " "); //$NON-NLS-1$
			  			ExpressionCache.appendVariable(sb, mems[i]);
						if (attrs)
							ExpressionCache.appendVariableAttributes(sb, mems[i]);
					}
				}
			}
		}
		else
		{
	  		for(int i=0; i<mems.length; i++)
	  		{
	  			sb.append(m_newline + " "); //$NON-NLS-1$
	  			ExpressionCache.appendVariable(sb, mems[i]);
				if (attrs)
					ExpressionCache.appendVariableAttributes(sb, mems[i]);
	  		}
		}

  		return sb.toString();
  	}

	//
	//
	// End of Context API implementation 
	//
	//

	// determine the type from the VariableFacade or use the value object
	int determineType(Variable var, Object value)
	{
		int type = VariableType.UNKNOWN;

		if (var instanceof VariableFacade && ((VariableFacade)var).getVariable() == null)
		{
			if (value instanceof Number)
				type = VariableType.NUMBER;
			else if (value instanceof Boolean)
				type = VariableType.BOOLEAN;
			else 
				type = VariableType.STRING;
		}
		else 
			type = var.getValue().getType();

		return type;
	}
	

	// used to assign a value to an internal variable 
	boolean assignInternal(InternalProperty var, Object v) throws NoSuchVariableException, NumberFormatException
	{
		// otherwise set it
		long l = ArithmeticExp.toLong(v);
		m_cache.put(var.getName(), (int)l);
		return true;
	}

	/**
	 * Resolve the object into a variable by various means and 
	 * using the current context.
	 * @return variable, or <code>null</code>
	 */
	Variable resolveToVariable(Object o) throws PlayerDebugException
	{
		Variable v = null;

		// if o is a variable already, then we're done!
		if (o instanceof Variable)
			return (Variable)o;

		/**
		 * Resolve the name to something
		 */
		{
			// not an id so try as name 
			String name = o.toString();
			long id = nameAsId(name);

			/**
			 * if #N was used just pick up the variable, otherwise
			 * we need to use the current context to resolve 
			 * the name to a member
			 */
			if (id != Value.UNKNOWN_ID)
			{
				// TODO what here?
			}
			else
			{
				// try to resolve as a member of current context (will set context if null)
				id = determineContext(name);
				v = locateForNamed((int)id, name, true);
				if (v != null)
					v = new VariableFacade(v, id);
				else if (v == null && m_createIfMissing && name.charAt(0) != '$')
					v = new VariableFacade(id, name);
			}
		}

		/* return the variable */
		return v;
	}

	/*
	 * Resolve the object into a variable by various means and 
	 * using the current context.
	 */
	Value resolveToValue(Object o) throws PlayerDebugException
	{
		Value v = null;

		// if o is a variable or a value already, then we're done!
		if (o instanceof Value)
			return (Value)o;
		else if (o instanceof Variable)
			return ((Variable)o).getValue();

		/**
		 * Resolve the name to something
		 */
		{
			// not an id so try as name 
			String name = o.toString();
			long id = nameAsId(name);

			/**
			 * if #N was used just pick up the variable, otherwise
			 * we need to use the current context to resolve 
			 * the name to a member
			 */
			if (id != Value.UNKNOWN_ID)
			{
				v = getSession().getValue((int)id);
			}
			else
			{
				// TODO what here?
			}
		}

		/* return the value */
		return v;
	}

	// special code for #N support. I.e. naming a variable via an ID
	long nameAsId(String name)
	{
		long id = Value.UNKNOWN_ID;
		try
		{
			if (name.charAt(0) == '#')
				id = Long.parseLong(name.substring(1));
		}
		catch(Exception e) 
		{
			id = Value.UNKNOWN_ID;
		}
		return id;
	}

	/**
	 * Using the given id as a parent find the member named
	 * name.
	 * @throws NoSuchVariableException if id is UNKNOWN_ID
	 */
	Variable memberNamed(int id, String name) throws NoSuchVariableException, PlayerDebugException
	{
		Variable v = null;
		Value parent = getSession().getValue((int)id);

		if (parent == null)
			throw new NoSuchVariableException(name);

		/* got a variable now return the member if any */
		v = parent.getMemberNamed(getSession(), name);

		return v;
	}

	/**
	 * All the really good stuff about finding where name exists goes here!
	 * 
	 * If name is not null, then it implies that we use the existing
	 * m_current to find a member of m_current.  If m_current is null
	 * Then we need to probe variable context points attempting to locate
	 * name.  When we find a match we set the m_current to this context
	 *
	 * If name is null then we simply return the current context.
	 */
	int determineContext(String name) throws PlayerDebugException
	{
		long id = Value.UNKNOWN_ID;

		// have we already resolved our context...
		if (m_current != null)
		{
			Object value;

			if (m_current instanceof Variable)
				value = ((Variable) m_current).getValue().getValueAsObject();
			else if (m_current instanceof Value)
				value = ((Value) m_current).getValueAsObject();
			else
				value = m_current;

			id = ArithmeticExp.toLong( value );
		}

		// nothing to go on, so we're done
		else if (name == null)
			;

		// use the name and try and resolve where we are...
		else
		{
			// Each stack frame has a root variable under (BASE_ID-depth)
			// where depth is the depth of the stack.
			// So we query for our current stack depth and use that 
			// as the context for our base computation
			int baseId = Value.BASE_ID;
			int depth = ((Integer)m_cache.get(DebugCLI.DISPLAY_FRAME_NUMBER)).intValue();
			baseId -= depth;

			// obtain data about our current state 
			Variable contextVar = null;
			Value contextVal = null;
			Value val = null;

			// look for 'name' starting from local scope
			if ( (val = locateParentForNamed(baseId, name, false)) != null)
				;

			// get the this pointer, then look for 'name' starting from that point
			else if ( ( (contextVar = locateForNamed(baseId, "this", false)) != null ) &&  //$NON-NLS-1$
					  ( setName("this") && (val = locateParentForNamed(contextVar.getValue().getId(), name, true)) != null ) ) //$NON-NLS-1$
				;

			// now try to see if 'name' exists off of _root
			else if ( setName("_root") && (val = locateParentForNamed(Value.ROOT_ID, name, true)) != null ) //$NON-NLS-1$
				;

			// now try to see if 'name' exists off of _global
			else if ( setName("_global") && (val = locateParentForNamed(Value.GLOBAL_ID, name, true)) != null ) //$NON-NLS-1$
				;

			// now try off of class level, if such a thing can be found
			else if ( ( (contextVal = locate(Value.GLOBAL_ID, getCurrentPackageName(), false)) != null ) && 
					  ( setName("_global."+getCurrentPackageName()) && (val = locateParentForNamed(contextVal.getId(), name, true)) != null ) ) //$NON-NLS-1$
				;

			// if we found it then stake this as our context!
			if (val != null)
			{
				id = val.getId();
				pushName(name);
				lockName();
			}
		}
		
		return (int)id;
	}

	/**
	 * Performs a search for a member with the given name using the
	 * given id as the parent variable.
	 * 
	 * If a match is found then, we return the parent variable of
	 * the member that matched.  The proto chain is optionally traversed.
	 * 
	 * No exceptions are thrown
	 */
	Value locateParentForNamed(int id, String name, boolean traverseProto) throws PlayerDebugException
	{
		StringBuffer sb = new StringBuffer();

		Variable var = null;
		Value val = null;
		try
		{
			var = memberNamed(id, name);

			// see if we need to traverse the proto chain
			while (var == null && traverseProto)
			{
				// first attempt to get __proto__, then resolve name
				Variable proto = memberNamed(id, "__proto__"); //$NON-NLS-1$
 				sb.append("__proto__"); //$NON-NLS-1$
				if (proto == null)
					traverseProto = false;
				else
				{
					id = proto.getValue().getId();
					var = memberNamed(id, name);
					if (var == null)
						sb.append('.');
				}
			}
		}
		catch(NoSuchVariableException nsv)
		{
			// don't worry about this one, it means variable with id couldn't be found
		}
		catch(NullPointerException npe)
		{
			// probably no session
		}

		// what we really want is the parent not the child variable
		if (var != null)
		{
			pushName(sb.toString());
			val = getSession().getValue(id);
		}

		return val;
	}

	// variant of locateParentForNamed, whereby we return the child variable
	Variable locateForNamed(int id, String name, boolean traverseProto) throws PlayerDebugException
	{
		Variable var = null;
		Value v = locateParentForNamed(id, name, traverseProto);
		if (v != null)
			try { var = memberNamed(v.getId(), name); } catch(NoSuchVariableException nse) { v = null; }

		return var;
	}

	/**
	 * Locates the member via a dotted name starting at the given id.
	 * It will traverse any and all proto chains if necc. to find the name.
	 */
	Value locate(int startingId, String dottedName, boolean traverseProto) throws PlayerDebugException
	{
		if (dottedName == null)
			return null;

		// first rip apart the dottedName
		StringTokenizer names = new StringTokenizer(dottedName, "."); //$NON-NLS-1$
		Value val = getSession().getValue(startingId);

		while(names.hasMoreTokens() && val != null)
			val = locateForNamed(val.getId(), names.nextToken(), traverseProto).getValue();

		return val;
	}
}
