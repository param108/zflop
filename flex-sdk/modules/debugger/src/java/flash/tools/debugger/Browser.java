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

package flash.tools.debugger;

import java.io.File;

/**
 * Describes a web browser.
 * 
 * @author mmorearty
 */
public interface Browser
{
	/**
	 * Indicates an unknown browser type.
	 * 
	 * @see #getType()
	 */
	public static final int UNKNOWN = 0;

	/**
	 * Indicates Internet Explorer.
	 * 
	 * @see #getType()
	 */
	public static final int INTERNET_EXPLORER = 1;

	/**
	 * Indicates Netscape Navigator.
	 * 
	 * @see #getType()
	 */
	public static final int NETSCAPE_NAVIGATOR = 2;

	/**
	 * Indicates Opera.
	 * 
	 * @see #getType()
	 */
	public static final int OPERA = 3;

	/**
	 * Indicates the Mozilla browser, but <i>not</i> Firefox.
	 * 
	 * @see #getType()
	 */
	public static final int MOZILLA = 4;

	/**
	 * Indicates Firefox.
	 * 
	 * @see #getType()
	 */
	public static final int MOZILLA_FIREFOX = 5;

	/**
	 * Returns what type of Player this is, e.g. <code>INTERNET_EXPLORER</code>, etc.
	 */
	public int getType();

	/**
	 * Returns the path to the web browser executable -- e.g. the path to
	 * IExplore.exe, Firefox.exe, etc. (Filenames are obviously
	 * platform-specific.)
	 */
	public File getPath();
}
