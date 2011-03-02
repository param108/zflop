////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.rep.init.ArrayElementInitializer;
import flex2.compiler.util.IteratorList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * TODO this shouldn't really subclass Model. Either detach it, or block other Model methods like setProperty() below
 */
public class Array extends Model
{
	private Collection list;
	private Type elementType;

	public Array(MxmlDocument document, Type elementType, int line)
	{
		this(document, elementType, null, line);
	}

	public Array(MxmlDocument document, Type elementType, Model parent, int line)
	{
		super(document, document.getTypeTable().arrayType, parent, line);
		this.list = new ArrayList();
		this.elementType = elementType;
	}

	public void setProperty(String name, Object value)
	{
		assert false : "Array may not have properties";
	}
	
	public void addEntry(Model entry)
	{
		addEntry(entry, entry.getXmlLineNumber());
	}

	public void addEntry(Object entry, int line)
	{
		list.add(new ArrayElementInitializer(elementType, list.size(), entry, line));
	}

	public void addEntries(Collection entries, int line)
	{
		for (Iterator iter = entries.iterator(); iter.hasNext(); )
		{
			addEntry(iter.next(), line);
		}
	}

	public void setEntries(Collection entries)
	{
		this.list = entries;
	}

	public Collection getEntries()
	{
		return list;
	}

	public int size()
	{
		return list.size();
	}

	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	/**
	 * Note that we do *not* filter out bindings for element initializers.
	 */
	public final Iterator getElementInitializerIterator()
	{
		return list.iterator();
	}

	/**
	 *  iterator containing definitions from our initializers
	 */
	public Iterator getSubDefinitionsIterator()
	{
		IteratorList iterList = new IteratorList();

		addDefinitionIterators(iterList, getElementInitializerIterator());

		return iterList.toIterator();
	}

	/**
	 * override hasBindings to check entries
	 */
	public boolean hasBindings()
	{
		return bindingsOnly(getEntries().iterator()).hasNext() || super.hasBindings();
	}
}
