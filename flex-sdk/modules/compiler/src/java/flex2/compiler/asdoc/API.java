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

package flex2.compiler.asdoc;

import flex2.tools.*;
import flex2.tools.Compiler;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.CompilerException;
import flex2.compiler.SourceList;
import flex2.compiler.Transcoder;
import flex2.compiler.SourcePath;
import flex2.compiler.FileSpec;
import flex2.compiler.ResourceContainer;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.CompilerSwcContext;
import flex2.compiler.swc.Component;
import flex2.compiler.swc.SwcCache;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.CompilerMessage;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import flash.util.Trace;
import flash.util.FileUtils;
import flash.localization.LocalizationManager;

/**
 * The API class for ASDoc.  A call to ASDoc has four main parts:
 * 1. parameter handling, which done outside of this class in ConfigurationBuffer as well as in createASDocConfig()
 *    and createOverviews()
 * 2. a call to asc, done in createTopLevelXML()
 * 3. an avmplus call, done in createTopLevelClassesXML()
 * 4. XSL processing, done in createHTML()
 * For parameter handling and calling asc, this class works in the same way that Compiler
 * and Compc do. Parameters go through the Flex configuration scheme and the ASC call uses
 * Flex's infastructure for compiling. The avmplus call is a call to an executable
 * that contains avmplus as well as the abc code, and XSL processing is done by calling
 * org.apache.xalan.xslt.Process.
 *
 * @author Brian Deitte
 */
public class API
{
	public static boolean forceWindows = System.getProperty("asdoc.windows") != null;
	public static boolean forceMac = System.getProperty("asdoc.mac") != null;
	public static boolean forceLinux = System.getProperty("asdoc.linux") != null;

	public static void createASDocConfig(ASDocConfiguration config) throws CompilerException
	{
		String templatesPath = config.getTemplatesPath();
		BufferedWriter writer = null;
		Reader reader = null;
		try
		{
			writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(templatesPath + "ASDoc_Config.xml"), "UTF-8"));
			reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(templatesPath + "ASDoc_Config_Base.xml"), "UTF-8"));

			ASDocConfigHandler h = new ASDocConfigHandler(writer, config);
		    InputSource source = new InputSource(reader);

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
		    parser.parse(source, h);
		}
		catch (Exception e)
		{
			if (Trace.error)
				e.printStackTrace();

			CompilerMessage c = new CouldNotCreate("ASDoc_Config.xml", e.getMessage());
			ThreadLocalToolkit.log(c);
			throw c;
		}
		finally
		{
			if (writer != null)
			{
				try { writer.close(); } catch(IOException ioe) {}
			}
			if (reader != null)
			{
				try { reader.close(); } catch(IOException ioe) {}
			}
		}
	}

	public static void createOverviews(ASDocConfiguration config) throws CompilerException
	{
		String templatesPath = config.getTemplatesPath();
		BufferedWriter writer = null;
		Reader reader = null;
		try
		{
			writer = new BufferedWriter(new FileWriter(templatesPath + "overviews.xml"));
			reader = new BufferedReader(new FileReader(templatesPath + "Overviews_Base.xml"));

			OverviewsHandler h = new OverviewsHandler(writer, config);
			InputSource source = new InputSource(reader);

			SAXParserFactory factory = SAXParserFactory.newInstance();
		    SAXParser parser = factory.newSAXParser();
		    parser.parse(source, h);
		}
		catch (Exception e)
		{
			if (Trace.error)
				e.printStackTrace();

			CompilerMessage c = new CouldNotCreate("overviews.xml", e.getMessage());
			ThreadLocalToolkit.log(c);
			throw c;
		}
		finally
		{
			if (writer != null)
			{
				try { writer.close(); } catch(IOException ioe) {}
			}
			if (reader != null)
			{
				try { reader.close(); } catch(IOException ioe) {}
			}
		}
	}

	public static void createTopLevelXML(ASDocConfiguration configuration, LocalizationManager l10n)
			throws ConfigurationException, CompilerException
	{
		flex2.compiler.API.setupHeadless(configuration);

		String[] sourceMimeTypes = flex2.tools.API.getSourcePathMimeTypes();

		CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();

		// create a SourcePath...
		SourcePath sourcePath = new SourcePath(sourceMimeTypes, compilerConfig.allowSourcePathOverlap());
		sourcePath.addPathElements( compilerConfig.getSourcePath() );

		List[] array = flex2.compiler.API.getVirtualFileList(configuration.getDocSources(), java.util.Collections.EMPTY_SET,
															 new HashSet(Arrays.asList(sourceMimeTypes)),
															 sourcePath.getPaths());
		
		NameMappings mappings = flex2.compiler.API.getNameMappings(configuration);

		//	get standard bundle of compilers, transcoders
		Transcoder[] transcoders = flex2.tools.API.getTranscoders( configuration );
		flex2.compiler.Compiler[] compilers = getCompilers(compilerConfig, mappings, transcoders);

		// create a FileSpec... can reuse based on appPath, debug settings, etc...
		FileSpec fileSpec = new FileSpec(array[0], flex2.tools.API.getFileSpecMimeTypes(), false);

        // create a SourceList...
        SourceList sourceList = new SourceList(array[1], compilerConfig.getSourcePath(), null,
        									   flex2.tools.API.getSourceListMimeTypes(), false);

		ResourceContainer resources = new ResourceContainer();
		ResourceBundlePath bundlePath = new ResourceBundlePath(configuration.getCompilerConfiguration(), null);

		Map classes = new HashMap();
		List nsComponents = flex2.compiler.swc.API.setupNamespaceComponents(configuration.getNamespaces(), mappings,
				sourcePath, classes, configuration.getIncludeLookupOnly());
		flex2.compiler.swc.API.setupClasses(configuration.getClasses(), sourcePath, classes);

		// if exclude-dependencies is true, then we create a list of the only classes that we want to document
		Set includeOnly = null;
		if (configuration.excludeDependencies())
		{
			includeOnly = new HashSet();
			for (Iterator iterator = nsComponents.iterator(); iterator.hasNext();)
			{
				Component component = (Component)iterator.next();
				includeOnly.add(component.getClassName());
			}
			includeOnly.addAll(configuration.getClasses());
		}

		// set up the compiler extension which writes out toplevel.xml
		List excludeClasses = configuration.getExcludeClasses();
		Set packages = configuration.getPackagesConfiguration().getPackageNames();
		ASDocExtension asdoc = new ASDocExtension(excludeClasses, includeOnly, packages);
		((flex2.compiler.as3.Compiler)compilers[0]).addCompilerExtension(asdoc);
		((flex2.compiler.mxml.Compiler)compilers[1]).addImplementationCompilerExtension(asdoc);

		if (ThreadLocalToolkit.getBenchmark() != null)
		{
			ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new flex2.tools.Compiler.InitialSetup()));
		}

		// load SWCs
		CompilerSwcContext swcContext = new CompilerSwcContext(configuration.getCompatibilityVersionString());
		SwcCache cache = new SwcCache();
		// lazy read should only be set by mxmlc/compc/asdoc
		cache.setLazyRead(true);
		// for asdoc the theme and include-libraries values have been purposely not passed in below.
		swcContext.load( compilerConfig.getLibraryPath(),
		                 compilerConfig.getExternalLibraryPath(),
		                 null,
		                 null,
						 mappings,
						 I18nUtils.getTranslationFormat(compilerConfig),
						 cache );
		if (ThreadLocalToolkit.getBenchmark() != null)
		{
			ThreadLocalToolkit.getBenchmark().benchmark(l10n.getLocalizedTextString(new Compiler.LoadedSWCs(swcContext.getNumberLoaded())));
		}
		configuration.addExterns( swcContext.getExterns() );

		// validate CompilationUnits in FileSpec and SourcePath
		flex2.compiler.API.validateCompilationUnits(fileSpec, sourceList, sourcePath, bundlePath, resources, swcContext,
													null, false, configuration);

		Map licenseMap = configuration.getLicensesConfiguration().getLicenseMap();

		// we call compileSwc (and use CompcPreLink, both of which should probably be renamed) to "compile" ASDoc.
		// everything runs through the normal compilation route, but we discard all of the output other than
		// what ASDocExtension creates
		flex2.compiler.API.compile(fileSpec, sourceList, classes.values(), sourcePath, resources, bundlePath, swcContext,
				configuration, compilers, new CompcPreLink(null, null), licenseMap, new ArrayList()); // List<CompilationUnit>

		asdoc.finish();
		asdoc.saveFile(new File(configuration.getOutput(), "toplevel.xml"));

		if (excludeClasses.size() != 0)
		{
			StringBuffer sb = new StringBuffer();
			for (Iterator iterator = excludeClasses.iterator(); iterator.hasNext();)
			{
				sb.append(' ');
				sb.append(iterator.next());
			}
			ThreadLocalToolkit.log(new NotFound("exclude-classes", sb.toString()));
		}

		if (packages.size() != 0)
		{
			StringBuffer sb = new StringBuffer();
			for (Iterator iterator = packages.iterator(); iterator.hasNext();)
			{
				sb.append(' ');
				sb.append(iterator.next());
			}

			ThreadLocalToolkit.log(new NotFound("packages", sb.toString()));
		}
	}

	public static flex2.compiler.Compiler[] getCompilers(CompilerConfiguration compilerConfig, NameMappings mappings,
	                                                     Transcoder[] transcoders)
	{
		// support AS3
		flex2.compiler.as3.Compiler asc = new flex2.compiler.as3.Compiler(compilerConfig);

		// support MXML
		flex2.compiler.mxml.Compiler mxmlc = new flex2.compiler.mxml.Compiler(compilerConfig, compilerConfig,
		                                                                      mappings, transcoders);

		// support ABC
		flex2.compiler.abc.Compiler abc = new flex2.compiler.abc.Compiler(compilerConfig);

		// support i18n (.properties)
		flex2.compiler.i18n.Compiler prop = new flex2.compiler.i18n.Compiler(compilerConfig, transcoders);

		// support CSS
		flex2.compiler.css.Compiler css = new flex2.compiler.css.Compiler(compilerConfig, transcoders);

		return new flex2.compiler.Compiler[]{asc, mxmlc, abc, prop, css};
	}

	public static void createTopLevelClassesXML(String outputDir, String templatesPath) throws CompilerException
	{
		Process proc;
		try
		{
			String asDocHelper;
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows") || forceWindows)
			{
				asDocHelper = "asDocHelper.exe";
			}
			else if (osName.startsWith("mac os x") || forceMac)
			{
				asDocHelper = "asDocHelper";
			}

			else if (osName.indexOf("linux") != -1 || forceLinux)
			{
				asDocHelper = "asDocHelper.linux";
			}
			else
			{
				throw new UnknownOS();
			}
			File templatesFile = new File(templatesPath);
			Runtime r = Runtime.getRuntime();
			proc = r.exec(new String[] { templatesPath + asDocHelper,
					                     outputDir + "toplevel.xml",
					                     outputDir + "toplevel_classes.xml",
			                             templatesPath + "ASDoc_Config.xml"}, null, templatesFile);

			PumpStreamHandler streamHandler = new PumpStreamHandler();
			streamHandler.setProcessOutputStream(proc.getInputStream());
			streamHandler.setProcessErrorStream(proc.getErrorStream());
			streamHandler.start();

			// hang around until it's killed
			proc.waitFor();
		}
		catch (Throwable t)
		{
			if (Trace.error)
				t.printStackTrace();

			CompilerMessage c = new CouldNotCreate("toplevel.xml", t.getMessage());
			ThreadLocalToolkit.log(c);
			throw c;
		}

		if (proc.exitValue() != 0)
		{
			CompilerMessage c = new CouldNotCreate("toplevel.xml", "");
			ThreadLocalToolkit.log(c);
			throw c;
		}
	}

	public static void createHTML(String outputDir, String templatesDir, ASDocConfiguration config)
			throws CompilerException
	{
		// the XSL processing only works with forward slashes
		templatesDir = templatesDir.replace('\\', '/');
		File indexTmp = new File(outputDir + "index.tmp");

		// 0_processHTML
		String[] args = new String[] { "-in", templatesDir + "index.html",
				                       "-xsl", templatesDir + "processHTML.xsl",
				                       "-out", indexTmp.toString() };
		org.apache.xalan.xslt.Process.main(args);
		args = new String[] { "-in", templatesDir + "package-frame.html",
				              "-xsl", templatesDir + "processHTML.xsl",
				              "-out", outputDir + "package-frame.html" };
		org.apache.xalan.xslt.Process.main(args);
		args = new String[] { "-in", templatesDir + "index-list.html",
				              "-xsl", templatesDir + "processHTML.xsl",
				              "-out", outputDir + "index-list.html" };
		org.apache.xalan.xslt.Process.main(args);
		args = new String[] { "-in", templatesDir + "title-bar.html",
				              "-xsl", templatesDir + "processHTML.xsl",
				              "-out", outputDir + "title-bar.html" };
		org.apache.xalan.xslt.Process.main(args);
		args = new String[] { "-in", templatesDir + "mxml-tags.html",
				              "-xsl", templatesDir + "processHTML.xsl",
				              "-out", outputDir + "mxml-tags.html" };
		org.apache.xalan.xslt.Process.main(args);
		// 5_createClassFiles
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "class-files.xsl",
				              "-param", "outputPath", outputDir,
				              "-param", "showExamples", "true",
			                  "-param", "showIncludeExamples", "true" };
		org.apache.xalan.xslt.Process.main(args);
		// 6_createAllClassesList
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "all-classes.xsl",
				              "-out", outputDir + "all-classes.html" };
		org.apache.xalan.xslt.Process.main(args);
		// 7_createPackageList
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "package-list.xsl",
				              "-out", outputDir + "package-list.html" };
		org.apache.xalan.xslt.Process.main(args);
		// 8_createClassSummary
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "class-summary.xsl",
				              "-param", "overviewsFile", templatesDir + "overviews.xml",
				              "-out", outputDir + "class-summary.html" };
		org.apache.xalan.xslt.Process.main(args);
		// 9_createPackageDetail
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "package-detail.xsl",
				              "-param", "overviewsFile", templatesDir + "overviews.xml",
	                          "-param", "outputPath", outputDir };
		org.apache.xalan.xslt.Process.main(args);
		// 10_createPackageSummary
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "package-summary.xsl",
				              "-param", "overviewsFile", templatesDir + "overviews.xml",
				              "-out", outputDir + "package-summary.html" };
		org.apache.xalan.xslt.Process.main(args);
		// 11_createClassList
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "class-list.xsl",
				                 "-param", "outputPath", outputDir };
		org.apache.xalan.xslt.Process.main(args);
		// 12_createAllIndex
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "all-index.xsl",
				               "-param", "overviewsFile", templatesDir + "overviews.xml",
				              "-param", "outputPath", outputDir };
		org.apache.xalan.xslt.Process.main(args);
		// 13_createPackage
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "package.xsl",
				                "-param", "outputPath", outputDir };
		org.apache.xalan.xslt.Process.main(args);
		// 16_createAppendixes
		args = new String[] { "-in", outputDir + "toplevel_classes.xml",
				              "-xsl", templatesDir + "appendixes.xsl",
				              "-param", "overviewsFile", templatesDir + "overviews.xml",
				              "-out", outputDir + "appendixes.html" };
		org.apache.xalan.xslt.Process.main(args);

		File indexHtml = new File(outputDir + "index.html");
		if (config.getLeftFramesetWidth() == -1)
		{
			// we can't just originally name this as index.html because then the renaming in the other
			// case fails (because of Java)
			FileUtils.renameFile(indexTmp, indexHtml);
		}
		else
		{
			// here we do something that doesn't fit well into XSL, which is replacing the first frameset
			// value we find in index.html

			BufferedReader reader = null;
			BufferedWriter writer = null;
			File indexTmp2 = new File(outputDir + "index2.tmp");
			try
			{
				reader = new BufferedReader(new FileReader(indexTmp));
				writer = new BufferedWriter(new FileWriter(indexTmp2));
				boolean foundFrameset = false;
				String s;
				String search1 = "frameset cols=";
				String search2 = ",";
				while ((s = reader.readLine()) != null)
				{
					if (! foundFrameset)
					{
						int ind = s.indexOf(search1);
						if (ind != -1)
						{
							foundFrameset = true;
							int ind2 = s.indexOf(search2, ind);
							if (ind2 != -1)
							{
								s = s.substring(0, ind + search1.length() + 1) + config.getLeftFramesetWidth() + s.substring(ind2);
							}
						}

					}
					writer.write(s);
					writer.newLine();
				}
			}
			catch (Exception ex)
			{
				if (Trace.error)
					ex.printStackTrace();

				CompilerMessage c = new CouldNotCreate("index.html", ex.getMessage());
				ThreadLocalToolkit.log(c);
				throw c;
			}
			finally
			{
				if (writer != null)
				{
					try { writer.close(); } catch(IOException ioe) {}
				}
				if (reader != null)
				{
					try { reader.close(); } catch(IOException ioe) {}
				}

				FileUtils.renameFile(indexTmp2, indexHtml);
			}
		}
	}

	public static void copyFiles(String outputDir, String templatesPath) throws IOException
	{
		File templateFile = new File(templatesPath);
		File[] temArr = templateFile.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				name = name.toLowerCase();
				return (name.endsWith(".js") || name.endsWith(".css"));
			}}
		);
		for (int i = 0; i < temArr.length; i++)
		{
			File f = temArr[i];
			copyFile(new File(templatesPath, f.getName()), new File(outputDir, f.getName()));
		}

		File outImages = new File(outputDir, "images");
		outImages.mkdir();

		File temImages = new File(templatesPath, "images");
		File[] imageArr = temImages.listFiles(new FileFilter() {
            public boolean accept(File f)
            {
                //does not accept hidden files or files that cannot be read or .* files
                if (f.getName().startsWith(".") || f.isHidden() || !f.canRead())
                    return false;
                return true;
            }
        });
		for (int i = 0; i < imageArr.length; i++)
		{
			File f = imageArr[i];
			copyFile(new File(temImages, f.getName()), new File(outImages, f.getName()));
		}
	}

	public static void copyFile(File fromFile, File toFile) throws IOException
	{
	    FileInputStream fileInputStream = new FileInputStream( fromFile );
	    FileOutputStream fileOutputStream = new FileOutputStream( toFile );
	    int i;
	    byte bytes[] = new byte[ 2048 ];
	    while ( ( i = fileInputStream.read(bytes) ) != -1 )
	    {
	        fileOutputStream.write( bytes, 0, i );
	    }
	    fileInputStream.close();
	    fileOutputStream.close();
	}

	public static void removeXML(String outputDir, String templatesPath)
	{
		(new File(templatesPath + "ASDoc_Config.xml")).delete();
		(new File(templatesPath + "overviews.xml")).delete();
		(new File(outputDir + "index.tmp")).delete();
		(new File(outputDir + "index2.tmp")).delete();
		(new File(outputDir + "toplevel.xml")).delete();
		(new File(outputDir + "toplevel_classes.xml")).delete();
	}

	public static class CouldNotCreate extends CompilerMessage.CompilerError
	{
		public CouldNotCreate(String file, String message)
		{
			super();
			this.file = file;
			this.message = message;
		}

		public String file;
		public String message;
	}

	public static class UnknownOS extends CompilerMessage.CompilerError
	{
		public UnknownOS()
		{
		}
	}

	public static class NotFound extends CompilerMessage.CompilerWarning
	{
		public NotFound(String property, String names)
		{
			super();
			this.property = property;
			this.names = names;
		}

		public String property;
		public String names;
	}
}
