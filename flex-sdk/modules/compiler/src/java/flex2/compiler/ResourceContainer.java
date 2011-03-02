////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler;

import flex2.compiler.util.QName;
import flex2.compiler.util.QNameMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Clement Wong
 */
public final class ResourceContainer
{
	public ResourceContainer()
	{
		name2source = new LinkedHashMap();
		qname2source = new QNameMap();
	}

	private Map name2source;
	private QNameMap qname2source;

	public Source addResource(Source s)
	{
		Source old = (Source) name2source.get(s.getName());
		CompilationUnit u = old != null ? old.getCompilationUnit() : null;

		if (u == null || 
			(u != null && !u.isDone()) || 
			(old.getLastModified() != s.getLastModified()) ||
			old.isUpdated(s))
		{
			s.setOwner(this);
			name2source.put(s.getName(), s);
			return s;
		}
		else // if (u != null && u.isDone())
		{
			return old.copy();
		}
	}

	public Source findSource(String name)
	{
		return checkSource((Source) name2source.get(name));
	}

	Source findSource(String namespaceURI, String localPart)
	{
		assert localPart.indexOf('.') == -1 && localPart.indexOf('/') == -1 && localPart.indexOf(':') == -1
                : "findSource(" + namespaceURI + "," + localPart + ") has bad localPart";

		return checkSource((Source) qname2source.get(namespaceURI, localPart));
	}

	private Source checkSource(Source s)
	{
		CompilationUnit u = s != null ? s.getCompilationUnit() : null;

		if ((u != null && !u.isDone()) || (s != null && s.isUpdated()))
		{
			// s.removeCompilationUnit();
		}
		else if (u != null)
		{
			s = s.copy();
			assert s != null;
		}

		return s;
	}

	public void refresh()
	{
		qname2source.clear();
		
		for (Iterator i = name2source.values().iterator(); i.hasNext();)
		{
			Source s = (Source) i.next();
			CompilationUnit u = s.getCompilationUnit();
			if (u != null)
			{
				for (int j = 0, size = u.topLevelDefinitions.size(); j < size; j++)
				{
					QName qName = (QName) u.topLevelDefinitions.get(j);
					qname2source.put(qName, s);
				}
			}
		}
	}

	Collection sources()
	{
		return name2source.values();
	}
}
