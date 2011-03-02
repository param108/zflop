////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex.tools.debugger.cli;

import java.util.ArrayList;
import java.util.Iterator;

import flash.tools.debugger.Location;

/**
 * This object is a container for source locations
 * that represent the same underlying file and line
 * number. 
 *
 * The reason we need this is because multiple 
 * swfs each contain their own unique version of
 * a source file and we'd like to be able to 
 * refer to any one location freely 
 * 
 * It is modelled after the Collection interface
 */
public class LocationCollection
{
	private ArrayList m_locations = new ArrayList();

	public boolean		add(Location l)			{ return m_locations.add(l); }
	public boolean		contains(Location l)	{ return m_locations.contains(l); }
	public boolean		remove(Location l)		{ return m_locations.remove(l); }
	public boolean		isEmpty()				{ return m_locations.isEmpty(); }
	public Iterator		iterator()				{ return m_locations.iterator(); }

    // Return the first Location object or null
	public Location     first()					{ return (Location)( (m_locations.size() > 0) ? m_locations.get(0) : null ); }

	/**
	 * Removes Locations from the Collection which contain
	 * SourceFiles with Ids in the range [startingId, endingId].
	 */
	public void removeFileIdRange(int startingId, int endingId)
	{
		Iterator i = iterator();
		while(i.hasNext())
		{
			Location l = (Location)i.next();
			int id = (l.getFile() == null) ? -1 : l.getFile().getId();
			if (id >= startingId && id <= endingId)
				i.remove();
		}
	}

	/**
	 * See if the collection contains a Location 
	 * which is identical to the given file id and 
	 * line number
	 */
	public boolean contains(int fileId, int line)
	{
		boolean found = false;
		Iterator i = iterator();
		while(i.hasNext() && !found)
		{
			Location l = (Location)i.next();
			int id = (l.getFile() == null) ? -1 : l.getFile().getId();
			if (id == fileId && l.getLine() == line)
				found = true;
		}
		return found;
	}
	
	/** for debugging */
	public String toString()
	{
		return m_locations.toString();
	}
}
