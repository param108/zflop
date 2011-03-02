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

package flash.tools.debugger.expression;

/**
 * Thrown when an attempt is made to set the child of a node 
 * that does not exist
 */
public class NoChildException extends Exception
{
	public NoChildException() { super(); }
	public NoChildException(String s) { super(s); }
}
