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

package flex2.compiler.css;

public class Import
{
    private String value;
    private int lineNumber;

    public Import(String value, int lineNumber)
    {
        this.value = value;
        this.lineNumber = lineNumber;
    }

    public boolean equals(Object object)
    {
        boolean result = false;

        if (object instanceof Import)
        {
            Import importObject = (Import) object;

            if (importObject.getValue().equals(value))
            {
                result = true;
            }
        }

        return result;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getValue()
    {
        return value;
    }

    public int hashCode()
    {
        return value.hashCode();
    }
}
