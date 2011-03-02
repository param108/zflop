////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import java.util.LinkedHashMap;

/**
 * @author Clement Wong
 */
public class LinkedQNameMap extends LinkedHashMap
{
	public LinkedQNameMap()
	{
		super();
		key = new QName();
	}

	public LinkedQNameMap(int size)
	{
		super(size);
		key = new QName();
	}

	private QName key;

	public boolean containsKey(String ns, String name)
	{
		key.setNamespace(ns);
		key.setLocalPart(name);
		return containsKey(key);
	}

	public Object get(String ns, String name)
	{
		key.setNamespace(ns);
		key.setLocalPart(name);
		return get(key);
	}

	public Object put(String ns, String name, Object value)
	{
		return put(new QName(ns, name), value);
	}
}
