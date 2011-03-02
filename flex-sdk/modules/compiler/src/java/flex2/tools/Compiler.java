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

import flash.localization.LocalizationManager;
import flash.localization.ResourceBundleLocalizer;
import flash.localization.XLRLocalizer;
import flash.swf.Movie;
import flex2.compiler.*;
import flex2.compiler.common.*;
import flex2.compiler.config.*;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.SwcCache;
import flex2.compiler.swc.SwcException;
import flex2.compiler.util.Benchmark;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.linker.ConsoleApplication;
import flex2.linker.LinkerException;

import java.io.*;
import java.util.*;

/**
 * @author Clement Wong
 */
public final class Compiler extends Tool
{
    public static final String FILE_SPECS = "file-specs";

    /**
     * The entry-point for Mxmlc.
     * Note that if you change anything in this method, make sure to check Compc, Shell, and
     * the server's CompileFilter to see if the same change needs to be made there.  You
     * should also inform the Zorn team of the change.
     *
     * @param args
     */
    public static void main(String[] args)
    {
	    mxmlc(args);
	    System.exit(ThreadLocalToolkit.errorCount());
    }

    public static void mxmlc(String[] args)
    {
        flex2.compiler.Transcoder[] transcoders = null;
        
        try
        {
            flex2.compiler.API.useAS3();

            // setup the path resolver
            flex2.compiler.API.usePathResolver();

            // set up for localizing messages
            LocalizationManager l10n = new LocalizationManager();
            l10n.addLocalizer( new XLRLocalizer() );
            l10n.addLocalizer( new ResourceBundleLocalizer() );
            ThreadLocalToolkit.setLocalizationManager(l10n);

	        // setup the console logger. the configuration parser needs a logger.
            flex2.compiler.API.useConsoleLogger();

            // process configuration
            ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CommandLineConfiguration.class, Configuration.getAliases());
            
            // Do not set this.  The file-specs should be included in the configuration buffer 
            // checksum so changes to the class list can be detected during incremental builds.
            //cfgbuf.setDefaultVar(FILE_SPECS);
	        DefaultsConfigurator.loadDefaults( cfgbuf );
            CommandLineConfiguration configuration = (CommandLineConfiguration) processConfiguration(
                l10n, "mxmlc", args, cfgbuf, CommandLineConfiguration.class, FILE_SPECS);

            // well, setup the logger again now that we know configuration.getWarnings()???
            flex2.compiler.API.useConsoleLogger(true, true, configuration.getWarnings(), true);
            flex2.compiler.API.setupHeadless(configuration);

            if (configuration.benchmark())
            {
                flex2.compiler.API.runBenchmark();
            }
            else
            {
                flex2.compiler.API.disableBenchmark();
            }

            // make sure targetFile abstract pathname is an absolute path...
            VirtualFile targetFile = flex2.compiler.API.getVirtualFile(configuration.getTargetFile());
            flex2.tools.API.checkSupportedTargetMimeType(targetFile);

            // mxmlc only wants to take one file.
            List fileList = configuration.getFileList();
            if (fileList == null || fileList.size() != 1)
            {
                throw new flex2.compiler.common.ConfigurationException.OnlyOneSource( "filespec", null, -1);
            }

            List virtualFileList = flex2.compiler.API.getVirtualFileList(fileList); // List<VirtualFile>

            CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();
            NameMappings mappings = flex2.compiler.API.getNameMappings(configuration);

            // create a FileSpec... can reuse based on targetFile, debug settings, etc...
            FileSpec fileSpec = new FileSpec(Collections.EMPTY_LIST, flex2.tools.API.getFileSpecMimeTypes());

            // create a SourcePath...
            VirtualFile[] asClasspath = compilerConfig.getSourcePath();
            SourceList sourceList = new SourceList(virtualFileList,
                                                   asClasspath,
                                                   targetFile,
                                                   flex2.tools.API.getSourcePathMimeTypes());
            SourcePath sourcePath = new SourcePath(asClasspath,
                                                   targetFile,
                                                   flex2.tools.API.getSourcePathMimeTypes(),
                                                   compilerConfig.allowSourcePathOverlap());

            ResourceContainer resources = new ResourceContainer();
	        ResourceBundlePath bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), targetFile);

	        ArrayList sources = new ArrayList();
            List units = new ArrayList();
	        
	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new InitialSetup()));
	        }

            // load SWCs
            CompilerSwcContext swcContext = new CompilerSwcContext(configuration.getCompatibilityVersionString());
            SwcCache cache = new SwcCache();
	        // lazy read should only be set by mxmlc/compc
	        cache.setLazyRead(true);
	        
            swcContext.load( compilerConfig.getLibraryPath(),
                             Configuration.getAllExcludedLibraries(compilerConfig, configuration),
                             compilerConfig.getThemeFiles(),
                             compilerConfig.getIncludeLibraries(),
                             mappings,
                             I18nUtils.getTranslationFormat(compilerConfig),
                             cache );
            configuration.addExterns( swcContext.getExterns() );
            configuration.addIncludes( swcContext.getIncludes() );
            configuration.getCompilerConfiguration().addDefaultsCssFiles( swcContext.getDefaultsStyleSheets() );
            configuration.getCompilerConfiguration().addThemeCssFiles( swcContext.getThemeStyleSheets() );

	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new LoadedSWCs(swcContext.getNumberLoaded())));
	        }
	        
            // Figure out the name of the output file.
            File outputFile = getOutputFile(configuration, targetFile);

            // Checksums to figure out if incremental compile can be done.
            String incrementalFileName = null;
            SwcChecksums swcChecksums = null;

            // Should we attempt to build incrementally using the incremental file?
            boolean recompile = true;   
            
            // If incremental compilation is enabled and the output file exists, 
            // use the persisted store to figure out if a compile/link is necessary.
            // link without a compile is not supported without changes to the
            // persistantStore since units for Sources of type isSwcScriptOwner()
            // aren't stored/restored properly. units contains null entries for those 
            // type of Source.  To force a rebuild, with -incremental specified, delete the 
            // incremental file. 
            if (configuration.getCompilerConfiguration().getIncremental())
            {
                swcChecksums = new SwcChecksums(swcContext, cfgbuf, configuration);

                // If incremental compilation is enabled, read the cached
                // compilation units...  Do not include the checksum in the file name so that
                // cache files don't pile up as the configuration changes.  There needs
                // to be a 1-to-1 mapping between the swc file and the cache file. 
                incrementalFileName = outputFile.getPath() + ".cache";
                
                // If the output file doesn't exist don't bother loading the
                // cache since a recompile is needed.
                if (outputFile.exists())
                {
                    RandomAccessFile incrementalFile = null;
                    try
                    {
                        incrementalFile = new RandomAccessFile(incrementalFileName, "r");

                        // For loadCompilationUnits, loadedChecksums[1] must match 
                        // the cached value else IOException is thrown.
                        int[] loadedChecksums = swcChecksums.copy();

                        flex2.compiler.API.loadCompilationUnits(configuration, fileSpec, sourceList, sourcePath, resources, bundlePath, null, /* sources */
                            null, /*units */
                            loadedChecksums, 
                            swcChecksums.getSwcDefSignatureChecksums(), swcChecksums.getSwcFileChecksums(), 
                            null, /* archiveFiles */
                            incrementalFile, incrementalFileName, 
                            null /* font manager */);

                        if (!swcChecksums.isRecompilationNeeded(loadedChecksums) && !swcChecksums.isRelinkNeeded(loadedChecksums))
                        {
                            recompile = false;
                        }
                    }
                    catch (FileNotFoundException ex)
                    {
                        // the incremental file doesn't exist
                        ThreadLocalToolkit.logDebug(ex.getLocalizedMessage());
                    }
                    catch (IOException ex)
                    {
                        // error loading the incremental file - most likely checksum 
                        // mismatch or format mismatch
                        ThreadLocalToolkit.logInfo(ex.getLocalizedMessage());
                    }
                    finally
                    {
                        if (incrementalFile != null)
                        {
                            try
                            {
                                incrementalFile.close();
                            }
                            catch (IOException ex)
                            {
                            }
                            // If the load failed, or recompilation is needed, reset
                            // all the variables to their original state.
                            if (recompile)
                            {
                                fileSpec = new FileSpec(Collections.EMPTY_LIST, flex2.tools.API.getFileSpecMimeTypes());
                                sourceList = new SourceList(virtualFileList, asClasspath, targetFile, flex2.tools.API.getSourcePathMimeTypes());
                                sourcePath = new SourcePath(asClasspath, targetFile, flex2.tools.API.getSourcePathMimeTypes(), compilerConfig.allowSourcePathOverlap());
                                resources = new ResourceContainer();
                                bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), targetFile);
                            }
                        }
                    }
                }
            }
	        
            VirtualFile projector = configuration.getProjector();
            boolean createProjector = (projector != null && projector.getName().endsWith("avmplus.exe"));

            // Validate CompilationUnits in FileSpec and SourcePath.  If 
            // count > 0 something changed.
            int count = flex2.compiler.API.validateCompilationUnits(
                    fileSpec, sourceList, sourcePath, bundlePath, resources, 
                    swcContext, 
                    null    /* perCompileData */, 
                    false,  /* true is force recompile of all source units */ 
                    configuration);
            recompile = recompile || (count > 0);
            
            if (recompile)
            {
                // Get standard bundle of compilers, transcoders.
                transcoders = flex2.tools.API.getTranscoders( configuration );
                flex2.compiler.Compiler[] compilers = 
                    flex2.tools.API.getCompilers(compilerConfig, mappings, transcoders);
                
                // compile
                units = flex2.compiler.API.compile(fileSpec, sourceList, 
                        null, /* classes */
                        sourcePath, resources, bundlePath, swcContext, 
                        configuration, compilers, 
                        createProjector ? null : new PreLink(),
                        configuration.getLicensesConfiguration().getLicenseMap(), 
                        sources);

                OutputStream swfOut = new BufferedOutputStream(new FileOutputStream(outputFile));

                // link
                if (createProjector)
                {
                    ConsoleApplication app = flex2.linker.API.linkConsole(units, 
                            new PostLink(configuration), configuration);

                    createProjector(projector, app, swfOut);
                }
                else
                {
                    Movie movie = flex2.linker.API.link(units, 
                            new PostLink(configuration), configuration);

                    if (projector != null)
                    {
                        createProjector(projector, movie, swfOut);
                    }
                    else
                    {
                        flex2.compiler.API.encode(movie, swfOut);
                    }
                }
                
                swfOut.flush();
                swfOut.close();

                // If incremental compilation is enabled, save the compilation units.
                if (configuration.getCompilerConfiguration().getIncremental())
                {
                    // Make sure the checksums are all current.
                    swcChecksums.saveChecksums(units);                    
                    
                    RandomAccessFile incrementalFile = null;
                    try
                    {
                        incrementalFile = new RandomAccessFile(incrementalFileName, "rw");
                        
                        // In case we're reusing the file, clear it.
                        incrementalFile.setLength(0);

                        flex2.compiler.API.persistCompilationUnits(
                                configuration, fileSpec, sourceList, sourcePath, 
                                resources, bundlePath, 
                                sources,   /* sources */
                                units,   /* units */ 
                                swcChecksums.getChecksums(),
                                swcChecksums.getSwcDefSignatureChecksums(),
                                swcChecksums.getSwcFileChecksums(),
                                null,   /* archiveFiles */ 
                                "", incrementalFile);
                    }
                    catch (IOException ex)
                    {
                        ThreadLocalToolkit.logInfo(ex.getLocalizedMessage());
                        
                        // Get rid of the cache file since the write failed.
                        new File(incrementalFileName).deleteOnExit();
                    }
                    finally
                    {
                        if (incrementalFile != null)
                        {
                            try
                            {
                                incrementalFile.close();
                            }
                            catch (IOException ex)
                            {
                            }
                        }
                    }
                }

                ThreadLocalToolkit.log(new OutputMessage(FileUtil.getCanonicalPath(outputFile), 
                        Long.toString(outputFile.length())));                                
            }
            else
            {
                // swc is already up-to-date so no need to compile/link or rewrite file
                ThreadLocalToolkit.log(new NoUpdateMessage(FileUtil.getCanonicalPath(outputFile)));            
            }
            
            // fixme: an AOP like way to specify when to do this would be nice,
            // the heap should be close to its max right now though
			if (configuration.benchmark())
			{
				ThreadLocalToolkit.getBenchmark().captureMemorySnapshot();
			}

        }
        catch (flex2.compiler.config.ConfigurationException ex)
        {
            processConfigurationException(ex, "mxmlc");
        }
        catch (LicenseException ex)
        {
            ThreadLocalToolkit.logError(ex.getMessage());
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (LinkerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (Throwable t) // IOException, Throwable
        {
            ThreadLocalToolkit.logError(t.getLocalizedMessage());
            t.printStackTrace();
        }
        finally
        {
            if (ThreadLocalToolkit.getBenchmark() != null)
            {
                ThreadLocalToolkit.getBenchmark().totalTime();
                ThreadLocalToolkit.getBenchmark().peakMemoryUsage(true);
            }

            for (int i = 0, length = transcoders == null ? 0 : transcoders.length; i < length; i++)
            {
                ((Transcoder) transcoders[i]).clear();
            }

            flex2.compiler.API.removePathResolver();
        }
    }

    private static File getOutputFile(CommandLineConfiguration configuration, VirtualFile targetFile)
    {
        String name;
        VirtualFile projector = configuration.getProjector();
        boolean createProjector = (projector != null && projector.getName().endsWith("avmplus.exe"));
        
        if (createProjector)
        {
            // output .exe
            name = configuration.getOutput();
            if (name == null)
            {
                name = targetFile.getName();
                name = name.substring(0, name.lastIndexOf('.')) + ".exe";
            }       
        }
        else
        {
            // output SWF
            name = configuration.getOutput();
            if (name == null)
            {
                name = targetFile.getName();
                if (projector != null)
                {
                    name = name.substring(0, name.lastIndexOf('.')) + ".exe";
                }
                else
                {
                    name = name.substring(0, name.lastIndexOf('.')) + ".swf";                   
                }
            }                
        }
        
        return FileUtil.openFile(name, true);
    }
    
    public static void createProjector(VirtualFile projector, ConsoleApplication app, OutputStream out)
    {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	try
    	{
    		flex2.compiler.API.encode(app, baos);
    		createProjector(projector, baos, out);
    	}
    	catch (IOException ex)
    	{
    	}
    	finally
    	{
    	}
    }

    public static void createProjector(VirtualFile projector, Movie movie, OutputStream out)
    {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	try
    	{
    		flex2.compiler.API.encode(movie, baos);
    		createProjector(projector, baos, out);
    	}
    	catch (IOException ex)
    	{
    	}
    	finally
    	{
    	}
    }

    public static long createProjector(VirtualFile projector, ByteArrayOutputStream baos, OutputStream out)
    {
    	long size = 0;
    	BufferedInputStream in = null;
    	try
    	{
    		in = new BufferedInputStream(projector.getInputStream());
    		FileUtil.streamOutput(in, out);
    		byte header[] = new byte[8];
    		header[0] = 0x56;
    		header[1] = 0x34;
    		header[2] = 0x12;
    		header[3] = (byte) 0xFA;
    		header[4] = (byte) (baos.size() & 0xFF);
    		header[5] = (byte) ((baos.size() >> 8) & 0xFF);
    		header[6] = (byte) ((baos.size() >> 16) & 0xFF);
    		header[7] = (byte) ((baos.size() >> 24) & 0xFF);
    		out.write(baos.toByteArray());
    		out.write(header);
    		out.flush();
    		size = projector.size() + baos.size() + 8;
    	}
    	catch (IOException ex)
    	{
    		size = 0;
    	}
    	finally
    	{
    		if (in != null) { try { in.close(); } catch (IOException ex) {} }
    	}
    	
    	return size;
    }
    
    static private String l10nConfigPrefix = "flex2.configuration";
    
    public static Configuration processConfiguration( LocalizationManager lmgr, String program, String[] args,
    		ConfigurationBuffer cfgbuf, Class cls, String defaultVar)
    	throws flex2.compiler.config.ConfigurationException, IOException
    {
    		return processConfiguration(lmgr, program, args, cfgbuf, cls, defaultVar, false);
    }
 
    public static Configuration processConfiguration( LocalizationManager lmgr, String program, String[] args,
                                               ConfigurationBuffer cfgbuf, Class cls, String defaultVar, 
                                               boolean ignoreUnknownItems)
        throws flex2.compiler.config.ConfigurationException, IOException
    {
        SystemPropertyConfigurator.load( cfgbuf, "flex" );

        // Parse the command line a first time, to peak at stuff like
        // "flexlib" and "load-config".  The first parse is thrown
        // away after that and we intentionally parse a second time
        // below.  See note below.
        CommandLineConfigurator.parse( cfgbuf, defaultVar, args);

        String flexlib = cfgbuf.getToken( "flexlib" );
        if (flexlib == null)
        {
            String appHome = System.getProperty( "application.home" );

            if (appHome == null)
            {
                appHome = ".";
            }
            else
            {
                appHome += File.separator + "frameworks";       // FIXME - need to eliminate this from the compiler
            }
            cfgbuf.setToken( "flexlib", appHome );
        }
        
        String configname = cfgbuf.getToken( "configname" );
        if (configname == null)
        {
        	cfgbuf.setToken( "configname", "flex" );
        }

        String buildNumber = cfgbuf.getToken( "build.number" );
        if (buildNumber == null)
        {
        	if ("".equals(VersionInfo.getBuild()))
   			{
        		buildNumber = "workspace";
   			}
        	else 
        	{
        		buildNumber = VersionInfo.getBuild();
        	}
        	cfgbuf.setToken( "build.number", buildNumber);
        }


        // We need to intercept "help" options because we want to try to correctly
        // interpret them even when the rest of the configuration is totally screwed up.

        if (cfgbuf.getVar( "version" ) != null)
        {
            System.out.println(VersionInfo.buildMessage());
            System.exit(0);
        }

        processHelp(cfgbuf, program, defaultVar, lmgr, args);
        
        // at this point, we should have enough to know both
        // flexlib and the config file.

        ConfigurationPathResolver configResolver = new ConfigurationPathResolver();

        List configs = cfgbuf.peekConfigurationVar( "load-config" );

        if (configs != null)
        {
            for (Iterator cfgit = configs.iterator(); cfgit.hasNext(); )
            {
                ConfigurationValue cv = (ConfigurationValue) cfgit.next();
                for (Iterator pathit = cv.getArgs().iterator(); pathit.hasNext(); )
                {
                    String path = (String) pathit.next();
                    VirtualFile configFile = ConfigurationPathResolver.getVirtualFile( path, configResolver, cv );
                	cfgbuf.calculateChecksum(configFile);
                    InputStream in = configFile.getInputStream();
                    if (in != null)
                    {
                        FileConfigurator.load(cfgbuf, new BufferedInputStream(in), configFile.getName(),
                                              configFile.getParent(), "flex-config", ignoreUnknownItems);
                    }
                    else
                    {
                        throw new flex2.compiler.config.ConfigurationException.ConfigurationIOError( path, cv.getVar(), cv.getSource(), cv.getLine() );
                    }
                }
            }
        }

        PathResolver resolver = ThreadLocalToolkit.getPathResolver();
        // Load project file, if any...
        List fileValues = cfgbuf.getVar( FILE_SPECS );
        if ((fileValues != null) && (fileValues.size() > 0))
        {
            ConfigurationValue cv = (ConfigurationValue) fileValues.get( fileValues.size() - 1 );
            if (cv.getArgs().size() > 0)
            {
                String val = (String) cv.getArgs().get( cv.getArgs().size() - 1 );
                int index = val.lastIndexOf( '.' );
                if (index != -1)
                {
                    String project = val.substring( 0, index ) + "-config.xml";
                    VirtualFile projectFile = resolver.resolve( configResolver, project );
                    if (projectFile != null)
                    {
                    	cfgbuf.calculateChecksum(projectFile);
                        InputStream in = projectFile.getInputStream();
                        if (in != null)
                        {
                            FileConfigurator.load( cfgbuf, new BufferedInputStream(in),
                                                   projectFile.getName(), projectFile.getParent(), "flex-config",
                                                   ignoreUnknownItems);
                        }
                    }
                }
            }
        }

        // The command line needs to take precedence over all defaults and config files.
        // This is a bit gross, but by simply re-merging the command line back on top,
        // we will get the behavior we want.
        cfgbuf.clearSourceVars( CommandLineConfigurator.source );
        CommandLineConfigurator.parse(cfgbuf, defaultVar, args);

        ToolsConfiguration toolsConfiguration = null;
        try
        {
            toolsConfiguration = (ToolsConfiguration)cls.newInstance();
            toolsConfiguration.setConfigPathResolver( configResolver );
        }
        catch (Exception e)
        {
	        LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
	        throw new flex2.compiler.config.ConfigurationException(l10n.getLocalizedTextString(new CouldNotInstantiate(toolsConfiguration)));
        }
        cfgbuf.commit( toolsConfiguration );

        // enterprise service config file has other config file dependencies. add them here...
        calculateServicesChecksum(toolsConfiguration, cfgbuf);
        
        toolsConfiguration.validate( cfgbuf );

        // consolidate license keys...
		VirtualFile licenseFile = toolsConfiguration.getLicenseFile();
		if (licenseFile != null)
		{
			Map fileLicenses = Tool.getLicenseMapFromFile(licenseFile.getName());
			Map cmdLicenses = toolsConfiguration.getLicensesConfiguration().getLicenseMap();
			if (cmdLicenses == null)
			{
				toolsConfiguration.getLicensesConfiguration().setLicenseMap(fileLicenses);
			}
			else if (fileLicenses != null)
			{
				fileLicenses.putAll(cmdLicenses);
				toolsConfiguration.getLicensesConfiguration().setLicenseMap(fileLicenses);
			}
		}

        return toolsConfiguration;
    }

    static void processHelp(ConfigurationBuffer cfgbuf, String program, String defaultVar, LocalizationManager lmgr, String[] args)
    {
        if (cfgbuf.getVar( "help" ) != null)
        {
            Set keywords = new HashSet();
            List vals = cfgbuf.getVar( "help" );
            for (Iterator it = vals.iterator(); it.hasNext();)
            {
                ConfigurationValue val = (ConfigurationValue) it.next();
                for (Iterator k = val.getArgs().iterator(); k.hasNext();)
                {
                    String keyword = (String) k.next();
                    while (keyword.startsWith( "-" ))
                        keyword = keyword.substring( 1 );
                    keywords.add( keyword );
                }
            }
            if (keywords.size() == 0)
            {
                keywords.add( "help" );
            }

            System.out.print( getStartMessage( program ) );
            System.out.println();
            System.out.println( CommandLineConfigurator.usage( program, defaultVar, cfgbuf, keywords, lmgr, l10nConfigPrefix ));
            System.exit( 1 );
        }

        if (args.length == 0 && ("mxmlc".equals(program) || "compc".equals(program)))
        {
            System.err.println( getStartMessage( program ) );
            System.err.println( CommandLineConfigurator.brief( program, defaultVar, lmgr, l10nConfigPrefix ));
            System.exit(1);
        }
    }
    
    private static void calculateServicesChecksum(Configuration config, ConfigurationBuffer cfgbuf)
    {
		Map services = null;
		if (config.getCompilerConfiguration().getServicesDependencies() != null)
		{
			services = config.getCompilerConfiguration().getServicesDependencies().getConfigPaths();
		}

		if (services != null)
		{
			for (Iterator iter = services.entrySet().iterator(); iter.hasNext(); )
			{
				Map.Entry entry = (Map.Entry) iter.next();
				cfgbuf.calculateChecksum((String) entry.getKey(), (Long) entry.getValue());
			}
		}
	}
          
    public static void processConfigurationException(flex2.compiler.config.ConfigurationException ex, String program)
    {
        ThreadLocalToolkit.log( ex );

        if (ex.source == null || ex.source.equals("command line"))
        {
            Map p = new HashMap();
            p.put( "program", program );
            String help = ThreadLocalToolkit.getLocalizationManager().getLocalizedTextString( "flex2.compiler.CommandLineHelp", p );
            if (help != null)
            {
                // "Use '" + program + " -help' for information about using the command line.");
                System.err.println( help );
            }
        }
    }

    private static String getStartMessage( String program )
    {
	    LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();

	    return l10n.getLocalizedTextString(new StartMessage(program, VersionInfo.buildMessage()));
    }

	// error messages

	public static class InitialSetup extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = 1333039844101599298L;

        public InitialSetup()
		{
			super();
		}
	}

    public static class DumpConfig extends CompilerMessage.CompilerInfo
    {
        private static final long serialVersionUID = 953067728556782737L;
        public DumpConfig(String filename)
        {
            this.filename = filename;
        }
        public final String filename;
    }

    public static class LoadedSWCs extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = 5287457959220324715L;

        public LoadedSWCs(int num)
		{
			super();
			this.num = num;
		}

		public final int num;
	}

	public static class CouldNotInstantiate extends CompilerMessage.CompilerInfo
	{
       private static final long serialVersionUID = -8970190710117830662L;

        public CouldNotInstantiate(Configuration config)
		{
			super();
			this.config = config;
		}

		public final Configuration config;
	}

	public static class StartMessage extends CompilerMessage.CompilerInfo
	{
        private static final long serialVersionUID = 4807822711658875257L;

        public StartMessage(String program, String buildMessage)
		{
			super();
			this.program = program;
			this.buildMessage = buildMessage;
		}

		public final String program, buildMessage;
	}

    public static class OutputMessage extends CompilerMessage.CompilerInfo
    {
        private static final long serialVersionUID = -4859993585489031839L;
        public String name;
        public String length;

        public OutputMessage(String name, String length)
        {
            this.name = name;
            this.length = length;
        }
    }

    public static class NoUpdateMessage extends CompilerMessage.CompilerInfo
    {                
        private static final long serialVersionUID = 6943388392279226490L;
        public String name;

        public NoUpdateMessage(String name)
        {
            this.name = name;
        }
    }

}

