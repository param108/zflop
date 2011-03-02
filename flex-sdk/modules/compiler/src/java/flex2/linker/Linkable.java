////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.linker;

import java.util.Iterator;

/**
 * @author Roger Gonzalez
 */
public interface Linkable
{
    public String getName();
    public Iterator getDefinitions();
    public Iterator getPrerequisites();
    public Iterator getDependencies();
    public long getLastModified();
    public long getSize();
    public boolean isNative();
}
