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

package flex2.compiler.swc;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Roger Gonzalez
 */
public class SwcDependencySet
{
    public final static String INHERITANCE = "i";
    public final static String NAMESPACE = "n";
    public final static String SIGNATURE = "s";
    public final static String EXPRESSION = "e";

    public void addDependency( String type, String dep )
    {
        Set deps = (Set) depTypeMap.get( type );

        if (deps == null)
        {
            deps = new HashSet();
            depTypeMap.put( type, deps );
        }
        deps.add( dep );
    }

    public void addDependencies( String type, Iterator deps )
    {
        while (deps.hasNext())
        {
            addDependency( type, (String) deps.next() );
        }
    }

    private Set getDependencies( String type )
    {
        return (Set) depTypeMap.get( type );
    }

    public Iterator getDependencyIterator( String type )
    {
        Set deps = getDependencies( type );
        return (deps == null)? null : deps.iterator();
    }

    public Iterator getTypeIterator()
    {
        return depTypeMap.keySet().iterator();
    }

    private Map depTypeMap = new HashMap();
}
