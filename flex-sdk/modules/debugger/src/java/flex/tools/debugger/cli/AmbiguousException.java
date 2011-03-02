////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex.tools.debugger.cli;

/**
 * An exception that is thrown when some ambiguous condition or state
 * was encountered.  It is usually not fatal, and normally caused
 * by some user interaction which can be overcome. 
 */
public class AmbiguousException extends Exception
{
	public AmbiguousException() {}
	public AmbiguousException(String s) { super(s); }
}
