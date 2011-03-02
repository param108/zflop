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

import flash.swf.tags.DefineFont;
import flash.util.FileUtils;
import flash.util.StringJoiner;
import flash.util.Trace;
import flex.messaging.config.ServicesDependencies;
import flex2.compiler.*;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.common.Configuration;
import flex2.compiler.common.FramesConfiguration;
import flex2.compiler.common.MxmlConfiguration;
import flex2.compiler.css.StylesContainer;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.TextFile;
import flex2.compiler.swc.Digest;
import flex2.compiler.swc.Swc;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.VelocityException;
import flex2.linker.CULinkable;
import flex2.linker.DependencyWalker.LinkState;
import flex2.linker.LinkerException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Clement Wong
 * @author Roger Gonzalez (mixin, flexinit, bootstrap)
 * @author Basil Hosmer (service config)
 * @author Brian Deitte (font)
 * @author Cathy Murphy (accessibility)
 * @author Gordon Smith (i18n)
 */
public class PreLink implements flex2.compiler.PreLink
{
	public void run(List sources, List units,
	                FileSpec fileSpec, SourceList sourceList, SourcePath sourcePath, ResourceBundlePath bundlePath,
	                ResourceContainer resources, SymbolTable symbolTable, CompilerSwcContext swcContext,
	                Configuration configuration)
	{
		processMainUnit(sources, units, resources, symbolTable, configuration);
				
		//	add synthetic link-in units
		postGenerateExtraCode(sources, units, configuration, swcContext);
	}

	private void processMainUnit(List sources, List units, ResourceContainer resources,
	                             SymbolTable symbolTable, Configuration configuration)
	{
		for (int i = 0, length = units.size(); i < length; i++)
		{
			CompilationUnit u = (CompilationUnit) units.get(i);

			if (u.isRoot())
			{
				swfmetadata(u, configuration);

				if (u.loaderClass != null)
				{
					configuration.setRootClassName(u.loaderClass);
				}

				// set the last top level definition as the main definition.  Setting the last one allows
				// for Embed classes at the top of the file
				QName qName = (QName) u.topLevelDefinitions.last();
				if (qName != null)
				{
					String def = qName.toString();
					configuration.setMainDefinition(def);
					u.getContext().setAttribute("mainDefinition", def);

                    if (u.loaderClass != null)  // i.e. isApplication... need loader class for styles
                    {
                        StylesContainer stylesContainer = new StylesContainer(configuration.getCompilerConfiguration(),
                                                                              u,
                                                                              symbolTable.perCompileData);

                        List linkables = new LinkedList();

                        for (Iterator it2 = units.iterator(); it2.hasNext();)
                        {
                            linkables.add( new CULinkable( (CompilationUnit) it2.next() ) );
                        }

                        try
                        {
                            LinkState state = new LinkState(linkables, new HashSet(), configuration.getIncludes(), new HashSet());

	                        // C: generate style classes for components which we want to link in.
                            List styleSources = stylesContainer.processDependencies(state.getDefNames(), resources);

                            if (u.getStylesContainer() != null)
                            {
                                u.getStylesContainer().checkForUnusedTypeSelectors(state.getDefNames());
                            }

	                        // put all the names into sourceSet
	                        Set sourceSet = new HashSet(sources.size());
	                        for (int j = 0, size = sources.size(); j < size; j++)
	                        {
		                        Source s = (Source) sources.get(j);
		                        sourceSet.add(s.getName());
	                        }

	                        for (int j = 0, size = styleSources.size(); j < size; j++)
	                        {
		                        Source styleSrc = (Source) styleSources.get(j);
		                        if (!sourceSet.contains(styleSrc.getName()))
		                        {
			                        sources.add(styleSrc);
		                        }
	                        }
                        }
                        catch (LinkerException e)
                        {
                            ThreadLocalToolkit.log( e ); 
                        }
                    }
				}
				else
				{
					ThreadLocalToolkit.log(new NoExternalVisibleDefinition(), u.getSource());
				}

				break;
			}
		}

		for (int i = 0, length = units.size(); i < length; i++)
		{
			CompilationUnit u = (CompilationUnit) units.get(i);
			if (u.isDone())
			{
				// C: we don't need the styles container anymore
				u.setStylesContainer(null);
			}
		}
	}

//    private static boolean isApplication(SymbolTable symbolTable, String def)
//    {
//        boolean result = false;
//
//        Class topLevelClass = symbolTable.getClass(def);
//
//        if (topLevelClass != null)
//        {
//            String superTypeName = topLevelClass.getSuperTypeName();
//
//            if (superTypeName != null)
//            {
//                if ((superTypeName.equals("mx.core:Application") || superTypeName.endsWith( ":Module" )))
//                {
//                    result = true;
//                }
//                else
//                {
//                    result = isApplication(symbolTable, superTypeName);
//                }
//            }
//        }
//
//        return result;
//    }

	private static void swfmetadata(CompilationUnit u, Configuration cfg)
	{
		if (u.swfMetaData != null)
		{
			String widthString = u.swfMetaData.getValue("width");
			if (widthString != null)
			{
				cfg.setWidth(widthString);
			}

			String heightString = u.swfMetaData.getValue("height");
			if (heightString != null)
			{
				cfg.setHeight(heightString);
			}

			String widthPercent = u.swfMetaData.getValue("widthPercent");
			if (widthPercent != null)
			{
				cfg.setWidthPercent(widthPercent);
			}

			String heightPercent = u.swfMetaData.getValue("heightPercent");
			if (heightPercent != null)
			{
				cfg.setHeightPercent(heightPercent);
			}

			String scriptRecursionLimit = u.swfMetaData.getValue("scriptRecursionLimit");
			if (scriptRecursionLimit != null)
			{
				try
				{
					cfg.setScriptRecursionLimit(Integer.parseInt(scriptRecursionLimit));
				}
				catch (NumberFormatException nfe)
				{
					ThreadLocalToolkit.log(new CouldNotParseNumber(scriptRecursionLimit, "scriptRecursionLimit"));
				}
			}

			String scriptTimeLimit = u.swfMetaData.getValue("scriptTimeLimit");
			if (scriptTimeLimit != null)
			{
				try
				{
					cfg.setScriptTimeLimit(Integer.parseInt(scriptTimeLimit));
				}
				catch (NumberFormatException nfe)
				{
					ThreadLocalToolkit.log(new CouldNotParseNumber(scriptTimeLimit, "scriptTimeLimit"));
				}
			}

			String frameRate = u.swfMetaData.getValue("frameRate");
			if (frameRate != null)
			{
				try
				{
					cfg.setFrameRate(Integer.parseInt(frameRate));
				}
				catch (NumberFormatException nfe)
				{
					ThreadLocalToolkit.log(new CouldNotParseNumber(frameRate, "frameRate"));
				}
			}

			String backgroundColor = u.swfMetaData.getValue("backgroundColor");
			if (backgroundColor != null)
			{
				try
				{
					cfg.setBackgroundColor(Integer.decode(backgroundColor).intValue());
				}
				catch (NumberFormatException numberFormatException)
				{
					ThreadLocalToolkit.log(new InvalidBackgroundColor(backgroundColor), u.getSource());
				}
			}

			String pageTitle = u.swfMetaData.getValue("pageTitle");
			if (pageTitle != null)
			{
				cfg.setPageTitle(pageTitle);
			}

			// fixme: error on SWF metadata we don't understand
		}
	}

	private SortedSet resourceBundleNames = new TreeSet();
	private SortedSet externalResourceBundleNames = new TreeSet();

	/**
	 * generate sources for units which require complete set of original units as type context
	 */
	private void postGenerateExtraCode(List sources, List units,
									   flex2.compiler.common.Configuration configuration,
									   CompilerSwcContext swcContext)
	{
        LinkedList extraSources = new LinkedList();
        LinkedList mixins = new LinkedList();
        LinkedList fonts = new LinkedList();

		// Build a set, such as { "core", "controls" }, of the names
		// of all resource bundles used in all compilation units.
		// This will be used to set the compiledResourceBundleNames
		// property of the module factory's info() Object
		// and the compileResourceBundleNames property
		// of the _CompileResourceBundleInfo class.
		processResourceBundleNames(units, configuration);
        
		// TODO - factor out the unit iteration / list discovery to a more clear separate step
        
		// Autogenerate the _MyApp_FlexInit class.
		processInitClass(units, configuration, extraSources, mixins, fonts, swcContext);
		
		// Autogenerate the _MyApp_mx_managers_SystemManager class.
		boolean generatedLoaderClass = processLoaderClass(units, configuration, extraSources, mixins, fonts, swcContext);
		
		// Autogenerate the _CompiledResourceBundleInfo class if we didn't autogenerate a loader class.
		// This enables non-framework apps which simply extend Sprite to use the ResourceManager.
		if (!generatedLoaderClass)
			processCompiledResourceBundleInfoClass(units, configuration, extraSources, mixins, fonts, swcContext);
		
		sources.addAll(extraSources);
	}

	private void processResourceBundleNames(List units, flex2.compiler.common.Configuration configuration)
	{
        Set externs = configuration.getExterns();
	    
 		for (Iterator it = units.iterator(); it.hasNext();)
		{
			CompilationUnit unit = (CompilationUnit) it.next();
			if (unit.resourceBundleHistory.size() > 0)
			{
				resourceBundleNames.addAll(unit.resourceBundleHistory);
				
				if (externs.contains(unit.topLevelDefinitions.first().toString()))
				{
					externalResourceBundleNames.addAll(unit.resourceBundleHistory);
				}
			}
		}
	}
	       
	private String codegenFlexInit(String flexInitClassName, Set accessibilityList,
								   Map remoteClassAliases, Map effectTriggers,
                                   Set inheritingStyles, Configuration configuration)
	{
        CompilerConfiguration compilerConfig = configuration.getCompilerConfiguration();
        ServicesDependencies servicesDependencies = compilerConfig.getServicesDependencies();

		StringBuffer sb = new StringBuffer();
		sb.append("package {\n");
		sb.append("import flash.utils.*;\n");
		sb.append("import mx.core.IFlexModuleFactory;\n");
		sb.append(codegenAccessibilityImports(accessibilityList));
        sb.append(codegenRemoteClassImports( remoteClassAliases ));
		sb.append(codegenEffectTriggerImports(effectTriggers));
        if (servicesDependencies != null)
            sb.append(servicesDependencies.getImports());

        sb.append("\n[Mixin]\n");
		sb.append("public class " + flexInitClassName + "\n");
		sb.append("{\n");
		sb.append("   public function " + flexInitClassName + "()\n");
		sb.append("   {\n");
		sb.append("       super();\n");
		sb.append("   }\n");
		sb.append("   public static function init(fbs:IFlexModuleFactory):void\n");
		sb.append("   {\n");
		sb.append(codegenEffectTriggerRegistration(effectTriggers));
		sb.append(codegenAccessibilityList(accessibilityList));
        sb.append(codegenRemoteClassAliases(remoteClassAliases));
        sb.append(codegenInheritingStyleRegistration(inheritingStyles));
        if (servicesDependencies != null)
            sb.append(servicesDependencies.getServerConfigXmlInit());
		sb.append("   }\n");
        if (servicesDependencies != null)
            sb.append(servicesDependencies.getReferences());

        sb.append("}  // FlexInit\n");
		sb.append("}  // package\n");

		return sb.toString();
	}

    private void processInitClass(List units, Configuration configuration,
								  List extraSources, LinkedList mixins, LinkedList fonts,
								  CompilerSwcContext swcContext)
    {
		Set accessibilityList = null;
        Map remoteClassAliases = new TreeMap()
        {
            private static final long serialVersionUID = -8015004853369794727L;

            /**
             *  Override so warning messages can be logged. 
             */
            public Object put(Object key, Object value)
            {
                // check for duplicate values and log a warning if any remote 
                // classes try to use the same alias.
                if (containsValue(value))
                {
                   Object existingKey = null;
                   for (Iterator iter = entrySet().iterator(); iter.hasNext();)
                   {
                       Map.Entry entry = (Map.Entry)iter.next();
                       if (value != null && value.equals(entry.getValue()))
                       {
                           existingKey = entry.getKey();
                           break;
                       }
                   }
                   ThreadLocalToolkit.log(new ClassesMappedToSameRemoteAlias((String)key, (String)existingKey, (String)value));
                }
                return super.put(key, value);
            }
        
        };
		Map effectTriggers = new TreeMap();
        Set inheritingStyles = new HashSet();
        CompilationUnit mainUnit = null;
        Set externs = swcContext.getExterns();
        
		for (int i = 0, size = units.size(); i < size; i++)
		{
			CompilationUnit u = (CompilationUnit) units.get(i);

            if (u.isRoot())
            {
                mainUnit = u;
            }

			List fontList = u.getAssets().getFonts();
			
			// don't add font assets for definitions that have been externed.
			if (fontList != null && !fontList.isEmpty() &&
				!isCompilationUnitExternal(u, externs) &&  
				!u.getSource().isInternal())
			{
                fonts.addAll(fontList);    // save for later...
			}

            remoteClassAliases.putAll( u.remoteClassAliases );

			effectTriggers.putAll( u.effectTriggers );
            mixins.addAll( u.mixins );

            inheritingStyles.addAll( u.styles.getInheritingStyles() );

			if (configuration.getCompilerConfiguration().accessible())
			{
				Set unitAccessibilityList = u.getAccessibilityClasses();
				if (unitAccessibilityList != null)
				{
					if (accessibilityList == null)
					{
						accessibilityList = new HashSet();
					}
					accessibilityList.addAll(unitAccessibilityList);
				}
			}
		}

        String flexInitClass = null;
        if (mainUnit != null)
        {
            for (Iterator it = mainUnit.extraClasses.iterator(); it.hasNext();)
            {
                String extraClass = (String) it.next();
                // FIXME - Depending on the contents of the classname is not the solution we want.
                if (extraClass.indexOf("FlexInit") != -1)
                {
                    flexInitClass = extraClass;
                    break;
                }
            }
        }

        if (flexInitClass != null)
        {
		    String code = codegenFlexInit(flexInitClass, accessibilityList, remoteClassAliases,
                                          effectTriggers, inheritingStyles, configuration);
    		String name = flexInitClass + "-generated.as";

    		if (configuration.getCompilerConfiguration().keepGeneratedActionScript())
    		{
    			saveGenerated(name, code, configuration.getCompilerConfiguration().getGeneratedDirectory());
    		}

    		Source s = new Source(new TextFile(code, name, null, MimeMappings.getMimeType(name)), "", flexInitClass, null, false, false, false);
    		// C: It doesn't look like this Source needs any path resolution. null is fine...
    		s.setPathResolver(null);
    		extraSources.add(s);
            mixins.addFirst( flexInitClass );   // we already iterated, lets put this one at the head in any case
        }

    }

    
    /**
     * 
     * @param unit compilation unit, may not be null
     * @param externs - list of externs, may not be null
     * @return true if the compilation unit, u, has any definitions that are in the list of 
     * 		   interns.
     */
	public static boolean isCompilationUnitExternal(CompilationUnit unit, Set externs)
	{
    	for (int i = 0, size = unit == null ? 0 : unit.topLevelDefinitions.size(); i < size; i++)
    	{
    		if (externs.contains(unit.topLevelDefinitions.get(i).toString()))
    		{
    			return true;
    		}
    	}
    	
    	return false;
	}

	
	private boolean processLoaderClass(List units,
	                                   Configuration configuration,
	                                   List sources,
                                       List mixins,
                                       List fonts,
                                       CompilerSwcContext swcContext)
	{
		if (!configuration.generateFrameLoader)
		{
			return false;
		}

        LinkedList frames = new LinkedList();
        frames.addAll( configuration.getFrameList() );

        CompilationUnit mainUnit = null;
 		for (Iterator it = units.iterator(); it.hasNext();)
		{
			CompilationUnit unit = (CompilationUnit) it.next();
			if (unit.isRoot())
			{
				mainUnit = unit;
				break;
			}
		}

		if (mainUnit == null)
		{
			return false;
		}

	    // If we built the main unit from source on this pass, we will have saved
		// off information that will help us determine whether we need to generate
		// an IFlexModuleFactory derivative.
		//
		// IMPORTANT: Having frame metadata is NOT the indicator!  We only generate
		// a system manager in sync with compiling a MXML application from source;
		// otherwise, the generated class is assumed to already exist!

        String generateLoaderClass = null;
        String baseLoaderClass = null;
        String windowClass = null;
        //String preloaderClass = null;
        Map rootAttributes = null;
        //boolean usePreloader = false;
        List cdRsls = configuration.getRslPathInfo();
        List rsls = configuration.getRuntimeSharedLibraries();
		String[] locales = configuration.getCompilerConfiguration().getLocales();

        // ALGORITHM:
        // Generate a loader class iff all the below are true:
        // 1a. We compiled MXML on this compilation run.
        //   or
        // 1b. We were not MXML but the base class does know a loader.
        // 2. We found Frame loaderClass metadata in a superclass
        // 3. We did not find Frame loaderClass metadata in the app



		if ((mainUnit.loaderClass != null) && (mainUnit.auxGenerateInfo != null))
		{
			generateLoaderClass = (String) mainUnit.auxGenerateInfo.get("generateLoaderClass");
			baseLoaderClass = (String) mainUnit.auxGenerateInfo.get("baseLoaderClass");
			windowClass = (String) mainUnit.auxGenerateInfo.get("windowClass");
			//preloaderClass = (String) mainUnit.auxGenerateInfo.get("preloaderClass");
			//Boolean b = (Boolean) mainUnit.auxGenerateInfo.get("usePreloader");
			rootAttributes = (Map) mainUnit.auxGenerateInfo.get("rootAttributes");

			// mainUnit.auxGenerateInfo = null;    // All done, thanks!

			assert generateLoaderClass != null;

			//usePreloader = ((b == null) || b.booleanValue());

			//assert usePreloader || (preloaderClass != null);

			// Is there any way we can eliminate having default class here?
			// Seems like this should be in SystemManager, not the compiler.

			//if (usePreloader && (preloaderClass == null))   //
			//{
			//	preloaderClass = "mx.preloaders.DownloadProgressBar";
			//}
        }
        else if ((mainUnit.loaderClass == null) && (mainUnit.loaderClassBase != null))
        {
            // AS project, but the base class knows of a loader.
            baseLoaderClass = mainUnit.loaderClassBase;
            windowClass = mainUnit.topLevelDefinitions.last().toString();
            generateLoaderClass = (windowClass + "_" + mainUnit.loaderClassBase).replaceAll("[^A-Za-z0-9]", "_");

            mainUnit.loaderClass = generateLoaderClass;
        }
        else if ((mainUnit.loaderClass == null) && 
        		((rsls.size() > 0) && (cdRsls.size() > 0)))
        {
	        ThreadLocalToolkit.log(new MissingFactoryClassInFrameMetadata(), mainUnit.getSource());
            return false;
        }
        else
        {
            return false;
        }

		
		String generatedLoaderCode = codegenModuleFactory(baseLoaderClass.replace(':', '.'),
			                                                  generateLoaderClass.replace(':', '.'),
			                                                  windowClass.replace(':', '.'),
			                                                  rootAttributes,
			                                                  cdRsls,
			                                                  rsls,
                                                              mixins,
                                                              fonts,
                                                              frames,
															  locales,
                                                              resourceBundleNames,
                                                              externalResourceBundleNames,
                                                              configuration,
                                                              swcContext,
                                                              false);

		String generatedLoaderFile = generateLoaderClass + ".as";

		Source s = new Source(new TextFile(generatedLoaderCode,
			                                    generatedLoaderFile,
			                                    null,
			                                    MimeMappings.getMimeType(generatedLoaderFile)),
			                   "", generateLoaderClass, null, false, false, false);
		// C: It doesn't look like this Source needs any path resolution. null is fine...
		s.setPathResolver(null);
		sources.add(s);

		if (configuration.getCompilerConfiguration().keepGeneratedActionScript())
		{
			saveGenerated(generatedLoaderFile, generatedLoaderCode, configuration.getCompilerConfiguration().getGeneratedDirectory());
		}

		return true;
	}


	private String codegenAccessibilityImports(Set accessibilityImplementations)
	{
		StringBuffer sb = new StringBuffer();

		sb.append("import flash.system.*\n");
		if (accessibilityImplementations != null)
		{
			for (Iterator it = accessibilityImplementations.iterator(); it.hasNext();)
			{
				sb.append("import " + (String) it.next() + ";\n");
			}
		}

		return sb.toString();
	}

	//	TODO save to alt location instead of mangling name, to keep code compilable under OPD
	//	TODO make sure code generators obey OPD in name <-> code
	public static void saveGenerated(String name, String code, String dir)
	{
		final String suffix = "-generated.as";
		final String as3ext = ".as";
		if (!name.endsWith(suffix) && name.endsWith(as3ext))
		{
			name = name.substring(0, name.length() - as3ext.length()) + suffix;
		}

        name = FileUtils.addPathComponents( dir, name, File.separatorChar );

		try
		{
			FileUtil.writeFile(name, code);
		}
		catch (IOException e)
		{
			ThreadLocalToolkit.log(new VelocityException.UnableToWriteGeneratedFile(name, e.getLocalizedMessage()));
		}
	}

	
	private static String codegenCdRslList(List cdRsls,
									Configuration configuration,
									CompilerSwcContext swcContext) 
	{
		
		// ignore -rslp option if -static-rsls is set
		if (configuration.getStaticLinkRsl()) {
			return "[]";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (Iterator iter = cdRsls.iterator(); iter.hasNext();)
		{
			Configuration.RslPathInfo info = (Configuration.RslPathInfo)iter.next();
			
			// write array of rsl urls
			buf.append("{\"rsls\":[");
			for (Iterator iter2 = info.getRslUrls().iterator(); iter2.hasNext();) 
			{
				buf.append("\"" + iter2.next() + "\"");
				if (iter2.hasNext())
				{
					buf.append(",");
				}
			}
			buf.append("],\n"); // end of rsls array
			
			// write array of policy urls
			buf.append("\"policyFiles\":[");
			for (Iterator iter2 = info.getPolicyFileUrls().iterator(); iter2.hasNext();) 
			{
				buf.append("\"" + iter2.next() + "\"");
				if (iter2.hasNext())
				{
					buf.append(",");
				}
			}
			
			buf.append("]\n"); 	// end of policy files array
			
			// write digest info to startup info
			codegenDigestArrays(swcContext, configuration, info, buf);
			
			// end of one object in the array
			buf.append("}");
			
			if (iter.hasNext()) {
				buf.append(",\n");
			}
			
		}
		
		// end of all rsls
		buf.append("]\n");
		
		return buf.toString();
	}
	

	/**
	 * Append digest, type, and signed arrays to the cross-domain startup info.
	 * 
	 * @param swcContext 
	 * @param info cross-domain rsl info
	 * @param buf StringBuffer to append info to
	 */
	private static void codegenDigestArrays(CompilerSwcContext swcContext,
									 Configuration configuration,
									 Configuration.RslPathInfo info,
									 StringBuffer buf) {
		// get the swc for current rsl
		Swc swc = swcContext.getSwc(info.getSwcVirtualFile().getName());
		
		if (swc == null) 
		{
			throw new IllegalStateException("codegenCdRslList: swc not resolved, " + 
											info.getSwcPath());
		}
		
		// write digest for each rsl in the list
		boolean secureRsls = configuration.getVerifyDigests();
		boolean haveDigest = false;		// true if write a digest to the startup info
		boolean haveUnsignedRsl = false;   // true if we wrote an unsigned rsl to the startup info
		StringBuffer digestBuffer = new StringBuffer("\"digests\":[");
		StringBuffer typeBuffer = new StringBuffer("\"types\":[");
		StringBuffer signedBuffer = new StringBuffer("\"isSigned\":[");
		
		for (Iterator iter2 = info.getSignedFlags().iterator(); iter2.hasNext();) {
			Boolean isSigned = (Boolean)iter2.next();
			Digest digest = swc.getDigest(Swc.LIBRARY_SWF, 
										  Digest.SHA_256, 
										  isSigned.booleanValue());
			if (digest != null && digest.hasDigest())
			{
				String digestValue = digest.getValue();
				
				// unsigned Rsls may be unsecured by a command line option.
				if (!secureRsls && !isSigned.booleanValue()) {
					digestValue = "";		// won't verify an empty digest
				}
				digestBuffer.append("\"" + digestValue + "\"");
				if (iter2.hasNext())
				{
					digestBuffer.append(",");
				}
				typeBuffer.append("\"" + digest.getType() + "\"");
				if (iter2.hasNext())
				{
					typeBuffer.append(",");
				}
				signedBuffer.append(isSigned.toString());
				if (iter2.hasNext())
				{
					signedBuffer.append(",");
				}
				haveDigest = true;
				
				if (!isSigned.booleanValue()) {
					haveUnsignedRsl = true;
				}
			}
			else if (!haveDigest)
			{
				// if the digest is not available then throw an exception,
				// "No digest found in catalog.xml. Either compile the application with 
				// the -verify-digests=false or compile the library with 
				// -create-digest=true"
				if (isSigned.booleanValue()) {
					ThreadLocalToolkit.log(new MissingSignedLibraryDigest(swc.getLocation())); 
				}
				else {
					ThreadLocalToolkit.log(new MissingUnsignedLibraryDigest(swc.getLocation())); 
				}
				return;
			}
		}
		
		// terminate arrays
		digestBuffer.append("],\n");
		typeBuffer.append("],\n");
		signedBuffer.append("]\n");

		// If we only write signed RSLs without an unsigned RSL failover then 
		// verify the player supports signed rsls.
		// If not, log a warning.
		if (!haveUnsignedRsl && !configuration.isSignedRslSupported())
		{
			ThreadLocalToolkit.log(new SignedRslsNotSupported(info.getSwcVirtualFile().getName()));
		}
		
		buf.append(",");
		buf.append(digestBuffer);

		// write hash types
		buf.append(typeBuffer);

		// write signed flags
		buf.append(signedBuffer);

	}
	
	
    private static String codegenRslList(List rsls)
    {
        if ((rsls != null) && (rsls.size() > 0))
        {
            StringBuffer rb = new StringBuffer();

            rb.append("[");
            for (Iterator it = rsls.iterator(); it.hasNext();)
            {
                rb.append("{url: \"" + it.next() + "\", size: -1}");
                if (it.hasNext())
                {
                    rb.append(", ");
                }
            }
            rb.append("]\n");

            return rb.toString();
        }
        return "[]";
    }

    private static String codegenMixinList(List mixins)
    {
        assert mixins != null && mixins.size() > 0;
        StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
        return "[ " + StringJoiner.join(mixins, ", ", itemStringer) + " ]";
    }

    private static String codegenFrameClassList(List frames)
    {
        assert frames != null && frames.size() > 0;
        StringBuffer mb = new StringBuffer();
        mb.append("{");

        for (Iterator it = frames.iterator(); it.hasNext();)
        {
            FramesConfiguration.FrameInfo frameInfo = (FramesConfiguration.FrameInfo) it.next();
            mb.append("\"");
            mb.append(frameInfo.label);
            mb.append("\":\"");
            mb.append(frameInfo.frameClasses.get(0));
            mb.append("\"");
            if (it.hasNext())
            {
                mb.append(", ");
            }
        }
        mb.append("}\n");
        return mb.toString();
    }



    private static String codegenFontList(List fonts)
	{
		if ((fonts == null) || (fonts.size() == 0))
		{
			return "";
		}

        class FontInfo
        {
            boolean plain;
            boolean bold;
            boolean italic;
            boolean bolditalic;
        }

        Map fontMap = new TreeMap();
        for (Iterator it = fonts.iterator(); it.hasNext();)
        {
            DefineFont font = (DefineFont) it.next();
            FontInfo fi = (FontInfo) fontMap.get( font.getFontName() );
            if (fi == null)
            {
                fi = new FontInfo();
                fontMap.put( font.getFontName(), fi );
            }

            fi.plain |= (!font.isBold() && !font.isItalic());
            fi.bolditalic |= (font.isBold() && font.isItalic());
            fi.bold |= font.isBold();
            fi.italic |= font.isItalic();
        }

		StringBuffer sb = new StringBuffer();

		sb.append("      {\n");

        for (Iterator it = fontMap.entrySet().iterator(); it.hasNext();)
		{
            Map.Entry e = (Map.Entry) it.next();
            String fontName = (String) e.getKey();
            FontInfo fontInfo = (FontInfo) e.getValue();

		    sb.append("\"" + fontName + "\" : {" +
                      "regular:" + (fontInfo.plain? "true":"false") +
                      ", bold:" + (fontInfo.bold? "true":"false") +
                      ", italic:" + (fontInfo.italic? "true":"false") +
                      ", boldItalic:" + (fontInfo.bolditalic? "true":"false") + "}\n");
            if (it.hasNext())
            {
                sb.append(",\n");
            }
        }
		sb.append("}\n");

		return sb.toString();
	}

	private String codegenAccessibilityList(Set accessibilityImplementations)
	{
		if ((accessibilityImplementations == null) || (accessibilityImplementations.size() == 0))
		{
			return "";
		}

		StringBuffer sb = new StringBuffer();

		if ((accessibilityImplementations != null) && (accessibilityImplementations.size() != 0))
		{
			sb.append("      // trace(\"Flex accessibility startup: \" + Capabilities.hasAccessibility);\n");
			sb.append("      if (Capabilities.hasAccessibility) {\n");
			for (Iterator it = accessibilityImplementations.iterator(); it.hasNext();)
			{
				sb.append("         " + (String) it.next() + ".enableAccessibility();\n");
			}
			sb.append("      }\n");
		}

		if (Trace.accessible)
		{
			Trace.trace("codegenAccessibilityList");
			if (sb.length() > 0)
			{
				Trace.trace(sb.toString());
			}
			else
			{
				Trace.trace("empty");
			}
		}

		return sb.toString();
	}

    private String codegenRemoteClassImports( Map remoteClassAliases )
    {
        StringBuffer sb = new StringBuffer();

        if (remoteClassAliases.size() > 0)
            sb.append( "import flash.net.registerClassAlias;\nimport flash.net.getClassByAlias;\n" );

        for (Iterator it = remoteClassAliases.keySet().iterator(); it.hasNext(); )
        {
            String className = (String) it.next();
            sb.append( "import " + className + ";\n" );
        }
        return sb.toString();

    }
    private String codegenRemoteClassAliases( Map remoteClassAliases )
    {
        StringBuffer sb = new StringBuffer();

        for (Iterator it = remoteClassAliases.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry e = (Map.Entry) it.next();
            String className = (String) e.getKey();
            String alias = (String) e.getValue();

			sb.append( "      try {\n");
            sb.append( "      if (flash.net.getClassByAlias(\"" + alias + "\") == null){\n");
			sb.append( "          flash.net.registerClassAlias(\"" + alias + "\", " + className + ");}\n");
			sb.append( "      } catch (e:Error) {\n");
			sb.append( "          flash.net.registerClassAlias(\"" + alias + "\", " + className + "); }\n");
        }
        return sb.toString();
    }

	private String codegenEffectTriggerImports( Map effectTriggers )
	{
		if (effectTriggers.size() > 0)
		{
			return "import mx.effects.EffectManager;\n" + "import mx.core.mx_internal;\n";
		}
		else
		{
			return "";
		}
	}

	private String codegenEffectTriggerRegistration( Map effectTriggers )
	{
		StringBuffer sb = new StringBuffer();

		for (Iterator it = effectTriggers.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry e = (Map.Entry) it.next();
            String name = (String) e.getKey();
            String event = (String) e.getValue();

            sb.append( "      EffectManager.mx_internal::registerEffectTrigger(\"" + name + "\", \"" + event + "\");\n");
		}

		return sb.toString();
	}

    private String codegenInheritingStyleRegistration( Set inheritingStyles )
    {
		StringBuffer sb = new StringBuffer();
		sb.append("      var styleNames:Array = [");

        Iterator iterator = inheritingStyles.iterator();

        while ( iterator.hasNext() )
        {
            String styleName = (String) iterator.next();
            sb.append("\"" + styleName + "\"");
            if ( iterator.hasNext() )
            {
                sb.append(", ");
            }
		}

        sb.append("];\n\n");
        sb.append("      import mx.styles.StyleManager;\n\n");
        sb.append("      for (var i:int = 0; i < styleNames.length; i++)\n");
        sb.append("      {\n");
        sb.append("         StyleManager.registerInheritingStyle(styleNames[i]);\n");
        sb.append("      }\n");

		return sb.toString();
    }

    /**
     * Returns a string like
     *   [ "en_US", "ja_JP" ]
     */
    private static String codegenCompiledLocales( String[] locales )
    {
    	StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
        return "[ " + StringJoiner.join(locales, ", ", itemStringer) + " ]";
    }

    /**
     * Returns a string like
     *   [ "core", "controls", "MyApp" ]
     */
    private static String codegenCompiledResourceBundleNames( SortedSet bundleNames )
    {
    	StringJoiner.ItemStringer itemStringer = new StringJoiner.ItemQuoter();
        return "[ " + StringJoiner.join(bundleNames, ", ", itemStringer) + " ]";
	}

    private static String codegenCompatibilityCall(Configuration configuration)
    {
		String compatibilityCall;
		String compatibilityVersion = configuration.getCompatibilityVersionString();
		if (compatibilityVersion == null)
		{
			compatibilityCall = "";
		}
		else
		{
			compatibilityCall = "        FlexVersion.compatibilityVersionString = \"" + compatibilityVersion + "\";";
		}		
		return compatibilityCall;
    }
    
    static String codegenModuleFactory(String base,
            							String rootClassName,
							            String topLevelWindowClass,
							            Map rootAttributes,
							            List cdRsls,
							            List rsls,
							            List mixins,
							            List fonts,
							            List frames,
										String[] locales,
							            SortedSet resourceBundleNames,
							            SortedSet externalResourceBundleNames,
							            Configuration configuration,
							            CompilerSwcContext swcContext,
							            boolean isLibraryCompile)
    {
        boolean hasFonts = (fonts == null ? false : fonts.size() > 0);
		String lineSep = System.getProperty("line.separator");
		String[] codePieces = new String[]
	    {
	    	"package", lineSep,
	    	"{", lineSep, lineSep,
	    	codegenImports(base, rootAttributes, fonts, configuration, isLibraryCompile, hasFonts),
	    	codegenResourceBundleMetadata(externalResourceBundleNames),
            "/**", lineSep,
            " *  @private", lineSep,
            " */" , lineSep,
            "[ExcludeClass]", lineSep,	    	
	    	"public class ", rootClassName, lineSep,
	    	"    extends ", base, lineSep,
	    	"    implements IFlexModuleFactory", lineSep,
	    	"{", lineSep,
            codegenLinkInCrossDomainRSLItem(configuration, lineSep, cdRsls),
	    	"    public function ", rootClassName, "()", lineSep,
	    	"    {", lineSep,
	    	codegenCompatibilityCall(configuration), lineSep,
	    	"        super();", lineSep,
	    	codegenAddRslCompleteListener(isLibraryCompile, hasFonts, lineSep),
	    	"    }", lineSep, lineSep,
	    	!isLibraryCompile ?
	    	"    override " : "",
	    	"    public function create(... params):Object", lineSep,
	    	"    {", lineSep,
	    	codegenCreateApply(isLibraryCompile, lineSep),
	    	codegenGetMainClassName(topLevelWindowClass, configuration, lineSep),
		    "        var mainClass:Class = Class(getDefinitionByName(mainClassName));", lineSep,
		    "        if (!mainClass)", lineSep,
		    "            return null;", lineSep, lineSep,
		    "        var instance:Object = new mainClass();", lineSep,
		    "        if (instance is IFlexModule)", lineSep,
		    "            (IFlexModule(instance)).moduleFactory = this;", lineSep,
		    codegenRegisterEmbeddedFonts(fonts, lineSep),
		    "        return instance;", lineSep,
		    "    }", lineSep, lineSep,
		    !isLibraryCompile ? 
		    "    override" : "",
		    "    public function info():Object", lineSep,
		    "    {", lineSep,
		    "        return {", lineSep,
		    codegenInfo(topLevelWindowClass, rootAttributes, cdRsls, rsls, mixins, fonts,
		    			frames, locales, resourceBundleNames, configuration, swcContext),
		    "        }", lineSep,
		    "    }", lineSep, lineSep,
		    codegenRSLSecurityWrapper(isLibraryCompile, lineSep), lineSep,
		    codegenModuleFactorySecurityWrapper(isLibraryCompile, hasFonts, lineSep), lineSep,
		    codegenRslCompleteListener(isLibraryCompile, hasFonts, lineSep),
		    "}", lineSep, lineSep,
		    "}", lineSep,
	    };
		
		return StringJoiner.join(codePieces, null);
    }
    
	
    private static String codegenLinkInCrossDomainRSLItem(
                                        Configuration configuration, 
                                        String lineSep, 
                                        List cdRsls)
    {
        if (cdRsls == null || cdRsls.isEmpty()) 
        {
            return "";
        }
        
        String[] code = {
                "    // Cause the CrossDomainRSLItem class to be linked into this application.", lineSep,
                "    import mx.core.CrossDomainRSLItem; CrossDomainRSLItem;", lineSep, lineSep,
        };
        
        return StringJoiner.join(code, null);
    }

    private static String codegenImportEmbeddedFontRegistry(List fonts, String lineSep)
	{
    	if (fonts == null || fonts.size() == 0) 
    	{
    		return "";
    	}
    	
    	String[] code = {
    			"import mx.core.EmbeddedFontRegistry;", lineSep,
    	};
    	
		return StringJoiner.join(code, null);
	}

    
    private static String codegenRegisterEmbeddedFonts(List fonts, String lineSep)
	{
    	if (fonts == null || fonts.size() == 0) 
    	{
    		return "";
    	}
    	
    	String[] code = {
    	"        if (params.length == 0)", lineSep,
    	"            EmbeddedFontRegistry.registerFonts(info()[\"fonts\"], this);", lineSep,
    	};
    	
		return StringJoiner.join(code, null);
	}

	private static String codegenCreateApply(boolean isLibraryCompile, String lineSep)
	{
    	if (isLibraryCompile) 
    	{
    		return "";
    	}
    	
    	String[] code = {
    	"        if (params.length > 0 && !(params[0] is String))", lineSep,
    	"            return super.create.apply(this, params);", lineSep, lineSep,
    	};
    	
		return StringJoiner.join(code, null);
	}

	private static String codegenGetMainClassName(String topLevelWindowClass, Configuration configuration, String lineSep)
	{
		String[] code = {
				topLevelWindowClass == null ?
				"        var mainClassName:String = String(params[0])" :
		    	"        var mainClassName:String = params.length == 0 ? \"",
		    	topLevelWindowClass == null ? "" : topLevelWindowClass, 
		    	topLevelWindowClass == null ? "" : "\" : String(params[0]);", lineSep,
		};  
		return StringJoiner.join(code, null);


	}

	private static String codegenResourceBundleMetadata(SortedSet resourceBundleNames)
    {
    	if (resourceBundleNames == null) 
    	{
    		return "";
    	}
		String lineSep = System.getProperty("line.separator");
		
		StringBuffer codePieces = new StringBuffer();
		for (Iterator i = resourceBundleNames.iterator(); i.hasNext(); )
		{
			codePieces.append("[ResourceBundle(\"" + i.next() + "\")]" + lineSep);
		}
		
		return codePieces.toString();
    }

    
	private static String codegenImports(String base, Map rootAttributes, List fonts, 
										Configuration configuration, boolean isLibraryCompile,
										boolean hasFonts)
	{
		String lineSep = System.getProperty("line.separator");
		
		String[] codePieces = new String[]
	    {
			codegenEventImport(isLibraryCompile, hasFonts, lineSep),
			"import flash.display.LoaderInfo;", lineSep,
			"import flash.text.Font;", lineSep,
			"import flash.text.TextFormat;", lineSep,
			"import flash.system.ApplicationDomain;", lineSep,
            "import flash.system.Security;", lineSep,
			"import flash.utils.getDefinitionByName;", lineSep,
            "import flash.utils.Dictionary;", lineSep,
			"import mx.core.IFlexModule;", lineSep,
			"import mx.core.IFlexModuleFactory;", lineSep,
			codegenImportEmbeddedFontRegistry(fonts, lineSep),
			configuration.getCompatibilityVersionString() == null ? "" : "import mx.core.FlexVersion;", lineSep,
			"import ", base, ";", lineSep,
	    };
		
		String code = StringJoiner.join(codePieces, null);
	
		// TODO - eliminate any special handling of preloaderDisplayClass!
		if ((rootAttributes != null) && (rootAttributes.containsKey( "preloader")))
		{
			code += "import " + rootAttributes.get("preloader") + ";" + lineSep;
		}
		
		code += lineSep;
		
		return code;
	}
	
	
	/**
	 * Add mx.events.Event import to support RSL event handler.
	 * Only needed when compiling a SWC.
	 * 
	 * @param isLibraryCompile
	 * @param lineSep
	 * @return
	 */
	private static String codegenEventImport(boolean isLibraryCompile, boolean hasFonts, String lineSep)
	{
		if (!(isLibraryCompile && hasFonts)) 
		{
			return "";
		}

		String[] eventImport = {   			
				"import flash.events.Event", lineSep,
		};  
		return StringJoiner.join(eventImport, null);

	}
	
	
	private static String codegenInfo(String topLevelWindowClass,
					   Map rootAttributes,
					   List cdRsls,
			           List rsls,
			           List mixins,
			           List fonts,
			           List frames,
					   String[] locales,
			           SortedSet resourceBundleNames,
			           Configuration configuration,
			           CompilerSwcContext swcContext)
	{	
		// Build a map of the name/value pairs for the info
		TreeMap t = new TreeMap();
		
		t.put("currentDomain", "ApplicationDomain.currentDomain");
		
		if (topLevelWindowClass != null)
		{
			t.put("mainClassName", "\"" + topLevelWindowClass + "\"");
		}
				
		if (rootAttributes != null)
		{
			for (Iterator it = rootAttributes.entrySet().iterator(); it.hasNext(); )
			{
				Map.Entry e = (Map.Entry)it.next();
				
				// TODO - eliminate any special handling of preloaderDisplayClass!
				if ("preloader".equals(e.getKey()) || "usePreloader".equals(e.getKey()))
				{
					t.put(e.getKey(), e.getValue());
				}
				else if ("implements".equals(e.getKey()))
				{
					// skip
				}
				else
				{
					t.put(e.getKey(), "\"" + e.getValue() + "\"");
				}
			}
		}
		
		if ((cdRsls != null) && (cdRsls.size() > 0))
			t.put("cdRsls", codegenCdRslList(cdRsls, configuration, swcContext));
		
		if ((rsls != null) && (rsls.size() > 0))
			t.put("rsls", codegenRslList(rsls));
		
		if ((mixins != null) && (mixins.size() > 0))
			t.put("mixins", codegenMixinList(mixins));
		
		if ((fonts != null) && (fonts.size() > 0))
			t.put("fonts", codegenFontList(fonts) );
		
		if ((frames != null) && (frames.size() > 0))
			t.put("frames", codegenFrameClassList(frames));
		
		if (locales != null)
			t.put("compiledLocales", codegenCompiledLocales(locales));
		
		if ((resourceBundleNames != null) && (resourceBundleNames.size() > 0))
			t.put("compiledResourceBundleNames", codegenCompiledResourceBundleNames(resourceBundleNames));
		
		// Codegen a string from that map.
		String lineSep = System.getProperty("line.separator");
		StringJoiner.ItemStringer itemStringer = new StringJoiner.MapEntryItemWithColon();
		return "            " +
			   StringJoiner.join(t.entrySet(), "," + lineSep + "            ", itemStringer) +
			   lineSep;
	}
	
	/**
	 * Add an RSL complete listener to this RSL.
	 * 
     * @param isLibraryCompile true if we are compiling the library.swf for a swc.
	 * @param lineSep
	 * @return ActionScript code to add an RSL complete listener. If we are not compiling
	 * 		   the library.swf(RSL) of a SWC then no code is added.
	 */
	private static String codegenAddRslCompleteListener(boolean isLibraryCompile, boolean hasFonts,
	                          String lineSep) {
	
        if (!(isLibraryCompile && hasFonts)) 
		{
			return "";
		}

		String[] addCompleteListenerCall = {   			
    			"        this.root.loaderInfo.addEventListener(Event.COMPLETE, RSLRootCompleteListener);",
    			lineSep };  
		return StringJoiner.join(addCompleteListenerCall, null);
	}
	
	
	/**
	 * Add an RSL complete listener handler to this RSL.
	 * 
	 * @param isLibraryCompile true if we are compiling the library.swf for a swc.
	 * @param lineSep
     * @return ActionScript code to add an RSL complete listener. If we are not compiling
     *         the library.swf(RSL) of a SWC then no code is added.
	 */
	private static String codegenRslCompleteListener(boolean isLibraryCompile, boolean hasFonts,
	                            String lineSep)
	{
        if (!(isLibraryCompile && hasFonts)) 
		{
			return "";
		}
		
		String[] completeListener = {   			
				"    private function RSLRootCompleteListener(event:Event):void", lineSep,
				"    {", lineSep,
		    	"        EmbeddedFontRegistry.registerFonts(info()[\"fonts\"], this)", lineSep,
		    	"        this.root.removeEventListener(Event.COMPLETE, RSLRootCompleteListener);", lineSep,
		    	"    }", lineSep,	};
		
		return StringJoiner.join(completeListener, null);
	}
	
    /**
     * Implement the allowDomain() and allowInsecureDomain() of the IFlexModuleFactory interface.
     * This code is only generated for compiled applications and modules, not for RSLs code in a SWC.
     * 
     * @param isLibraryCompile true if we are compiling the library.swf for a swc.
     * @param lineSep
     * @return
     */
    private static String codegenModuleFactorySecurityWrapper(boolean isLibraryCompile, boolean hasFonts, 
                            String lineSep)
    {
        if (isLibraryCompile && !hasFonts) 
        {
            return "";
        }
        
        String[] code = {
            "    /**", lineSep,
            "     *  @private", lineSep,
            "     */", lineSep,
            "    private var _preloadedRSLs:Dictionary; // key: LoaderInfo, value: RSL URL", lineSep, lineSep,
            "    /**", lineSep,
            "     *  The RSLs loaded by this system manager before the application",  lineSep,
            "     *  starts. RSLs loaded by the application are not included in this list.", lineSep,
            "     */", lineSep,
            !isLibraryCompile ?
            "    override " : "",
            "    public function get preloadedRSLs():Dictionary", lineSep,
            "    {", lineSep,
            "        if (_preloadedRSLs == null)", lineSep,
            "           _preloadedRSLs = new Dictionary(true);", lineSep,
            "        return _preloadedRSLs;", lineSep,                
            "    }", lineSep, lineSep, 
            "    /**", lineSep,
            "     *  Calls Security.allowDomain() for the SWF associated with this IFlexModuleFactory", lineSep,
            "     *  plus all the SWFs assocatiated with RSLs preLoaded by this IFlexModuleFactory.", lineSep,
            "     *", lineSep, 
            "     */", lineSep,
            !isLibraryCompile ?
            "    override " : "",
            "    public function allowDomain(... domains):void", lineSep,
            "    {", lineSep,
            "        Security.allowDomain(domains);", lineSep, lineSep,
            "        for (var loaderInfo:Object in _preloadedRSLs)", lineSep,
            "        {", lineSep,
            "            if (loaderInfo.content && (\"allowDomainInRSL\" in loaderInfo.content))", lineSep,
            "            {", lineSep,
            "                loaderInfo.content[\"allowDomainInRSL\"](domains);", lineSep,
            "            }", lineSep,
            "        }", lineSep,
            "    }", lineSep, lineSep,
    
            "    /**", lineSep,
            "     *  Calls Security.allowInsecureDomain() for the SWF associated with this IFlexModuleFactory", lineSep,
            "     *  plus all the SWFs assocatiated with RSLs preLoaded by this IFlexModuleFactory.", lineSep,
            "     *", lineSep, 
            "     */", lineSep,
            !isLibraryCompile ?
            "    override " : "",
            "    public function allowInsecureDomain(... domains):void", lineSep,
            "    {", lineSep,
            "        Security.allowInsecureDomain(domains);", lineSep, lineSep,
            "        for (var loaderInfo:Object in _preloadedRSLs)", lineSep,
            "        {", lineSep,
            "            if (loaderInfo.content && (\"allowInsecureDomainInRSL\" in loaderInfo.content))", lineSep,
            "            {", lineSep,
            "                loaderInfo.content[\"allowInsecureDomainInRSL\"](domains);", lineSep,
            "            }", lineSep,
            "        }", lineSep,
            "    }", lineSep, lineSep,
        };
        
        return StringJoiner.join(code, null);
    }

    /**
     * Generate flash player Security wrapper calls.
     * 
     * @param lineSep
     * @return
     */
    static String codegenRSLSecurityWrapper(boolean isLibraryCompile, String lineSep)
    {
        if (!isLibraryCompile)
            return "";
        
        String[] code = {               
                "   /*", lineSep,
                "    *  Calls Security.allowDomain() for the SWF associated with this RSL", lineSep,
                "    *  @param a list of domains to trust. This parameter is passed to Security.allowDomain().", lineSep,
                "    */", lineSep,
                "   public function allowDomainInRSL(... domains):void", lineSep,
                "   {", lineSep,
                "       Security.allowDomain(domains);", lineSep,
                "   }", lineSep, lineSep,
                "   /*", lineSep,
                "    *  Calls Security.allowInsecureDomain() for the SWF associated with this RSL", lineSep,
                "    *  @param a list of domains to trust. This parameter is passed to Security.allowInsecureDomain().", lineSep,
                "    */", lineSep,
                "   public function allowInsecureDomainInRSL(... domains):void", lineSep,
                "   {", lineSep,
                "       Security.allowInsecureDomain(domains);", lineSep,
                "   }", lineSep,
        };
        
        return StringJoiner.join(code, null);
    }

    private void processCompiledResourceBundleInfoClass(List units,
            Configuration configuration,
            List sources,
            List mixins,
            List fonts,
            CompilerSwcContext swcContext)
	{
		CompilerConfiguration config = configuration.getCompilerConfiguration();

		// Don't add the _CompiledResourceBundleInfo class
		// if we are compiling in Flex 2 compatibility mode,
		// or if there are no locales,
		// or if there are no resource bundle names.

		int version = config.getCompatibilityVersion();
		if (version < MxmlConfiguration.VERSION_3_0)
		{
			return;
		}

		String[] locales = config.getLocales();
		if (locales.length == 0)
		{
			return;
		}
		
		if (resourceBundleNames.size() == 0)
		{
			return;
		}
		
		String className = I18nUtils.COMPILED_RESOURCE_BUNDLE_INFO;
		String code = I18nUtils.codegenCompiledResourceBundleInfo(locales, resourceBundleNames);
		
		String generatedFileName = className + "-generated.as";
		if (config.keepGeneratedActionScript())
		{
			saveGenerated(generatedFileName, code, config.getGeneratedDirectory());
		}

		Source s = new Source(new TextFile(code, generatedFileName, null,
							  MimeMappings.getMimeType(generatedFileName)),
							  "", className, null, false, false, false);
		s.setPathResolver(null);
		sources.add(s);
		
		// Ensure that this class gets linked in,
		// because no other code depends on it.
		// (ResourceManager looks it up by name.)
		configuration.getIncludes().add(className);
	}
	

	// error messages

	public static class NoExternalVisibleDefinition extends CompilerMessage.CompilerError
	{
		public NoExternalVisibleDefinition()
		{
			super();
		}
	}

	public static class MissingFactoryClassInFrameMetadata extends CompilerMessage.CompilerWarning
	{
		public MissingFactoryClassInFrameMetadata()
		{
			super();
		}
	}

	public static class InvalidBackgroundColor extends CompilerMessage.CompilerError
	{
		public String backgroundColor;

		public InvalidBackgroundColor(String backgroundColor)
		{
			super();
			this.backgroundColor = backgroundColor;
		}
	}

	public static class CouldNotParseNumber extends CompilerMessage.CompilerError
	{
		public CouldNotParseNumber(String num, String attribute)
		{
			this.num = num;
			this.attribute = attribute;
		}

		public String num;
		public String attribute;
	}
	
	public static class MissingSignedLibraryDigest extends CompilerMessage.CompilerError
	{
		public MissingSignedLibraryDigest(String libraryPath)
		{
			this.libraryPath = libraryPath;
		}

		public String libraryPath;
	}
	
	public static class MissingUnsignedLibraryDigest extends CompilerMessage.CompilerError
	{
		public MissingUnsignedLibraryDigest(String libraryPath)
		{
			this.libraryPath = libraryPath;
		}

		public String libraryPath;
	}

	public static class SignedRslsNotSupported extends CompilerMessage.CompilerWarning
	{
		public SignedRslsNotSupported(String libraryPath)
		{
			this.libraryPath = libraryPath;
		}

		public String libraryPath;
	}

	/**
	 *  Warn users with [RemoteClass] metadata that ends up mapping more than one class to the same alias. 
	 */
    public static class ClassesMappedToSameRemoteAlias extends CompilerMessage.CompilerWarning
    {
        private static final long serialVersionUID = 4365280637418299961L;
        
        public ClassesMappedToSameRemoteAlias(String className, String existingClassName, String alias)
        {
            this.className = className;
            this.existingClassName = existingClassName;
            this.alias = alias;
        }

        public String className;
        public String existingClassName;
        public String alias;
    }
	
}
