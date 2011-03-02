////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

/**
 * Implementation of line number map which supports both 'permanent' and 'temporary' line mappings.
 * Temporary mappings can be flushed.
 */
public class DualModeLineNumberMap extends LineNumberMap
{
    private LineNumberMap temp;

    public DualModeLineNumberMap(String name, String newName)
    {
        super(name, newName);
        temp = new LineNumberMap(null, null);
    }

    //  DualMode impl

    /**
     * add a line number mapping, either temporarily or permanently (note that LineNumberMapping impl defaults to perm)
     */
    public void put(int oldStart, int newStart, int extent, boolean temporary)
    {
        (temporary ? temp : this).put(oldStart, newStart, extent);
    }

    /**
     * flush temporary line mappings
     */
    public void flushTemp()
    {
        temp = new LineNumberMap(null, null);
    }

    /**
     *  look in temp map first 
     */
    public int get(int newLine)
    {
        int tempLine = temp.get(newLine);
        return tempLine > 0 ? tempLine : super.get(newLine);
    }
}
