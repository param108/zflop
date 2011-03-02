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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
public class MultiNameSet
{
	public MultiNameSet()
	{
		s = null;
		key = null;
	}

	public MultiNameSet(int id, int size)
	{
		this();
		this.id = id;
		preferredSize = size;
	}

	private HashSet s;
	private MultiName key;
	private int id;
	private int preferredSize;

	public boolean contains(String[] ns, String name)
	{
		if (s == null) return false;

		if (key == null)
		{
			key = new MultiName(ns, name);
		}
		else
		{
			key.namespaceURI = ns;
			key.localPart = name;
		}

		return s.contains(key);
	}

	public boolean add(Object obj)
	{
		if (s == null)
		{
			s = new HashSet(preferredSize);
		}

		return s.add(obj);
	}

	public boolean add(String[] ns, String name)
	{
		if (!contains(ns, name))
		{
			if (s == null)
			{
				s = new HashSet(preferredSize);
			}
			return s.add(new MultiName(ns, name));
		}
		else
		{
			return false;
		}
	}

	public int size()
	{
		return s == null ? 0 : s.size();
	}

	public boolean addAll(Collection c)
	{
		if (s == null)
		{
			s = new HashSet(preferredSize);
		}

		return s.addAll(c);
	}

	public boolean addAll(MultiNameSet c)
	{
		if (c.s != null)
		{
			return addAll(c.s);
		}
		else
		{
			return false;
		}
	}

	public Iterator iterator()
	{
		if (s != null)
		{
			return s.iterator();
		}
		else
		{
			return i;
		}
	}

	public void clear()
	{
		if (s != null)
		{
			s.clear();
		}
	}

	public int getId()
	{
		return id;
	}
	
	private static final Iterator i = new Iterator()
	{
		public boolean hasNext() { return false; }
		public Object next() { return null; }
		public void remove() {}
	};
	
	public String toString()
	{
		return s == null ? "" : s.toString();
	}
}
