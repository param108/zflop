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
import flash.swf.*;
import flex2.compiler.*;
import flex2.compiler.SourceList.UnsupportedFileType;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.as3.EmbedExtension;
import flex2.compiler.as3.SignatureExtension;
import flex2.compiler.as3.StyleExtension;
import flex2.compiler.as3.binding.BindableExtension;
import flex2.compiler.as3.managed.ManagedExtension;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.common.Configuration;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.media.*;
import flex2.compiler.swc.SwcCache;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Clement Wong
 */
public final class API extends Tool
{
    /**
     * This method is the entry point for the web tier compiler.  Please coordinate any
     * API changes with the FDS team.
     */
	public static Target compile(VirtualFile targetFile, Configuration configuration, SwcCache swcCache, Map licenseMap)
	    throws CompilerException, LicenseException
	{
		Target target = new Target();

		try
		{
			if (configuration.benchmark())
			{
				flex2.compiler.API.runBenchmark();
			}
			else
			{
				flex2.compiler.API.disableBenchmark();
			}

			target.configuration = configuration;

			flex2.compiler.API.useAS3();
			flex2.compiler.API.usePathResolver();
			flex2.compiler.API.setupHeadless(configuration);

			// set up for localizing messages
			LocalizationManager l10n = new LocalizationManager();
			l10n.addLocalizer( new XLRLocalizer() );
			l10n.addLocalizer( new ResourceBundleLocalizer() );
			ThreadLocalToolkit.setLocalizationManager( l10n );

			checkSupportedTargetMimeType(targetFile);

			List virtualFileList = new ArrayList();
			virtualFileList.add(targetFile);

			CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();
			NameMappings mappings = flex2.compiler.API.getNameMappings(configuration);

			//	get standard bundle of compilers, transcoders
			flex2.compiler.Transcoder[] transcoders = getTranscoders(configuration);
			flex2.compiler.Compiler[] compilers = getCompilers(compilerConfig, mappings, transcoders);

			// create a FileSpec...
			target.fileSpec = new FileSpec(Collections.EMPTY_LIST, getFileSpecMimeTypes());

			VirtualFile[] asClasspath = compilerConfig.getSourcePath();

			// create a SourceList...
			target.sourceList = new SourceList(virtualFileList,
			                                   asClasspath,
			                                   targetFile,
			                                   getSourcePathMimeTypes());
			// create a SourcePath...
			target.sourcePath = new SourcePath(asClasspath,
			                                   targetFile,
			                                   getSourcePathMimeTypes(),
			                                   compilerConfig.allowSourcePathOverlap());

			// create a ResourceContainer
			target.resources = new ResourceContainer();

			target.bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), targetFile);

			if (ThreadLocalToolkit.getBenchmark() != null)
			{
				ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new Compiler.InitialSetup()));
			}

			// load SWCs
			CompilerSwcContext swcContext = new CompilerSwcContext(configuration.getCompatibilityVersionString());
			swcContext.load(compilerConfig.getLibraryPath(),
							Configuration.getAllExcludedLibraries(compilerConfig, configuration),
			                compilerConfig.getThemeFiles(),
			                compilerConfig.getIncludeLibraries(),
			                mappings,
			                I18nUtils.getTranslationFormat(compilerConfig),
			                swcCache);
			configuration.addExterns(swcContext.getExterns());
			configuration.addIncludes( swcContext.getIncludes() );
			configuration.getCompilerConfiguration().addDefaultsCssFiles(swcContext.getDefaultsStyleSheets());
			configuration.getCompilerConfiguration().addThemeCssFiles(swcContext.getThemeStyleSheets());

			if (ThreadLocalToolkit.getBenchmark() != null)
			{
				ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new Compiler.LoadedSWCs(swcContext.getNumberLoaded())));
			}

			// validate CompilationUnits in FileSpec, SourceList and SourcePath
			flex2.compiler.API.validateCompilationUnits(target.fileSpec, target.sourceList, target.sourcePath,
					target.bundlePath, target.resources, swcContext, null, false, configuration);

			// create a SymbolTable...
			final SymbolTable symbolTable = SymbolTable.newSymbolTable(configuration);
			target.perCompileData = symbolTable.perCompileData;

			// compile
			target.units = flex2.compiler.API.compile(target.fileSpec, target.sourceList, null, target.sourcePath, target.resources,
			                                          target.bundlePath, swcContext, symbolTable, configuration, compilers,
					                                  new PreLink(), licenseMap, new ArrayList());

			return target;
		}
		catch (LicenseException ex)
		{
			throw ex;
		}
		catch (CompilerException ex)
		{
			throw ex;
		}
		catch (Throwable t)
		{
			String message = t.getMessage();
            if (message == null)
            {
                message = t.getClass().getName();
            }
            ThreadLocalToolkit.logError(message);
            throw new CompilerException(message);
		}
		finally
		{
			flex2.compiler.API.removePathResolver();
		}
	}

	public static long optimize(InputStream in, OutputStream out, Configuration configuration) throws IOException
	{
		// decoder
		Movie movie = new Movie();
		TagDecoder tagDecoder = new TagDecoder(in);
		MovieDecoder movieDecoder = new MovieDecoder(movie);
		tagDecoder.parse(movieDecoder);

		// optimize
		optimize(movie, configuration);

        //TODO PERFORMANCE: A lot of unnecessary recopying and buffering here
		// encode
		TagEncoder handler = new TagEncoder();
		MovieEncoder encoder = new MovieEncoder(handler);
		encoder.export(movie);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		handler.writeTo(baos);
		out.write(baos.toByteArray());
        
		return baos.size();
	}

	public static void optimize(Movie m, Configuration configuration)
	{
		// don't keep debug opcodes
		// abc merge
		// peephole optimization
		m.enableDebugger = null;
		m.uuid = null;
		PostLink postLink = (configuration != null) ? new PostLink(configuration) : new PostLink(false, true);
		postLink.run(m);
	}

	public static long optimize(InputStream in, OutputStream out) throws IOException
	{
		return optimize(in, out, null);
	}

	public static Transcoder[] getTranscoders( Configuration cfg )
	{
		// create a list of supported transcoders
		return new Transcoder[]{new JPEGTranscoder(), new LosslessImageTranscoder(), //new JAITranscoder(),
								new SVGTranscoder(), new SoundTranscoder(),
								new MovieTranscoder(), new FontTranscoder( cfg ),
                                new DataTranscoder(), new XMLTranscoder(),
                                new SkinTranscoder()
        };
	}

    // IMPORTANT: If you update extensions in this method, you may also want
    // to update them in flex2.compile.mxml.ImplementationCompiler
	public static flex2.compiler.Compiler[] getCompilers(CompilerConfiguration compilerConfig, NameMappings mappings,
	                                                     Transcoder[] transcoders)
	{
		// support .AS3
		flex2.compiler.as3.Compiler asc = new flex2.compiler.as3.Compiler(compilerConfig);

		// signature generation should occur before other extensions can touch the syntax tree
        if (!compilerConfig.getDisableIncrementalOptimizations())
        {
            SignatureExtension.init(compilerConfig);
		    asc.addCompilerExtension(SignatureExtension.getInstance());
        }
		final String gendir = (compilerConfig.keepGeneratedActionScript()
		                            ? compilerConfig.getGeneratedDirectory()
		                            : null);
		asc.addCompilerExtension(new EmbedExtension(transcoders, gendir, compilerConfig.showDeprecationWarnings()));
		asc.addCompilerExtension(new StyleExtension());
		asc.addCompilerExtension(new BindableExtension(gendir));
		asc.addCompilerExtension(new ManagedExtension(gendir, compilerConfig.getServicesDependencies()));
        // asc.addCompilerExtension(new flex2.compiler.util.TraceExtension());
        
		// support MXML
		flex2.compiler.mxml.Compiler mxmlc = new flex2.compiler.mxml.Compiler(compilerConfig, compilerConfig,
		                                                                      mappings, transcoders);

		// support ABC
		flex2.compiler.abc.Compiler abc = new flex2.compiler.abc.Compiler(compilerConfig);
		abc.addCompilerExtension(new StyleExtension());
		// abc.addCompilerExtension(new flex2.compiler.util.TraceExtension());

		// support i18n (.properties)
		flex2.compiler.i18n.Compiler prop = new flex2.compiler.i18n.Compiler(compilerConfig, transcoders);

		// support CSS
		flex2.compiler.css.Compiler css = new flex2.compiler.css.Compiler(compilerConfig, transcoders);

		return new flex2.compiler.Compiler[]{asc, mxmlc, abc, prop, css};
	}

	public static void checkSupportedTargetMimeType(VirtualFile targetFile) throws CompilerException
	{
		String[] mimeTypes = getTargetMimeTypes();

		for (int i = 0, length = mimeTypes.length; i < length; i++)
		{
			if (mimeTypes[i].equals(targetFile.getMimeType()))
			{
				return;
			}
		}

        UnsupportedFileType ex = new UnsupportedFileType(targetFile.getName());
        ThreadLocalToolkit.log(ex);
        throw ex;
	}

	public static String[] getFileSpecMimeTypes()
	{
		return new String[]{MimeMappings.AS, MimeMappings.MXML, MimeMappings.CSS, MimeMappings.ABC};
	}

	public static String[] getSourceListMimeTypes()
	{
		return new String[]{MimeMappings.AS, MimeMappings.MXML, MimeMappings.CSS};
	}

	public static String[] getSourcePathMimeTypes()
	{
		return new String[]{MimeMappings.AS, MimeMappings.MXML};
	}

	public static String[] getTargetMimeTypes()
	{
		return new String[]{MimeMappings.AS, MimeMappings.MXML, MimeMappings.CSS};
	}

	// error messages
}
