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
import flex2.compiler.mxml.rep.init.VisualChildInitializer;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.IteratorList;

import java.util.*;

/**
 * @author Edwin Smith
 * TODO remove when you-know-what happens
 */
public class MovieClip extends Model
{
	private Set children;

	public MovieClip(MxmlDocument document, Type type, Model parent, int line)
	{
		super(document, type, parent, line);
		children = new LinkedHashSet();
	}

	public void addChild(MovieClip child)
	{
		children.add(new VisualChildInitializer(child));

		if (StandardDefs.isRepeater(child.getType()))
		{
			getDocument().ensureDeclaration(this);
		}
	}

	public Set children()
	{
		return children;
	}

	public boolean hasChildren()
	{
		return !children.isEmpty();
	}

	/**
	 *
	 */
	public Iterator getChildInitializerIterator()
	{
		return children.iterator();
	}

	/**
	 *
	 */
	public Iterator getSubDefinitionsIterator()
	{
		IteratorList iterList = new IteratorList();

		iterList.add(super.getSubDefinitionsIterator());
		
		addDefinitionIterators(iterList, getChildInitializerIterator());

		return iterList.toIterator();
	}

	/**
	 * override hasBindings to check children
	 */
	public boolean hasBindings()
	{
		return bindingsOnly(children().iterator()).hasNext() || super.hasBindings();
	}

}
