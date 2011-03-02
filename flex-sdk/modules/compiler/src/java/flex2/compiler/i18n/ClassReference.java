////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.i18n;

/**
 * Represents a ClassReference(...) resource value in a .properties file.
 * 
 * @author Gordon Smith
 */
public class ClassReference
{
    private String value;
    private int lineNumber;

    public ClassReference(String value, int lineNumber)
    {
        this.value = value;
        this.lineNumber = lineNumber;
    }

    public boolean equals(Object object)
    {
        boolean result = false;

        if (object instanceof ClassReference)
        {
            ClassReference classReferenceObject = (ClassReference)object;
           if (classReferenceObject.getValue().equals(value))
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
    
    public String toString()
    {
    	return value;
    }
}
