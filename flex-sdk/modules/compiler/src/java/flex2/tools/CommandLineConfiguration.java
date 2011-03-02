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

import flash.util.FileUtils;
import flex2.compiler.API;
import flex2.compiler.common.Configuration;
import flex2.compiler.common.ConfigurationException;
import flex2.compiler.common.ConfigurationPathResolver;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.config.FileConfigurator;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roger Gonzalez
 * @author Clement Wong
 */
public class CommandLineConfiguration extends ToolsConfiguration
{
	private String resourceModulePath;

	public String getTargetFile()
	{
        if (compilingResourceModule())
		{
			return resourceModulePath;
		}
		
		return (fileSpecs.size() > 0) ? (String) fileSpecs.get( fileSpecs.size() - 1 ) : null;
	}

	public List getFileList()
	{
        if (compilingResourceModule())
		{
			List fileList = new ArrayList();
			fileList.add(resourceModulePath);
			return fileList;
		}

		return fileSpecs;
	}

	public boolean compilingResourceModule()
	{
		boolean b = fileSpecs.size() == 0 && getIncludeResourceBundles().size() > 0;
		if (b && resourceModulePath == null)
		{
			resourceModulePath = I18nUtils.getGeneratedResourceModule(this).getPath();
		}
		return b;
	}

	public void validate(ConfigurationBuffer cfgbuf) throws flex2.compiler.config.ConfigurationException
	{
        super.validate( cfgbuf );

        if (dumpConfigFile != null)
        {
            ThreadLocalToolkit.log(new Compiler.DumpConfig( dumpConfigFile ));
            File f = new File(dumpConfigFile);
            // fixme - nuke the private string for the localization prefix...
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

        String targetFile = getTargetFile();
		if (targetFile == null)
		{
    	    throw new ConfigurationException.MustSpecifyTarget( null, null, -1);
		}

        VirtualFile virt = getVirtualFile(targetFile);
        if (virt == null && checkTargetFileInFileSystem())
        {
            throw new flex2.compiler.config.ConfigurationException.IOError(targetFile);
        }

        if (virt != null && getCompilerConfiguration().keepGeneratedActionScript())
        {
            String dir = FileUtils.addPathComponents( virt.getParent(), "generated", File.separatorChar );
            getCompilerConfiguration().setGeneratedDirectory( FileUtils.canonicalPath( new File( dir ) ) );
            File gd = new File( getCompilerConfiguration().getGeneratedDirectory() );
            gd.mkdirs();
        }
	}

	/**
	 * Subclass could override this method.
	 */
	protected VirtualFile getVirtualFile(String targetFile) throws flex2.compiler.config.ConfigurationException
	{
		return API.getVirtualFile(targetFile);
	}
	
	/**
	 * Subclass could override this method.
	 */
	protected boolean checkTargetFileInFileSystem()
	{
		return true;
	}
	
    private VirtualFile getVirtualFile(String file, ConfigurationValue cfgval)
    {
    	try
    	{
    		return ConfigurationPathResolver.getVirtualFile( file, configResolver, cfgval );
    	}
    	catch (flex2.compiler.config.ConfigurationException ex)
    	{
    		return null;
    	}
    }

	//
	// 'dump-config-file' option
	//

    private String dumpConfigFile = null;

	public String getDumpConfig()
	{
		return dumpConfigFile;
	}

	public void cfgDumpConfig(ConfigurationValue cv, String filename) throws flex2.compiler.config.ConfigurationException
	{
		dumpConfigFile = Configuration.getOutputPath(cv, filename);
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
	// 'file-specs' option
	//
	
	// list of filespecs, default var for command line
	private List fileSpecs = new ArrayList();

	public List getFileSpecs()
	{
		return fileSpecs;
	}

	public void cfgFileSpecs(ConfigurationValue cv, List args) throws flex2.compiler.config.ConfigurationException
	{
		this.fileSpecs.addAll( args );
	}

    public static ConfigurationInfo getFileSpecsInfo()
    {
        return new ConfigurationInfo( -1, new String[] { "path-element" } )
        {
            public boolean allowMultiple()
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
	// 'help' option
	//
	
    // dummy, just a trigger for help text
	public void cfgHelp(ConfigurationValue cv, String[] keywords)
	{

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
	// 'load-config' option
	//
	
	private VirtualFile configFile;

	public VirtualFile getLoadConfig()
	{
		return configFile;
	}

	// dummy, ignored - pulled out of the buffer
	public void cfgLoadConfig(ConfigurationValue cv, String filename) throws flex2.compiler.config.ConfigurationException
	{
		// C: resolve the flex-config.xml path to a VirtualFile so incremental compilation can detect timestamp change.
		configFile = ConfigurationPathResolver.getVirtualFile(filename,
		                                                      configResolver,
		                                                      cv);
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
	
	private String output = null;

	public String getOutput()
	{
	    return output;
	}

	public void cfgOutput(ConfigurationValue val, String output) throws flex2.compiler.config.ConfigurationException
	{
        this.output = Configuration.getOutputPath(val, output);
	}

	public static ConfigurationInfo getOutputInfo()
	{
	    return new ConfigurationInfo(1, "filename")
	    {
	        public boolean isRequired()
	        {
	            return false;
	        }
	    };
	}

	//
	// 'projector' option (hidden)
	//
	
    private VirtualFile projector;

    public VirtualFile getProjector()
    {
        return projector;
    }

	public void cfgProjector( ConfigurationValue cfgval, String path )
	{
		projector = getVirtualFile(path, cfgval);
	}

    public static ConfigurationInfo getProjectorInfo()
    {
        return new ConfigurationInfo()
        {
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

}
