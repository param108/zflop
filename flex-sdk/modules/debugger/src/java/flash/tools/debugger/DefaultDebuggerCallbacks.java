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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import flash.util.Trace;

/**
 * @author mmorearty
 */
public class DefaultDebuggerCallbacks implements IDebuggerCallbacks
{
	private boolean m_computedExeLocations;
	private File m_httpExe;
	private File m_playerExe;

	private static final String UNIX_DEFAULT_BROWSER = "firefox"; //$NON-NLS-1$
	private static final String UNIX_FLASH_PLAYER = "flashplayer"; //$NON-NLS-1$

	private static final int WINDOWS = 0;
	private static final int MAC = 1;
	private static final int UNIX = 2;
	
	/**
	 * Returns WINDOWS, MAC, or UNIX
	 */
	private static int getOS() {
		String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		if (osName.startsWith("windows")) //$NON-NLS-1$
			return WINDOWS;
		else if (osName.startsWith("mac os x")) // as per http://developer.apple.com/technotes/tn2002/tn2110.html //$NON-NLS-1$
			return MAC;
		else
			return UNIX;
	}

	/*
	 * @see flash.tools.debugger.IDebuggerCallbacks#getHttpExe()
	 */
	public synchronized File getHttpExe()
	{
		if (!m_computedExeLocations)
			recomputeExeLocations();
		return m_httpExe;
	}

	/*
	 * @see flash.tools.debugger.IDebuggerCallbacks#getPlayerExe()
	 */
	public synchronized File getPlayerExe()
	{
		if (!m_computedExeLocations)
			recomputeExeLocations();
		return m_playerExe;
	}

	/*
	 * @see flash.tools.debugger.IDebuggerCallbacks#recomputeExeLocations()
	 */
	public synchronized void recomputeExeLocations()
	{
		int os = getOS();
		if (os == WINDOWS)
		{
			m_httpExe = determineExeForType("http"); //$NON-NLS-1$
			m_playerExe = determineExeForType("ShockwaveFlash.ShockwaveFlash"); //$NON-NLS-1$
		}
		else if (os == MAC)
		{
			m_httpExe = null;
			m_playerExe = null;
		}
		else // probably Unix
		{
			// "firefox" is default browser for unix
			m_httpExe = findUnixProgram(UNIX_DEFAULT_BROWSER);

			// "flashplayer" is standalone flash player on unix
			m_playerExe = findUnixProgram(UNIX_FLASH_PLAYER);
		}
		m_computedExeLocations = true;
	}

	public String getHttpExeName()
	{
		if (getOS() == UNIX)
			return UNIX_DEFAULT_BROWSER;
		else
			return Bootstrap.getLocalizationManager().getLocalizedTextString("webBrowserGenericName"); //$NON-NLS-1$
	}

	public String getPlayerExeName()
	{
		if (getOS() == UNIX)
			return UNIX_FLASH_PLAYER;
		else
			return Bootstrap.getLocalizationManager().getLocalizedTextString("flashPlayerGenericName"); //$NON-NLS-1$
	}

	/**
	 * Looks for a Unix program.  Checks the PATH, and if not found there,
	 * checks the directory specified by the "application.home" Java property.
	 * ("application.home" was set by the "fdb" shell script.)
	 * 
	 * @param program program to find, e.g. "firefox"
	 * @return path, or <code>null</code> if not found.
	 */
	private File findUnixProgram(String program)
	{
		String[] cmd = { "/bin/sh", "-c", "which " + program }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try
		{
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			if (line != null)
			{
				File f = new File(line);
				if (f.exists())
				{
					return f;
				}
			}

			// Check in the Flex SDK's "bin" directory.  The "application.home"
			// property is set by the "fdb" shell script.
			String flexHome = System.getProperty("application.home"); //$NON-NLS-1$
			if (flexHome != null)
			{
				File f = new File(flexHome, "bin/" + program); //$NON-NLS-1$
				if (f.exists())
				{
					return f;
				}
			}
		}
		catch (IOException e)
		{
			// ignore
		}
		return null;
	}

	/**
	 * Note, this function is Windows-specific.
	 */
	private File determineExeForType(String type)
	{
		String it = null;
		try
		{
			String[] cmd = new String[] { "cmd", "/c", "ftype", type }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Process p = Runtime.getRuntime().exec(cmd);
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			type += "="; //$NON-NLS-1$
			while( it == null && (line = lnr.readLine()) != null)
			{
				if (line.substring(0, type.length()).compareToIgnoreCase(type) == 0)
				{
					it = line;
					break;
				}
			}
			p.destroy();

			// if we have one extract cmd = " "
			if (it != null)
			{
				int equalSign = it.indexOf('=');
				if (equalSign != -1)
					it = it.substring(equalSign+1);

				it = extractExenameFromCommandString(it);
			}
		}
		catch (IOException e)
		{
			// means it didn't work
		}

		if (it != null)
			return new File(it);
		else
			return null;
	}

	/**
	 * Given a command string of the form
	 * 		"path_to_exe" args
	 * or
	 * 		path_to_exe args
	 * 
	 * return the path_to_exe.  Note that path_to_exe may contain spaces.
	 */
	protected String extractExenameFromCommandString(String cmd)
	{
		// now strip trailing junk if any
		if (cmd.startsWith("\"")) { //$NON-NLS-1$
			// ftype is enclosed in quotes
			int closingQuote =  cmd.indexOf('"', 1);
			if (closingQuote == -1)
				closingQuote = cmd.length();
			cmd = cmd.substring(1, closingQuote);
		} else {
			// Some ftypes don't use enclosing quotes.  This is tricky -- we have to
			// scan through the string, stopping at each space and checking whether
			// the filename up to that point refers to a valid filename.  For example,
			// if the input string is
			//
			//     C:\Program Files\Macromedia\Flash 9\Players\SAFlashPlayer.exe %1
			//
			// then we need to stop at each space and see if that is an EXE name:
			//
			//     C:\Program.exe
			//     C:\Program Files\Macromedia\Flash.exe
			//     C:\Program Files\Macromedia\Flash 9\Players\SAFlashPlayer.exe

			int endOfFilename = -1;
			for (;;) {
				int nextSpace = cmd.indexOf(' ', endOfFilename+1);
				if (nextSpace == -1) {
					endOfFilename = -1;
					break;
				}
				String filename = cmd.substring(0, nextSpace);
				if (!filename.toLowerCase().endsWith(".exe")) //$NON-NLS-1$
					filename += ".exe"; //$NON-NLS-1$
				if (new File(filename).exists()) {
					endOfFilename = nextSpace;
					break;
				}
				endOfFilename = nextSpace;
			}
			if (endOfFilename != -1 && endOfFilename < cmd.length())
				cmd = cmd.substring(0, endOfFilename);
		}
		return cmd;
	}

	/*
	 * @see flash.tools.debugger.IDebuggerCallbacks#launchDebugTarget(java.lang.String[])
	 */
	public Process launchDebugTarget(String[] cmd) throws IOException
	{
		return Runtime.getRuntime().exec(cmd);
	}

	/**
	 * This implementation of queryWindowsRegistry() does not make any native
	 * calls.  I had to do it this way because it is too hard, at this point,
	 * to add native code to the Flex code tree.
	 */
	public String queryWindowsRegistry(String key, String value) throws IOException
	{
		Process p = null;
		String result = null;

		List arguments = new ArrayList(6);
		arguments.add("reg.exe"); //$NON-NLS-1$
		arguments.add("query"); //$NON-NLS-1$
		arguments.add(key);
		if (value == null || value.length() == 0)
		{
			arguments.add("/ve"); //$NON-NLS-1$
		}
		else
		{
			arguments.add("/v"); //$NON-NLS-1$
			arguments.add(value);
		}

		// This line must not be in try/catch -- if it throws an exception,
		// we want that to propagate out to our caller.
		p = Runtime.getRuntime().exec((String[]) arguments.toArray(new String[arguments.size()]));

		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.equals(key))
				{
					line = reader.readLine();
					if (line != null)
					{
						int lastTab = line.lastIndexOf('\t');
						if (lastTab != -1)
							result = line.substring(lastTab+1);
					}
					break;
				}
			}
		}
		catch (IOException e)
		{
			if (Trace.error)
				e.printStackTrace();
		}
		finally
		{
			if (p != null)
			{
				p.destroy();
				p = null;
			}
		}

		return result;
	}
}
