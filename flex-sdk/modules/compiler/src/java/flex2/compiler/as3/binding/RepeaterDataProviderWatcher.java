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

package flex2.compiler.as3.binding;

public class RepeaterDataProviderWatcher extends PropertyWatcher
{
    public RepeaterDataProviderWatcher(int id)
    {
        super(id, DATA_PROVIDER);
        addChangeEvent("collectionChange");
    }

    public PropertyWatcher getChild(String value)
    {
        if (value.equals(Watcher.CURRENT_ITEM) || value.equals(Watcher.CURRENT_INDEX))
        {
            value = REPEATER_ITEM;
        }
        return super.getChild(value);
    }
}
