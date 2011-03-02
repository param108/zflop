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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

import flash.localization.LocalizationManager;
import flash.localization.ResourceBundleLocalizer;
import flash.swf.Movie;
import flash.swf.MovieDecoder;
import flash.swf.MovieEncoder;
import flash.swf.TagDecoder;
import flash.swf.TagEncoder;
import flex2.compiler.common.ConfigurationException;
import flex2.compiler.common.DefaultsConfigurator;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationFilter;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.io.FileUtil;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 * The post-link optimizer as a command-line tool...
 * 
 * @author Clement Wong
 */
public class Optimizer
{
	public static void main(String[] args)
	{
        flex2.compiler.API.useAS3();

        // setup the path resolver
        flex2.compiler.API.usePathResolver();

        LocalizationManager l10n = new LocalizationManager();
        l10n.addLocalizer( new ResourceBundleLocalizer() );
        ThreadLocalToolkit.setLocalizationManager(l10n);
        
		flex2.compiler.API.useConsoleLogger();
 
		// create of list of options that we want. All the other options that 
		// we inherit will be filtered out.
		final String[] configVars = new String[]{"help", "version", "load-config", "input", "output",
											   "compiler.keep-as3-metadata"};
		Arrays.sort(configVars);
		ConfigurationBuffer cfgbuf = new ConfigurationBuffer(OptimizerConfiguration.class, 
				new HashMap(),
				new ConfigurationFilter() {

			public boolean select(String name)
			{
				return Arrays.binarySearch(configVars, name) >= 0;
			}

		});
		
        cfgbuf.setDefaultVar("input");
        try
        {
        	DefaultsConfigurator.loadMinimumDefaults(cfgbuf);
	        OptimizerConfiguration configuration = (OptimizerConfiguration) Compiler.processConfiguration(
	                l10n, "optimizer", args, cfgbuf, OptimizerConfiguration.class, "input", true);

	        // user is not allowed to control "debug" and "optimize" settings so
	        // set them here.
    		configuration.setKeepDebugOpcodes(false);
    		configuration.setOptimize(true);
	       
        	//OptimizerConfiguration configuration = (OptimizerConfiguration) processConfiguration(cfgbuf, args, l10n);
        	String input = configuration.getInput(), output = configuration.getOutput();
        	
        	File inputFile = FileUtil.openFile(input), outputFile = FileUtil.openFile(output);
        	if (inputFile.exists())
        	{
        		InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        		System.out.println(input + " (" + inputFile.length() + " bytes)");
        		
        		// decoder
        		Movie movie = new Movie();
        		TagDecoder tagDecoder = new TagDecoder(in);
        		MovieDecoder movieDecoder = new MovieDecoder(movie);
        		tagDecoder.parse(movieDecoder);

        		if (movie.version > 0) {
            		// optimize
            		flex2.tools.API.optimize(movie, configuration);

                    //TODO PERFORMANCE: A lot of unnecessary recopying and buffering here
            		// encode
            		TagEncoder handler = new TagEncoder();
            		MovieEncoder encoder = new MovieEncoder(handler);
            		encoder.export(movie);
            		ByteArrayOutputStream out = new ByteArrayOutputStream();
            		handler.writeTo(out);
            		
            		FileUtil.writeBinaryFile(output, out.toByteArray());
            		System.out.println(output + " (" + outputFile.length() + " bytes)");        			
        		}
        		else 
        		{
    				ThreadLocalToolkit.logError(l10n.getLocalizedTextString(new NotAValidSwfFile(inputFile.getAbsolutePath())));
        		}
        		
        	}
        }
        catch (flex2.compiler.config.ConfigurationException ex)
        {
            Compiler.processConfigurationException(ex, "optimizer");
        }
        catch (IOException ex)
        {
        	ex.printStackTrace();
        }
	}
	
	public static class OptimizerConfiguration extends CommandLineConfiguration
	{
	    //
		// 'input' option
		//
		
		private String input = null;

		public String getInput()
		{
			return input;
		}

		public void cfgInput(ConfigurationValue val, String input) throws ConfigurationException
		{
	        this.input = input;
		}

		public static ConfigurationInfo getInputInfo()
		{
		    return new ConfigurationInfo(1, "filename")
		    {
		        public boolean isRequired()
		        {
		            return true;
		        }
		    };
		}

		//
		// 'output' option
		//
		
		private String output = "output.swf";
		
		public String getOutput()
		{
		    return output;
		}

		public void cfgOutput(ConfigurationValue val, String output) throws ConfigurationException
		{
	        this.output = output;
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

		
		/*
		 * Since we are filtering out a lot of command line options, we need to override validate to 
		 * keep it from throwing an exception.
		 * 
		 * (non-Javadoc)
		 * @see flex2.tools.CommandLineConfiguration#validate(flex2.compiler.config.ConfigurationBuffer)
		 */
		public void validate(ConfigurationBuffer cfgbuf) throws flex2.compiler.config.ConfigurationException
		{
			// nothing to validate
		}
		
		
	}
	
	public static class NotAValidSwfFile extends CompilerMessage.CompilerError
	{
		public String fileName;
		
		public NotAValidSwfFile(String fileName)
		{
			super();
			this.fileName = fileName;
		}
	}

}
