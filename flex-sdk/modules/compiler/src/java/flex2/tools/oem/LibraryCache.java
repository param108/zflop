////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem;

import flex2.compiler.swc.SwcCache;

/**
 * A cache of library files that is narrowly designed to be used to compile Application 
 * objects that use the same library path.
 *
 * @since 3.0
 * @author dloverin
 */
public class LibraryCache
{
    private SwcCache swcCache;      // the cache that does all the work.
    
    
    public LibraryCache()
    {
    }
    
    /**
     * Remove data that is put in the cache as a result of the 
     * compilation process. After this method returns the 
     * cache my be used in another build.
     *
     */
    public void cleanExtraData()
    {
        if (swcCache != null)
        {
            swcCache.cleanExtraData();
        }
    }


    /**
     * Get the SwcCache current being used by this class.
     * 
     * @return the current SwcCache.
     */
    SwcCache getSwcCache()
    {
        return swcCache;
    }


    /**
     * Set the swcCache to be used by this cache. The reference to the
     * previous cache is overwritten.
     * 
     * @param swcCache the new SwcCache object.
     */
    void setSwcCache(SwcCache swcCache)
    {
        this.swcCache = swcCache;
    }
}
