////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.net.URI;

import flex2.compiler.API;
import flex2.compiler.CompilerException;
import flex2.compiler.CompilerSwcContext;
import flex2.compiler.FileSpec;
import flex2.compiler.LicenseException;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.ResourceContainer;
import flex2.compiler.SourceList;
import flex2.compiler.SourcePath;
import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.Component;
import flex2.compiler.swc.SwcArchive;
import flex2.compiler.swc.SwcCache;
import flex2.compiler.swc.SwcDirectoryArchive;
import flex2.compiler.swc.SwcException;
import flex2.compiler.swc.SwcMovie;
import flex2.compiler.swc.SwcWriteOnlyArchive;
import flex2.compiler.util.CompilerControl;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.linker.LinkerException;
import flex2.linker.SimpleMovie;
import flex2.tools.CompcPreLink;
import flex2.tools.Compiler;
import flex2.tools.ToolsConfiguration;
import flex2.tools.oem.internal.LibraryData;
import flex2.tools.oem.internal.OEMConfiguration;
import flex2.tools.oem.internal.OEMReport;
import flex2.tools.oem.internal.OEMUtil;

/**
 * The <code>Library</code> class represents a SWC archive or a RSL. It implements the <code>Builder</code> interface
 * which allows for building the library incrementally. The following example defines a SWC archive or RSL:
 *
 * <pre>
 * Library lib = new Library();
 * </pre>
 *
 * You can add components to the <code>Library</code> object in the following ways:
 * 
 * <pre>
 * 1. String              - Specify a fully-qualified name.
 * 2. File                - Specify a source file.
 * 3. VirtualLocalFile    - Specify an in-memory source object.
 * 4. URI                 - Specify a namespace URI.
 * </pre>
 * 
 * <p>
 * To add resource bundles to the <code>Library</code>, you can use the <code>addResourceBundle()</code> method,
 * as the following example shows:
 * 
 * <pre>
 * lib.addResourceBundle("mx.controls"));
 * </pre>
 * 
 * <p>
 * To add archive files to the <code>Library</code>, you can use the <code>addArchiveFile()</code> method, as the following
 * example shows:
 * 
 * <pre>
 * lib.addArchiveFile("defaults.css", new File("path1/myStyle.css"));
 * </pre>
 * 
 * Before you can compile with a <code>Library</code> object, you must configure it. The following
 * four methods are the most common methods you use to configure the <code>Library</code> object:
 *
 * <pre>
 * 1. setLogger()        - Use this to set a Logger so that the client can be notified of events that occurred during the compilation.
 * 2. setConfiguration() - Optional. Use this to specify compiler options.
 * 3. setOutput()        - Optional. Use this to specify an output file name.
 * 4. setDirectory()     - Optional. Use this to specify an RSL output directory.
 * </pre>
 *
 * You must implement the <code>flex2.tools.oem.Logger</code> interface and use the implementation as the <code>Logger</code>
 * for the compilation. The following is an example <code>Logger</code> implementation:
 * 
 * <pre>
 * lib.setLogger(new flex2.tools.oem.Logger()
 * {
 *     public void log(Message message, int errorCode, String source)
 *     {
 *         System.out.println(message);
 *     }
 * });
 * </pre>
 * 
 * To specify compiler options for the <code>Library</code> object, you
 * must get a <code>Configuration</code> object that is populated with default values. Then, you set
 * compiler options programmatically using methods of the <code>Configuration</code> class. 
 *
 * <p>
 * The <code>setOutput()</code> method lets you specify where the <code>Library</code> object writes
 * the output to. If you call the <code>setOutput()</code> method, the <code>build(boolean)</code> method
 * writes directly to the specified location; for example:
 * 
 * <pre>
 * lib.setOutput(new File("MyLib.swc"));
 * lib.build(true);
 * </pre>
 * 
 * If you do not call the <code>setOutput()</code> method, you can use the <code>build(OutputStream, boolean)</code>
 * method. This requires that you provide a buffered output stream; for example:
 * 
 * <pre>
 * lib.build(new BufferedOutputStream(new FileOutputStream("MyLib.swc")), true);
 * </pre>
 * 
 * The <code>setDirectory()</code> method lets you output RSLs to the specified directory; for example:
 * 
 * <pre>
 * lib.setDirectory(new File("dir1"));
 * lib.build(true);
 * </pre>
 * 
 * You can save the <code>Library</code> object compilation
 * data for reuse. You do this using the <code>save(OutputStream)</code> method. Subsequent compilations can use
 * the <code>load(OutputStream)</code> method to get the old data into the <code>Library</code> object; for example:
 *
 * <pre>
 * lib.save(new BufferedOutputStream(new FileOutputStream("MyLib.incr")));
 * </pre>
 * 
 * When a cache file (for example, <code>MyLib.incr</code>) from a previous compilation is available before the
 * compilation, you can call the <code>load(OutputStream)</code> method before you call the <code>build()</code> method; for example:
 * 
 * <pre>
 * lib.load(new BufferedInputStream(FileInputStream("MyLib.incr")));
 * lib.build(true);
 * </pre>
 * 
 * The <code>build(false)</code> and <code>build(OutputStream, false)</code> methods always rebuild the library. 
 * The first time you build the <code>Library</code>
 * object, the <code>build(true)/build(OutputStream, true)</code> methods do a complete build, which
 * is equivalent to the <code>build(false)/build(OutputStream, false)</code> methods, respectively. After you call the 
 * <code>clean()</code> method, the <code>Library</code> object always does a full build.
 * 
 * <p>
 * The <code>clean()</code> method cleans up compilation data in the <code>Library</code> object the output
 * file, if the <code>setOutput()</code> method was called.
 *
 * <p>
 * You can use the <code>Library</code> class to build a library from a combination of source
 * files in the file system and in-memory, dynamically-generated source objects. You
 * must use the <code>addComponent(VirtualLocalFile)</code>, <code>addResourceBundle(VirtualLocalFile)</code>, and
 * <code>addArchiveFile(String, VirtualLocalFile)</code> methods to use in-memory objects.
 *
 * <p>
 * The <code>Library</code> class can be part of a <code>Project</code>.
 *
 * @see flex2.tools.oem.Configuration
 * @see flex2.tools.oem.Project
 * @version 2.0.1
 * @author Clement Wong
 */
public class Library implements Builder
{
    static
    {
        // This should trigger the static initialization of Application which locates
        // flex-compiler-oem.jar and set application.home correctly.
        Class c = Application.class;
    }
    
    /**
     * Constructor.
     */
    public Library()
    {
        sources = new TreeSet(new Comparator()
        {
            public int compare(Object arg0, Object arg1)
            {
                VirtualFile f0 = (VirtualFile) arg0, f1 = (VirtualFile) arg1;
                return f0.getName().compareTo(f1.getName());
            }
        }); // TreeSet<VirtualFile>
        classes = new TreeSet(); // TreeSet<String>
        namespaces = new TreeSet(); // TreeSet<URI>
        resourceBundles = new TreeSet(); // TreeSet<String>
        files = new TreeMap(); // TreeMap<String, VirtualFile>
        stylesheets = new TreeMap(); // TreeMap<String, VirtualFile>
        
        configuration = null;
        logger = null;
        output = null;
        directory = null;
        mimeMappings = new MimeMappings();
        meter = null;
        resolver = null;
        cc = new CompilerControl();

        data = null;
        cacheName = null;
        configurationReport = null;
        messages = new ArrayList();
    }
    
    private Set sources, classes, namespaces, resourceBundles;
    private Map files, stylesheets;
    private OEMConfiguration configuration;
    private Logger logger;
    private File output, directory;
    private MimeMappings mimeMappings;
    private ProgressMeter meter;
    protected PathResolver resolver;
    private CompilerControl cc;

    // clean() would null out the following variables
    LibraryData data;
    private String cacheName, configurationReport;
    private List messages;

    /**
     * Adds a class, function, variable, or namespace to this <code>Library</code> object.
     * 
     * This is the equilvalent of the <code>include-classes</code> option of the compc compiler.
     * 
     * @param includeClass A fully-qualified name.
     */
    public void addComponent(String includeClass)
    {
        classes.add(includeClass);
    }

    /**
     * Adds a component to this <code>Library</code> object.
     * This is the equilvalent of the <code>include-sources</code> option of the compc compiler.
     * 
     * @param includeSource A source file.
     */
    public void addComponent(File includeSource)
    {
        sources.add(new LocalFile(includeSource));
    }

    /**
     * Adds a component to this <code>Library</code> object.
     * 
     * This is equilvalent to the <code>include-sources</code> option of the compc compiler.
     * 
     * @param includeSource An in-memory source object.
     */
    public void addComponent(VirtualLocalFile includeSource)
    {
        sources.add(includeSource);
    }

    /**
     * Adds a list of components to this <code>Library</code> object.
     * 
     * This is equilvalent to the <code>include-namespaces</code> option of the compc compiler.
     * 
     * @param includeNamespace A namespace URI.
     */
    public void addComponent(URI includeNamespace)
    {
        namespaces.add(includeNamespace);
    }

    /**
     * Removes the specified component from this <code>Library</code> object. 
     * The name can be a class, a function, a variable, or a namespace.
     * 
     * @param includeClass A fully-qualified name.
     */
    public void removeComponent(String includeClass)
    {
        classes.remove(includeClass);
    }

    /**
     * Removes the specified component from this <code>Library</code> object. 
     * 
     * @param includeSource A source file.
     */
    public void removeComponent(File includeSource)
    {
        sources.remove(new LocalFile(includeSource));
    }

    /**
     * Removes the specified component from this <code>Library</code> object.
     * 
     * @param includeSource An in-memory source object.
     */
    public void removeComponent(VirtualLocalFile includeSource)
    {
        sources.remove(includeSource);
    }

    /**
     * Removes the specified list of components from this <code>Library</code> object. The input argument is a namespace URI.
     * 
     * @param includeNamespace A namespace URI.
     */
    public void removeComponent(URI includeNamespace)
    {
        namespaces.remove(includeNamespace);
    }
    
    /**
     * Removes all the components from this <code>Library</code> object.
     */
    public void removeAllComponents()
    {
        sources.clear();
        classes.clear();
        namespaces.clear();
    }

    /**
     * Adds a resource bundle to this <code>Library</code> object. 
     * 
     * This is equilvalent to the <code>include-resource-bundles</code> option of the compc compiler.
     * 
     * @param resourceBundle A resource bundle name.
     */
    public void addResourceBundle(String resourceBundle)
    {
        resourceBundles.add(resourceBundle);
    }

    /**
     * Removes the specified resource bundle name from this <code>Library</code> object.
     *  
     * @param resourceBundle A resource bundle name.
     */
    public void removeResourceBundle(String resourceBundle)
    {
        resourceBundles.remove(resourceBundle);
    }

    /**
     * Removes all the resource bundles from this <code>Library</code> object.
     *
     */
    public void removeAllResourceBundles()
    {
        resourceBundles.clear();
    }

    /**
     * Adds a file to this <code>Library</code> object. This is equilvalent to the <code>include-file</code> option of the compc compiler.
     * 
     * @param name The name in the archive.
     * @param file The file to be added.
     */
    public void addArchiveFile(String name, File file)
    {
        files.put(name, new LocalFile(file));
    }

    /**
     * Adds an in-memory source object to this <code>Library</code> object. This is equilvalent to the <code>
     * include-file</code> option of the compc compiler.
     * 
     * @param name The name in the archive.
     * @param file The in-memory source object to be added.
     */
    public void addArchiveFile(String name, VirtualLocalFile file)
    {
        files.put(name, file);
    }

    /**
     * Removes the specified file from this <code>Library</code> object.
     * 
     * @param name The name in the archive.
     */
    public void removeArchiveFile(String name)
    {
        files.remove(name);
    }
    
    /**
     * Removes all the archive files from this <code>Library</code> object.
     */
    public void removeAllArchiveFiles()
    {
        files.clear();
    }

    /**
     * Adds a CSS stylesheet to this <code>Library</code> object. This is equilvalent to the <code>include-stylesheet</code> option of the compc compiler.
     * 
     * @param name The name in the archive.
     * @param file The file to be added.
     * @since 3.0
     */
    public void addStyleSheet(String name, File file)
    {
        stylesheets.put(name, new LocalFile(file));
    }

    /**
     * Adds an in-memory CSS stylesheet object to this <code>Library</code> object. This is equilvalent to the <code>
     * include-stylesheet</code> option of the compc compiler.
     * 
     * @param name The name in the archive.
     * @param file The in-memory source object to be added.
     * @since 3.0
     */
    public void addStyleSheet(String name, VirtualLocalFile file)
    {
        stylesheets.put(name, file);
    }

    /**
     * Removes the specified CSS stylesheet from this <code>Library</code> object.
     * 
     * @param name The name in the archive.
     * @since 3.0
     */
    public void removeStyleSheet(String name)
    {
        stylesheets.remove(name);
    }
    
    /**
     * Removes all the CSS stylesheets from this <code>Library</code> object.
     * @since 3.0
     */
    public void removeAllStyleSheets()
    {
    	stylesheets.clear();
    }

    /**
     * @inheritDoc
     */
    public void setConfiguration(Configuration configuration)
    {
        this.configuration = (OEMConfiguration) configuration;
    }
    
    /**
     * @inheritDoc
     */
    public Configuration getDefaultConfiguration()
    {
        return getDefaultConfiguration(false);
    }

    /**
     * 
     * @param processDefaults
     * @return
     */
    private Configuration getDefaultConfiguration(boolean processDefaults)
    {
        return OEMUtil.getLibraryConfiguration(constructCommandLine(null), false,
                                               OEMUtil.getLogger(logger, messages), resolver,
                                               mimeMappings, processDefaults);
    }

    /**
     * @inheritDoc
     */
    public Configuration getConfiguration()
    {
        return configuration;
    }
    
    /**
     * @inheritDoc
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
    
    /**
     * @inheritDoc
     */
    public Logger getLogger()
    {
        return logger;
    }
    
    /**
     * @inheritDoc
     */
    public void setSupportedFileExtensions(String mimeType, String[] extensions)
    {
        mimeMappings.set(mimeType, extensions);
    }

    /**
     * Sets the output destination. This method is necessary if you use the <code>build(boolean)</code> method.
     * If you use the <code>build(OutputStream, boolean)</code> method, there is no need to use this method.
     *  
     * @param output An instance of the <code>java.io.File</code> class.
     */
    public void setOutput(File output)
    {
        this.output = output;
    }
    
    /**
     * Gets the output destination. This method returns <code>null</code> if you did not call the 
     * <code>setOutput()</code> method.
     * 
     * @return An instance of the <code>java.io.File</code> class, or <code>null</code> if you did not
     * call the <code>setOutput()</code> method.
     */
    public File getOutput()
    {
        return output;
    }
    
    /**
     * Sets the RSL output directory.
     * 
     * @param directory An RSL directory.
     */
    public void setDirectory(File directory)
    {
        this.directory = directory;
    }
    
    /**
     * Gets the RSL output directory.
     * 
     * @return A <code>java.io.File</code>, or <code>null</code> if you did not call the <code>setDirectory()</code> method.
     */
    public File getDirectory()
    {
        return directory;
    }
    
    /**
     * @inheritDoc
     */
    public void setProgressMeter(ProgressMeter meter)
    {
        this.meter = meter;
    }

    /**
     * @inheritDoc
     */
    public void setPathResolver(PathResolver resolver)
    {
    	this.resolver = resolver;
    }

    /**
     * @inheritDoc
     */
    // IMPORTANT: If you make changes here, you probably want to mirror them in Application.build()
    public long build(boolean incremental) throws IOException
    {
        // I know that directory is not referenced anywhere in here...
        // if you setDirectory but do not setOutput, then output==null but dirctory!=null
        // so this silly looking IF needs to be like this...
        if (output != null || directory != null)
        {
            InputStream tempIn = null;
            ByteArrayOutputStream tempOut = null;
            OutputStream out = null;
            long size = 0;

            //TODO PERFORMANCE: A lot of unnecessary recopying and buffering here
            try
            {
                int result = compile(incremental);
                if (result == SKIP || result == LINK || result == OK)
                {
                    // write to a temp buffer...
                if (output != null)
                {
                        tempOut = new ByteArrayOutputStream();
                    }
                    
                    size = link(tempOut);
                    
                    if (output != null)
            {
                        tempOut.flush();

                        if (size > 0)
                    {
                            tempIn = new ByteArrayInputStream(tempOut.toByteArray());
                            out = new BufferedOutputStream(new FileOutputStream(output));
                            FileUtil.streamOutput(tempIn, out);
                    }
                    }
                }

                return size;
                    }
            finally
                    {
                if (tempIn != null) { try { tempIn.close(); } catch (Exception ex) {} }
                if (tempOut != null) { try { tempOut.close(); } catch (Exception ex) {} }
                if (out != null) { try { out.close(); } catch (Exception ex) {} }
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * @inheritDoc
     */
    public long build(OutputStream out, boolean incremental) throws IOException
    {
        int result = compile(incremental);
        if (result == OK || result == LINK)
        {
            return link(out);
        }
        else if (result == SKIP)
        {
            return link(out);
        }
        else
        {
            return 0;
        }
    }

    /**
     * @return  {@link Builder#OK} if this method call resulted in compilation of some/all parts of the application;
     *          {@link Builder#LINK} if this method call did not compile anything in the application but advise the caller to link again;
     *          {@link Builder#SKIP} if this method call did not compile anything in the application;
     *          {@link Builder#FAIL} if this method call encountered errors during compilation.
     */
    private int recompile(boolean fullRecompile)
    {
        // if there is no configuration, use the default... but don't populate this.configuration.
        OEMConfiguration c;
        if (configuration == null)
        {
            c = (OEMConfiguration) getDefaultConfiguration(true);
        }
        else
        {
            c = OEMUtil.getLibraryConfiguration(constructCommandLine(configuration), configuration.keepLinkReport(),
                                                OEMUtil.getLogger(logger, messages), resolver, mimeMappings);
        }
        
        // if c is null, which indicates problems, this method will return.
        if (c == null)
        {
            clean(false, false, false);
            return FAIL;
        }
        else if (configuration != null && configuration.keepConfigurationReport())
        {
            configurationReport = OEMUtil.formatConfigurationBuffer(c.cfgbuf);
        }
        
        if (configuration != null)
        {
            configuration.cfgbuf = c.cfgbuf;
        }

        // initialize some ThreadLocal variables...
        cc.run();
        OEMUtil.init(OEMUtil.getLogger(logger, messages), mimeMappings, meter, resolver, cc);

        // if there are no SWC inputs, output an error and return -1
        VirtualFile[] includeLibs = (c.configuration == null) ? null : c.configuration.getCompilerConfiguration().getIncludeLibraries();
        if (sources.size() == 0 && classes.size() == 0 && namespaces.size() == 0 &&
            resourceBundles.size() == 0 && files.size() == 0 && stylesheets.size() == 0 &&
            (includeLibs == null || includeLibs.length == 0))
        {
            ThreadLocalToolkit.log(new flex2.compiler.common.ConfigurationException.NoSwcInputs( null, null, -1 ));
            clean(false, false, false);
            return FAIL;
        }

        Map licenseMap = OEMUtil.getLicenseMap(c.configuration);
            
        data = new LibraryData();
        data.configuration = c.configuration;
        data.cacheName = cacheName;

        API.setupHeadless(c.configuration);
        NameMappings mappings = API.getNameMappings(c.configuration), copy = mappings.copy();

        CompilerConfiguration compilerConfig = c.configuration.getCompilerConfiguration();
        compilerConfig.setMetadataExport(true);

        if (output != null || directory != null)
        {
        	OEMUtil.setGeneratedDirectory(compilerConfig, output != null ? output : directory);
        }
        
        Transcoder[] transcoders = flex2.tools.API.getTranscoders( c.configuration );
        flex2.compiler.Compiler[] compilers = flex2.tools.API.getCompilers(compilerConfig, mappings, transcoders);

        if ((data.fileSet = processSources()) == null)
        {
            clean(true, false, false);
            return FAIL;
        }
        
        data.fileSet.addAll(processStylesheets());
        
        if (!setupSourceContainers(c.configuration, data.fileSet))
        {
            clean(true, false, false);
            return FAIL;
        }

        // load SWCs
        data.swcCache = new SwcCache();
        CompilerSwcContext swcContext = new CompilerSwcContext(true, true,
															   compilerConfig.getCompatibilityVersionString());
        try
        {
	        swcContext.load( compilerConfig.getLibraryPath(),
	                         compilerConfig.getExternalLibraryPath(),
	                         null,
	                         compilerConfig.getIncludeLibraries(),
	                         mappings,
	                         I18nUtils.getTranslationFormat(compilerConfig),
	                         data.swcCache );
        }
        catch (SwcException ex)
        {
        	clean(false, false, false);
        	return FAIL;
        }
        data.includes = new HashSet(swcContext.getIncludes());
        data.excludes = new HashSet(swcContext.getExterns());
        c.configuration.addExterns( swcContext.getExterns() );
        c.configuration.addIncludes( swcContext.getIncludes() );
        
        data.swcArchiveFiles = new HashMap(swcContext.getIncludeFiles());

        data.cmdChecksum = c.cfgbuf.checksum_ts(); // OEMUtil.calculateChecksum(data, swcContext, c);
        data.linkChecksum = c.cfgbuf.link_checksum_ts();
        data.swcChecksum = swcContext.checksum();
        int[] checksums = new int[] { 0, data.cmdChecksum, data.linkChecksum, data.swcChecksum };

        // C: must do loadCompilationUnits() after checksum calculation...
        if (!fullRecompile) 
        {
            if (!loadCompilationUnits(c.configuration, data.fileSet, swcContext, checksums))
            {
                clean(true, false, false);
                return FAIL;
            }

            data.checksum = checksums[0];
            if (data.units != null && 
                data.units.size() > 0 &&
                OEMUtil.isRecompilationNeeded(data, swcContext, c))
            {
                if (!setupSourceContainers(c.configuration, data.fileSet))
                {
                    clean(true, false, false);
                    return FAIL;
                }                           
            }
        }

        // validate CompilationUnits...
        int count = flex2.compiler.API.validateCompilationUnits(data.fileSpec, data.sourceList, data.sourcePath, data.bundlePath,
                                                    data.resources, swcContext, null, false, c.configuration);

        /*
        if (data.cmdChecksum == checksums[1] &&
            data.linkChecksum == checksums[2] &&
            data.swcChecksum == checksums[3] &&
        	data.sources != null &&
        	data.units != null &&
            count == 0)
        {
        	clean(false, false, false);
        	return OK;
        }
        else
        {
        	data.sources = null;
        	data.units = null;
        }
        */

        final SymbolTable symbolTable = SymbolTable.newSymbolTable(c.configuration);
        data.perCompileData = symbolTable.perCompileData;

        Map classes = new TreeMap();
        if ((data.nsComponents = processInputs(swcContext, copy, classes)) == null)
        {
            clean(true, false, false);
            return FAIL;
        }

        data.sources = new ArrayList();
        data.units = compile(compilers, swcContext, symbolTable, licenseMap, classes, 
        					data.sources, c);

        // need to update the checksum here since doing a compile could add some 
        // some signature checksums and change it.
        data.checksum = OEMUtil.calculateChecksum(data, swcContext, c);
        boolean forcedToStop = API.forcedToStop();
        if (data.units == null || forcedToStop)
        {
            clean(true, false, false);
            return FAIL;
        }
        else
        {
            clean(false, false, false);
            return OK;
        }
    }
    
    /**
     * @inheritDoc
     */
    public void stop()
    {
        cc.stop();
    }

    /**
     * @inheritDoc
     */
    public void clean()
    {
        clean(true, true, true, true, true);
    }

    /**
     * @inheritDoc
     */
    public void load(InputStream in) throws IOException
    {
        cacheName = OEMUtil.load(in, cacheName);
        clean(true, false, false);
    }
    
    /**
     * @inheritDoc
     */
    public long save(OutputStream out) throws IOException
    {
        return OEMUtil.save(out, cacheName, data);
    }
    
    /**
     * @inheritDoc
     */
    public Report getReport()
    {
        OEMUtil.setupLocalizationManager();
        return new OEMReport(data == null ? null : data.sources,
                             data == null ? null : data.movie,
                             data == null ? null : data.configuration,
                             configurationReport,
                             messages);
    }

    /**
     * 
     * @param c
     * @return
     */
    private String[] constructCommandLine(OEMConfiguration c)
    {
        return (c != null) ? c.getCompilerOptions() : new String[0];
    }

    /**
     * 
     * @param swcContext
     * @param mappings
     * @param classes
     * @return
     */
    private Set processInputs(CompilerSwcContext swcContext, NameMappings mappings, Map classes)
    {
        try
        {
            Set nsComponents = processNamespaces(mappings, classes);
            if (nsComponents == null)
            {
                return null;
            }
            
            if (!processClasses(classes))
            {
                return null;
            }
            
            return nsComponents;
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
            return null;
        }
    }
    
    /**
     * 
     * @param classes
     * @return
     */
    private boolean processClasses(Map classes)
    {
        try
        {
            flex2.compiler.swc.API.setupClasses(new ArrayList(this.classes), data.sourcePath, classes);
            return true;
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
            return false;
        }
    }

    /**
     * This must be called before CompilerSwcContext.load().
     * 
     * @param mappings
     * @param classes
     * @return
     */
    private Set processNamespaces(NameMappings mappings, Map classes)
    {
        Set nsComponents = null;
        
        try
        {
            List list = flex2.compiler.swc.API.setupNamespaceComponents(toStrings(namespaces),
                                                                        mappings, data.sourcePath, classes);
            nsComponents = new TreeSet(new Comparator()
            {
                public int compare(Object arg0, Object arg1)
                {
                    Component c0 = (Component) arg0, c1 = (Component) arg1;
                    return c0.getClassName().compareTo(c1.getClassName());
                }
            }); // TreeSet<VirtualFile>
            nsComponents.addAll(list);
        }
        catch (ConfigurationException ex)
        {
            Compiler.processConfigurationException(ex, "oem");
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }

        return nsComponents;
    }
    
    /**
     * 
     * @param set
     * @return
     */
    private List toStrings(Set set)
    {
        List a = new ArrayList(set.size());
        for (Iterator i = set.iterator(); i.hasNext(); )
        {
            URI uri = (URI) i.next();
            a.add(uri.toString());
        }
        return a;
    }
    
    /**
     * @param configuration
     * @param fileList
     * @return true, unless a CompilerException occurs.
     */
    private boolean setupSourceContainers(flex2.compiler.common.Configuration configuration, Set fileSet)
    {
        CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();
        VirtualFile[] asClasspath = compilerConfig.getSourcePath();
        boolean result = false;
        
        try
        {
            // create a SourcePath...
            data.sourcePath = new SourcePath(flex2.tools.API.getSourcePathMimeTypes(),
                                             compilerConfig.allowSourcePathOverlap());
            data.sourcePath.addPathElements( asClasspath );

        	List[] array = API.getVirtualFileList(fileSet, data.sourcePath.getPaths());
            
        	// create a FileSpec...
        	data.fileSpec = new FileSpec(array[0], flex2.tools.API.getFileSpecMimeTypes(), false);

        	// create a SourceList...
        	data.sourceList = new SourceList(array[1], asClasspath, null, flex2.tools.API.getSourceListMimeTypes(), false);
        	
            // create a ResourceContainer...
            data.resources = new ResourceContainer();

            // create a ResourceBundlePath...
            data.bundlePath = new ResourceBundlePath(compilerConfig, null);
            
            // clear these...
            if (data.sources != null) data.sources.clear();
            if (data.units != null) data.units.clear();
            if (data.swcDefSignatureChecksums != null) data.swcDefSignatureChecksums.clear();
            if (data.swcFileChecksums != null) data.swcFileChecksums.clear();

            result = true;
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        
        return result;
    }

    /**
     * 
     * @return
     */
    private Set processSources()
    {
        Set fileSet = null;
        
        try
        {
            List fileList = API.getVirtualFileList(new ArrayList(sources), new HashSet(Arrays.asList(flex2.tools.API.getSourcePathMimeTypes())));
            fileSet = new TreeSet(new Comparator()
            {
                public int compare(Object arg0, Object arg1)
                {
                    VirtualFile f0 = (VirtualFile) arg0, f1 = (VirtualFile) arg1;
                    return f0.getName().compareTo(f1.getName());
                }
            }); // TreeSet<VirtualFile>
            fileSet.addAll(fileList);
        }
        catch (ConfigurationException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        
        return fileSet;
    }

    /**
     * 
     * @return
     */
    private Set processStylesheets()
    {
        Set fileSet = null;
        
        try
        {
            List fileList = API.getVirtualFileList(new ArrayList(stylesheets.values()), new HashSet(Arrays.asList(new String[] { MimeMappings.CSS })));
            fileSet = new TreeSet(new Comparator()
            {
                public int compare(Object arg0, Object arg1)
                {
                    VirtualFile f0 = (VirtualFile) arg0, f1 = (VirtualFile) arg1;
                    return f0.getName().compareTo(f1.getName());
                }
            }); // TreeSet<VirtualFile>
            fileSet.addAll(fileList);
        }
        catch (ConfigurationException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        
        return fileSet;
    }

    /**
     * 
     * @param configuration
     * @param fileList
     * @return
     */
    private boolean loadCompilationUnits(ToolsConfiguration configuration, Set fileSet, CompilerSwcContext swcContext, int[] checksums)
    {
        if (data.cacheName == null) // note: NOT (cacheName == null)
        {
            return true;
        }
        
        RandomAccessFile cacheFile = null;

        try
        {
            cacheFile = new RandomAccessFile(data.cacheName, "r");
            flex2.compiler.API.loadCompilationUnits(configuration, data.fileSpec, data.sourceList,
                                             data.sourcePath, data.resources, data.bundlePath,
                                             data.sources = new ArrayList(),
                                             data.units = new ArrayList(),
                                             checksums,
                                             data.swcDefSignatureChecksums = new HashMap(),
                                             data.swcFileChecksums = new HashMap(),
                                             cacheFile, data.cacheName);

            /* 
            for (int i = 0, size = data.sources.size(); i < size; i++)
            {
            	Object obj = data.sources.get(i);
            	if (obj instanceof String)
            	{
	            	String name = (String) obj;
	            	Source s = swcContext.getSource(name);
	            	data.sources.set(i, s);
	            	data.units.set(i, s != null ? s.getCompilationUnit() : null);
            	}
            }
            */
        }
        catch (FileNotFoundException ex)
        {
            ThreadLocalToolkit.logInfo(ex.getMessage());
            // if the cache file is not found, no big deal... return true so that we recompile.
            return true;
        }
        catch (IOException ex)
        {
            ThreadLocalToolkit.logInfo(ex.getMessage());

            if (!setupSourceContainers(configuration, fileSet))
            {
                return false;
            }
        }
        finally
        {
            if (cacheFile != null) try { cacheFile.close(); } catch (IOException ex) {}
        }
        
        return true;
    }

    /**
     * Compiles the <code>Library</code>. This method does not link the <code>Library</code>.
     * 
     * @param incremental If <code>true</code>, build incrementally; if <code>false</code>, rebuild.
     * @return  {@link Builder#OK} if this method call resulted in compilation of some/all parts of the application;
     *          {@link Builder#LINK} if this method call did not compile anything in the application but advise the caller to link again;
     *          {@link Builder#SKIP} if this method call did not compile anything in the application;
     *          {@link Builder#FAIL} if this method call encountered errors during compilation.
     */
    protected int compile(boolean incremental)
    {
        messages.clear();
        
        // if nothing has been built yet, let's rebuild.
        if (data == null || !incremental)
        {
            return recompile(false);
        }

        // if there is no configuration, use the default... but don't populate this.configuration.
        OEMConfiguration c;
        if (configuration == null)
        {
            c = (OEMConfiguration) getDefaultConfiguration(true);
        }
        else
        {
            c = OEMUtil.getLibraryConfiguration(constructCommandLine(configuration), configuration.keepLinkReport(),
                                                OEMUtil.getLogger(logger, messages), resolver, mimeMappings);
        }
        
        // if c is null, which indicates problems, this method will return.
        if (c == null)
        {
            clean(false, false, false);
            return FAIL;
        }
        else if (configuration != null && configuration.keepConfigurationReport())
        {
            configurationReport = OEMUtil.formatConfigurationBuffer(c.cfgbuf);
        }
        
        if (configuration != null)
        {
            configuration.cfgbuf = c.cfgbuf;
        }

        // initialize some ThreadLocal variables...
        cc.run();
        OEMUtil.init(OEMUtil.getLogger(logger, messages), mimeMappings, meter, resolver, cc);

        // if there are no SWC inputs, output an error and return -1
        VirtualFile[] includeLibs = (c.configuration == null) ? null : c.configuration.getCompilerConfiguration().getIncludeLibraries();
        if (sources.size() == 0 && classes.size() == 0 && namespaces.size() == 0 &&
            resourceBundles.size() == 0 && files.size() == 0 && stylesheets.size() == 0 &&
            (includeLibs == null || includeLibs.length == 0))
        {
            ThreadLocalToolkit.log(new flex2.compiler.common.ConfigurationException.NoSwcInputs( null, null, -1 ));
            clean(false, false, false);
            return FAIL;
        }
        
        // if there is any problem getting the licenses, this method will return.
        Map licenseMap = OEMUtil.getLicenseMap(c.configuration);

        API.setupHeadless(c.configuration);
        NameMappings mappings = flex2.compiler.API.getNameMappings(c.configuration), copy = mappings.copy();
        
        data.sourcePath.clearCache();
        data.bundlePath.clearCache();
        data.resources.refresh();

        CompilerConfiguration compilerConfig = c.configuration.getCompilerConfiguration();
        compilerConfig.setMetadataExport(true);

        if (output != null || directory != null)
        {
        	OEMUtil.setGeneratedDirectory(compilerConfig, output != null ? output : directory);
        }

        flex2.compiler.Transcoder[] transcoders = flex2.tools.API.getTranscoders(c.configuration);
        flex2.compiler.Compiler[] compilers = flex2.tools.API.getCompilers(compilerConfig, mappings, transcoders);
        
        CompilerSwcContext swcContext = new CompilerSwcContext(false, true,
															   compilerConfig.getCompatibilityVersionString());
        try
        {
	        swcContext.load( compilerConfig.getLibraryPath(),
	                         compilerConfig.getExternalLibraryPath(),
	                         null,
	                         compilerConfig.getIncludeLibraries(),
	                         mappings,
	                         I18nUtils.getTranslationFormat(compilerConfig),
	                         data.swcCache );
        }
        catch (SwcException ex)
        {
        	clean(false, false, false);
        	return FAIL;
        }
        data.includes = new HashSet(swcContext.getIncludes());
        data.excludes = new HashSet(swcContext.getExterns());
        c.configuration.addExterns( swcContext.getExterns() );
        c.configuration.addIncludes( swcContext.getIncludes() );
        
        data.swcArchiveFiles = new HashMap(swcContext.getIncludeFiles());

        int count = flex2.compiler.API.validateCompilationUnits(data.fileSpec, data.sourceList, data.sourcePath,
                                                                data.bundlePath, data.resources, swcContext,
                                                                data.perCompileData, false, c.configuration);

        // If checksum is different, rebuild.
        if (OEMUtil.isRecompilationNeeded(data, swcContext, c))
        {
            clean(true, false, false);
            return recompile(true);
        }

        // If --include-sources is different, rebuild.
        Set fileSet = null;
        if ((fileSet = processSources()) == null)
        {
            clean(false, false, false);
            return FAIL;
        }

        // If --include-stylesheets is different, rebuild.
        fileSet.addAll(processStylesheets());

        boolean isFileSpecDifferent = isDifferent(data.fileSet, fileSet);
        if (isFileSpecDifferent)
        {
            clean(true, false, false);
            return recompile(true);
        }

        Map classes = new TreeMap();
        Set nsComponents = null;

        if ((nsComponents = processInputs(swcContext, copy, classes)) == null)
        {
            clean(false, false, false);
            return FAIL;
        }

        // If the other --include-* are different, build incrementally.
        boolean isDifferent = isDifferent(data.classes, classes.keySet());
        if (count > 0 || isDifferent || isResourceBundleListDifferent() ||
                data.swcChecksum != swcContext.checksum())
        {
            // create a symbol table
            SymbolTable symbolTable = new SymbolTable(data.perCompileData);
            data.configuration = c.configuration;
            data.nsComponents = nsComponents;
            data.classes = classes.keySet();
            data.fileSet = fileSet;
            data.linkChecksum = c.cfgbuf.link_checksum_ts();
            data.swcChecksum = swcContext.checksum();
                
            // compile
            data.sources = new ArrayList();
            data.units = compile(compilers, swcContext, symbolTable, licenseMap, classes, 
            					data.sources, c);
            boolean forcedToStop = API.forcedToStop();
            if (data.units == null || forcedToStop)
            {
                clean(true, false, false);
                return FAIL;
            }
            else
            {
                clean(false, false, false);
                return OK;               
            }
        }
        else
        {
        	int retVal = SKIP;
        	if (data != null)
        	{
        		flex2.compiler.API.displayWarnings(data.units);
                if (data.linkChecksum != c.cfgbuf.link_checksum_ts())
                {
                	retVal = LINK;
                }
        	}
        	else
        	{
        		retVal = LINK;
        	}
            data.linkChecksum = c.cfgbuf.link_checksum_ts();
            data.swcChecksum = swcContext.checksum();
            if (API.forcedToStop()) retVal = FAIL;
            if (retVal == LINK)
            {
            	clean(false, false, false, false, false);
            }
            else
            {
            	clean(false, false, false);
            }
            return retVal;
        }
    }

    /**
     * 
     * @param compilers
     * @param swcContext
     * @param symbolTable
     * @param licenseMap
     * @param classes
     */
    private List compile(flex2.compiler.Compiler[] compilers, CompilerSwcContext swcContext,
                         SymbolTable symbolTable, Map licenseMap, Map classes, 
                         List sources, OEMConfiguration oemConfig)
    {
        List units = null;
        Map rbFiles = new HashMap();
        
        try
        {
            // compile
            units = flex2.compiler.API.compile(data.fileSpec, data.sourceList, classes.values(), data.sourcePath, data.resources,
                                                  data.bundlePath, swcContext, symbolTable, data.configuration,
                                                  compilers, new CompcPreLink(rbFiles, new ArrayList(resourceBundles)), licenseMap, sources);
        }
        catch (LicenseException ex)
        {
            ThreadLocalToolkit.logError(ex.getMessage());
        }
        catch (CompilerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        finally
        {
            data.sourcePath.clearCache();
            data.bundlePath.clearCache();
            data.resources.refresh();
            
            data.classes = classes.keySet();
            data.rbFiles = rbFiles;
            
            Map m = data.configuration.getCSSArchiveFiles();
            if (m != null)
            {
            	data.cssArchiveFiles = new HashMap(m);
            }
            
            m = data.configuration.getL10NArchiveFiles();
            if (m != null)
            {
            	data.l10nArchiveFiles = new HashMap(m);
            }

            OEMUtil.saveSignatureChecksums(units, data, data.configuration);
           	OEMUtil.saveSwcFileChecksums(swcContext, data, data.configuration);
        }
        
        return units;
    }

    /**
     * Links the <code>Library</code>. This method writes the output to the output stream specified by
     * the client. You should use a buffered output stream for best performance.
     * 
     * <p>
     * This method is protected. In most circumstances, the client only needs to call the 
     * <code>build()</code> method. Subclasses can call this method so that it links and outputs
     * the application without recompiling.
     * 
     * @param out The <code>OutputStream</code>.
     * @return The size of the application, in bytes.
     * @throws IOException Thrown when an I/O error occurs during linking.
     */
    protected long link(OutputStream out) throws IOException
    {
        if (data == null || data.units == null)
        {
            return 0;
        }

        boolean hasChanged = (configuration == null) ? false : configuration.hasChanged();
        OEMConfiguration c = null;
        flex2.compiler.common.Configuration config = null;

        if (hasChanged)
        {
            c = OEMUtil.getLinkerConfiguration(configuration.getLinkerOptions(), configuration.keepLinkReport(),
                                               OEMUtil.getLogger(logger, messages), mimeMappings, resolver,
                                               data.configuration, configuration.newLinkerOptionsAfterCompile,
                                               data.includes, data.excludes);
            if (c == null)
            {
                clean(false, false, false, false, false);
                return 0;
            }
            
            config = c.configuration;
        }
        else
        {
            config = data.configuration;
        }

        try
        {
            OEMUtil.init(OEMUtil.getLogger(logger, messages), mimeMappings, meter, resolver, cc);
            
            SimpleMovie temp = data.movie;
            data.movie = flex2.compiler.swc.API.link((flex2.linker.Configuration) config, data.units);
            
            // link
            SwcArchive archive = null;
            Map archiveFiles = new TreeMap();
            if (data.swcArchiveFiles != null) archiveFiles.putAll(data.swcArchiveFiles);
            if (data.cssArchiveFiles != null) archiveFiles.putAll(data.cssArchiveFiles);
            if (data.l10nArchiveFiles != null) archiveFiles.putAll(data.l10nArchiveFiles);
            archiveFiles.putAll(files);
            
            if (directory != null)
            {
                archive = new SwcDirectoryArchive(FileUtil.getCanonicalPath(directory));
                flex2.compiler.swc.API.exportSwc(archive,
                								 archiveFiles,
                								 this.stylesheets,
                								 (flex2.linker.Configuration) config,
                								 (SwcMovie) data.movie,
                								 new ArrayList(data.nsComponents),
                								 data.swcCache,
                								 data.rbFiles);
            }
            
            long size = 0;
            
            if (out != null)
            {
                // TODO PERFORMANCE: A lot of unnecessary recopying and buffering here
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                archive = new SwcWriteOnlyArchive("oem", baos);
                flex2.compiler.swc.API.exportSwc(archive,
                								 archiveFiles,
                								 this.stylesheets,
                								 (flex2.linker.Configuration) config,
                								 (SwcMovie) data.movie,
                								 new ArrayList(data.nsComponents),
                								 data.swcCache,
                								 data.rbFiles);
                
                ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
                FileUtil.streamOutput(in, out);
                size = baos.size();
            }

            if (hasChanged && temp != null)
            {
                data.movie = temp;
            }

            return size;
        }
        catch (LinkerException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
            return 0;
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
            return 0;
        }
        catch (Exception ex)
        {
            if (ex instanceof IOException)
            {
                throw (IOException) ex;
            }
            ThreadLocalToolkit.logError(ex.getMessage());
            return 0;
        }
        finally
        {
            clean(false, false, false, true, false);
        }
    }

    /**
     * 
     * @param cleanData
     * @param cleanCache
     * @param cleanOutput
     */
    private void clean(boolean cleanData, boolean cleanCache, boolean cleanOutput)
    {
        clean(cleanData, cleanCache, cleanOutput, true, false);
    }

    /**
     * 
     * @param cleanData
     * @param cleanCache
     * @param cleanOutput
     * @param cleanConfig
     */
    private void clean(boolean cleanData, boolean cleanCache, boolean cleanOutput, boolean cleanConfig, boolean cleanMessages)
    {
        // make absolutely sure that these thread local variables are cleared.
        OEMUtil.clean();

        if (configuration != null && cleanConfig)
        {
            configuration.reset();
        }

        if (cleanData)
        {
            data = null;
            configurationReport = null;
        }
        
        if (cleanCache)
        {               
            if (cacheName != null)
            {
                File dead = FileUtil.openFile(cacheName);
                if (dead != null && dead.exists())
                {
                    dead.delete();
                }               
                cacheName = null;
            }
        }
        
        if (cleanOutput)
        {   
            if (output != null && output.exists())
            {
                output.delete();
            }
        }
        
        if (cleanMessages)
        {
            messages.clear();
        }
    }
    
    /**
     * 
     * @param s1
     * @param s2
     * @return
     */
    private boolean isDifferent(Collection s1, Collection s2)
    {
        for (Iterator i = s2.iterator(); i.hasNext(); )
        {
            if (!s1.contains(i.next()))
            {
                return true;
            }
        }
        
        return s1.size() > s2.size();
    }
    
    /**
     * 
     * @return
     */
    private boolean isResourceBundleListDifferent()
    {
    	int size1 = (data == null || data.rbFiles == null) ? 0 : data.rbFiles.size();
    	int size2 = resourceBundles == null ? 0 : resourceBundles.size();
    	return size1 != size2;
    }
}

