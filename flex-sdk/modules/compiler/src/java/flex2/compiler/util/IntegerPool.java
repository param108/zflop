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

package flex2.compiler.util;

/**
 * This is for minimizing object creations in situations like - map.put(key, map.get(key) + 1);
 * 
 * @author Clement Wong
 */
public final class IntegerPool
{
	public static Integer getNumber(final int num)
	{
		return macromedia.asc.util.IntegerPool.getNumber(num);
	}
}
