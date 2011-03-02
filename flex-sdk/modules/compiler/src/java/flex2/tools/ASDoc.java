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

package flex2.tools;

import flex2.compiler.*;
import flex2.compiler.swc.SwcException;
import flex2.compiler.common.DefaultsConfigurator;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.util.*;
import flash.localization.LocalizationManager;
import flash.localization.XLRLocalizer;
import flash.localization.ResourceBundleLocalizer;
import flash.util.Trace;

/**
 * The entry-point for ASDoc.  See asdoc.API for more information on how ASDoc works.
 *
 * @author Brian Deitte
 */
public class ASDoc extends Tool
{
    public static void main(String[] args)
    {
	    asdoc(args);
	    System.exit(ThreadLocalToolkit.errorCount());
    }

	public static void asdoc(String[] args)
	{
		String outputStr = null, templatesPath = null;
		boolean keepXML = false;
        try
        {
	        flex2.compiler.API.useAS3();

            ConfigurationBuffer cfgbuf = new ConfigurationBuffer(ASDocConfiguration.class, ASDocConfiguration.getAliases());
            cfgbuf.setDefaultVar("doc-classes");

            // setup the path resolver
	        flex2.compiler.API.usePathResolver();

            // set up for localizing messages
            LocalizationManager l10n = new LocalizationManager();
            l10n.addLocalizer( new XLRLocalizer() );
            l10n.addLocalizer( new ResourceBundleLocalizer() );
            ThreadLocalToolkit.setLocalizationManager( l10n );

            // setup a console-based logger...
	        flex2.compiler.API.useConsoleLogger();

            // process configuration
	        DefaultsConfigurator.loadASDocDefaults( cfgbuf );

	        ASDocConfiguration configuration = (ASDocConfiguration) Compiler.processConfiguration(
                l10n, "asdoc", args, cfgbuf, ASDocConfiguration.class, "doc-classes");

            flex2.compiler.API.useConsoleLogger(true, true, configuration.getWarnings(), true);
            
	        if (configuration.benchmark())
	        {
		        flex2.compiler.API.runBenchmark();
	        }
	        else
	        {
		        flex2.compiler.API.disableBenchmark();
	        }

	        keepXML = configuration.keepXml();
	        outputStr = configuration.getOutput();
	        templatesPath = configuration.getTemplatesPath();

	        if (Trace.asdoc) System.out.println("Creating ASDoc_Config.xml");
	        flex2.compiler.asdoc.API.createASDocConfig(configuration);

	        if (Trace.asdoc) System.out.println("Creating overviews.xml");
	        flex2.compiler.asdoc.API.createOverviews(configuration);

	        if (Trace.asdoc) System.out.println("Creating toplevel.xml");
	        flex2.compiler.asdoc.API.createTopLevelXML(configuration, l10n);

	        if (Trace.asdoc) System.out.println("Creating toplevel_classes.xml");
	        flex2.compiler.asdoc.API.createTopLevelClassesXML(outputStr, templatesPath);

	        if (! configuration.skipXsl())
	        {
		        if (Trace.asdoc) System.out.println("Create HTML pages");
		        flex2.compiler.asdoc.API.createHTML(outputStr, templatesPath, configuration);

		        if (Trace.asdoc) System.out.println("Copying files");
		        flex2.compiler.asdoc.API.copyFiles(outputStr, templatesPath);

		        ThreadLocalToolkit.log(new OutputMessage(outputStr));
	        }
        }
        catch (ConfigurationException ex)
        {
            displayStartMessage();
            Compiler.processConfigurationException(ex, "asdoc");
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
        catch (Throwable t) // IOException, Throwable
        {
	        if (Trace.error)
		        t.printStackTrace();

            ThreadLocalToolkit.logError(t.getMessage());
        }
	    finally
        {
	        if (! keepXML && outputStr != null && templatesPath != null)
	        {
		        flex2.compiler.asdoc.API.removeXML(outputStr, templatesPath);
	        }

	        if (ThreadLocalToolkit.getBenchmark() != null)
	        {
		        ThreadLocalToolkit.getBenchmark().totalTime();
		        ThreadLocalToolkit.getBenchmark().peakMemoryUsage(true);
	        }

	        flex2.compiler.API.removePathResolver();
        }
    }

	public static void displayStartMessage()
	{
		LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
		System.out.println(l10n.getLocalizedTextString(new StartMessage(VersionInfo.buildMessage())));
	}

	public static class StartMessage extends CompilerMessage.CompilerInfo
	{
		public StartMessage(String buildMessage)
		{
			super();
			this.buildMessage = buildMessage;
		}

		public final String buildMessage;
	}

    public static class OutputMessage extends CompilerMessage.CompilerInfo
    {
        public OutputMessage(String location)
        {
            this.location = location;
        }
	    public String location;
    }
}

