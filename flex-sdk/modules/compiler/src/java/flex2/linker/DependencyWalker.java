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

import flex2.compiler.util.Visitor;

import java.util.*;

/**
 * @author Roger Gonzalez
 *
 * Walk the dependency graph implied by a collection of linkables, and visit
 * each linkable; prerequisites form a DAG and are visited in DFS order.
 * (Non-prerequisite connected linkables are visited in an arbitrary order.)
 *
 * Topological sort of DAG G is equivilent to the DFS of the graph G'
 * where for every edge (u,v) in G there is an edge (v,u) in the transposed
 * graph G'.
 *
 * This is handy since dependencies in Flex are a transposed DAG
 * (edges point to predecessors, not successors).
 *
 */
public class DependencyWalker
{
    public static class LinkState
    {
        public LinkState( Collection linkables, Set extdefs, Set includes, Set unresolved )
                throws LinkerException
        {
            this.extdefs = extdefs;
	        this.includes = includes;
            this.unresolved = unresolved;

            // Build the defname -> linkable map and check for non-unique linkables

            for (Iterator li = linkables.iterator(); li.hasNext();)
            {
                Linkable l = (Linkable) li.next();

                if (lmap.containsKey( l.getName() ))
                {
                    throw new LinkerException.DuplicateSymbolException( l.getName() );
                }
                LinkableContext lc = new LinkableContext( l );
                lmap.put( l.getName(), lc);

                String external = null;
                for (Iterator di = l.getDefinitions(); di.hasNext();)
                {
                    String def = (String) di.next();
					LinkableContext c = (LinkableContext) defs.get( def );
                    if (c != null)
                    {
                        throw new LinkerException.MultipleDefinitionsException( def, l.getName(), c.l.getName() ); 
                    }
                    defs.put( def, lc );

                    if (extdefs.contains( def ))
                    {
                        external = def;
                    }
                    else if (external != null)
                    {
                         throw new LinkerException.PartialExternsException( lc.l.getName(), def, external );
                    }
                }
            }
        }
        public Set getUnresolved()
        {
            return unresolved;
        }
        public Set getExternal()
        {
            return extdefs;
        }
	    public Set getIncludes()
	    {
	        return includes;
	    }
        public Set getDefNames()
        {
            return defs.keySet();
        }
        public Collection getLinkables()
        {
            return lmap.values();
        }
        public Collection getVisitedLinkables()
        {
            return vmap.values();
        }
        Map vmap = new HashMap();
        Map lmap = new HashMap();
        Map defs = new HashMap();
        Set extdefs;
	    Set includes;
        Set unresolved;
    }

    /**
     * @param defs     the base definition set to start traversal, if null, link all.
     * @param state      a (mostly opaque) state object that can be used for multiple traversals
     * @param v             the visitor to invoke for each linkable
     * @throws LinkerException
     */
    public static void traverse( List defs, LinkState state, boolean allowExternal, boolean exportIncludes, Visitor v )
            throws LinkerException
    {
        if (defs == null)
        {
            defs = new LinkedList();
            for (Iterator it = state.getDefNames().iterator(); it.hasNext();)
            {
                String def = (String) it.next();
                if (!state.getExternal().contains( def ))
                {
                    defs.add( def );
                }
            }
        }

	    if (exportIncludes)
	    {
		    for (Iterator iterator = state.getIncludes().iterator(); iterator.hasNext();)
		    {
			    String def = (String)iterator.next();
			    defs.add( def );
		    }
	    }

        Stack stack = new Stack();           // holds contexts
        LinkedList queue = new LinkedList(); // holds contexts

        for (Iterator it = defs.iterator(); it.hasNext();)
        {
            String defname = (String) it.next();
            LinkableContext start = resolve( defname, state, allowExternal, exportIncludes );
            if (start == null)
                continue;
            queue.add( start );
        }

        while (!queue.isEmpty())
        {
            LinkableContext qc = (LinkableContext) queue.removeFirst();

            if (qc.visited)
                continue;

            qc.progress = true;
            stack.push( qc );

            while (!stack.isEmpty())
            {
                LinkableContext c = (LinkableContext) stack.peek();

                if (c.visited)
                {
                    stack.pop();
                    continue;
                }

                if (c.pi.hasNext())
                {
                    LinkableContext prereq = resolve( (String) c.pi.next(), state, allowExternal, exportIncludes );
                    if (prereq != null)
                    {
                        if (prereq.progress)
                        {
                            throw new LinkerException.CircularReferenceException( c.l.getName() );
                        }
                        if (!prereq.visited)
                        {
                            prereq.progress = true;
                            stack.push( prereq );
                        }
                    }
                    continue;
                }

//                if (c.visited)
//                {
//                    throw new DependencyException( DependencyException.CIRCULAR,
//                                                   c.l.getName(),
//                                                   "prerequisites of " + c.l.getName() + " contain a circular reference" );
//                }


                v.visit( c.l );
                c.visited = true;
                c.progress = false;
                state.vmap.put( c.l.getName(), c.l );
                stack.pop();

                while (c.di.hasNext())
                {
                    LinkableContext dc = resolve( (String) c.di.next(), state, allowExternal, exportIncludes );

                    if ((dc == null) || dc.visited)
                        continue;

                    queue.add( dc );
                }
            }
        }
    }

    static LinkableContext resolve( String name, LinkState state, boolean allowExternal, boolean exportIncludes ) throws LinkerException
    {
        if (allowExternal && (state.extdefs != null) && state.extdefs.contains( name ))
        {
            state.unresolved.add( name );
            return null;
        }

	    if (! exportIncludes && (state.includes != null) && state.includes.contains( name ))
	    {
		    state.includes.remove(name);
	    }

        LinkableContext lc = (LinkableContext) state.defs.get( name );

        if (lc == null)
        {
            if (state.unresolved == null)
                throw new LinkerException.UndefinedSymbolException( name );
            else
                state.unresolved.add( name );
        }
        else
        {
            if (lc.l.isNative())
            {
                state.unresolved.add( name );   // natives are always external
                return null;
            }
            if (!allowExternal && state.extdefs.contains( name ))
            {
                state.extdefs.remove( name );   // not external anymore, we had to resolve it.
            }
            lc.activate();
        }
        return lc;
    }

    public static String dump(  LinkState state )
    {
        StringBuffer buf = new StringBuffer( 2048 );
        buf.append( "<report>\n" );
        buf.append( "  <scripts>\n" );
        for (Iterator scripts = state.getVisitedLinkables().iterator(); scripts.hasNext();)
        {
            CULinkable l = (CULinkable) scripts.next();

            buf.append( "    <script name=\"")
               .append(l.getName())
               .append("\" mod=\"")
               .append(l.getLastModified())
               .append("\" size=\"")
               .append(l.getSize())
               // optimizedsize is often considerably smaller than size
               .append("\" optimizedsize=\"")
               .append(macromedia.abc.Optimizer.optimize(l.getUnit().bytes).size())
               .append("\">\n");
            
            for (Iterator defs = l.getDefinitions(); defs.hasNext();)
            {
                buf.append( "      <def id=\"" + (String) defs.next() + "\" />\n" );
            }
            for (Iterator pre = l.getPrerequisites(); pre.hasNext();)
            {
                buf.append( "      <pre id=\"" + (String) pre.next() + "\" />\n" );
            }
            for (Iterator dep = l.getDependencies(); dep.hasNext();)
            {
                buf.append( "      <dep id=\"" + (String) dep.next() + "\" />\n" );
            }
            buf.append( "    </script>\n" );
        }
        buf.append( "  </scripts>\n" );

        if ((state.getExternal() != null) || (state.getUnresolved() != null))
        {
            buf.append( "  <external-defs>\n");
            for (Iterator external = state.getExternal().iterator(); external.hasNext();)
            {
                String ext = (String) external.next();
                if (!state.getUnresolved().contains( ext ))    // only print exts we actually depended on
                    continue;

                buf.append( "    <ext id=\"" + ext + "\" />\n" );
            }
            for (Iterator unresolved = state.getUnresolved().iterator(); unresolved.hasNext();)
            {
                String unr = (String)unresolved.next();
                if (state.getExternal().contains( unr ))
                    continue;
                buf.append( "    <missing id=\"" + (String) unr + "\" />\n" );
            }
            buf.append( "  </external-defs>\n");
        }

        buf.append( "</report>\n" );

        return buf.toString();
    }

    static private class LinkableContext
    {
        public LinkableContext( Linkable l )
        {
            this.l = l;
        }
        public void activate()
        {
            if (!active)
            {
                active = true;
                pi = l.getPrerequisites();
                di = l.getDependencies();
            }
        }
        public String toString()
        {
            return l.getName() + " " + (visited? "v":"") + (progress? "p":"");
        }
        public final Linkable l;
        public Iterator pi;
        public Iterator di;
        public boolean active = false;
        public boolean visited = false;
        public boolean progress = false;
    }

}
