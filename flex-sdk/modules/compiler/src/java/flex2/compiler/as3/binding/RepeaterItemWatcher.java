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

public class RepeaterItemWatcher extends PropertyWatcher
{
    public static final String REPEATER_ITEM = "repeaterItem";

    public RepeaterItemWatcher(int id)
    {
        super(id, REPEATER_ITEM);
    }
}
