////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger;

/**
 * An identifier for the type of a Variable.
 */
public interface VariableType
{
	/* Types of a variable (one of) */
    public static final int NUMBER					=  0;
    public static final int BOOLEAN					=  1;
    public static final int STRING					=  2;
    public static final int OBJECT					=  3;
    public static final int FUNCTION				=  4;
    public static final int MOVIECLIP				=  5;
    public static final int NULL				    =  6;
    public static final int UNDEFINED				=  7;
	public static final int UNKNOWN					=  8;
}
