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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import flash.swf.tags.DoABC;

/**
 * Represents one Script in an ABC within a SWC
 *
 * @author Roger Gonzalez
 */
public class SwcScript
{
    private final SwcLibrary library;
    private final long modtime;
    private final Long signatureChecksum;
    private final Set defs;
    private final String name;
    private final SwcDependencySet deps;
    private Set symbolClasses;
	private Map misc;
    private DoABC doABC;

    public SwcScript( SwcLibrary library, String name, Set defs, SwcDependencySet deps, long modtime,
    				Long signatureChecksum )
    {
        this.library = library;
        this.name = name;
        this.defs = defs;
        this.deps = deps;
        this.modtime = modtime;
        this.signatureChecksum = signatureChecksum;
    }

    public void setDoABC( DoABC doABC )
    {
        this.doABC = doABC;
    }

    public DoABC getDoABC()
    {
        if (doABC == null)
        {
            library.parse();
            assert( doABC != null );
        }

        return doABC;
    }

    public SwcLibrary getLibrary()
    {
        library.parse();
        return library;
    }

	public String getSwcLocation()
	{
		return library.getSwcLocation();
	}

    public String getName()
    {
        return name;
    }

    public long getLastModified()
    {
        return modtime;
    }

    public Iterator getDefinitionIterator()
    {
        return defs.iterator();
    }

    public SwcDependencySet getDependencySet()
    {
        return deps;
    }

	public void setMiscData(Map misc)
	{
		this.misc = misc;
	}
	
	public Map getMiscData()
	{
		return misc;
	}
	
	public Set getSymbolClasses()
	{
		if (symbolClasses == null)
		{
			symbolClasses = new HashSet();
			for (Iterator i = getDefinitionIterator(); i.hasNext(); )
			{
				library.getSymbolClasses((String) i.next(), symbolClasses);
			}
		}
		
		return symbolClasses;
	}
	
	// C: Only the Flex Compiler API (flex-compiler-oem.jar) uses this method.
	//    Do not use it in the mxmlc/compc codepath.
	public flex2.tools.oem.Script toScript(boolean includeBytecodes)
	{
		return new ScriptImpl(this, includeBytecodes);
	}

	/**
	 * 
	 * @return signature checksum of the source script.
	 */
	public Long getSignatureChecksum()
	{
		return signatureChecksum;
	}
}



class ScriptImpl implements flex2.tools.oem.Script
{
	ScriptImpl(SwcScript swcScript, boolean includeBytecodes)
	{
		location = swcScript.getSwcLocation();
		lastModified = swcScript.getLastModified();
		
		Set names = new LinkedHashSet();
		
		for (Iterator i = swcScript.getDefinitionIterator(); i.hasNext(); )
		{
			names.add(i.next());
		}
		
		names.toArray(definitions = new String[names.size()]);

        SwcDependencySet set = swcScript.getDependencySet();

		names = new TreeSet();		

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.INHERITANCE); i != null && i.hasNext();)
        {
        	names.add((String) i.next());
        }

		names.toArray(prerequisites = new String[names.size()]);

		names.clear();
		
        for (Iterator i = set.getDependencyIterator(SwcDependencySet.SIGNATURE); i != null && i.hasNext();)
        {
        	names.add((String) i.next());
        }

		names.toArray(signatures = new String[names.size()]);

		names.clear();

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.NAMESPACE); i != null && i.hasNext();)
        {
        	names.add((String) i.next());
        }

		names.toArray(namespaces = new String[names.size()]);

		names.clear();

        for (Iterator i = set.getDependencyIterator(SwcDependencySet.EXPRESSION); i != null && i.hasNext();)
        {
        	names.add((String) i.next());
        }

        for (Iterator i = swcScript.getSymbolClasses().iterator(); i.hasNext(); )
        {
        	names.add((String) i.next());
        }
        
		names.toArray(expressions = new String[names.size()]);
		
		if (includeBytecodes)
		{
			bytecodes = swcScript.getDoABC().abc;
		}
	}

	private String location;
	private long lastModified;
	private String[] definitions, prerequisites, signatures, namespaces, expressions;
	private byte[] bytecodes;
	
	public String[] getDefinitionNames()
	{
		return definitions;
	}

	public String[] getDependencies(Object type)
	{
		if (type == INHERITANCE)
		{
			return prerequisites;
		}
		else if (type == SIGNATURE)
		{
			return signatures;
		}
		else if (type == NAMESPACE)
		{
			return namespaces;
		}
		else if (type == EXPRESSION)
		{
			return expressions;
		}
		else
		{
			return null;
		}
	}

	public long getLastModified()
	{
		return lastModified;
	}

	public String getLocation()
	{
		return location;
	}

	public String[] getPrerequisites()
	{
		return prerequisites;
	}
	
	public byte[] getBytecodes()
	{
		return bytecodes;
	}
}