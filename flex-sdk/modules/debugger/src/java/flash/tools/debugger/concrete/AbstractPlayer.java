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

package flash.tools.debugger.concrete;

import java.io.File;

import flash.tools.debugger.Browser;
import flash.tools.debugger.Player;

/**
 * @author mmorearty
 */
public abstract class AbstractPlayer implements Player
{
	private Browser m_browser;
	private File m_flashPlayer;

	public AbstractPlayer(File webBrowser, File flashPlayer)
	{
		if (webBrowser != null)
			m_browser = new DBrowser(webBrowser);
		m_flashPlayer = flashPlayer;
	}

	/*
	 * @see flash.tools.debugger.Player#getPath()
	 */
	public File getPath()
	{
		return m_flashPlayer;
	}

	/*
	 * @see flash.tools.debugger.Player#getBrowser()
	 */
	public Browser getBrowser()
	{
		return m_browser; // this is null if we're using the standalone player
	}
}
