////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.linker;

import flex2.compiler.CompilationUnit;
import flex2.compiler.util.MultiNameSet;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * @author Roger Gonzalez
 */
public class CULinkable implements Linkable
{
    public CULinkable( CompilationUnit unit )
    {
        this.unit = unit;
        defs.addAll( unit.topLevelDefinitions.getStringSet() );
        addDeps( prereqs, unit.inheritance );
        addDeps( deps, unit.expressions );
        addDeps( deps, unit.namespaces );
        addDeps( deps, unit.types );
        deps.addAll( unit.extraClasses );
	    deps.addAll( unit.resourceBundles );	    
    }
    public String getName()
    {
        return unit.getSource().getName();
    }

    public CompilationUnit getUnit()
    {
        return unit;
    }

    public long getLastModified()
    {
        return unit.getSource().getLastModified();
    }

    public long getSize()
    {
        return unit.bytes.size();
    }

    public boolean hasDefinition( String defName )
    {
        return defs.contains( defName );
    }

    public Iterator getDefinitions()
    {
        return defs.iterator();
    }

    public Iterator getPrerequisites()
    {
        return prereqs.iterator();
    }

    public Iterator getDependencies()
    {
        return deps.iterator();
    }
    public String toString()
    {
        return unit.getSource().getName();
    }

    public void addDep( String val )
    {
        deps.add( val );
    }

    public boolean dependsOn( String s )
    {
        return deps.contains( s ) || prereqs.contains( s );
    }

    public boolean isNative()
    {
        return unit.getSource().isInternal();
    }

    // todo - nuke this
    private void addDeps( Set set, MultiNameSet mns )
    {
        for (Iterator it = mns.iterator(); it.hasNext();)
        {
            // FIXME - this multinameset sometimes holds qnames.
            Object o = it.next();

            if (o instanceof MultiName)
            {
                MultiName mname = (MultiName) o;
                // FIXME - deps should only have a single qname in their multiname

                assert mname.getNumQNames() == 1;

                set.add( mname.getQName( 0 ).toString() );
            }
            else
            {
                assert o instanceof QName;
                set.add( o.toString() );
            }
        }
    }
    private final Set defs = new HashSet();
    private final Set prereqs = new HashSet();
    private final Set deps = new HashSet();

    private final CompilationUnit unit;
}
