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

import flex2.linker.SimpleMovie;
import flex2.linker.LinkerException;
import flex2.linker.CULinkable;
import flex2.linker.DependencyWalker;
import flex2.linker.Configuration;
import flex2.linker.FlexMovie;
import flex2.compiler.CompilationUnit;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.Visitor;

import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import flash.swf.Frame;
import flash.swf.tags.FrameLabel;

/**
 * @author Roger Gonzalez
 *
 * This is similar to FlexMovie in that it cares about externs and unresolved symbols, but
 * unlike FlexMovie it tries to export all CompilationUnits, not just ones that are referenced.
 */
public class SwcMovie extends SimpleMovie
{
    private Set externs;
	private Set includes;
    private Set unresolved;
	private SortedSet resourceBundles;

    public SwcMovie( Configuration configuration )
    {
        super( configuration );

        // C: SwcMovie should keep its own copy of externs, includes, unresolved and resourceBundles
        //    so that incremental compilation can do the single-compile-multiple-link scenario.
        externs = new HashSet(configuration.getExterns());
	    includes = new HashSet(configuration.getIncludes());
        unresolved = new HashSet(configuration.getUnresolved());
        generateLinkReport = configuration.generateLinkReport();
        generateRBList = configuration.generateRBList();

	    resourceBundles = new TreeSet(configuration.getResourceBundles());
    }
    public void generate( List units ) throws LinkerException // List<CompilationUnit>
    {
        List linkables = new LinkedList();

        for (Iterator it = units.iterator(); it.hasNext();)
        {
            linkables.add( new CULinkable( (CompilationUnit) it.next() ) );
        }

        frames = new ArrayList();

        try
        {
            DependencyWalker.LinkState state = new DependencyWalker.LinkState( linkables, externs, includes, unresolved );
            final Frame frame = new Frame();

            DependencyWalker.traverse( null, state, true, true,
                                       new Visitor()
                                       {
                                           public void visit( Object o )
                                           {
                                               CULinkable l = (CULinkable) o;
                                               exportUnitOnFrame( l.getUnit(), frame, true );
                                           }
                                       } );

            frames.add( frame );
            if (Swc.FNORD)
            {
                // add some magic simpleminded tamperproofing to the SWC.  Alpha code won't add this, release will refuse to run without it.
                frame.label = new FrameLabel();
                frame.label.label = Integer.toString( SimpleMovie.getCodeHash( frame ) );
            }

            if (generateLinkReport)
            {
            	linkReport = DependencyWalker.dump( state );
            }
            if (generateRBList)
            {
            	rbList = FlexMovie.dumpRBList(resourceBundles);
            }

            if (unresolved.size() != 0)
            {
                for (Iterator it = unresolved.iterator(); it.hasNext();)
                {
                    String u = (String) it.next();
                    if (!externs.contains( u ))
                    {
                        ThreadLocalToolkit.log(  new LinkerException.UndefinedSymbolException( u ) );
                    }
                }
            }
            topLevelClass = formatSymbolClassName( rootClassName );
            
        }
        catch (LinkerException e)
        {
            ThreadLocalToolkit.log( e );
        }
    }
}
