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

package flex2.compiler.swc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import flash.swf.MovieEncoder;
import flash.swf.TagEncoder;
import flex2.compiler.CompilationUnit;
import flex2.compiler.common.MxmlConfiguration;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.catalog.CatalogReader;
import flex2.compiler.swc.catalog.CatalogWriter;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.MultiNameSet;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.linker.LinkerException;
import flex2.tools.PreLink;
import flex2.tools.VersionInfo;


/**
 * The representation of a SWC.  Contains the main entry points for the compiler to use when getting information
 * about a SWC.
 *
 * A Swc can be used by multiple threads at the same time so it should not hold onto anything from a specific
 * compile.  It also can be dumped when its stale so it should not be stored (or pieces of it stored, like
 * the Catalog) from anywhere other than SwcGroup.
 *
 * @author Brian Deitte
 * @author Roger Gonzalez
 *
 * Reading:
 *  - A SWC can exist in a partially decoded state.
 *  - When the SWC is fully read, it is FULLY READ AND CLOSED, and the backing file can be deleted.
 *  - The SWC should be able to discern if the in-memory snapshot is out of date.
 *
 * Writing:
 *  - The state of the SWC object after writing should be the same as if it had been fully read in.
 *
 * Updating:
 *  - The SWC should be entirely rewritten to a temporary location, then copied atomically to the new location.
 *
 */
public class Swc
{
    protected static boolean FNORD = false;      // change this when we ship, true = release, false = alpha

    public static String LIBRARY_SWF = "library.swf";
    public static String CATALOG_XML = "catalog.xml";
    
    public Swc( SwcArchive archive ) throws Exception
    {
        this( archive, false );
    }

    // not public on purpose- use SwcCache.getSwcGroup() instead
    Swc( SwcArchive archive, boolean load ) throws Exception
    {
        this.archive = archive;
        if (load)
        {
            read();
        }
    }

    long getLastModified()
    {
        return lastModified;
    }

    void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }

    /**
     * The location of the swc in the file system
     */
    public String getLocation()
    {
        return archive.getLocation();
    }


    public Iterator getLibraryIterator()
    {
        return libraries.values().iterator();
    }

    public SwcLibrary buildLibrary( String libname, flex2.linker.Configuration configuration, List units )
    		throws IOException, LinkerException
    {
    	SwcMovie m = flex2.compiler.swc.API.link(configuration, units);
    	return buildLibrary(libname, configuration, m);
    }

    /**
     * buildLibrary - Given a bunch of compile state, produce a SwcLibrary and all associated SwcScripts
     */
    public SwcLibrary buildLibrary( String libname, flex2.linker.Configuration configuration, SwcMovie movie)
            throws IOException, LinkerException
    {
        int version = configuration.getCompatibilityVersion();
        forceLibraryVersion1 = version < MxmlConfiguration.VERSION_3_0;
        
        // get SWF bytes
        ByteArrayOutputStream swfOut = new ByteArrayOutputStream();
        TagEncoder encoder = new TagEncoder();
        new MovieEncoder(encoder).export(movie);
        encoder.writeTo(swfOut);

        swfOut.flush();
        byte[] swf = swfOut.toByteArray();
        swfOut.close();

        String libPath = libname + ".swf";

        SwcLibrary lib = new SwcLibrary( this, libPath );
        VirtualFile swfFile = new InMemoryFile(swf, libPath,
                                               MimeMappings.getMimeType(libPath), new Date().getTime());
        archive.putFile( swfFile );
        libraries.put( libPath, lib );

        // check if we should compute the digest.
        if (configuration.getComputeDigest())
        {
            // set digest info
            Digest digest = new Digest();
            digest.setSigned(false);
            digest.setType(Digest.SHA_256);
            digest.computeDigest(swf);
            
            lib.setDigest(digest);
        }
        
        // initialize metadata from configuration
        initMetadata(lib, configuration);
        
        // If we linked without error, the unresolved list will contain nothing but valid externs.
        Set externs = lib.getExterns();
        externs.addAll( configuration.getUnresolved() );
        
        Set librariesProcessed = new HashSet();
        for (Iterator iterator = movie.getExportedUnits().iterator(); iterator.hasNext();)
        {
            CompilationUnit unit = (CompilationUnit) iterator.next();
            flex2.compiler.Source unitSource = unit.getSource();

            SwcDependencySet depset = new SwcDependencySet();
            addDeps( depset, SwcDependencySet.INHERITANCE, unit.inheritance );
            addDeps( depset, SwcDependencySet.SIGNATURE, unit.types );
            addDeps( depset, SwcDependencySet.NAMESPACE, unit.namespaces );
            addDeps( depset, SwcDependencySet.EXPRESSION, unit.expressions );

            addExtraClassesDeps( depset, unit.extraClasses );

            Set scriptDefs = unit.topLevelDefinitions.getStringSet();
            checkDefs(scriptDefs, unitSource.getName());

            String sourceName = NameFormatter.nameFromSource(unitSource);
            lib.addScript( sourceName, scriptDefs, depset, unitSource.getLastModified(),
            			   unit.getSignatureChecksum());
            addIcons(unit, sourceName);
            
            // find the source and add the metadata
            if (unitSource.isSwcScriptOwner() && !unitSource.isInternal() && 
                !PreLink.isCompilationUnitExternal(unit, externs))
            {
                SwcScript script = (SwcScript)unitSource.getOwner();
                SwcLibrary library = script.getLibrary();
                
                // lots of scripts, but not many swcs, so avoid added the same metadata
                // over and over.
                if (!librariesProcessed.contains(library))
                {
                    librariesProcessed.add(library);
                    lib.addMetadata(script.getLibrary().getMetadata());
                }
            }
        }
        return lib;
    }

    /**
     * init metadata from -keep-as3-metadata option
     * 
     * @param configuration
     */
    private void initMetadata(SwcLibrary swcLibrary, flex2.linker.Configuration configuration)
    {
    	String[] configMetaData = configuration.getMetadataToKeep();
    	
    	if (configMetaData == null) 
    	{
    		return;
    	}
    	
    	if (configMetaData.length > 0)
    	{
    		swcLibrary.addMetadata(Arrays.asList(configMetaData));
    	}
    }
    
    private void addIcons(CompilationUnit unit, String sourceName)
            throws IOException
    {
        String icon = unit.iconFile == null ? null : unit.iconFile.getValue(0);
        if (icon != null)
        {
            VirtualFile iconFile = unit.getSource().resolve(icon);

            if (iconFile == null)
            {
                if (unit.getSource().isSwcScriptOwner())
                {
                    for (int i = 0, s = unit.topLevelDefinitions.size();i < s; i++)
                    {
                        String def = unit.topLevelDefinitions.get(i).toString();
                        if (components.containsKey(def))
                        {
                            String swcIcon = ((Component)components.get(def)).getIcon();
                            if (swcIcon != null)
                            {
                                iconFile = (((SwcScript)unit.getSource().getOwner()).getLibrary().getSwc().getFile(swcIcon));
                                if (iconFile != null)
                                {
                                    // we then put the resolved file into an InMemoryFile so that we can changed its name
                                    VirtualFile inMemFile = new InMemoryFile(iconFile.getInputStream(), swcIcon,
                                                                             MimeMappings.getMimeType(swcIcon), iconFile.getLastModified());
                                    archive.putFile( inMemFile );
                                    return;
                                }
                            }
                        }
                    }
                    if (iconFile == null)
                        return;
                }
            }

            if (iconFile == null)
            {
                throw new SwcException.MissingIconFile( icon, sourceName );
            }

            // yes using both toDot and toColon here feels very wacky
            String workingSourceName = NameFormatter.toColon(NameFormatter.toDot(sourceName, '/'));
            Component comp = (Component)components.get(workingSourceName);
            String rel = unit.getSource().getRelativePath();
            String iconName = (rel == null || rel.length() == 0) ? icon : rel + "/" + icon;
            if (comp != null)
            {
                comp.setIcon(iconName);
            }

            // we then put the resolved file into an InMemoryFile so that we can changed its name
            VirtualFile inMemFile = new InMemoryFile(iconFile.getInputStream(), iconName,
                                                     MimeMappings.getMimeType(iconName), iconFile.getLastModified());
            archive.putFile( inMemFile );
        }
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected void checkDefs(Set scriptDefs, String sourceName)
    {
        for (Iterator iter2 = scriptDefs.iterator(); iter2.hasNext();)
        {
            String str = (String)iter2.next();
            String script = (String)defs.get(str);
            if (script != null)
            {
                throw new SwcException.DuplicateDefinition(str, script, sourceName);
            }
            defs.put(str, sourceName);
        }
    }

    private static void addDeps( SwcDependencySet depset, String type, MultiNameSet mns )
    {
        for (Iterator it = mns.iterator(); it.hasNext();)
        {
            // FIXME - this multinameset sometimes holds qnames.
            Object o = it.next();

            if (o instanceof MultiName)
            {
                MultiName mname = (MultiName) o;
                assert mname.getNumQNames() == 1;
                depset.addDependency( type, mname.getQName( 0 ).toString() );
            }
            else
            {
                assert o instanceof QName;
                depset.addDependency( type, o.toString() );
            }
        }
    }

    private static void addExtraClassesDeps( SwcDependencySet depset, Set extraClasses )
    {
        for (Iterator it = extraClasses.iterator(); it.hasNext();)
        {
            String extraClass = (String) it.next();
            depset.addDependency(SwcDependencySet.EXPRESSION, extraClass);
        }
    }

    // not public on purpose- use SwcCache.export() instead
    synchronized boolean save() throws Exception
    {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	    Writer out = new OutputStreamWriter(byteOut, "UTF-8");

	    // TODO - move feature setting out somewhere
	    if (components.size() > 0)
		    features.setComponents( true );
	    if (archive.getFiles().size() > 0)
		    features.setFiles( true );

		/*System.out.println("catalog save: VersionInfo.getLibVersion() = " + VersionInfo.getLibVersion() +
				", VersionInfo.getFlexVersion() = " + VersionInfo.getFlexVersion() +
				", VersionInfo.getBuildAsLong() = " + VersionInfo.getBuildAsLong());*/

	    // get the version we will save the swc libraries as.
	    String currentVersion = determineSwcLibraryVersion();
	    
		versions.setLibVersion(currentVersion);
	    versions.setFlexVersion(VersionInfo.getFlexVersion());
	    versions.setFlexBuild(VersionInfo.getBuild());

	    for (Iterator it = libraries.values().iterator(); it.hasNext();)
	    {
		    SwcLibrary l = (SwcLibrary) it.next();
		    if (l.getExterns().size() > 0)
		    {
			    features.setExternalDeps( true );
			    break;
		    }
	    }
	   
	    CatalogWriter writer = new CatalogWriter(out, versions, features, 
	    										components.values(), 
	    										libraries.values(),
	                                            archive.getFiles().entrySet());
	    writer.write();
	    out.close();
	    archive.putFile( CATALOG_XML, byteOut.toByteArray(), new Date().getTime() );

	    archive.save();

	    return ThreadLocalToolkit.errorCount() == 0;
    }

    /**
     * Get the version this swc should be saved as.
     *
     */
    private String determineSwcLibraryVersion() {
        
        // the flex library version was 1.0 in Flex 2.0.1 sdk.
        if (forceLibraryVersion1)
        {
            return VersionInfo.LIB_VERSION_1_0; 
        }
        
        return VersionInfo.LIB_VERSION_1_2;            
    }
    
    public Map getCatalogFiles()
    {
        return catalogFiles;
    }

    public VirtualFile getFile(String path)
    {
        return (VirtualFile) catalogFiles.get(path);
    }

    public void addFile( VirtualFile file )
    {
        archive.putFile( file );
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected void read() throws Exception
    {
	    InputStream stream = null;
	    try
	    {
		    archive.load();

		    VirtualFile catalogFile = archive.getFile( CATALOG_XML );
		    if (catalogFile == null)
		    {
			    throw new SwcException.CatalogNotFound();
		    }
		    stream = catalogFile.getInputStream();
		    CatalogReader reader = new CatalogReader(new BufferedInputStream(stream), this, archive);
		    reader.read();

		    versions = reader.getVersions();
		    features = reader.getFeatures();
		    components = reader.getComponents();
		    libraries = reader.getLibraries();
		    catalogFiles = reader.getFiles();

			/**
			 * version checking:
			 * - a failure results in a warning, not an error
			 * - we do an across-the-board check for SWC major lib version <= compiler major lib version
			 * - all other checks are ad-hoc and will accumulate as we rev lib version
			 * - see VersionInfo for more version info
			 */
		    // double swcLibVersion = versions.getLibVersion();
		    // double compilerLibVersion = VersionInfo.getLibVersion();

			//	System.out.println("read(): swcLibVersion=" + swcLibVersion + ", compilerLibVersion=" + compilerLibVersion);

			//	Warn if the SWC was built with a newer compiler
			// if (Math.floor(swcLibVersion) > Math.floor(compilerLibVersion))
		    if (versions.getLibVersion() != null && VersionInfo.IsNewerLibVersion(versions.getLibVersion(), true))
		    {
			    OldVersion oldVersion = new OldVersion(archive.getLocation(), versions.getLibVersion(),
						VersionInfo.getLibVersion());
			    ThreadLocalToolkit.log(oldVersion);
		    }

			/**
			 * Other major-version-specific range tests would go here
			 */
		}
	    finally
	    {
		    if (stream != null)
		    {
			    try {
				    stream.close();
			    }
			    catch(IOException ioe) {
				    // ignore
			    }
		    }
	    }
    }

	public void close()
	{
		archive.close();
	}

    public Iterator getComponentIterator()
    {
        return components.values().iterator();
    }
    
    public Component getComponent(String className)
    {
    	return (Component) components.get(className);
    }

	public Versions getVersions()
	{
	    return versions;
	}

    public Features getFeatures()
    {
        return features;
    }

 
    /**
     * Get the digest of a specified library, using the default hash type.
     * 
     * @param libPath
     * 			name of library path. If in doubt pass in LIBRARY_PATH.
     * @param isSigned
     * 			if true return a signed digest, if false return an unsigned digest.
     * 
     * @return the digest of the specified library. May be null if not digest is found.
     */
    public Digest getDigest(String libPath, boolean isSigned)
    {
    	return getDigest(libPath, Digest.SHA_256, isSigned);
    }
    
    
    /**
     * Get the digest of a specified library.
     * 
     * @param libPath
     * 			name of library path. If in doubt pass in LIBRARY_PATH.
     * @param hashType
     * 			type of hash. Only valid choice is Digest.SHA_256.
     * @param isSigned
     * 			if true return a signed digest, if false return an unsigned digest.
     * 
     * @return the digest of the specified library. May be null if not digest is found.
     */
    public Digest getDigest(String libPath, String hashType, boolean isSigned)
    {
    	if (libPath == null)
    	{
    		throw new NullPointerException("libPath may not be null");
    	}
    	
    	if (hashType == null)
    	{
    		throw new NullPointerException("hashType may not be null");
    	}
    	
        SwcLibrary lib = (SwcLibrary) libraries.get(Swc.LIBRARY_SWF);
        if (lib != null)
        {
            return lib.getDigest(hashType, isSigned);
        }
        
        return null;
    }
    
    
    /**
     * Add a new digest to the swc or replace the existing digest if a
     * digest for libPath already exists.
     * 
     * @param libPath name of the library file, may not be null
     * @param digest digest of libPath
     * @throws NullPointerException if libPath or digest are null.
     */
    public void setDigest(String libPath, Digest digest)
    {
    	if (libPath == null)
    	{
    		throw new NullPointerException("setDigest: libPath may not be null"); // $NON-NLS-1$
    	}
    	if (digest == null)
    	{
    		throw new NullPointerException("setDigest:  digest may not be null");  // $NON-NLS-1$
    	}

    	SwcLibrary lib = (SwcLibrary) libraries.get(Swc.LIBRARY_SWF);
        if (lib != null)
        {
            lib.setDigest(digest);
        }
    }
    
    
    public void addComponent(Component c)
    {
        components.put( c.getClassName(), c );
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected SwcArchive getArchive()
    {
        return archive;
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected final SwcArchive archive;
    private long lastModified = -1;

    /** Each object in the Map is of type SwcLibrary.
     *  The object is hashed into the Map with the path of the library.
     */
    // changed next 5 from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected Map libraries = new HashMap();
    protected Map catalogFiles;
    protected Map components = new TreeMap();
	protected Versions versions = new Versions();
    protected Features features = new Features();
    private Map defs = new HashMap();
    private boolean forceLibraryVersion1;       // if true for swc to library version 1.0
    
    
	public static class OldVersion extends CompilerMessage.CompilerWarning
	{
		public OldVersion(String swc, String swcVer, String compilerVer)
		{
			this.swc = swc;
			this.swcVer = swcVer;
			this.compilerVer = compilerVer;
		}
		public String swc;
		public String swcVer;
		public String compilerVer;
	}
}
