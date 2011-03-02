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

package flex2.compiler.as3.binding;

public class RepeaterComponentWatcher extends PropertyWatcher
{
    private int repeaterLevel;

    public RepeaterComponentWatcher(int id, String property, int repeaterLevel)
    {
        super(id, property);
        this.repeaterLevel = repeaterLevel;
    }

    public int getRepeaterLevel()
    {
        return repeaterLevel;
    }
}
