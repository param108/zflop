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

package flex2.compiler.mxml.reflect;

/**
 * Note: Parameters are MXML 'properties' in the sense of being lvalues. A more precise factoring would have
 * Property and Parameter both extend (e.g.) LValue, but taking a low-disturbance approach for now. Only cost
 * is that implementations of Parameter have to implement a few always-default stubs for things like is-read-only.
 */
public interface Parameter extends Property
{
	/**
	 * true iff default value was specified
	 */
	boolean hasDefault();
}
