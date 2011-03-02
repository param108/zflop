////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.reflect.*;

/**
 * Encapsulates the order in which we attempt to resolve an MXML attribute or child node name, against an AS3 type.
 * <p>Implementer overrides handler routines.
 * <p>NOTE: search order applies to both attribute and child declarations, of both faceless and visual components.
 */
public abstract class DeclarationHandler
{
	/**
	 * name resolves to Event
	 */
	protected abstract void event(Event event);

	/**
	 * name resolves to declared property
	 */
	protected abstract void property(Property property);

	/**
	 * name resolves to Effect name
	 * @param effect
	 */
	protected abstract void effect(Effect effect);

	/**
	 * name resolves to Style
	 */
	protected abstract void style(Style style);

	/**
	 * name resolves to dynamic property
	  */
	protected abstract void dynamicProperty(String name);

	/**
	 * name fails to resolve
	 */
	protected abstract void unknown(String name);

	/**
	 * Search (in order) the following places in our Type for the given name:
	 * <li>- event
	 * <li>- property
	 * <li>- effect
	 * <li>- style
	 * <li>- dynamic property (if target type is dynamic)
	 */
	protected void invoke(Type type, String name)
	{
		//	String msg = "\tDeclarationHandler.invoke(" + type.getName() + ",'" + name + "'): ";

		Event event = type.getEvent(name);
		if (event != null)
		{
			//	System.out.println(msg + "event()");
			event(event);
			return;
		}

		Property property = type.getProperty(name);
		if (property != null)
		{
			//	System.out.println(msg + "property()");
			property(property);
			return;
		}

		Effect effect = type.getEffect(name);
		if (effect != null)
		{
			//	System.out.println(msg + "effect()");
			effect(effect);
			return;
		}

		Style style = type.getStyle(name);
		if (style != null)
		{
			//	System.out.println(msg + "style()");
			style(style);
			return;
		}

		if (type.hasDynamic())
		{
			//	System.out.println(msg + "dynamicProperty()");
			dynamicProperty(name);
			return;
		}

		//	System.out.println(msg + "unknown()");
		unknown(name);
	}
}
