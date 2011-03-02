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

package flex2.compiler.common;

import flex2.compiler.io.VirtualFile;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * A file resolution layer above VirtualFile.resolve() that allows for multiple resolves() to be used as needed.
 *
 * @author Brian Deitte
 */
public class PathResolver implements SinglePathResolver
{
    private List bases;

    /**
     * Resolve the current file used the registered SinglePathResolvers
     */
    public VirtualFile resolve( String pathStr )
    {
        return resolve((SinglePathResolver)null, pathStr);
    }

    /**
     * Resolve the current file, checking the passed in resolver first
     */
    public VirtualFile resolve( SinglePathResolver resolver, String pathStr )
    {
        if (pathStr == null)
        {
            return null;
        }

        VirtualFile virt = null;
        if (resolver != null)
        {
            virt = resolver.resolve(pathStr);
        }
        if (virt == null)
        {
            virt = checkSinglePathResolvers(virt, pathStr);
        }

        return virt;
    }

    /**
     * Resolve the current file, checking the passed in resolvers first
     */
    public VirtualFile resolve( SinglePathResolver[] resolvers, String pathStr )
    {
        if (pathStr == null)
        {
            return null;
        }

        VirtualFile virt = null;
        if (resolvers != null)
        {
            for (int i = 0; i < resolvers.length; i++)
            {
                SinglePathResolver resolver = resolvers[i];
                virt = resolver.resolve(pathStr);
                if (virt != null)
                    break;
            }
        }
        if (virt == null)
        {
            virt = checkSinglePathResolvers(virt, pathStr);
        }

        return virt;
    }

    /**
     * Add a resolver that will be used in all resolves
     */
    public void addSinglePathResolver( SinglePathResolver resolver )
    {
        int index = 0;

        if (bases != null)
        {
            index = bases.size();
        }

        addSinglePathResolver(index, resolver);
    }

    /**
     * Add a resolver that will be used in all resolves
     */
    public void addSinglePathResolver( int index, SinglePathResolver resolver )
    {
        assert resolver != null;

        if (bases == null)
        {
            bases = new ArrayList();
        }

        bases.add(index, resolver);
    }

    private VirtualFile checkSinglePathResolvers(VirtualFile virt, String pathStr)
    {
        if (bases != null)
        {
            // fixme: should we just grab the first one or check mod times?
            for (Iterator iterator = bases.iterator(); iterator.hasNext();)
            {
                SinglePathResolver baseResolver = (SinglePathResolver)iterator.next();
                virt = baseResolver.resolve(pathStr);
                if (virt != null)
                {
                    break;
                }
            }
        }
        return virt;
    }

    public void removeSinglePathResolver( SinglePathResolver resolver )
    {
        for (Iterator iterator = bases.iterator(); iterator.hasNext();)
        {
            // Intentionally using == here.
            if (iterator.next() == resolver)
            {
                iterator.remove();
                return;
            }
        }
    }
}
