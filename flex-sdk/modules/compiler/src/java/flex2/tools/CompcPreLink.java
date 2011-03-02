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

import flash.util.StringJoiner;
import flex2.compiler.FileSpec;
import flex2.compiler.ResourceBundlePath;
import flex2.compiler.Source;
import flex2.compiler.SourceList;
import flex2.compiler.SourcePath;
import flex2.compiler.ResourceContainer;
import flex2.compiler.SymbolTable;
import flex2.compiler.CompilerSwcContext;
import flex2.compiler.CompilationUnit;
import flex2.compiler.i18n.I18nUtils;
import flex2.compiler.swc.Digest;
import flex2.compiler.swc.SwcException;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.common.Configuration;
import flex2.compiler.io.TextFile;
import flex2.compiler.io.VirtualFile;

import java.io.File;
import java.util.*;

/**
 * @author Brian Deitte
 */
public class CompcPreLink implements flex2.compiler.PreLink
{
	public CompcPreLink(Map rbFiles, List defaults)
	{
		this.rbFiles = rbFiles;
		this.defaults = defaults;
	}
	
	private Map rbFiles;
	private List defaults;
	
    public void run(List sources, List units,
                    FileSpec fileSpec, SourceList sourceList, SourcePath sourcePath, ResourceBundlePath bundlePath,
                    ResourceContainer resources, SymbolTable symbolTable, CompilerSwcContext swcContext,
                    Configuration configuration)
    {
    	postGenerateExtraSwcCode(sources, units, symbolTable, sourceList, sourcePath, bundlePath, resources, swcContext, configuration);
	    processResourceBundles(rbFiles, configuration, sources, defaults, symbolTable, bundlePath, swcContext);
    }

    private void postGenerateExtraSwcCode(List sources, List units, SymbolTable symbolTable, SourceList sourceList, SourcePath sourcePath,
    								  ResourceBundlePath bundlePath, ResourceContainer resources, CompilerSwcContext swcContext, Configuration configuration)
    {
    	LinkedList fonts = new LinkedList();
    	boolean isAccessible = configuration.getCompilerConfiguration().accessible();
    	Set accessibilityList = new HashSet(), externs = configuration.getExterns();
    	ArrayList checksumList = new ArrayList(units.size());
    	
    	for (int i = 0, size = units == null ? 0 : units.size(); i < size; i++)
    	{
    		CompilationUnit u = (CompilationUnit) units.get(i);
    		if (u != null && !PreLink.isCompilationUnitExternal(u, externs) && 
    			!u.getSource().isInternal())
    		{
    			if (isAccessible) {
        			Set unitAccessibilityList = u.getAccessibilityClasses();
        			if (unitAccessibilityList != null)
        			{
        				accessibilityList.addAll(unitAccessibilityList);
        			}
    			}

    			// don't add font assets for definitions that have been externed.
    			List fontList = u.getAssets().getFonts();
    			if (fontList != null && !fontList.isEmpty())
    			{
    				fonts.addAll(fontList);    // save for later...
    			}
    			
    			if (u.getSignatureChecksum() != null)
    			    checksumList.add(u.getSignatureChecksum());
    		}
    	}

    	if (accessibilityList.size() > 0)
    	{
    		for (Iterator it = accessibilityList.iterator(); it.hasNext();)
    		{
    			String className = (String) it.next();
    			MultiName mName = new MultiName(NameFormatter.retrievePackageName(className), NameFormatter.retrieveClassName(className));
    			flex2.compiler.API.resolveMultiName(mName, sources, sourceList, sourcePath, resources, swcContext, symbolTable);
    		}
    	}

    	// Sort the checksums to make sure the checksums are always in the same order.
    	// Later we will be creating a unique name for the root class from the digests
    	// of the checksums. That's why the order is important.
        StringBuffer checksumBuffer = new StringBuffer();
    	Collections.sort(checksumList);
    	for (Iterator iter = checksumList.iterator(); iter.hasNext();)
    	{
    	    Long checksum = (Long)iter.next();
            checksumBuffer.append(checksum.longValue());
    	}

        String uniqueRootClassName = new Digest().computeDigest(checksumBuffer.toString().getBytes());
	    codegenRootClass(sources, units, swcContext, configuration, fonts, uniqueRootClassName);
    }
     
    private void processResourceBundles(Map rbFiles, Configuration configuration, List sources, List defaults,
    									SymbolTable symbolTable, ResourceBundlePath bundlePath, CompilerSwcContext swcContext)
    {
	    if (rbFiles != null && !configuration.generateRBList())
	    {
    		String[] locales = configuration.getCompilerConfiguration().getLocales();
	    	Set s = new TreeSet(), externs = configuration.getExterns();
	    	
	    	for (int i = 0, size = sources.size(); i < size; i++)
	    	{
	    		Source src = (Source) sources.get(i);
	    		CompilationUnit unit = src == null ? null : src.getCompilationUnit();
	    		if (unit != null && !PreLink.isCompilationUnitExternal(unit, externs) && 
	    		    !src.isInternal())
	    		{
	    			s.addAll(unit.resourceBundleHistory);
	    		}
	    	}
	    	
	    	for (int i = 0, size = defaults == null ? 0 : defaults.size(); i < size; i++)
	    	{
	    		s.add(defaults.get(i));
	    	}

	    	for (Iterator i = s.iterator(); i.hasNext(); )
	    	{
	    		String rbName = NameFormatter.toColon((String) i.next());
	    		QName qName = new QName(rbName);
	    		
	    		VirtualFile[] files = bundlePath.findVirtualFiles(rbName);

	    		if (files == null)
	    		{
	    			files = swcContext.getVirtualFiles(locales, qName.getNamespace(), qName.getLocalPart());
	    		}

                if (files == null)
                {
                    // Handle Flex 2 style precompiled resource bundles.
                    QName precompiledQName = new QName(rbName + I18nUtils.CLASS_SUFFIX);
                    Source source = swcContext.getSource(precompiledQName.getNamespace(), 
                                                         precompiledQName.getLocalPart());

                    if (source != null)
                    {
                        externs.add(qName);
                        continue;
                    }
                }

	    		if (files == null && locales.length > 0)
	    		{
                    ThreadLocalToolkit.log(new SwcException.NoResourceBundleSource(rbName));
	    		}
	    		
	    		for (int j = 0, size = files == null ? 0 : files.length; j < size; j++)
	    		{
	    			if (files[j] != null)
	    			{
	    				String ext = MimeMappings.getExtension(files[j].getMimeType());
	    				String key = "locale/" + locales[j] + "/" + rbName.replace(':', '.').replace('.', '/') + ext;
	    				rbFiles.put(key, files[j]);
	    			}
	    		}
	    		
	    		if (files != null)
	    		{
	    			QName[] qNames = flex2.compiler.API.resolveResourceBundleName(rbName, sources, null, bundlePath, null, swcContext, symbolTable, locales);
	    			configuration.addExterns(qNames);
	    		}
	    	}
	    }
    }
    
    /**
     * Output code to create an RSL root class that is executed when the swf is loaded.
     * 
     * @param sources
     * @param units
     * @param swcContext
     * @param configuration
     * @param fonts
     * @param uniqueRootClassName - unique part of the root class name.
     */
	private void codegenRootClass(List sources, List units, 
						CompilerSwcContext swcContext, Configuration configuration,
						List fonts, String uniqueRootClassName)
	{
		// If there are no fonts in the swc, then we only need a small root class to all 
	    // wrappers for Security.allowDomain() and Security.allowInsecureDomain().
	    // Otherwise generate a module factory to load the font.
        String rootClassName = "_" + uniqueRootClassName + "_";
        String sourceText = null;
        
        if (fonts.size() == 0)
	    {
            rootClassName += "flash_display_Sprite";
            sourceText = codegenRSLRootClass("flash.display.Sprite", rootClassName);
	        
	    }
	    else
	    {
	        rootClassName += "mx_core_FlexModuleFactory";
	    	sourceText = PreLink.codegenModuleFactory("flash.display.Sprite", 
	    						rootClassName, 
	    						null,
	    						null,
	    						null,
	    						null, 
	    						null, 
	    						fonts,
	    						null,
	    						null,
	    						null,
	    						null,
	    						configuration, 
	    						swcContext,
	    						true);
	    }

        String generatedLoaderFile = rootClassName + ".as";
        Source s = new Source(new TextFile(sourceText,
                                                generatedLoaderFile,
                                                null,
                                                MimeMappings.getMimeType(generatedLoaderFile)),
                               "", rootClassName, null, false, false, false);
        // C: It doesn't look like this Source needs any path resolution. null is fine...
        s.setPathResolver(null);
        sources.add(s);
        configuration.setRootClassName(rootClassName);

        if (configuration.getCompilerConfiguration().keepGeneratedActionScript())
        {
            PreLink.saveGenerated(generatedLoaderFile, sourceText, configuration.getCompilerConfiguration().getGeneratedDirectory());
        }
	}


	/**
	 * 
	 * @param pathName
	 * @return filename of the swc with the extension removed and the integer
	 */
	private String getSwcClassName(String pathName)
	{
		File file = new File(pathName);
		String fileName = file.getName();
		int ext = fileName.lastIndexOf(".");
		if (ext != -1) 
		{
			fileName = fileName.substring(0, ext);
		}
		
		// replace non-word characters with an underscore.
		fileName = fileName.replaceAll("[\\W]", "_");

		return fileName;
	}

	/**
	 * Generate a root class for an RSL with wrapper calls to Security.allowDomain() and
	 * Security.allowInsecureDomain(). The purpose is to allow callers to trust the RSL SWFs
	 * in the same way they can trust an application swf.
	 * 
	 * @param base The class root class extends.
	 * @param rootClassName
	 * @return The root class actionscript class definition as a String.
	 */
    private static String codegenRSLRootClass(String base,
			String rootClassName)
	{
    	String lineSep = System.getProperty("line.separator");
    	String[] codePieces = new String[]
    	{
            "package", lineSep,
            "{", lineSep, lineSep,
            "import flash.display.Sprite;", lineSep,
            "import flash.system.Security;", lineSep, lineSep,
            "/**", lineSep,
            " *  @private", lineSep,
            " */" , lineSep,
            "[ExcludeClass]", lineSep,
            "public class ", rootClassName, lineSep,
            "    extends ", base, lineSep,
            "{", lineSep,
            "    public function ", rootClassName, "()", lineSep,
            "    {", lineSep,
            "        super();", lineSep,
            "    }", lineSep, lineSep,
            PreLink.codegenRSLSecurityWrapper(true, lineSep),
            "}", lineSep, lineSep,
            "}", lineSep,
    	};
    	
    	return StringJoiner.join(codePieces, null);
	}
}
