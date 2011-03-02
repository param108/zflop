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

import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
public class NamespacesConfiguration
{
	private ConfigurationPathResolver configResolver;

	public void setConfigPathResolver( ConfigurationPathResolver resolver )
	{
	    this.configResolver = resolver;
	}

	//
	// 'compiler.namespaces.namespace' option
	//
	
	private Map manifestMappings;

	public Map getManifestMappings() // Map<String, VirtualFile[]>
	{
		return manifestMappings;
	}

	public void setManifestMappings(Map manifestMappings)
	{
		this.manifestMappings = manifestMappings;
	}

	public VirtualFile[] getNamespace()
	{
		if (manifestMappings != null)
		{
			VirtualFile[] a = new VirtualFile[manifestMappings.size()];
			int j = 0;

			for (Iterator i = manifestMappings.values().iterator(); i.hasNext();)
			{
				a[j++] = (VirtualFile) i.next();
			}

			return a;
		}
		else
		{
			return null;
		}
	}
	
	public void cfgNamespace( ConfigurationValue cfgval, String uri, String manifest)
	    throws flex2.compiler.config.ConfigurationException
	{
	    if (manifest == null)
	    {
		    throw new ConfigurationException.CannotOpen( manifest, cfgval.getVar(), cfgval.getSource(), cfgval.getLine() );
	    }

        PathResolver resolver = ThreadLocalToolkit.getPathResolver();
        assert resolver != null && configResolver != null: "Path resolvers must be set before calling this method.";
        if (resolver == null || configResolver == null)
        {
            throw new ConfigurationException.CannotOpen( manifest, cfgval.getVar(), cfgval.getSource(), cfgval.getLine() );
        }

        VirtualFile file = ConfigurationPathResolver.getVirtualFile( manifest,
                                                                     configResolver,
                                                                     cfgval );

		if (manifestMappings == null)
		{
			manifestMappings = new HashMap();
		}

		manifestMappings.put(uri, file);
	}

	public static ConfigurationInfo getNamespaceInfo()
	{
	    return new ConfigurationInfo( new String[] {"uri", "manifest"} )
	    {
		    public boolean allowMultiple()
		    {
		    	return true;
		    }
	    };
	}
}
