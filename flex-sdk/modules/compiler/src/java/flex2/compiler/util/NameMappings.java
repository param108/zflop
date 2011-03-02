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

package flex2.compiler.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * Stores the mappings of name and uri to classname
 *
 * @author Brian Deitte
 * @author Clement Wong
 */
public class NameMappings
{
	public NameMappings()
	{
		namespaceMap = new HashMap();
        lookupOnly = new HashSet();
	}

    private Map namespaceMap;
    private Set lookupOnly;    

    public NameMappings copy()
    {
    	NameMappings m = new NameMappings();
    	for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext(); )
    	{
    		String uri = (String) i.next();
    		Map classMap = (Map) namespaceMap.get(uri);
    		m.namespaceMap.put(uri, new HashMap(classMap));
    	}
    	m.lookupOnly.addAll(lookupOnly);
    	return m;
    }
    
	public String lookupPackageName(String nsURI, String localPart)
	{
		String className = lookupClassName(nsURI, localPart);
		if (className == null)
		{
			return null;
		}
		int index = className.indexOf(":");
		return (index == -1) ? "" : className.substring(0, index);
	}

	public String lookupClassName(String nsURI, String localPart)
	{
        Map classMap = (Map) namespaceMap.get(nsURI);
        return classMap == null ? null : (String)classMap.get(localPart);
	}

	/**
	 * Look up namespace;classname against registered entries, then fault to package-style namespace handling
	 * NOTE: the contract here is that a null return value definitely indicates a missing definition, but a non-null
	 * return value *by no means* ensures that a definition will be available. E.g. an entry in a manifest doesn't mean
	 * that the backing code is correct, defines the specified class or even exists. Also, for package-style namespaces
	 * we simply concatenate the parameters together, since (e.g.) checking the presence or absence of a suitable entry
	 * on the classpath gives a similarly non-definitive answer.
	 */
	public String resolveClassName(String namespaceURI, String localPart)
	{
		String className = lookupClassName(namespaceURI, localPart);

		if (className == null)
		{
			// C: if namespaceURI is in the form of p1.p2...pn.*...
            // HIGHLY recommend handling this as old compiler did.  --rg
			if ("*".equals(namespaceURI))
			{
				className = localPart;
			}
			else if (namespaceURI.length() > 2 && namespaceURI.endsWith(".*"))
			{
				className = namespaceURI.substring(0, namespaceURI.length() - 2) + ':' + localPart;
				className = className.intern();
			}
		}

		return className;
	}

    public Map getNamespace(String nsURI)
    {
        return (Map) namespaceMap.get(nsURI);
    }

    public Set getNamespaces()
    {
        return namespaceMap.keySet();
    }

    public void addMappings( NameMappings other )
    {
        for (Iterator nit = other.namespaceMap.entrySet().iterator(); nit.hasNext();)
        {
            Map.Entry e = (Map.Entry) nit.next();
            String namespaceURI = (String) e.getKey();
            Map mappings = (Map) e.getValue();

            for (Iterator it = mappings.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry lc = (Map.Entry) it.next();
                String local = (String) lc.getKey();
                String className = (String) lc.getValue();

                addClass( namespaceURI, local, className );
            }
        }
    }

    public boolean addClass(String namespaceURI, String localPart, String className)
    {
        Map classMap = null;

        if (namespaceMap.containsKey(namespaceURI))
        {
            classMap = (Map) namespaceMap.get(namespaceURI);
        }
        else
        {
            classMap = new HashMap();
            namespaceMap.put(namespaceURI.intern(), classMap);
        }

        String current = (String)classMap.get(localPart);
        if (current == null)
        {
            classMap.put(localPart.intern(), className.intern());
        }
        else if (! current.equals(className))
        {
            return false;
        }
        return true;
    }

    public void addLookupOnly(String cls)
    {
        lookupOnly.add(cls);
    }

    public boolean isLookupOnly(String cls)
    {
        return lookupOnly.contains(cls);
    }
}
