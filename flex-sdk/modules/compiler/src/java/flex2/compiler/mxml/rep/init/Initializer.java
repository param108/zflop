////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep.init;

import flex2.compiler.mxml.reflect.Type;

import java.util.Iterator;

/**
 *
 */
public interface Initializer
{
	/**
	 *
	 */
	int getLineRef();

	/**
	 *
	 */
	boolean isBinding();

	/**
	 *
	 */
	Type getLValueType();

	/**
	 *
	 */
	String getValueExpr();

	/**
	 *
	 */
	String getAssignExpr(String lvalueBase);

	/**
	 *
	 */
	boolean hasDefinition();

	/**
	 *
	 */
	Iterator getDefinitionsIterator();
}
