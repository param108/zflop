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

package flash.tools.debugger;

import java.io.InputStream;

/**
 * A callback interface which should be implemented by the client debugger
 * (such as fdb), to locate source files.
 * 
 * This is only necessary if the client debugger wants the DJAPI to "own"
 * the source code.  Zorn, for example, will probably *not* want to
 * implement this interface, because Eclipse itself will load the source
 * files from disk.
 */
public interface SourceLocator
{
	/**
	 * Callback from DJAPI to the debugger, to find a source file.
	 * Returns null if it can't find the file.
	 */
    public InputStream locateSource(String path, String pkg, String name);

	/**
	 * Returns a number which indicates how many times this SourceLocator's
	 * search algorithm has been changed since it was created.  For example,
	 * if a SourceLocator allows the user to change the list of directories
	 * that are searched, then each time the user changes that list of
	 * directories, the return value from getChangeCount() should change.
	 * 
	 * The DJAPI uses this in order to figure out if it should try again
	 * to look for a source file that it had previously been unable to
	 * find.
	 */
	public int getChangeCount();
}
