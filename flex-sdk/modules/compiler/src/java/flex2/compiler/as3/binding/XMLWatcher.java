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

package flex2.compiler.as3.binding;

/**
 * @author Roger Gonzalez
 */
public class XMLWatcher extends PropertyWatcher
{
    public XMLWatcher( int id, String prop )
    {
        super( id, prop );
    }

    public boolean shouldWriteSelf()
    {
        return !(getParent() instanceof XMLWatcher);
    }

}
