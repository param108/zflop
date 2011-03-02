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

package flex2.tools;

import flex2.compiler.common.Configuration;
import flex2.compiler.common.ConfigurationPathResolver;
import flex2.compiler.common.ConfigurationException;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.config.FileConfigurator;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;
import flash.util.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Brian Deitte
 */
public class CompcConfiguration extends ToolsConfiguration
{
	public static Map getAliases()
    {
        // FIXME: would make more sense to have these as part of ConfigurationInfo
        Map map = new HashMap();
        map.put( "o", "output" );
        map.put( "ic", "include-classes" );
        map.put( "in", "include-namespaces" );
        map.put( "is", "include-sources" );
        map.put( "if", "include-file" );
		map.put( "ir", "include-resource-bundles" );
	    map.putAll(Configuration.getAliases());
		return map;
    }

    public void validate( ConfigurationBuffer cfgbuf ) throws flex2.compiler.config.ConfigurationException
    {
    	super.validate( cfgbuf );
    	
        if (dumpConfigFile != null)
        {
            ThreadLocalToolkit.log(new Compiler.DumpConfig( dumpConfigFile ));
            File f = new File(dumpConfigFile);
            // fixme - nuke the private string for the localization prefix.
            String text = FileConfigurator.formatBuffer(cfgbuf, "flex-config", ThreadLocalToolkit.getLocalizationManager(), "flex2.configuration" );
            try
            {
                PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)));
                out.write(text);
                out.close();
            }
            catch (Exception e)
            {
                throw new flex2.compiler.config.ConfigurationException.IOError(dumpConfigFile);
            }
        }

        if (getIncludeSources().isEmpty() && getClasses().isEmpty() && getNamespaces().isEmpty()
            && ((getCompilerConfiguration().getIncludeLibraries() == null) || (getCompilerConfiguration().getIncludeLibraries().length == 0))
			&& getFiles().isEmpty() && getIncludeResourceBundles().isEmpty())
	    {
		    throw new ConfigurationException.NoSwcInputs( null, null, -1 );
        }

        if (getCompilerConfiguration().keepGeneratedActionScript())
        {
            String output = getOutput();
            assert output != null;

            String canonical = FileUtils.canonicalPath( output );

            if (canonical != null)
                output = canonical;

            String parent = new File( output ).getParent();

            String generated = null;
            if (parent == null)
            {
                generated = new File( "generated" ).getAbsolutePath();
            }
            else
            {
                generated = FileUtils.addPathComponents( parent, "generated", File.separatorChar );
            }
            getCompilerConfiguration().setGeneratedDirectory( generated );
            new File( generated ).mkdirs();
        }
    }

    //
    // 'directory' option
    //
    
    private boolean isDirectory;

    public boolean isDirectory()
    {
        return isDirectory;
    }

    public void cfgDirectory(ConfigurationValue val, boolean directory)
    {
        this.isDirectory = directory;
    }

    //
    // 'dump-config' option
    //
    
    private String dumpConfigFile = null;

	public String getDumpConfig()
	{
		return dumpConfigFile;
	}

    public void cfgDumpConfig(ConfigurationValue cv, String filename) throws flex2.compiler.config.ConfigurationException
    {
        dumpConfigFile = filename;
        // can't print here, we want to aggregate all the settings found and then print.
    }

    public static ConfigurationInfo getDumpConfigInfo()
    {
        return new ConfigurationInfo( 1, "filename" )
        {
            public boolean isAdvanced()
			{
				return true;
			}

            public boolean isDisplayed()
            {
                return false;
            }
        };
    }

	//
	// 'help' option
	//
	
    // dummy, just a trigger for help text
    public void cfgHelp(ConfigurationValue cv, String[] keywords)
    {
        // intercepted upstream in order to allow help text to be printed even when args are otherwise bad
    }
    
    public static ConfigurationInfo getHelpInfo()
    {
        return new ConfigurationInfo( -1, "keyword" )
        {
            public boolean isGreedy()
			{
				return true;
			}

            public boolean isDisplayed()
			{
				return false;
			}
        };
    }

    //
	// 'include-classes' option
	//
	
    private List classes = new LinkedList();

    public List getClasses()
    {
        return classes;
    }

    public void cfgIncludeClasses(ConfigurationValue cv, List args) throws flex2.compiler.config.ConfigurationException
    {
        classes.addAll( toQNameString(args) );
    }

    public static ConfigurationInfo getIncludeClassesInfo()
    {
        return new ConfigurationInfo( -1, new String[] { "class" } )
        {
            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

	//
	// 'include-file' option
	//
	
    private Map files = new HashMap();

    public Map getFiles()
    {
        return files;
    }

    public void addFiles(Map f)
    {
        files.putAll(f);
    }

    public void cfgIncludeFile( ConfigurationValue cfgval, String name, String path)
            throws flex2.compiler.config.ConfigurationException
    {
        if (files.containsKey(name))
        {
            throw new ConfigurationException.RedundantFile(name, cfgval.getVar(), cfgval.getSource(), cfgval.getLine() );
        }
        VirtualFile f = ConfigurationPathResolver.getVirtualFile( path, configResolver, cfgval );
        files.put(name, f);
    }

    public static ConfigurationInfo getIncludeFileInfo()
    {
        return new ConfigurationInfo( new String[] {"name", "path"} )
        {
            public boolean isPath()
            {
                return true;
            }

            public boolean allowMultiple()
			{
				return true;
			}
        };
    }

	//
	// 'include-stylesheet' option
	//
	
    private Map stylesheets = new HashMap();

    public Map getStylesheets()
    {
        return stylesheets;
    }

    public void addStylesheets(Map f)
    {
        stylesheets.putAll(f);
    }

    public void cfgIncludeStylesheet( ConfigurationValue cfgval, String name, String path)
            throws flex2.compiler.config.ConfigurationException
    {
        if (stylesheets.containsKey(name))
        {
            throw new ConfigurationException.RedundantFile(name, cfgval.getVar(), cfgval.getSource(), cfgval.getLine() );
        }
        VirtualFile f = ConfigurationPathResolver.getVirtualFile( path, configResolver, cfgval );
        stylesheets.put(name, f);
    }

    public static ConfigurationInfo getIncludeStylesheetInfo()
    {
        return new ConfigurationInfo( new String[] {"name", "path"} )
        {
            public boolean isPath()
            {
                return true;
            }

            public boolean allowMultiple()
			{
				return true;
			}
        };
    }

	//
	// 'include-lookup-only' option
	//
	
	private boolean includeLookupOnly = false;

	public boolean getIncludeLookupOnly()
	{
		return includeLookupOnly;
	}

	/**
	 * include-lookup-only (hidden)
	 * if true, manifest entries with lookupOnly=true are included in SWC catalog. default is false.
	 * This exists only so that manifests can mention classes that come in from filespec rather than classpath,
	 * e.g. in playerglobal.swc.
	 * TODO could make this a per-namespace setting. Or, if we modify catalog-builder to handle defs from filespecs,
	 * could remove it entirely.
	 */
	public void cfgIncludeLookupOnly(ConfigurationValue val, boolean includeLookupOnly)
	{
		this.includeLookupOnly = includeLookupOnly;
	}

	public static ConfigurationInfo getIncludeLookupOnlyInfo()
	{
		return new ConfigurationInfo()
		{
			public boolean isAdvanced()
			{
				return true;
			}
		};
	}

	//
    // 'include-namespaces' option
    //
    
    private List namespaces = new LinkedList();

    public List getNamespaces()
    {
        return namespaces;
    }

    public void cfgIncludeNamespaces(ConfigurationValue val, List includeNamespaces)
    {
        namespaces.addAll(includeNamespaces);
    }

    public static ConfigurationInfo getIncludeNamespacesInfo()
    {
        return new ConfigurationInfo( -1, new String[] { "uri" } )
        {
            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'include-resource-bundles' option
    //
    
	private List resourceBundles = new LinkedList();

    public List getIncludeResourceBundles()
	{
		return resourceBundles;
	}

	public void cfgIncludeResourceBundles(ConfigurationValue val, List includeResourceBundles)
	{
		resourceBundles.addAll(toQNameString(includeResourceBundles));
	}

	public static ConfigurationInfo getIncludeResourceBundlesInfo()
	{
		return new ConfigurationInfo( -1, new String[] { "bundle" } )
		{
			public boolean allowMultiple()
			{
				return true;
			}
		};
	}
	
    //
	// 'include-sources' option
	//
	
    private List sources = new LinkedList();
 
    public List getIncludeSources()
    {
        return sources;
    }

    public void cfgIncludeSources(ConfigurationValue cv, List args) throws flex2.compiler.config.ConfigurationException
    {
        sources.addAll( args );
    }

    public static ConfigurationInfo getIncludeSourcesInfo()
    {
        return new ConfigurationInfo( -1, new String[] { "path-element" } )
        {
            public boolean allowMultiple()
            {
                return true;
            }

            public boolean isPath()
            {
                return true;
            }
        };
    }

	//
	// 'load-config' option
	//
	
    // dummy, ignored - pulled out of the buffer
    public void cfgLoadConfig(ConfigurationValue cv, String filename) throws flex2.compiler.config.ConfigurationException
    {
    }

    public static ConfigurationInfo getLoadConfigInfo()
    {
        return new ConfigurationInfo( 1, "filename" )
        {
            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
	// 'output' option
	//
	
	private String output;

	public String getOutput()
    {
        return output;
    }

    public void cfgOutput(ConfigurationValue val, String output) throws flex2.compiler.config.ConfigurationException
    {
		if (output != null && (output.startsWith(File.separator) || output.startsWith("/") || FileUtils.isAbsolute(new File(output))))
		{
			this.output = output;
		}
		else if (val.getContext() != null)
        {
            this.output = FileUtils.addPathComponents( val.getContext(), output, File.separatorChar );
        }
        else
        {
            this.output = output;
        }

        if (isDirectory)
        {
            File d = new File( this.output );

            if (d.exists())
            {
                if (!d.isDirectory())
                    throw new ConfigurationException.NotDirectory( this.output, val.getVar(), val.getSource(), val.getLine() );
                else
                {
                    File[] fl = d.listFiles();
                    if ((fl != null) && (fl.length > 0))
                    {
                        throw new ConfigurationException.DirectoryNotEmpty(this.output, val.getVar(), val.getSource(), val.getLine() );
                    }
                }
            }
        }
    }

    public static ConfigurationInfo getOutputInfo()
    {
        return new ConfigurationInfo(1, "filename")
        {
            public String[] getPrerequisites()
            {
                return new String[] {"directory"} ;
            }

            public boolean isRequired()
            {
                return true;
            }
        };
    }

    //
    // 'root' option
    //
     
	public void cfgRoot(ConfigurationValue val, String rootStr)
            throws flex2.compiler.config.ConfigurationException
    {
        throw new ConfigurationException.ObsoleteVariable( "source-path", val.getVar(),
                                                            val.getSource(), val.getLine() );
    }

    public static ConfigurationInfo getRootInfo()
    {
        return new ConfigurationInfo()
        {
            public boolean isAdvanced()
            {
                return true;
            }

            public boolean isHidden()
            {
                return true;
            }
        };
    }

	//
	// 'version' option
	//
	
    // dummy, just a trigger for version info
    public void cfgVersion(ConfigurationValue cv, boolean dummy)
    {
        // intercepted upstream in order to allow version into to be printed even when required args are missing
    }

    
	//
	// compute-digest option
	//
	
	private boolean computeDigest = true;
	
	public boolean getComputeDigest()
	{
		return computeDigest;
	}
	
	/**
	 * compute-digest option
	 * 
	 * @param cv
	 * @param b
	 */
	public void cfgComputeDigest(ConfigurationValue cv, boolean b)
	{
		computeDigest = b;
	}
}
