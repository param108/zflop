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

package flex2.compiler.css;

import flex2.compiler.Source;
import flex2.compiler.abc.MetaData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map of style names to metadata declarations
 * <p/>
 * Note: for now anyway, we simply point to the original metadata. Higher-level wrappers are implemented
 * by each compiler's reflection package. But clearly, if we throw exceptions on inequivalent adds (or generally do
 * anything more than a straight, low-level equality test when testing), then there's higher-level awareness here, and
 * we should factor out something like mxml.reflect.TypeTable.StyleHelper to here.
 * <p/>
 * Note: both old [Style] and new [StyleProperty] metadata formats may be stored.
 * <p/>
 */

public class Styles {
    private static final String FORMAT = "format";
    private static final String COLOR = "Color";
    private static final String INHERIT = "inherit";
    private static final String YES = "yes";

	private Map declMap;	//	Map<String, MetaData>
	private Map locationMap; // Map<String, Source>

	public Styles(int preferredSize)
	{
		declMap = new HashMap(preferredSize);
		locationMap = new HashMap(preferredSize);
	}

	public Styles() {
		this(16);
	}

	public int size()
	{
		return declMap.size();
	}

	public void addStyle(String name, MetaData md, Source source)
			throws StyleConflictException {
		if (isInherit(md) ? isNonInheritingStyle(name) : isInheritingStyle(name))
			throw new StyleConflictException(name, (Source)locationMap.get(name));
		declMap.put(name, md);
        locationMap.put(name, source);
	}

	public void addStyles(Styles styles)
			throws StyleConflictException {
		for (Iterator i = styles.declMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			String name = (String) e.getKey();
			addStyle(name, (MetaData) e.getValue(), styles.getLocation(name));
		}
	}

	public Source getLocation(String name) {
		return (Source) locationMap.get(name);
	}

	public MetaData getStyle(String name) {
		return (MetaData) declMap.get(name);
	}

	public boolean isInheritingStyle(String name) {
		MetaData md = getStyle(name);
		return md != null && isInherit(md);
	}

	public boolean isNonInheritingStyle(String name) {
		MetaData md = getStyle(name);
		return md != null && !isInherit(md);
	}

	private static boolean isInherit(MetaData md) {
		String inherit = md.getValue(INHERIT);
		return inherit != null && YES.equals(inherit);
	}

	public Iterator getStyleNames()
	{
		return declMap.keySet().iterator();
	}

    public Set getInheritingStyles()
    {
        Set result = new HashSet();
        Iterator iterator = getStyleNames();

        while ( iterator.hasNext() )
        {
            String styleName = (String) iterator.next();

            if (isInheritingStyle(styleName))
            {
                result.add(styleName);
            }
        }

        return result;
    }

	public void clear()
	{
		declMap.clear();
	}
}
