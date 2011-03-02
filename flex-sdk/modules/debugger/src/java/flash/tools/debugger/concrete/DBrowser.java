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

package flash.tools.debugger.concrete;

import java.io.File;

import flash.tools.debugger.Browser;

/**
 * @author mmorearty
 */
public class DBrowser implements Browser
{
	private File m_path;
	private int m_type;

	public DBrowser(File exepath)
	{
		m_path = exepath;
		String exename = exepath.getName().toLowerCase();
		if (exename.equals("iexplore.exe")) //$NON-NLS-1$
			m_type = INTERNET_EXPLORER;
		else if (exename.equals("mozilla.exe")) //$NON-NLS-1$
			m_type = MOZILLA;
		else if (exename.equals("firefox.exe")) //$NON-NLS-1$
			m_type = MOZILLA_FIREFOX;
		else if (exename.equals("opera.exe")) //$NON-NLS-1$
			m_type = OPERA;
		else if (exename.equals("netscape.exe")) //$NON-NLS-1$
			m_type = NETSCAPE_NAVIGATOR;
		else
			m_type = UNKNOWN;
	}

	/*
	 * @see flash.tools.debugger.Browser#getType()
	 */
	public int getType()
	{
		return m_type;
	}

	/*
	 * @see flash.tools.debugger.Browser#getPath()
	 */
	public File getPath()
	{
		return m_path;
	}
}
