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

package flex2.compiler.swc.catalog;

import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * Context that allows for retrieval of current element and parent elements
 *
 * @author Brian Deitte
 */
public class ReadContext
{
    private String currentName;
    private Attributes currentAttributes;
    private Stack parents = new Stack();
    private Stack parentNames = new Stack();

    public String getCurrentName()
    {
        return currentName;
    }

    public Attributes getCurrentAttributes()
    {
        return currentAttributes;
    }

    public CatalogReadElement getCurrentParent()
    {
        return parents.size() == 0 ? null : (CatalogReadElement)parents.peek();
    }

    public void setCurrent(String element, Attributes attributes)
    {
        currentName = element;
        currentAttributes = attributes;
    }

    public void setCurrentParent(CatalogReadElement currentParent, String element)
    {
        parents.push(currentParent);
        parentNames.push(element);
    }

    public void clearCurrentParent(String element)
    {
        String name = parentNames.size() == 0 ? null : (String)parentNames.peek();
        if (element.equals(name))
        {
            parents.pop();
            parentNames.pop();
        }
    }

    public void clear()
    {
        currentName = null;
        currentAttributes = null;

        assert(parents.size() == 0);
        assert(parentNames.size() == 0);

        parents.clear();
        parentNames.clear();
    }
}
