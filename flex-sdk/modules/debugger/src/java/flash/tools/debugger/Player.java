////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger;

import java.io.File;

/**
 * Describes a Flash player.
 * 
 * @author mmorearty
 */
public interface Player
{
	/**
	 * Indicates a standalone Flash player, e.g. FlashPlayer.exe.
	 * 
	 * @see #getType()
	 */
	public static final int STANDALONE = 1;

	/**
	 * Indicates a Netscape-plugin Flash player, e.g. NPSWF32.dll.  Used on Windows
	 * by all Netscape-based browsers (e.g. Firefox etc.).
	 * 
	 * @see #getType()
	 */
	public static final int NETSCAPE_PLUGIN = 2;

	/**
	 * Indicates an ActiveX-control Flash player, e.g. Flash.ocx.  Used on Windows
	 * by Internet Explorer.
	 * 
	 * @see #getType()
	 */
	public static final int ACTIVEX = 3;

	/**
	 * Indicates the Flash player inside AIR.
	 */
	public static final int AIR = 4;

	/**
	 * Returns what type of Player this is: <code>STANDALONE</code>, <code>NETSCAPE_PLUGIN</code>,
	 * <code>ACTIVEX</code>, or <code>AIR</code>.
	 */
	public int getType();

	/**
	 * Returns the path to the Flash player file -- e.g. the path to
	 * SAFlashPlayer.exe, NPSWF32.dll, or Flash.ocx.  (Filenames are
	 * obviously platform-specific.)
	 * 
	 * Note that the file is not guaranteed to exist.  You can use
	 * File.exists() to test that.
	 */
	public File getPath();

	/**
	 * Returns the web browser with which this player is associated,
	 * or <code>null</code> if this is the standalone player or if
	 * we're not sure which browser will be run.
	 */
	public Browser getBrowser();
}
