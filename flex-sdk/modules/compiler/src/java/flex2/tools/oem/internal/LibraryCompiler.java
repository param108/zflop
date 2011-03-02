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

package flex2.tools.oem.internal;

import java.io.*;
import java.net.*;
import java.util.*;

import flash.localization.LocalizationManager;
import flash.localization.ResourceBundleLocalizer;
import flex2.compiler.API;
import flex2.compiler.common.DefaultsConfigurator;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.SwcException;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.linker.FlexMovie;
import flex2.tools.CompcConfiguration;
import flex2.tools.Compiler;
import flex2.tools.oem.Library;
import flex2.tools.oem.Report;

/**
 * @version 2.0.1
 * @author Clement Wong
 *
 */
public class LibraryCompiler
{
	public static void main(String[] args)
	{
	    final OEMConsole console = new OEMConsole();

        try
        {
            init();
            run(console, args);
        }
        catch (ConfigurationException ex)
        {
            Compiler.processConfigurationException(ex, "compc");
        }
        catch (SwcException ex)
        {
            assert ThreadLocalToolkit.errorCount() > 0;
        }
        catch (Throwable t) // IOException, Throwable
        {
            ThreadLocalToolkit.logError(t.getMessage());
            t.printStackTrace();
        }
        finally
        {
            clean();
        }

        System.exit(console.errorCount());
	}

	public static void run(OEMConsole console, String[] args)
        throws ConfigurationException, IOException, URISyntaxException
	{
	    ThreadLocalToolkit.setLogger(console);
        
		final Library lib = new Library();
		lib.setLogger(console);
		OEMConfiguration c1 = (OEMConfiguration) lib.getDefaultConfiguration();
		CompcConfiguration c2 = getCompcConfiguration(args);
		OEMConfiguration c3 = new OEMConfiguration(null, c2);
		c1.importDefaults(c3);
		lib.setConfiguration(c1);
	
		// transfer the value of compute-digest from CompcConfiguration to OEMConfiguration.
		c1.enableDigestComputation(c2.getComputeDigest());
		
		for (Iterator i = c2.getClasses().iterator(); i.hasNext(); )
		{
			lib.addComponent((String) i.next());
		}
		
		List fileList = flex2.compiler.API.getVirtualFileList(c2.getIncludeSources(),
															  new HashSet(Arrays.asList(flex2.tools.API.getSourcePathMimeTypes())));

		for (Iterator i = fileList.iterator(); i.hasNext(); )
		{
			lib.addComponent(new File(((VirtualFile) i.next()).getName()));
		}

		Map ss = c2.getStylesheets();
		for (Iterator i = ss.keySet().iterator(); i.hasNext(); )
		{
			String key = (String) i.next();
			lib.addStyleSheet(key, new File(((VirtualFile) ss.get(key)).getName()));
		}

		for (Iterator i = c2.getIncludeResourceBundles().iterator(); i.hasNext(); )
		{
			lib.addResourceBundle((String) i.next());
		}

		for (Iterator i = c2.getNamespaces().iterator(); i.hasNext(); )
		{
			lib.addComponent(new URI((String) i.next()));
		}
		
		Map m = c2.getFiles();
		for (Iterator i = m.keySet().iterator(); i.hasNext(); )
		{
			String key = (String) i.next();
			lib.addArchiveFile(key, new File(((VirtualFile) m.get(key)).getName()));
		}
        
        try
		{
            lib.load(new BufferedInputStream(new FileInputStream(new File(c2.getOutput() + ".incr"))));
            // load() wipes out our ThreadLocal and we lose our logger
            ThreadLocalToolkit.setLogger(console);
		}
		catch (IOException ex)
		{
		}
        
		long size = 0;
		if ((size = lib.build(new BufferedOutputStream(new FileOutputStream(new File(c2.getOutput()))), true)) == 0)
		{
            ThreadLocalToolkit.logError("Build unsuccessful.");
		}
		else
		{
			System.out.println(c2.getOutput() + " (" + size + " bytes)");
		    if (c2.generateRBList() && c2.getRBListFileName() != null)
		    {
		    	Report r = lib.getReport();
		    	String[] rbNames = r.getResourceBundleNames();
		    	HashSet set = new HashSet();
		    	for (int i = 0, l = rbNames == null ? 0 : rbNames.length; i < l; i++)
		    	{
		    		set.add(rbNames[i]);
		    	}
		    	String list = FlexMovie.dumpRBList(set);
		    	FileUtil.writeFile(c2.getRBListFileName(), list);
		    }

		}

		lib.save(new BufferedOutputStream(new FileOutputStream(new File(c2.getOutput() + ".incr"))));		
		lib.clean();
	}
	
	private static CompcConfiguration getCompcConfiguration(String[] args)
        throws ConfigurationException, IOException
	{
        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CompcConfiguration.class,
        													 CompcConfiguration.getAliases());
        cfgbuf.setDefaultVar("include-classes");
        DefaultsConfigurator.loadCompcDefaults( cfgbuf );
        Object obj = Compiler.processConfiguration(ThreadLocalToolkit.getLocalizationManager(),
        										   "compc",
        										   args,
        										   cfgbuf,
        										   CompcConfiguration.class,
        										   "include-classes");
        return (CompcConfiguration) obj;
	}
	
	static final void init()
	{
        API.useAS3();
        API.usePathResolver();
        setupLocalizationManager();
	}
    
    static final void clean()
    {
        API.removePathResolver();
        ThreadLocalToolkit.setLogger(null);
        ThreadLocalToolkit.setLocalizationManager(null);
    }

	static final LocalizationManager setupLocalizationManager()
	{
        // set up for localizing messages
		LocalizationManager l10n = new LocalizationManager();
		l10n.addLocalizer(new ResourceBundleLocalizer());
		ThreadLocalToolkit.setLocalizationManager(l10n);
		
		return l10n;
	}
}
