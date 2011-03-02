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

package flex2.compiler.swc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;

import flash.util.Trace;
import flash.util.FileUtils;
import flash.util.LRUCache;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 * Loads, stores, and caches SWCs.  This is the class to start with when working with SWCs.  There's only
 * two public methods for SwcCache, one for getting SWCs and one for saving SWCs- getSwcGroup() and export().
 *
 * SwcCache takes care of caching and synchronization.  There should be only one SwcCache.
 *
 * The class has two levels of caching.  There is a cache of SWCs as well as a cache of directories of SWCs.
 * When a directory is asked for, we check the timestamp and return the cached value if its the same.  If its
 * not, then we look for SWCs in the directory.  If we have a cached SWC, we check the timestampe and return
 * the cache SWC if its the same.
 *
 * For now, we are just synchronizing on the whole cache on each get or export.  We could add more logic to do
 * read/write synchronization on specific SwcGroups.  From tests though it looks like the two layers of caching will
 * provide enough for performance.
 *
 * @author Brian Deitte
 */
public class SwcCache
{
    public static int CACHE_INITIAL_SIZE = 20;
    public static int CACHE_MAX_SIZE = 50;

    public static final String SWC_EXTENSION = ".swc";
    public static final String GENSWC_EXTENSION = "-generated.swc";

    // cache of Swcs, with the key in the map being the path of the SWC
    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected SwcLRUCache swcs = new SwcLRUCache();

    // for testing under load
    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected boolean useCache = true;

	// changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
	protected boolean lazyRead = false;

    /**
     * Loads the current path SWCs and returns a SwcGroup. There will be one SwcGroup per compile,
     * and this is the piece that the compiler will mostly deal with for SWCs.
     */
    public SwcGroup getSwcGroup( VirtualFile[] paths )
    {
        // fixme - this could be improved.
        String[] urls = new String[paths.length];
        for (int i = 0; i < paths.length; ++i)
            urls[i] = paths[i].getName();
        return getSwcGroup( urls );
    }

    public synchronized SwcGroup getSwcGroup(String[] paths)
    {
        SwcGroup group;
        String path;
        Map swcs = new LinkedHashMap();
        for (int i = 0; i < paths.length; i++)
        {
            path = paths[i];
            swcs.putAll(getSwcs(path));
        }
        group = new SwcGroup(swcs);

        return group;
    }

    // todo - this could be made much more efficient by avoiding re-merging swcs
    public synchronized SwcGroup getSwcGroup(List groups)
    {
        Map swcs = new LinkedHashMap();
        for (Iterator it = groups.iterator(); it.hasNext();)
        {
            SwcGroup g = (SwcGroup) it.next();
            if (g != null)
            {
                swcs.putAll( g.getSwcs() );
            }
        }
        SwcGroup group = null;
        if (swcs.size() > 0)
        {
            group = new SwcGroup( swcs );
        }
        return group;
    }

    /**
     * Saves the given SWC to disk and adds to the cache
     */
    public synchronized boolean export(Swc swc)
            throws FileNotFoundException, IOException
    {
        try
        {
            if (! swc.save())
            {
	            return false;
            }

            if (Trace.swc)
            {
                Trace.trace("Exported SWC " + swc.getLocation() + "(" + swc.getLastModified() + ")");
            }

            // add to Swc cache
            swcs.put(swc.getLocation(), swc);
        }
        catch (Exception e)
        {
            if (Trace.error)
            {
                e.printStackTrace();
            }
	        if (e instanceof SwcException)
	        {
	        	throw (SwcException) e;
	        }
	        else
	        {
	        	SwcException ex = new SwcException.SwcNotExported(swc.getLocation(), e);
	        	ThreadLocalToolkit.log(ex);
	        	throw ex;
	        }
        }
	    return true;
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected Map getSwcs(String path)
    {
        Map map = new LinkedHashMap();
		File f = new File(path);
        if (!f.exists())
        {
            throw new SwcException.SwcNotFound(path);
        }
        File catalog = new File( FileUtils.addPathComponents( path, Swc.CATALOG_XML, File.separatorChar ) );

        if (!f.isDirectory() || catalog.exists())
        {
            Swc swc = getSwc( f );
            if (swc != null)
            {
                map.put( swc.getLocation(), swc );
            }
        }
        else
        {
            File[] files = FileUtils.listFiles( f );
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];

                // we don't want to snarf an entire directory tree, just a single level.
                if ((!file.isDirectory()) && file.canRead())
                {
                    String lowerCase = file.getName().toLowerCase();

                    if (lowerCase.endsWith( GENSWC_EXTENSION ))   // never automatically read genswcs
                        continue;

                    if (lowerCase.endsWith( SWC_EXTENSION ))
                    {
                        Swc swc = getSwc( file );
                        if (swc != null)
                        {
	                        map.put( swc.getLocation(), swc );
                        }
                    }
                }
            }
        }
        return map;
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected Swc getSwc(File file)
    {
        Swc swc;
        try
        {
            String location = FileUtils.canonicalPath(file);
            swc = (Swc)swcs.get(location);

            long lastModified = file.lastModified();
            if (swc == null || (lastModified != swc.getLastModified()))
            {
                if (Trace.swc)
                {
                    Trace.trace("Getting new version of " + location + " (" + (swc == null ? "null" : swc.getLocation()) +
                            ", " + (swc == null ? -1 : swc.getLastModified()) + ", " + lastModified + ")");
                }

                SwcArchive archive = file.isDirectory()?
                        (SwcArchive) new SwcDirectoryArchive( location ) :
	                    lazyRead ? new SwcLazyReadArchive( location ) : new SwcDynamicArchive( location );

                swc = new Swc( archive, true );
                swc.setLastModified(lastModified);

                if (ThreadLocalToolkit.errorCount() > 0)
                {
                    swc = null;
                }
                else if (useCache)
                {
                    swcs.put(location, swc);
                }
            }
            else if (Trace.swc)
            {
                Trace.trace("Loading cached version of " + location);
            }
        }
        catch(Exception e)
        {
            if (Trace.error)
            {
                e.printStackTrace();
            }
	        SwcException.SwcNotLoaded ex = new SwcException.SwcNotLoaded(file.getName(), e);
	        ThreadLocalToolkit.log(ex);
	        throw ex;
        }
        return swc;
    }

	public void setLazyRead(boolean lazyRead)
	{
		this.lazyRead = lazyRead;
	}
    

    /**
     * Clean the swcs of any extra data that was added as a side
     * effect of the compilation process. The purpose of the clean
     * is to enable the reuse of the cache.
     */
    public void cleanExtraData()
    {
       for (Iterator iter = swcs.getSwcs().iterator(); iter.hasNext();)
       {
           Swc swc = (Swc)iter.next();
           for (Iterator libit = swc.getLibraryIterator(); libit.hasNext();)
           {
               SwcLibrary lib = (SwcLibrary) libit.next();

               for (Iterator scriptit = lib.getScriptIterator(); scriptit.hasNext();)
               {
                   SwcScript script = (SwcScript) scriptit.next();
                   Map miscData = script.getMiscData();
                   if (miscData != null)
                   {
                       miscData.clear();
                   }
               }
           }
       }
    }
    
    static class SwcLRUCache extends LRUCache
    {
        SwcLRUCache()
        {
            super(CACHE_INITIAL_SIZE, CACHE_MAX_SIZE);
        }

        protected Object fetch(Object key)
        {
            return null;
        }

        /**
         * Get a list of swcs in the cache.
         * 
         * @return a list of swcs, each of type Swc. 
         */
        public List getSwcs()
        {
            ArrayList swcs = new ArrayList(size());
            for (Iterator iter = entrySet().iterator(); iter.hasNext();)
            {
                Map.Entry entry = (Map.Entry)iter.next();
                LRUListEntry lruEntry = (LRUListEntry)entry.getValue();
                swcs.add(lruEntry.getValue());
            }
            
            return swcs;
        }
    }

}
