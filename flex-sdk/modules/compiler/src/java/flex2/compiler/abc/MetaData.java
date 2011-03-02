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

package flex2.compiler.abc;

import java.util.Map;

/**
 * @author Clement Wong
 */
public interface MetaData
{
	String getID();

	String getKey(int index);

	String getValue(int index);

	String getValue(String name);

    Map getValueMap();

	int count();
}
