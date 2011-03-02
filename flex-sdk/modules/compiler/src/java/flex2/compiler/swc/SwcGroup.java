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

import flash.swf.tags.DoABC;
import flash.util.Trace;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;

import java.util.*;

/**
 * SwcGroup returns information about a set of SWCs returned from SwcCache. This
 * grouping is used instead of a List of Swcs because it allows us to precompute
 * certain information about the view, like name mappings.
 * 
 * @author Brian Deitte
 */
public class SwcGroup
{
	// map of Swcs in this group
	private Map swcs;

	// used to determine a component's name
	private NameMappings nameMappings = new NameMappings();

	// list of files in file section of catalogs
	private Map files = new HashMap();

	private Set qnames;

	private Map def2script;

	// use SwcCache.getSwcGroup() to get a SwcGroup
	SwcGroup(Map swcs)
	{
		this.swcs = swcs;
		updateNameMappings();
		updateFiles();
		updateMaps();
	}

	public int getNumberLoaded()
	{
		return swcs.size();
	}

	/**
	 * Returns a NameMapping class which can be used to determine a component's
	 * name.
	 */
	public NameMappings getNameMappings()
	{
		return nameMappings;
	}

	public Map getScriptMap()
	{
		return def2script;
	}

	public Set getQNames()
	{
		return qnames;
	}

	public Map getFiles()
	{
		return files;
	}

	/**
	 * Returns a file in a specific SWC. This should only be used for files
	 * already resolved by getFiles().
	 */
	public VirtualFile getFile(String name)
	{
		VirtualFile swcFile = null;
		String location = SwcFile.getSwcLocation(name);
		String fileName = SwcFile.getFilePath(name);
		if (location != null && fileName != null)
		{
			Swc swc = (Swc) swcs.get(location);
			if (swc != null)
			{
				swcFile = swc.getFile(fileName);
			}
		}
		return swcFile;
	}

	/**
	 * Get an individual swc from this group.
	 * 
	 * @param name -
	 *            name of the swc's virtual filename, may not be null.
	 * @return Swc - the swc in the group or null if the swc is not found.
	 * @throws NullPointerException -
	 *             if name is null
	 */
	public Swc getSwc(String name)
	{
		if (name == null)
		{
			throw new NullPointerException("getSwc: name may not be null");
		}

		return (Swc) swcs.get(name);
	}

	public Map getSwcs()
	{
		return swcs;
	}

	public List getSwcTimes()
	{
		List lastModified = new ArrayList();

		for (Iterator iterator = swcs.values().iterator(); iterator.hasNext();)
		{
			Swc swc = (Swc) iterator.next();
			lastModified.add(new Long(swc.getLastModified()));
		}

		return lastModified;
	}

	public void close()
	{
		for (Iterator iterator = swcs.values().iterator(); iterator.hasNext();)
		{
			Swc swc = (Swc) iterator.next();
			swc.close();
		}
	}

	private void updateNameMappings()
	{
		for (Iterator iter = swcs.values().iterator(); iter.hasNext();)
		{
			Swc swc = (Swc) iter.next();
			Iterator iter2 = swc.getComponentIterator();
			while (iter2.hasNext())
			{
				Component component = (Component) iter2.next();
				String namespaceURI = component.getUri();
				String name = component.getName();

				if (namespaceURI == null) continue;
				
				if ("".equals(namespaceURI))
				{
					if (name != null)
					{
						SwcException e = new SwcException.EmptyNamespace(name);
						ThreadLocalToolkit.log(e);
						throw e;
					}
					continue;
				}
				String className = component.getClassName();
				if (name == null)
				{
					name = NameFormatter.retrieveClassName(className);
				}

				boolean success = nameMappings.addClass(namespaceURI, name,
						className);
				if (!success)
				{
					SwcException e = new SwcException.ComponentDefinedTwice(
							name, className, nameMappings.lookupClassName(
									namespaceURI, name));
					ThreadLocalToolkit.log(e);
					throw e;
				}
			}
		}
	}

	private void updateFiles()
	{
		for (Iterator iterator = swcs.values().iterator(); iterator.hasNext();)
		{
			Swc swc = (Swc) iterator.next();
			for (Iterator it = swc.getCatalogFiles().values().iterator(); it.hasNext();)
			{
				VirtualFile file = (VirtualFile) it.next();
				String name = file.getName();
				String swcName = SwcFile.getFilePath(name);
				if (swcName != null)
				{
					name = swcName;
				}
				VirtualFile curFile = (VirtualFile) files.get(name);
				if (curFile == null || file.getLastModified() > curFile.getLastModified())
				{
					files.put(name, file);
				}
			}
		}
	}

	private void updateMaps()
	{
		// Given a set of SWCs, we need to build a map from each top-level
		// definition back to scripts.

		ArrayList scriptList = new ArrayList();

		for (Iterator swcit = swcs.values().iterator(); swcit.hasNext();)
		{
			Swc swc = (Swc) swcit.next();

			for (Iterator libit = swc.getLibraryIterator(); libit.hasNext();)
			{
				SwcLibrary lib = (SwcLibrary) libit.next();

				for (Iterator scriptit = lib.getScriptIterator(); scriptit
						.hasNext();)
				{
					SwcScript script = (SwcScript) scriptit.next();
					scriptList.add(script);
				}
			}
		}

		Object[] scriptArray = scriptList.toArray();

		Arrays.sort(scriptArray, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				long o1mod = ((SwcScript) o1).getLastModified();
				long o2mod = ((SwcScript) o2).getLastModified();

				if (o1mod == o2mod)
				{
					return 0;
				} else if (o1mod < o2mod)
				{
					return 1;
				} else
				{
					return -1;
				}
			}

		});

		def2script = new HashMap();
		qnames = new HashSet();
		for (int i = 0; i < scriptArray.length; i++)
		{
			SwcScript s = (SwcScript) scriptArray[i];
			String name = s.getName();
			DoABC doABC = null;
			try
			{
				doABC = s.getDoABC();
			} catch (Exception e)
			{
				if (Trace.error)
				{
					e.printStackTrace();
				}
				SwcException.SwcNotLoaded ex = new SwcException.SwcNotLoaded(s
						.getSwcLocation(), e);
				ThreadLocalToolkit.log(ex);
				throw ex;
			}
			assert (name.equals(doABC.name));

			HashMap staging = new HashMap();
			for (Iterator defit = s.getDefinitionIterator(); defit.hasNext();)
			{
				String def = (String) defit.next();
				staging.put(def, s);

				if (def2script.containsKey(def))
				{
					// already have a newer definition, this script is obsolete.
					staging = null;
					break;
				}
			}
			if (staging != null)
			{
				for (Iterator iterator = staging.entrySet().iterator(); iterator
						.hasNext();)
				{
					Map.Entry entry = (Map.Entry) iterator.next();
					String def = (String) entry.getKey();

					qnames.add(new QName(def));
					def2script.put(def, entry.getValue());

					if (Trace.swc)
					{
						Trace.trace("Using " + def + " from "
								+ s.getLibrary().getSwcLocation() + "("
								+ s.getName() + ")");
					}
				}
			}
		}
	}
}
