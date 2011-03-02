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

package flex2.compiler;

import flash.swf.tags.DefineTag;
import flex2.compiler.i18n.TranslationFormat;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.ResourceFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.swc.Component;
import flex2.compiler.swc.Swc;
import flex2.compiler.swc.SwcCache;
import flex2.compiler.swc.SwcDependencySet;
import flex2.compiler.swc.SwcGroup;
import flex2.compiler.swc.SwcPathResolver;
import flex2.compiler.swc.SwcScript;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.QName;
import flex2.compiler.util.QNameMap;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Loads SWCs through SwcGroup and provides the information needed from these SWCs for a compilation
 *
 * @author Roger Gonzalez
 * @author Brian Deitte
 */
public class CompilerSwcContext
{
	private final static String DOT_CSS = ".css";
	private final static String DOT_PROPERTIES = ".properties";
	private final static String LOCALE_SLASH = "locale/";
	private String compatibilityVersionString;

	public CompilerSwcContext(String compatibilityVersionString)
	{
		this(true, false, compatibilityVersionString);
	}

	/**
	 * 
	 * @param fullCompile
	 * @param cacheSwcCompilationUnits - set to false to allow toolchains, such as the OEM interface, 
	 *     to share the SwcCache singleton among multiple compile targets.
	 * @param compatibilityVersionString
	 */
	public CompilerSwcContext(boolean fullCompile, boolean cacheSwcCompilationUnits, String compatibilityVersionString)
	{
		this.fullCompile = fullCompile;
		this.cacheSwcCompilationUnits = cacheSwcCompilationUnits;
		this.compatibilityVersionString = compatibilityVersionString;
	}

    public int load( VirtualFile[] libPath,
                     VirtualFile[] rslPath,
                     VirtualFile[] themeFiles,
					 VirtualFile[] includeLibraries,
					 NameMappings mappings,
					 TranslationFormat format,
                     SwcCache swcCache )
    {
        SwcGroup libGroup = null;
        if ((libPath != null) && (libPath.length > 0))
        {
            libGroup = swcCache.getSwcGroup(libPath);
            lookupDefaultsStyles(libGroup);
	        addTimeStamps(libGroup);
        }

		SwcGroup rslGroup = null;
        if ((rslPath != null) && (rslPath.length > 0))
        {
            rslGroup = swcCache.getSwcGroup(rslPath);
            externs.addAll( rslGroup.getScriptMap().keySet() );
            lookupDefaultsStyles(rslGroup);
	        addTimeStamps(rslGroup);
        }

		SwcGroup includeGroup = null;
		if ((includeLibraries != null) && (includeLibraries.length > 0))
		{
			includeGroup = swcCache.getSwcGroup(includeLibraries);
			includes.addAll(includeGroup.getScriptMap().keySet());
			addResourceIncludes(includeGroup.getFiles());
            lookupDefaultsStyles(includeGroup);
			addTimeStamps(includeGroup);

            files.putAll( includeGroup.getFiles() );
        }

		List groupList = new LinkedList();
        groupList.add( libGroup );
        groupList.add( rslGroup );
		groupList.add( includeGroup );

		for (int i = 0; themeFiles != null && i < themeFiles.length; ++i)
        {
            if (themeFiles[i].getName().endsWith( DOT_CSS ))
            {
                themeStyles.add( themeFiles[i] );
	            ts.append(themeFiles[i].getLastModified());
            }
            else
            {
                SwcGroup tmpThemeGroup = swcCache.getSwcGroup( new VirtualFile[] {themeFiles[i] } );
                groupList.add( tmpThemeGroup );
                for (Iterator it = tmpThemeGroup.getFiles().values().iterator(); it.hasNext();)
                {
                    VirtualFile f = (VirtualFile) it.next();
	                ts.append(f.getLastModified());
                    if (f.getName().endsWith( DOT_CSS ))
                        themeStyles.add( f );
                }
            }
        }

        swcGroup = swcCache.getSwcGroup( groupList );

        if (swcGroup == null)
        {
            return 0;
        }

        toQNameMap(def2script, name2script, swcGroup.getScriptMap()); // populate def2script
        updateResourceBundles(swcGroup.getFiles(), format);
        
	    Set qnames = swcGroup.getQNames();
	    for (Iterator iterator = qnames.iterator(); iterator.hasNext();)
	    {
		    QName qName = (QName)iterator.next();
		    packageNames.add(qName.getNamespace());
	    }

        ThreadLocalToolkit.getPathResolver().addSinglePathResolver(new SwcPathResolver(swcGroup.getFiles()));

        mappings.addMappings( swcGroup.getNameMappings() );
        int num = swcGroup.getNumberLoaded();
        loaded += num;
        return num;
    }

	public int load( VirtualFile[] libPath,
					 NameMappings mappings,
					 String resourceFileExt,
					 SwcCache swcCache)
	{
		int retval = load(libPath, null, null, null, mappings, null, swcCache);
		if (swcGroup != null)
		{
			updateResourceBundles(swcGroup.getFiles(), resourceFileExt);
		}
		return retval;
	}

    private void addResourceIncludes(Map files)
    {
        Iterator iterator = files.keySet().iterator();

        while (iterator.hasNext())
        {
            String fileName = (String) iterator.next();

            if (fileName.startsWith(LOCALE_SLASH) && fileName.endsWith(DOT_PROPERTIES))
            {
                int begin = LOCALE_SLASH.length();
                begin = fileName.indexOf("/", begin) + 1;
                int end = fileName.length() - DOT_PROPERTIES.length();
                resourceIncludes.put(fileName.substring(begin, end).replace('/', '.'),
                                     files.get(fileName));
            }
        }
    }

    private void lookupDefaultsStyles(SwcGroup swcGroup)
    {
        Iterator iterator = swcGroup.getSwcs().values().iterator();
        String versionDefaultsCssFileName = null;

        if (compatibilityVersionString != null)
        {
            versionDefaultsCssFileName = "defaults-" + compatibilityVersionString + DOT_CSS;
        }

        while ( iterator.hasNext() )
        {
            Swc swc = (Swc) iterator.next();
            VirtualFile defaultsCssFile = null;

            if (versionDefaultsCssFileName != null)
            {
                defaultsCssFile = swc.getFile(versionDefaultsCssFileName);
            }

            if (defaultsCssFile == null)
            {
                defaultsCssFile = swc.getFile("defaults.css");
            }

            if (defaultsCssFile != null)
            {
                defaultsStyles.add(defaultsCssFile);
            }
        }
    }

	/**
	 * Get a file from specific SWC.   This should only be used for files already resolved by getFiles().
	 * Format is swclocation$filename
 	 */
	public VirtualFile getFile(String name)
	{
		return (swcGroup != null) ? swcGroup.getFile(name) : null;
	}

	/**
	 * Get a map for files that are in this context's SwcGroup.
	 * 
	 * @return Map of file in this context's swc group; key = filename, value = VirtualFile
 	 */
	public Map getFiles()
	{
		return (swcGroup != null) ? swcGroup.getFiles() : Collections.EMPTY_MAP;
	}


	private void addTimeStamps(SwcGroup libGroup)
	{
		if (libGroup != null)
		{
			List lastModified = libGroup.getSwcTimes();
			for (int i = 0, size = lastModified.size(); i < size; i++)
			{
				ts.append(lastModified.get(i));
			}
		}
	}

	public VirtualFile[] getVirtualFiles(String[] locales, String namespaceURI, String localPart)
	{
        Map rbFiles = (Map) rb2file.get(namespaceURI, localPart); 
        if (rbFiles == null || locales.length == 0)
        {
        	return null;
        }
        
        VirtualFile[] rbList = locales.length == 0 ? null : new VirtualFile[locales.length];
        for (int i = 0; i < locales.length; i++)
        {
        	rbList[i] = (VirtualFile) rbFiles.get(locales[i]);
        }
        
        return rbList;        
	}
	
	public Source getResourceBundle(String[] locales, String namespaceURI, String localPart)
	{
		if (locales.length == 0) return null;
		
        if (rb2source.containsKey( namespaceURI, localPart ))
            return (Source) rb2source.get( namespaceURI, localPart );
                
        Source s = null;
        
        VirtualFile[] rbList = getVirtualFiles(locales, namespaceURI, localPart);
        if (rbList != null && rbList.length > 0)
        {
        	String name = null;
        	
        	for (int i = 0; i < rbList.length; i++)
        	{
        		if (rbList[i] != null)
        		{
        			name = rbList[i].getName();
        			break;
        		}
        	}
        	
        	if (name != null)
        	{
        		rb2source.put(namespaceURI, localPart,
        					  s = new Source(new ResourceFile(name, locales, rbList, new VirtualFile[rbList.length]),
        							  		 null,
        							  		 namespaceURI.replace('.', '/'),
        							  		 localPart,
        							  		 this,
        							  		 false,
        							  		 false,
        							  		 false));
        	}
        }
        
        return s;
	}

	public Source getSource( String name )
	{
        if (name2source.containsKey( name ))
            return (Source) name2source.get( name );

        SwcScript script = (SwcScript) name2script.get(name);
		Source s = createSource(script);
		if (s != null)
		{
			name2source.put(name, s);
			for (Iterator i = s.getCompilationUnit().topLevelDefinitions.iterator(); i.hasNext(); )
			{
				QName qName = (QName) i.next();
				def2source.put(qName.getNamespace(), qName.getLocalPart(), s);
			}
		}
		
		return s;
	}
	
    public Source getSource( String namespaceURI, String localPart )
    {
        if (def2source.containsKey( namespaceURI, localPart ))
            return (Source) def2source.get( namespaceURI, localPart );

        SwcScript script = (SwcScript) def2script.get( namespaceURI, localPart );
        Source s = createSource(script);
        if (s != null)
        {
	        def2source.put( namespaceURI, localPart, s );
	        name2source.put(s.getName(), s);
        }

        return s;
    }
    
    private Source createSource(SwcScript script)
    {
        if (script == null)
            return null;

        String loc = script.getLibrary().getSwcLocation();
        InMemoryFile f = new InMemoryFile( script.getDoABC().abc, loc + "(" + script.getName() + ")", MimeMappings.ABC, script.getLastModified() );

	    // FIXME: C: I tried to set playerglobal.swc as an externally lib, but FlexMovie seems to allow externs on the last frame??
	    Source s = (loc.endsWith(StandardDefs.SWC_PLAYERGLOBAL) ||
					loc.endsWith(StandardDefs.SWC_AIRGLOBAL) ||
					loc.endsWith(StandardDefs.SWC_AVMPLUS)) ?
					new Source(f, "", "", script, true, false, false) :
					new Source( f, "", "",  script, false, false, false);
		// C: abc-based Sources don't need path resolution. null is fine...
		s.setPathResolver(null);
        CompilationUnit u = s.newCompilationUnit(null, new flex2.compiler.Context());

        u.setSignatureChecksum(script.getSignatureChecksum());

        for (Iterator i = script.getDefinitionIterator(); i.hasNext();)
        {
            u.topLevelDefinitions.add(new QName((String) i.next()));
        }

        SwcDependencySet set = script.getDependencySet();

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.INHERITANCE); i != null && i.hasNext();)
        {
            u.inheritance.add(new MultiName((String) i.next()));
        }

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.SIGNATURE); i != null && i.hasNext();)
        {
            u.types.add(new MultiName((String) i.next()));
        }

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.NAMESPACE); i != null && i.hasNext();)
        {
            u.namespaces.add(new MultiName((String) i.next()));
        }

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.EXPRESSION); i != null && i.hasNext();)
        {
            u.expressions.add(new MultiName((String) i.next()));
        }
        
        // C: use symbol dependencies to obtain additional class dependencies,
        //    i.e. classX --> symbolX --> symbolY --> classY, but there is no dependency between classX and classY.
        for (Iterator i = script.getSymbolClasses().iterator(); i.hasNext(); )
        {
        	u.expressions.add(new MultiName((String) i.next()));
        }

	    Map misc = script.getMiscData();
	    if (misc == null)
	    {
		    if (cacheSwcCompilationUnits)
		    {
			    misc = new HashMap(4);
			    script.setMiscData(misc);
		    }
	    }
	    else if (!fullCompile && misc.containsKey(CompilationUnit.COMPILATION_UNIT))
	    {
		    CompilationUnit oldUnit = (CompilationUnit) misc.get(CompilationUnit.COMPILATION_UNIT);

		    // This else-if section should look similar to Source.copy().
		    Source.copyMetaData(oldUnit, u);
	    }

	    u.getContext().setAttribute("SwcScript.misc", misc);

        for (Iterator i = script.getDefinitionIterator(); i.hasNext();)
        {
            String name = (String) i.next();
            DefineTag tag = script.getLibrary().getSymbol( name );
            if (tag != null)
            {
                u.getAssets().add( name, tag );
            }
        }

        if (loc.trim().length() < loc.length())
        {
            errlocations.add( loc.trim() );
            return null;
        }
        return s;
    }

    public int getNumberLoaded()
    {
        return loaded;
    }

    public Set getExterns()
    {
        return externs;
    }

	public Set getIncludes()
	{
		return includes;
	}

	public Map getResourceIncludes() // Map<String, VirtualFile>
	{
		return resourceIncludes;
	}

    public Map getIncludeFiles()
    {
        return files;
    }

    public boolean hasPackage(String packageName)
	{
		return packageNames.contains(packageName);
	}

	public boolean hasDefinition(QName qName)
	{
		return def2script.get(qName.getNamespace(), qName.getLocalPart()) != null;
	}

	public List getDefaultsStyleSheets()
	{
		return defaultsStyles;
	}

	public List getThemeStyleSheets()
    {
        return themeStyles;
    }

    public List errorLocations()
    {
        return errlocations;
    }

	public int checksum()
	{
		byte[] b = null;

		try
		{
			b = ts.toString().getBytes("UTF8");
		}
		catch (UnsupportedEncodingException ex)
		{
			b = ts.toString().getBytes();
		}

		int checksum = 0;

		// C: There are better algorithms to calculate checksums than this. Let's worry about it later.
		for (int i = 0; i < b.length; i++)
		{
			checksum += b[i];
		}

		return checksum;
	}

	public void close()
	{
        if (!locked && swcGroup != null)
		{
			swcGroup.close();
		}
	}

    public void setLock(boolean lock)
    {
        locked = lock;
    }
    

    /**
	 * Get an individual swc.
	 * 
	 * @param name - name of the swc's virtual filename, may not be null.
	 * @return Swc - the swc in this context or null if the swc is not found.
	 * @throws NullPointerException - if name is null 
	 */
    public Swc getSwc(String name) 
    {
    	return (swcGroup != null) ? swcGroup.getSwc(name) : null;
    }
    
    private boolean locked = false;

    private SwcGroup swcGroup;
	private QNameMap def2source = new QNameMap();
    private QNameMap def2script = new QNameMap();
    private Map name2source = new HashMap();
    private Map name2script = new HashMap();
    private QNameMap rb2source = new QNameMap();
    private QNameMap rb2file = new QNameMap();
    private Set components;
	private Set packageNames = new HashSet();
    private Set externs = new HashSet();
    private Set includes = new HashSet();
    private Map resourceIncludes = new HashMap();
    private Map files = new HashMap();
    private int loaded = 0;
    private List defaultsStyles = new LinkedList();    // VirtualFile
    private List themeStyles = new LinkedList();    // VirtualFile
    private List errlocations = new LinkedList();
	private StringBuffer ts = new StringBuffer(); // last modified time of all the swc and css files...
	private boolean fullCompile; // whether or not this CompilerSwcContext participates in a full compilation...
	private boolean cacheSwcCompilationUnits; // if true, we setup storage for intermediate type info objects when doing incremental compilation...

	private void toQNameMap(QNameMap qNameMap, Map scriptNameMap, Map scriptMap)
	{
		for (Iterator i = scriptMap.keySet().iterator(); i.hasNext();)
		{
			String key = (String) i.next();
			SwcScript value = (SwcScript) scriptMap.get(key);
			qNameMap.put(new QName(key), value);
			/*
	        String loc = value.getLibrary().getSwcLocation();
	        String name = loc + "(" + value.getName() + ")";
			scriptNameMap.put(name, value);
			*/
		}
	}
	
	private void updateResourceBundles(Map files, TranslationFormat format)
	{
		for (Iterator i = files.keySet().iterator(); format != null && i.hasNext();)
		{
			String name = (String) i.next();
			if (name.startsWith("locale/"))
			{
				VirtualFile file = (VirtualFile) files.get(name);
				int prefixLength = "locale/".length(), index = name.indexOf('/', prefixLength);
				String mimeType = file.getMimeType();
				if (index != -1 && format.isSupported(mimeType))
				{
					String locale = name.substring(prefixLength, index);
					String ext = MimeMappings.getExtension(mimeType);
					QName rbName = new QName(NameFormatter.toColon(name.substring(index + 1, name.length() - ext.length()).replace('/', '.')));
					
					Map rbFiles = (Map) rb2file.get(rbName);
					if (rbFiles == null)
					{
						rb2file.put(rbName, rbFiles = new HashMap());
					}
					rbFiles.put(locale, file);
				}
			}
		}
	}
	
	private void updateResourceBundles(Map files, String ext)
	{
		for (Iterator i = files.keySet().iterator(); ext != null && i.hasNext();)
		{
			String name = (String) i.next();
			if (name.startsWith("locale/") && name.endsWith(ext))
			{
				VirtualFile file = (VirtualFile) files.get(name);
				int prefixLength = "locale/".length(), index = name.indexOf('/', prefixLength);
				if (index != -1)
				{
					String locale = name.substring(prefixLength, index);
					QName rbName = new QName(NameFormatter.toColon(name.substring(index + 1, name.length() - ext.length()).replace('/', '.')));
					
					Map rbFiles = (Map) rb2file.get(rbName);
					if (rbFiles == null)
					{
						rb2file.put(rbName, rbFiles = new HashMap());
					}
					rbFiles.put(locale, file);
				}
			}
		}
	}
	
	public Iterator getDefinitionIterator()
	{
		return def2script.keySet().iterator();
	}

	// C: Only the Flex Compiler API (flex-compiler-oem.jar) uses this method.
	//    Do not use it in the mxmlc/compc codepath.
	public flex2.tools.oem.Script getScript(QName def, boolean includeBytecodes)
	{
		SwcScript s = (SwcScript) def2script.get(def);
		return (s != null) ? s.toScript(includeBytecodes) : null;
	}

	// C: Only the Flex Compiler API (flex-compiler-oem.jar) uses this method.
	//    Do not use it in the mxmlc/compc codepath.
	public Iterator getComponentIterator()
	{
		if (components == null)
		{
			components = new HashSet();
			
			for (Iterator i = getDefinitionIterator(); i.hasNext(); )
			{
				QName def = (QName) i.next();
				SwcScript script = (SwcScript) def2script.get(def);
				Component c = script.getLibrary().getSwc().getComponent(def.toString());
				if (c != null)
				{
					components.add(c);
				}
			}
		}
		
		return components.iterator();
	}
	
	public NameMappings getNameMappings()
	{
		return (swcGroup != null) ? swcGroup.getNameMappings() : null;
	}
	
	/**
	 * Find the signature checksum of a definition.
	 * 
	 * @param def - may not be null
	 * @return Signature checksum of def, null if def not found or a 
	 * 			signature checksum does not exist for def.
	 * @throws NullPointerException if def is null.
	 */
	public Long getChecksum(QName def)
	{
		if (def == null)
		{
			throw new NullPointerException("getCheckSum: def may not be null");
		}
		
		SwcScript script = (SwcScript) def2script.get(def);
		if (script != null)
		{
			return script.getSignatureChecksum();
		}
		
		return null;
	}

}
