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

package flex2.compiler.mxml.reflect;

/**
 * [Effect(name="...",event="...")]
 */
public interface Effect
{
	String getName();

	String getEvent();

	
	String getDeprecatedMessage();
	
	String getDeprecatedReplacement();
	
	String getDeprecatedSince();
}
