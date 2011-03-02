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
 * Used as a 'marker' to denote which expressions only take a single argument.
 * That is they have no right hand side children.  For example ~ (bit-wise not)
 * and ! (boolean not)
 */
public interface SingleArgumentExp
{
}
