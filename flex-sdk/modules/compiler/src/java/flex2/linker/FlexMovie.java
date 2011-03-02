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

package flex2.linker;

import flex2.compiler.CompilationUnit;
import flex2.compiler.Source;
import flex2.compiler.common.FramesConfiguration;
import flex2.compiler.swc.SwcLibrary;
import flex2.compiler.swc.SwcScript;
import flex2.compiler.util.*;
import flash.swf.Frame;
import flash.swf.tags.FrameLabel;

import java.util.*;

/**
 * @author Roger Gonzalez
 */
public class FlexMovie extends SimpleMovie
{
    private List frameInfoList;
    private List configFrameInfoList;
    private String mainDef;
    private Set externs;
	private Set includes;
    private Set unresolved;
	private SortedSet resourceBundles;
    
    /**
     * List of metadata names that are the unions of the library's metadata names
     * that have script linked into this movie (both internal and external references).
     */
	private Set metadata;
    
    public FlexMovie( Configuration configuration )
    {
        super( configuration );
        mainDef = configuration.getMainDefinition();        

        // C: FlexMovie should keep its own copy of externs, includes, unresolved and resourceBundles
        //    so that incremental compilation can do the single-compile-multiple-link scenario.
        externs = new HashSet(configuration.getExterns());
	    includes = new HashSet(configuration.getIncludes());
        unresolved = new HashSet(configuration.getUnresolved());
        generateLinkReport = configuration.generateLinkReport();
        generateRBList = configuration.generateRBList();
	    resourceBundles = new TreeSet(configuration.getResourceBundles());

        frameInfoList = new LinkedList();
        configFrameInfoList = new LinkedList();
        configFrameInfoList.addAll( configuration.getFrameList() );
        metadata = new HashSet();
    }

    private void prelink( List units ) throws LinkerException
    {
        // Starting at the main definition, build the list of frames and frame classes.
        // No new classes can be discovered here, we're just building the frame class list.

        Map def2unit = new HashMap();
        for (Iterator it = units.iterator(); it.hasNext(); )
        {
            CompilationUnit unit = (CompilationUnit) it.next();
            mapAll( def2unit, unit.topLevelDefinitions.getStringSet(), unit );
        }

        buildFrames( def2unit, mainDef, new HashSet() );

        frameInfoList.addAll( configFrameInfoList );

        if (frameInfoList.size() > 0)
        {
            topLevelClass = formatSymbolClassName( (String) ((FramesConfiguration.FrameInfo) frameInfoList.get( 0 )).frameClasses.get( 0 ) );
        }
    }

    private boolean hasFrameClass( String queryClassName )
    {
        // This is horribly inefficient, but the inner loop will only get called a few times
        // for a typical Flex movie.
        for (Iterator fit = frameInfoList.iterator(); fit.hasNext();)
        {
            FramesConfiguration.FrameInfo frameInfo = (FramesConfiguration.FrameInfo) fit.next();

            for (Iterator cit = frameInfo.frameClasses.iterator(); cit.hasNext(); )
            {
                String className = (String) cit.next();
                if (className.equals( queryClassName ))
                    return true;
            }
        }
        return false;
    }

    private void buildFrames( Map def2unit, String className, Set progress ) throws LinkerException
    {
        if (hasFrameClass( className ))
            return;

        if (progress.contains( className ))
            return;

        progress.add( className );

        CompilationUnit unit = (CompilationUnit) def2unit.get( className );

        if (unit == null)   // this should get picked up elsewhere
            throw new LinkerException.UndefinedSymbolException( className ); // fixme - add special frame class error?

        if (unit.loaderClass != null)
        {
            buildFrames( def2unit, unit.loaderClass, progress );
        }
        FramesConfiguration.FrameInfo info = new FramesConfiguration.FrameInfo();
        info.label = className.replaceAll( "[^A-Za-z0-9]", "_" );
        info.frameClasses.add( className );
	    info.frameClasses.addAll( unit.resourceBundles );
        info.frameClasses.addAll( unit.extraClasses );
        frameInfoList.add( info );
    }

    // shouldn't need swcContext at this point - units should have all referenced defs by now.
    public void generate(List units) throws LinkerException // List<CompilationUnit>
    {
        try
        {
            prelink( units );
        }
        catch (LinkerException e)
        {
            // You can't actually throw a LinkerException from generate,
            // because an assert fires downstream that expects errorcount > 0!
            // So, we have to warn here and then rethrow.

            ThreadLocalToolkit.log( e );
            throw e;
        }

        List linkables = new LinkedList();

		//	TODO remove - see note below
        String serverConfigDef = null;

        CULinkable mainLinkable = null;
        for (Iterator it = units.iterator(); it.hasNext();)
        {
            CompilationUnit unit = (CompilationUnit) it.next();

			//	NOTE Here we watch for specific generated loose code units we have carnal knowledge of, and add their
			//	definitions as deps to the main unit.
			// 	TODO Remove once serverconfigdata is handled within the standard bootstrap setup.
			//
            Source source = unit.getSource();
			String sourceName = source.getName();

            if (sourceName.equals("serverConfigData.as"))
            {
                serverConfigDef = unit.topLevelDefinitions.first().toString();
            }
            CULinkable linkable = new CULinkable( unit );
            if (unit.isRoot())
                mainLinkable = linkable;

            if (source.isInternal())
            {
                externs.addAll( unit.topLevelDefinitions.getStringSet() ); 
            }

            linkables.add( linkable );            
        }

        frames = new ArrayList();

        // FIXME - hook serverconfigdata to FlexInit mixin
		if (mainLinkable != null)
		{
            if (serverConfigDef != null)
                mainLinkable.addDep(serverConfigDef);
		}

        try
        {
            final Set librariesProcessed = new HashSet();
            int counter = 0;
            DependencyWalker.LinkState state = new DependencyWalker.LinkState( linkables, externs, includes, unresolved );
            for (Iterator it = frameInfoList.iterator(); it.hasNext();)
            {
                FramesConfiguration.FrameInfo frameInfo = (FramesConfiguration.FrameInfo) it.next();
                final Frame f = new Frame();
	            f.pos = ++counter;

                if (frameInfo.label != null)
                {
                    f.label = new FrameLabel();
                    f.label.label = frameInfo.label;
                }

                // note that we only allow externs on the last frame
                DependencyWalker.traverse( frameInfo.frameClasses, state, !it.hasNext(), !it.hasNext(),
                                           new Visitor()
                {
                    public void visit( Object o )
                    {
                        // fixme - keep an eye on those lazy abcs... do we have loose script?
						//	TODO yep! delete "false &&" once loose-script bootstrapping code has been eliminated - see note above
                        CULinkable l = (CULinkable) o;
                        // exportUnitOnFrame( l.getUnit(), f, false);// && !l.hasDefinition( frameClass ) );
	                    exportUnitOnFrame( l.getUnit(), f, lazyInit);
                        
                        // for any scripts that we include from libraries, add the libraries keep-as3-metadata
                        // to the list of metadata we will preserve in postlink.
                        Source source = l.getUnit().getSource();
                        if (source.isSwcScriptOwner() && !source.isInternal())
                        {
                            SwcScript script = (SwcScript)source.getOwner();
                            SwcLibrary library = script.getLibrary();
                 
                            // lots of scripts, but not many swcs, so avoid adding the same metadata
                            // over and over.
                            if (!librariesProcessed.contains(library))
                            {
                                librariesProcessed.add(library);
                                metadata.addAll(library.getMetadata());
                            }
                        }
                    }
                });
                frames.add( f );
            }

            if (generateLinkReport)
            {
            	linkReport = DependencyWalker.dump( state );
            }
            if (generateRBList)
            {
            	rbList = dumpRBList(resourceBundles);
            }
            
	        if (unresolved.size() != 0)
	        {
	            boolean fatal = false;
	            for (Iterator it = unresolved.iterator(); it.hasNext();)
	            {
	                String u = (String) it.next();
	                if (!externs.contains( u ))
	                {
	                    ThreadLocalToolkit.log( new LinkerException.UndefinedSymbolException( u ) );
	                    fatal = true;
	                }
	            }
	            if (fatal)
	            {
	                throw new LinkerException.LinkingFailed();
	            }
	        }

        }
        catch (LinkerException e)
        {
            ThreadLocalToolkit.log( e );
            throw e;
        }
    }

	public static String dumpRBList(Set bundles)
	{
		StringBuffer b = new StringBuffer();
	    b.append("bundles = ");
	    for (Iterator iterator = bundles.iterator(); iterator.hasNext();)
	    {
		    String str = (String)iterator.next();
		    b.append(str + " ");
	    }		
	    return b.toString();
	}
	
	private static void mapAll( Map map, Set keys, Object val )
	{
	    for (Iterator it = keys.iterator(); it.hasNext();)
	    {
	        String defname = (String) it.next();
//            defname = defname.replace( ':', '.' );      // FIXME - which is the canonical form?
	        map.put( defname, val );
	    }
	}

    /**
     * Get the set of metadata names that should be preserved when optimizing this movie. 
     *
     * @return Set of metadata names to keep in the movie.
     */
    public Set getMetadata()
    {
        return metadata;
    }

// todo - move/refactor, this is temporary 'til linkable/script stuff gets hoisted out of Compunit
}