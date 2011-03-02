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

import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.catalog.CatalogReader;
import flex2.compiler.util.NameFormatter;
import flex2.linker.SimpleMovie;
import flash.swf.MovieDecoder;
import flash.swf.Frame;
import flash.swf.Movie;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;

import flash.swf.TagDecoder;
import flash.swf.Tag;
import flash.swf.tags.DoABC;
import flash.swf.tags.DefineTag;

/**
 * @author Roger Gonzalez
 */
public class SwcLibrary
{
    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected final Swc swc;
    protected final String path;

    // changed next 4 from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected List scripts;
    protected Map name2script;
    protected Map def2symbol;
    protected boolean parsed;
    private Set externs;
    private String location = null;
    private Set metadata;
    protected Map digests = new HashMap();
    
    
    public SwcLibrary( Swc swc, String path )
    {
        this.swc = swc;
        this.path = path;
        this.scripts = new LinkedList();
        this.parsed = false;
        this.name2script = new HashMap();
        this.externs = new HashSet();
        this.metadata = new HashSet();
    }

    public Swc getSwc()
    {
        return swc;
    }

    public String getPath()
    {
        return path;
    }

    public String getSwcLocation()
    {
        if (location != null)
        {
            return location;
        }
        else
        {
            return swc.getLocation();
        }
    }

	public long getSwcCreationTime()
	{
		return swc.getLastModified();
	}

    public void clear()
    {
        scripts = null;
        def2symbol = null;
    }

    public Set getExterns()
    {
        return externs;
    }

    public SwcScript addScript( String name, Set defs, SwcDependencySet deps, long modtime,
    							Long signatureChecksum)
    {
        SwcScript script = new SwcScript( this, name, defs, deps, modtime, signatureChecksum );
        scripts.add( script );
        name2script.put( script.getName(), script );
        assert ((name != null) && (name.length() > 0));
        return script;
    }

    public Iterator getScriptIterator()
    {
        return scripts.iterator();
    }

    public DefineTag getSymbol( String name )
    {
        // stagSymbolClasses likes dots, no colons.
        name = name.replace( ':', '.' );
        return (DefineTag) def2symbol.get( name );
    }

    // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    protected SwcScript getScript( DoABC doABC )
    {
        assert (doABC.code == Tag.stagDoABC2);
        return (SwcScript) name2script.get( doABC.name );
    }

    /**
     * The catalog should have been previously loaded such that we have a set of SwcScript objects.
     * Parse the SWF and associate a DoABC with each, and keep track of the stagSymbolClass values.
     */
    protected void parse()
    {
        if (parsed)
            return;

        VirtualFile swcFile = swc.getArchive().getFile( path );

        if (swcFile == null)
        {
            throw new SwcException.CatalogNotFound();
        }

        Movie movie = new Movie();
        MovieDecoder movieDecoder = new MovieDecoder( movie );

        try
        {
            TagDecoder tagDecoder = new TagDecoder( swcFile.getInputStream() );
            tagDecoder.parse( movieDecoder );
        }
        catch (IOException e)
        {
        }
        int c1 = 0;
        def2symbol = new HashMap();
        for (Iterator frames = movie.frames.iterator(); frames.hasNext(); )
        {
            Frame frame = (Frame) frames.next();
            for (Iterator abcit = frame.doABCs.iterator(); abcit.hasNext(); )
            {
                DoABC doABC = (DoABC) abcit.next();
                SwcScript script = getScript( doABC );
                script.setDoABC( doABC );
            }

            for (Iterator it = frame.symbolClass.class2tag.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry e = (Map.Entry) it.next();
                String className = (String) e.getKey();
                DefineTag tag = (DefineTag) e.getValue();

                def2symbol.put( className, tag );
            }

            if (Swc.FNORD)  // release SWCs must have magic hash label per frame
            {
                if (frame.label != null)
                {
                    try { c1 = Integer.parseInt( frame.label.label ); } catch (Exception e) {};
                }
                int c2 = SimpleMovie.getCodeHash( frame );

                if (c1 != c2)
                {
                    location = " " + swc.getLocation();
                }
            }
        }
        parsed = true;
    }
    
    void getSymbolClasses(String def, Set symbolClasses)
    {
    	// getSymbol() converts def from a.b:C to a.b.C
    	DefineTag t = getSymbol(def);    	
    	if (t != null)
    	{
    		for (Iterator i = t.getReferences(); i != null && i.hasNext(); )
    		{
    		    Tag r = (Tag)i.next();
    			if (r != null)
    			{
					for (Iterator j = def2symbol.keySet().iterator(); j.hasNext(); )
					{
						String className = (String) j.next();
						if (def2symbol.get(className) == r)
						{
							symbolClasses.add(NameFormatter.toColon(className));
						}
					}
    			}
    		}
    	}
    }
    
    /**
     * Get the set of metadata to keep when optimizing codes from this library.
     * 
     * @return Set of metadata names of type String.
     */
    public Set getMetadata()
    {
        return metadata;
    }
    
    /**
     * Set the metadata to the given Set.
     * 
     * @param metadata Collection of metadata names of type String.
     */
    public void setMetadata(Set metadata)
    {
        this.metadata = metadata;
    }
    
    /**
     * Add a collection of names to the list of metadata in this library.
     * 
     * @param metadata Collection of metadata names of type String.
     * 
     * @throws NullPointerException if metadata is null
     */
    public void addMetadata(Collection metadata)
    {
       this.metadata.addAll(metadata);
    }

    
    /**
     * Get the digest of a specified library.
     * 
     * @param libPath
     *          name of library path. If in doubt pass in LIBRARY_PATH.
     * @param hashType
     *          type of hash. Only valid choice is Digest.SHA_256.
     * @param isSigned
     *          if true return a signed digest, if false return an unsigned digest.
     * 
     * @return the digest of the specified library. May be null if not digest is found.
     */
    public Digest getDigest(String hashType, boolean isSigned)
    {
        if (hashType == null)
        {
            throw new NullPointerException("hashType may not be null");
        }
        
        return (Digest)digests.get(CatalogReader.createDigestHashValue(hashType, isSigned));
    }
    
    
    /**
     * Add a new digest to the swc or replace the existing digest if a
     * digest for libPath already exists.
     * 
     * @param libPath name of the library file, may not be null
     * @param digest digest of libPath
     * @throws NullPointerException if libPath or digest are null.
     */
    public void setDigest(Digest digest)
    {
        if (digest == null)
        {
            throw new NullPointerException("setDigest:  digest may not be null");  // $NON-NLS-1$
        }

        digests.put(CatalogReader.createDigestHashValue(digest.getType(), 
                                                        digest.isSigned()), 
                                                        digest);
    }
    
    /**
     * 
     * @return all the digests in this swc. May be empty, but not null.
     */
    public Map getDigests()
    {
        return digests;
    }
    
    
    /**
     * Set all the digest of the library. Typically this is called after reading from
     * all the digests from catalog.xml.
     *  
     * @param digests - Map of digests in this swc. 
     */
    public void setDigests(Map digests)
    {
        this.digests = digests;
    }
    
    
}
