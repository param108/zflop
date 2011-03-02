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
 * The Location object identifies a specific line number with a SourceFile.
 * It is used for breakpoint manipulation and obtaining stack frame context.
 */
public interface Location
{
	/**
	 * Source file for this location 
	 */
	public SourceFile getFile();

	/**
	 * Line number within the source for this location 
	 */
    public int getLine();

}
