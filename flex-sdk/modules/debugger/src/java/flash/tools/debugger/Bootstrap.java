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

import flash.localization.LocalizationManager;
import flash.tools.debugger.concrete.PlayerSessionManager;

/**
 * Entry point for access to the general API.  A debugger uses this
 * class to gain access to a SessionManager from which debugging
 * sessions may be controlled or initiated.
 */
public class Bootstrap
{
	static SessionManager m_mgr = null;
	private static LocalizationManager m_localizationManager;

	static
	{
        // set up for localizing messages
        m_localizationManager = new LocalizationManager();
        m_localizationManager.addLocalizer( new DebuggerLocalizer("flash.tools.debugger.djapi.") ); //$NON-NLS-1$
	}

	private Bootstrap () {}

	public static SessionManager sessionManager()
	{
		if (m_mgr == null)
			m_mgr = new PlayerSessionManager();
		return m_mgr;
	}

	static LocalizationManager getLocalizationManager()
	{
		return m_localizationManager;
	}
}
