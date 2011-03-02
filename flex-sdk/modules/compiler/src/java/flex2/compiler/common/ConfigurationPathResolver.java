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

import flash.util.FileUtils;
import flash.util.Trace;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.File;

/**
 * Resolves files in a way specific to configuration files.  Or, as Roger, points out,
 * this could be renamed RelativePathResolver or something, since it just resolves things
 * relative to a root directory.
 */
public class ConfigurationPathResolver implements SinglePathResolver
{
    private String root;

    /**
     * Set default root file.  For mxmlc, we only want the root to ever be the context of
     * a ConfigurationValue.  For example, if a ConfigurationValue comes from
     * "C:/flex/flex-config.xml", the root should be "C:/flex".  If a ConfigurationValue
     * comes from the command line, the root should be null.
     *
     * This method is public, because it's used by Flex Builder.
     */
    public void setRoot( String root )
    {
        this.root = root;
    }

    /**
     * Resolve the path as an absolute file or relative to the root or relative to the
     * current working directory if the root is null.
     */
    public VirtualFile resolve( String path )
    {
        VirtualFile resolved = null;

        File absoluteOrRelativeFile = FileUtil.openFile(path);

        if ((absoluteOrRelativeFile != null) &&
            FileUtils.exists(absoluteOrRelativeFile) &&
            FileUtils.isAbsolute(absoluteOrRelativeFile))
        {
            resolved = new LocalFile(absoluteOrRelativeFile);
        }
        else if (root != null)
        {
            String rootRelativePath = root + File.separator + path;
            File rootRelativeFile = FileUtil.openFile(rootRelativePath);
            if ((rootRelativeFile != null) && FileUtils.exists(rootRelativeFile))
            {
                resolved = new LocalFile(rootRelativeFile);
            }
        }
        else
        {
        	// C: must convert 'absoluteOrRelativeFile' into absolute before calling exists().
            absoluteOrRelativeFile = FileUtils.getAbsoluteFile(absoluteOrRelativeFile);
            if ((absoluteOrRelativeFile != null) &&
                FileUtils.exists(absoluteOrRelativeFile))
                // && !FileUtils.isAbsolute(absoluteOrRelativeFile)
            {
            	resolved = new LocalFile(absoluteOrRelativeFile);
            }
        }

        if ((resolved != null) && Trace.pathResolver)
        {
            Trace.trace("ConfigurationPathResolver.resolve: resolved " + path + " to " + resolved.getName());
        }

        return resolved;
    }

    
    // This should be moved, simplified, destroyed, something.

    public static VirtualFile getVirtualFile(String file,
                                             ConfigurationPathResolver configResolver,
                                             ConfigurationValue cfgval)
        throws flex2.compiler.config.ConfigurationException
    {
        ConfigurationPathResolver relative = null;
        String cfgContext = cfgval != null ? cfgval.getContext() : null;
        if (cfgContext != null)
        {
            relative = new ConfigurationPathResolver();
            relative.setRoot( cfgContext );
        }

        // check the PathResolver first and if nothing is found, then check the config
        // resolver.  This is done so that Zorn/WebTier can resolve these files as they
        // wish
        VirtualFile vFile = ThreadLocalToolkit.getPathResolver().resolve(relative, file);

        if (vFile == null)
        {
            String oldRoot = null;
            boolean rootChanged = false;
            try
            {   
                // If there is a configuration context for the configResolver, then use it.
                // If there is no context, then let the configResolver use its own root.
                if (cfgContext != null)
                {
                    oldRoot = configResolver.root;
                    rootChanged = true;
                    configResolver.setRoot(cfgContext);
                }
                vFile = configResolver.resolve(file);
            }
            finally
            {
                if (rootChanged)
                {
                    configResolver.setRoot(oldRoot);                
                }
            }
        }
        if (vFile == null)
        {
	        if (cfgval == null)
	        {
		        throw new ConfigurationException.CannotOpen( file, null, null, -1 );   
	        }
	        else
	        {
		        throw new ConfigurationException.CannotOpen( file, cfgval.getVar(), cfgval.getSource(), cfgval.getLine() );
	        }
        }
        return vFile;
    }




}
