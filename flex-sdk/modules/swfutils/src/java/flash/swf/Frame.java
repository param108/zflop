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

package flash.swf;

import flash.swf.tags.DefineTag;
import flash.swf.tags.FrameLabel;
import flash.swf.tags.SymbolClass;
import flash.swf.tags.DefineFont;

import java.util.*;

/**
 * one SWF frame.  each frame runs its initActions, doActions, and control
 * tags in a specific order, so we group them this way while forming the movie.
 *
 * @author Edwin Smith
 */
public class Frame
{
	public List doActions;		// DoAction[]
	public List controlTags;	// Tag[]
	public FrameLabel label;
	public List imports;        // ImportAssets[]
	public int pos = 1;	

	private Map exports;		// String -> DefineTag
	private List exportDefs;

	public List doABCs;		// DoABC

	public SymbolClass symbolClass;

	public List fonts;

	public Frame()
	{
		exports = new HashMap();
		exportDefs = new ArrayList();
		doActions = new ArrayList();
		controlTags = new ArrayList();
		imports = new ArrayList();
		fonts = new ArrayList();

		doABCs = new ArrayList();
		symbolClass = new SymbolClass();
	}

	public Iterator getReferences()
	{
		ArrayList list = new ArrayList();

		// exported symbols
		for (Iterator j = exportDefs.iterator(); j.hasNext();)
		{
			DefineTag def = (DefineTag) j.next();
			list.add(def);
		}

        list.addAll( symbolClass.class2tag.values() );

		// definitions for control tags
		for (Iterator j = controlTags.iterator(); j.hasNext();)
		{
			Tag tag = (Tag) j.next();
			for (Iterator k = tag.getReferences(); k.hasNext();)
			{
				DefineTag def = (DefineTag) k.next();
				list.add(def);
			}
		}

		return list.iterator();
	}

    public void mergeSymbolClass(SymbolClass symbolClass)
    {
        this.symbolClass.class2tag.putAll( symbolClass.class2tag );
    }
	public void addSymbolClass(String className, DefineTag symbol)
	{      
        DefineTag tag = (DefineTag)symbolClass.class2tag.get(className);
        // FIXME: error below should be possible... need to figure out why it is happening when running 'ant frameworks'
        //if (tag != null && ! tag.equals(symbol))
        //{
        //    throw new IllegalStateException("Attempted to define SymbolClass for " + className + " as both " +
        //            symbol + " and " + tag);
        //}
        this.symbolClass.class2tag.put( className, symbol );
	}

	public boolean hasSymbolClasses()
	{
		return !symbolClass.class2tag.isEmpty();
	}

	public void addExport(DefineTag def)
	{
		Object old = exports.put(def.name, def);
		if (old != null)
		{
			exportDefs.remove(old);
		}
		exportDefs.add(def);
	}

	public boolean hasExports()
	{
		return !exports.isEmpty();
	}

	public Iterator exportIterator()
	{
		return exportDefs.iterator();
	}

	public void removeExport(String name)
	{
		Object d = exports.remove(name);
		if (d != null)
		{
			exportDefs.remove(d);
		}
	}

	public void setExports(Map definitions)
	{
		for (Iterator i = definitions.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry entry = (Map.Entry) i.next();
			DefineTag def = (DefineTag) entry.getValue();
			addExport(def);
		}
	}

	public boolean hasFonts()
	{
		return !fonts.isEmpty();
	}

	public void addFont(DefineFont tag)
	{
		fonts.add(tag);
	}

	public Iterator fontsIterator()
	{
		return fonts.iterator();
	}
}
