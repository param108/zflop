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

package flex2.compiler.as3.binding;

public class ChangeEvent
{
    private String name;
    private boolean committing;

    public ChangeEvent(String name, boolean committing)
    {
        this.name = name;
        this.committing = committing;
    }

    public boolean equals(Object object)
    {
        boolean result = false;

        if (object instanceof ChangeEvent)
        {
            ChangeEvent changeEvent = (ChangeEvent) object;

            if (name.equals(changeEvent.getName()) && (committing == changeEvent.getCommitting()))
            {
                result = true;
            }
        }

        return result;
    }

    public boolean getCommitting()
    {
        return committing;
    }

    public String getName()
    {
        return name;
    }

    public int hashCode()
    {
        return name.hashCode();
    }
}
