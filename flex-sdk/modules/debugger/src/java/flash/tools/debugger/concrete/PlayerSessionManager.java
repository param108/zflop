////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.concrete;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import flash.localization.LocalizationManager;
import flash.tools.debugger.AIRLaunchInfo;
import flash.tools.debugger.CommandLineException;
import flash.tools.debugger.DebuggerLocalizer;
import flash.tools.debugger.DefaultDebuggerCallbacks;
import flash.tools.debugger.IDebuggerCallbacks;
import flash.tools.debugger.IProgress;
import flash.tools.debugger.Player;
import flash.tools.debugger.Session;
import flash.tools.debugger.SessionManager;
import flash.util.URLHelper;

public class PlayerSessionManager implements SessionManager
{
	ServerSocket m_serverSocket;
	HashMap		 m_prefs;
	boolean		 m_processDead;
	private IDebuggerCallbacks m_debuggerCallbacks;
	private static LocalizationManager m_localizationManager;
	private StringWriter m_processMessages;
	private int m_processExitValue;
	private String[] m_launchCommand;
	private static final String s_newline = System.getProperty("line.separator"); //$NON-NLS-1$

	static
	{
        // set up for localizing messages
        m_localizationManager = new LocalizationManager();
        m_localizationManager.addLocalizer( new DebuggerLocalizer("flash.tools.debugger.concrete.djapi.") ); //$NON-NLS-1$
	}

	public PlayerSessionManager()
	{
		m_debuggerCallbacks = new DefaultDebuggerCallbacks();

		m_serverSocket = null;
		m_prefs = new HashMap();

		// manager
		setPreference(PREF_ACCEPT_TIMEOUT, 120000); // 2 minutes
		setPreference(PREF_URI_MODIFICATION, 1);

		// session

		// response to requests
		setPreference(PREF_RESPONSE_TIMEOUT, 750); // 0.75s
		setPreference(PREF_CONTEXT_RESPONSE_TIMEOUT, 1000); // 1s
		setPreference(PREF_GETVAR_RESPONSE_TIMEOUT, 1500); // 1.5s
		setPreference(PREF_SETVAR_RESPONSE_TIMEOUT, 5000); // 5s
		setPreference(PREF_SWFSWD_LOAD_TIMEOUT, 5000);  // 5s

		// wait for a suspend to occur after a halt
		setPreference(PREF_SUSPEND_WAIT, 7000);

		// invoke getters by default
		setPreference(PREF_INVOKE_GETTERS, 1);

		// hierarchical variables view
		setPreference(PREF_HIERARCHICAL_VARIABLES, 0);
	}

	/**
	 * Set preference 
	 * If an invalid preference is passed, it will be silently ignored.
	 */
	public void			setPreference(String pref, int value)	{ m_prefs.put(pref, new Integer(value)); }
	public void			setPreference(String pref, String value){ m_prefs.put(pref, value);	}
	public Set			keySet()								{ return m_prefs.keySet(); }
	public Object		getPreferenceAsObject(String pref)		{ return m_prefs.get(pref); }

	/*
	 * @see flash.tools.debugger.SessionManager#getPreference(java.lang.String)
	 */
	public int getPreference(String pref)
	{
		int val = 0;
		Integer i = (Integer)m_prefs.get(pref);
		if (i == null)
			throw new NullPointerException();
		val = i.intValue();
		return val;
	}

	/*
	 * @see flash.tools.debugger.SessionManager#startListening()
	 */
	public void startListening() throws IOException 
	{
		if (m_serverSocket == null)
			m_serverSocket = new ServerSocket(DProtocol.DEBUG_PORT);
	}

	/*
	 * @see flash.tools.debugger.SessionManager#stopListening()
	 */
	public void stopListening() throws IOException
	{
		if (m_serverSocket != null)
		{
			m_serverSocket.close();
			m_serverSocket = null;
		}
	}

	/*
	 * @see flash.tools.debugger.SessionManager#isListening()
	 */
	public boolean isListening()
	{
		return (m_serverSocket == null) ? false : true;
	}

	private class LaunchInfo
	{
		private String m_uri;

		public LaunchInfo(String uri)
		{
			m_uri = uri;
		}
		
		public boolean isHttpOrAbout()
		{
			return m_uri.startsWith("http:") || m_uri.startsWith("https:") || m_uri.startsWith("about:"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		public boolean isWebPage()
		{
			return isHttpOrAbout() || m_uri.endsWith(".htm") || m_uri.endsWith(".html"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		public boolean isWebBrowserNativeLaunch()
		{
			return isWebPage() && (m_debuggerCallbacks.getHttpExe() != null);
		}
		
		public boolean isPlayerNativeLaunch()
		{
			return m_uri.length() > 0 && !isWebPage() && (m_debuggerCallbacks.getPlayerExe() != null);
		}
	}

	/*
	 * @see flash.tools.debugger.SessionManager#launch(java.lang.String, flash.tools.debugger.AIRLaunchInfo, boolean, flash.tools.debugger.IProgress)
	 */
	public Session launch(String uri, AIRLaunchInfo airLaunchInfo, boolean forDebugging, IProgress waitReporter) throws IOException
	{
		boolean modify = (getPreference(PREF_URI_MODIFICATION) != 0);
		LaunchInfo launchInfo = new LaunchInfo(uri);
		boolean nativeLaunch = launchInfo.isWebBrowserNativeLaunch() || launchInfo.isPlayerNativeLaunch();

		// one of these is assigned to launchAction
		final int NO_ACTION = 0;		// no special action
		final int SHOULD_LISTEN = 1;	// create a ProcessListener
		final int WAIT_FOR_LAUNCH = 2;	// block until process completes

		int launchAction; // either NO_ACTION, SHOULD_LISTEN, or WAIT_FOR_LAUNCH

		uri = uri.trim();

		String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		boolean isMacOSX = osName.startsWith("mac os x"); //$NON-NLS-1$
		boolean isWindows = osName.startsWith("windows"); //$NON-NLS-1$
		// if isMacOSX and isWindows are both false, then it's *NIX

		if (airLaunchInfo == null)
		{
			// first let's see if it's an HTTP URL or not
			if (launchInfo.isHttpOrAbout())
			{
				if (modify && forDebugging && !uri.startsWith("about:")) //$NON-NLS-1$
				{
					// escape spaces if we have any
					uri = URLHelper.escapeSpace(uri);

			        // be sure that ?debug=true is included in query string
					URLHelper urlHelper = new URLHelper(uri);
					Map params = urlHelper.getParameterMap();
					params.put("debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
					urlHelper.setParameterMap(params);

					uri = urlHelper.getURL();
	            }
	       	}
			else
			{
				// ok, its not an http: type request therefore we should be able to see
				// it on the file system, right?  If not then it's probably not valid
				File f = null;
				if (uri.startsWith("file:")) //$NON-NLS-1$
				{
					try
					{
						f = new File(new URI(uri));
					}
					catch (URISyntaxException e)
					{
						IOException ioe = new IOException(e.getMessage());
						ioe.initCause(e);
						throw ioe;
					}
				}
				else
				{
					f = new File(uri);
				}

				if (f != null && f.exists())
					uri = f.getCanonicalPath();
				else
					throw new FileNotFoundException(uri);
			}

			if (nativeLaunch) {
				// We used to have
				//
				//		launchAction = SHOULD_LISTEN;
				//
				// However, it turns out that when you launch Firefox, if there
				// is another instance of Firefox already running, then the
				// new instance just passes a message to the old one and then
				// immediately exits.  So, it doesn't work to abort when our
				// child process dies.
				launchAction = NO_ACTION;
			} else {
				launchAction = NO_ACTION;
			}

			/**
			 * Various ways to launch this stupid thing.  If we have the exe
			 * values for the player, then we can launch it directly, monitor
			 * it and kill it when we die; otherwise we launch it through
			 * a command shell (cmd.exe, open, or bash) and our Process object
			 * dies right away since it spawned another process to run the
			 * Player within.
			 */
			if (isMacOSX)
			{
				if (launchInfo.isWebBrowserNativeLaunch())
				{
					File httpExe = m_debuggerCallbacks.getHttpExe();
					m_launchCommand = new String[] { "/usr/bin/open", "-a", httpExe.toString(), uri }; //$NON-NLS-1$ //$NON-NLS-2$
				}
				else if (launchInfo.isPlayerNativeLaunch())
				{
					File playerExe = m_debuggerCallbacks.getPlayerExe();
					m_launchCommand = new String[] { "/usr/bin/open", "-a", playerExe.toString(), uri }; //$NON-NLS-1$ //$NON-NLS-2$
				}
				else
				{
					m_launchCommand = new String[] { "/usr/bin/open", uri }; //$NON-NLS-1$
				}
			}
			else
			{

				if (launchInfo.isWebBrowserNativeLaunch())
				{
					File httpExe = m_debuggerCallbacks.getHttpExe();
					m_launchCommand = new String[] { httpExe.toString(), uri };
				}
				else if (launchInfo.isPlayerNativeLaunch())
				{
					File playerExe = m_debuggerCallbacks.getPlayerExe();
					m_launchCommand = new String[] { playerExe.toString(), uri };
				}
				else
				{
					if (isWindows)
					{
						// We must quote all ampersands in the URL; if we don't, then
						// cmd.exe will interpret the ampersand as a command separator.
						uri = uri.replaceAll("&", "\"&\""); //$NON-NLS-1$ //$NON-NLS-2$

						m_launchCommand = new String[] { "cmd", "/c", "start", uri }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					else
					{
						String exeName;
						if (launchInfo.isWebPage())
							exeName = m_debuggerCallbacks.getHttpExeName();
						else
							exeName = m_debuggerCallbacks.getPlayerExeName();
						throw new FileNotFoundException(exeName);
					}
				}
			}
		}
		else // else, AIR
		{
			if (forDebugging)
				launchAction = SHOULD_LISTEN; // wait inside accept() until ADL exits
			else
				launchAction = NO_ACTION; // just launch it

			List cmdList = new LinkedList();

			cmdList.add(airLaunchInfo.airDebugLauncher.getPath());

			if (airLaunchInfo.airRuntimeDir != null && airLaunchInfo.airRuntimeDir.length() > 0)
			{
				cmdList.add("-runtime"); //$NON-NLS-1$
				cmdList.add(airLaunchInfo.airRuntimeDir.getPath());
			}

			if (airLaunchInfo.airSecurityPolicy != null && airLaunchInfo.airSecurityPolicy.length() > 0)
			{
				cmdList.add("-security-policy"); //$NON-NLS-1$
				cmdList.add(airLaunchInfo.airSecurityPolicy.getPath());
			}

			if (airLaunchInfo.airPublisherID != null && airLaunchInfo.airPublisherID.length() > 0)
			{
				cmdList.add("-pubid"); //$NON-NLS-1$
				cmdList.add(airLaunchInfo.airPublisherID);
			}

			// If it's a "file:" URL, then pass the actual filename; otherwise, use the URL
			// ok, its not an http: type request therefore we should be able to see
			// it on the file system, right?  If not then it's probably not valid
			File f = null;
			if (uri.startsWith("file:")) //$NON-NLS-1$
			{
				try
				{
					f = new File(new URI(uri));
					cmdList.add(f.getPath());
				}
				catch (URISyntaxException e)
				{
					IOException ioe = new IOException(e.getMessage());
					ioe.initCause(e);
					throw ioe;
				}
			}
			else
			{
				cmdList.add(uri);
			}

			if (airLaunchInfo.applicationContentRootDir != null)
			{
				cmdList.add(airLaunchInfo.applicationContentRootDir.getAbsolutePath());
			}

			if (airLaunchInfo.applicationArguments != null && airLaunchInfo.applicationArguments.length() > 0)
			{
				cmdList.add("--"); //$NON-NLS-1$
				cmdList.addAll(splitArgs(airLaunchInfo.applicationArguments));
			}

			m_launchCommand = (String[]) cmdList.toArray(new String[cmdList.size()]);
		}

		ProcessListener pl = null; 
		PlayerSession session = null;
		try
		{
			// create the process and attach a thread to watch it during our accept phase
			Process proc = m_debuggerCallbacks.launchDebugTarget(m_launchCommand);

			m_processMessages = new StringWriter();
			new StreamListener(new InputStreamReader(proc.getInputStream()), m_processMessages).start();
			new StreamListener(new InputStreamReader(proc.getErrorStream()), m_processMessages).start();
			try
			{
				OutputStream stm = proc.getOutputStream();
				if (stm != null)
					stm.close();
			}
			catch (IOException e)
			{
				/* not serious; ignore */
			}

			switch (launchAction)
			{
			case NO_ACTION:
				break;

			case SHOULD_LISTEN:
			{
				// allows us to hear when the process dies
				pl = new ProcessListener(this, proc);
				pl.start();
				break;
			}

			case WAIT_FOR_LAUNCH:
			{
				// block until the process completes
				boolean done = false;
				while (!done)
				{
					try {
						proc.waitFor();
						done = true;
					} catch (InterruptedException e) {
						/* do nothing */
					}
				}
				if (proc.exitValue() != 0)
				{
					throw new IOException(m_processMessages.toString());
				}
				break;
			}
			}

			if (forDebugging)
			{
				/* now wait for a connection */
				session = (PlayerSession)accept(waitReporter, airLaunchInfo != null);
				session.setProcess(proc);
				session.setLaunchUrl(uri);
				session.setAIRLaunchInfo(airLaunchInfo);
			}
		}
		finally
		{
			if (pl != null)
				pl.finish();
		}
		return session;
	}

	/**
	 * This is annoying: We must duplicate the operating system's behavior
	 * with regard to splitting arguments.
	 * 
	 * @param arguments A single string of arguments that are intended to
	 * be passed to an AIR application.  The tricky part is that some
	 * of the arguments may be quoted, and if they are, then the quoting
	 * will be in a way that is specific to the current platform.  For
	 * example, on Windows, strings are quoted with the double-quote character
	 * ("); on Mac and Unix, strings can be quoted with either double-quote
	 * or single-quote.
	 * @return The equivalent
	 */
	private List splitArgs(String arguments)
	{
		List retval = new ArrayList();

		arguments = arguments.trim();

		// Windows quotes only with double-quote; Mac and Unix also allow single-quote.
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win"); //$NON-NLS-1$ //$NON-NLS-2$
		boolean isMacOrUnix = !isWindows;

		int i=0;
		while (i<arguments.length()) {
			char ch = arguments.charAt(i);
			if (ch == ' ' || ch == '\t') {
				// keep looping
				i++;
			} else if (ch == '"' || (isMacOrUnix && ch == '\'')) {
				char quote = ch;
				int nextQuote = arguments.indexOf(quote, i+1);
				if (nextQuote == -1) {
					retval.add(arguments.substring(i+1));
					return retval;
				} else {
					retval.add(arguments.substring(i+1, nextQuote));
					i = nextQuote+1;
				}
			} else {
				int startPos = i;
				while (i<arguments.length()) {
					ch = arguments.charAt(i);
					if (ch == ' ' || ch == '\t') {
						break;
					}
					i++;
				}
				retval.add(arguments.substring(startPos, i));
			}
		}

		return retval;
	}

	/*
	 * @see flash.tools.debugger.SessionManager#playerForUri(java.lang.String)
	 */
	public Player playerForUri(String url)
	{
		// Find the Netscape plugin
		if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			File flashPlugin = new File("/Library/Internet Plug-Ins/Flash Player.plugin"); //$NON-NLS-1$
			return new NetscapePluginPlayer(m_debuggerCallbacks.getHttpExe(), flashPlugin);
		}
		else
		{
			LaunchInfo launchInfo = new LaunchInfo(url);
			if (launchInfo.isWebBrowserNativeLaunch())
			{
				File httpExe = m_debuggerCallbacks.getHttpExe();
				if (httpExe.getName().equalsIgnoreCase("iexplore.exe")) //$NON-NLS-1$
				{
					// IE on Windows: Find the ActiveX control
					String activeXFile = null;
					try
					{
						activeXFile = m_debuggerCallbacks.queryWindowsRegistry("HKEY_CLASSES_ROOT\\CLSID\\{D27CDB6E-AE6D-11cf-96B8-444553540000}\\InprocServer32", null); //$NON-NLS-1$
					}
					catch (IOException e)
					{
						// ignore
					}
					if (activeXFile == null)
						return null; // we couldn't find the player
					File file = new File(activeXFile);
					return new ActiveXPlayer(httpExe, file);
				}
				else
				{
					// Find the Netscape plugin
					File browserDir = httpExe.getParentFile();

					// Opera puts plugins under "program\plugins" rather than under "plugins"
					if (httpExe.getName().equalsIgnoreCase("opera.exe")) //$NON-NLS-1$
						browserDir = new File(browserDir, "program"); //$NON-NLS-1$

					File pluginsDir = new File(browserDir, "plugins"); //$NON-NLS-1$
					File flashPlugin = new File(pluginsDir, "NPSWF32.dll"); // WARNING, Windows-specific //$NON-NLS-1$

					// Bug 199175: The player is now installed via a registry key, not
					// in the "plugins" directory.
					//
					// Although Mozilla does not document this, the actual behavior of
					// the browser seems to be that it looks first in the "plugins" directory,
					// and then, if the file is not found there, it looks in the registry.
					// So, we mimic that behavior.
					if (!flashPlugin.exists())
					{
						File pathFromRegistry = getWindowsMozillaPlayerPathFromRegistry();

						if (pathFromRegistry != null)
							flashPlugin = pathFromRegistry;
					}

					return new NetscapePluginPlayer(httpExe, flashPlugin);
				}
			}
			else if (launchInfo.isPlayerNativeLaunch())
			{
				File playerExe = m_debuggerCallbacks.getPlayerExe();
				return new StandalonePlayer(playerExe);
			}
		}

		return null;
	}

	/**
	 * Look in the Windows registry for the Mozilla version of the Flash player.
	 */
	private File getWindowsMozillaPlayerPathFromRegistry()
	{
		final String KEY = "\\SOFTWARE\\MozillaPlugins\\@adobe.com/FlashPlayer"; //$NON-NLS-1$
		final String PATH = "Path"; //$NON-NLS-1$

		// According to
		//
		//    http://developer.mozilla.org/en/docs/Plugins:_The_first_install_problem
		//
		// the MozillaPlugins key can be written to either HKEY_CURRENT_USER or
		// HKEY_LOCAL_MACHINE.  Unfortunately, as of this writing, Firefox
		// (version 2.0.0.2) doesn't actually work that way -- it only checks
		// HKEY_LOCAL_MACHINE, but not HKEY_CURRENT_USER.
		//
		// But in hopeful anticipation of a fix for that, we are going to check both
		// locations.  On current builds, that won't do any harm, because the
		// current Flash Player installer only writes to HKEY_LOCAL_MACHINE.  In the
		// future, if Mozilla gets fixed and then the Flash player installer gets
		// updated, then our code will already work correctly.
		//
		// Another quirk: In my opinion, it would be better for Mozilla to look first
		// in HKEY_CURRENT_USER, and then in HKEY_LOCAL_MACHINE.  However, according to
		//
		//    http://developer.mozilla.org/en/docs/Installing_plugins_to_Gecko_embedding_browsers_on_Windows
		//
		// they don't agree with that -- they want HKEY_LOCAL_MACHINE first.

		String[] roots = { "HKEY_LOCAL_MACHINE", "HKEY_CURRENT_USER" }; //$NON-NLS-1$ //$NON-NLS-2$
		for (int i=0; i<roots.length; ++i)
		{
			try
			{
				String path = m_debuggerCallbacks.queryWindowsRegistry(roots[i] + KEY, PATH);
				if (path != null)
					return  new File(path);
			}
			catch (IOException e)
			{
				// ignore
			}
		}

		return null;
	}

	/*
	 * @see flash.tools.debugger.SessionManager#supportsLaunch()
	 */
	public boolean supportsLaunch()
	{
		return true;
	}

	/**
	 * Callback for ProcessListener 
	 */
	public void setProcessDead(int exitValue)
	{
		m_processExitValue = exitValue;
		m_processDead = true; // called if process we launch dies
	}

	/*
	 * @see flash.tools.debugger.SessionManager#accept(flash.tools.debugger.IProgress)
	 */
	public Session accept(IProgress waitReporter) throws IOException
	{
		boolean isAIRapp = false; // we don't know whether we're waiting for an AIR app
		return accept(waitReporter, isAIRapp);
	}
	
	/**
	 * A private variation on <code>accept()</code> that also has an argument
	 * indicating that the process we are waiting for is an AIR application. If
	 * it is, then we can sometimes give slightly better error messages (see bug
	 * FB-7544).
	 * 
	 * @param isAIRapp
	 *            if <code>true</code>, then the process we are waiting for
	 *            is an AIR application. This is only used to give better error
	 *            messages in the event that we can't establish a connection to
	 *            that process.
	 */
	private Session accept(IProgress waitReporter, boolean isAIRapp) throws IOException
	{
		// get timeout 
		int timeout = getPreference(PREF_ACCEPT_TIMEOUT);
		int totalTimeout = timeout;
		int iterateOn = 100;

		PlayerSession session = null;
		try
		{
			m_processDead = false;
			m_serverSocket.setSoTimeout(iterateOn);

			// Wait 100ms per iteration.  We have to do that so that we can report how long
			// we have been waiting.
			Socket s = null;
			while(s == null && !m_processDead)
			{
				try
				{
					s = m_serverSocket.accept();
				}
				catch(IOException ste)
				{
					timeout -= iterateOn;
					if (timeout < 0 || m_serverSocket == null || m_serverSocket.isClosed())
						throw ste; // we reached the timeout, or someome called stopListening()
				}

				// Tell the progress monitor we've waited a little while longer,
				// so that the Eclipse progress bar can keep chugging along
				if (waitReporter != null)
					waitReporter.setProgress(totalTimeout - timeout, totalTimeout);
			}

			if (s == null && m_processDead)
			{
				IOException e = null;
				String detailMessage = getLocalizationManager().getLocalizedTextString("processTerminatedWithoutDebuggerConnection"); //$NON-NLS-1$

				if (m_processMessages != null)
				{
					String commandLineMessage = m_processMessages.toString();
					if (commandLineMessage.length() > 0)
						e = new CommandLineException(detailMessage, m_launchCommand, commandLineMessage, m_processExitValue);
				}

				if (e == null)
				{
					if (isAIRapp)
					{
						// For bug FB-7544: give the user a hint about what might have gone wrong.
						detailMessage += s_newline;
						detailMessage += getLocalizationManager().getLocalizedTextString("maybeAlreadyRunning"); //$NON-NLS-1$
					}

					e = new IOException(detailMessage);
				}

				throw e;
			}

			/* create a new session around this socket */
			session = PlayerSession.createFromSocket(s);

			// transfer preferences 
			session.setPreferences(m_prefs);
		}
		catch(NullPointerException npe)
		{
			throw new BindException(getLocalizationManager().getLocalizedTextString("serverSocketNotListening")); //$NON-NLS-1$
		}
		finally
		{
			m_processMessages = null;
			m_launchCommand = null;
		}

		return session;
	}

	/*
	 * @see flash.tools.debugger.SessionManager#getDebuggerCallbacks()
	 */
	public IDebuggerCallbacks getDebuggerCallbacks()
	{
		return m_debuggerCallbacks;
	}

	/*
	 * @see flash.tools.debugger.SessionManager#setDebuggerCallbacks(flash.tools.debugger.IDebuggerCallbacks)
	 */
	public void setDebuggerCallbacks(IDebuggerCallbacks debuggerCallbacks)
	{
		m_debuggerCallbacks = debuggerCallbacks;
	}

	/**
	 * Returns the localization manager.  Use this for all localized strings.
	 */
	public static LocalizationManager getLocalizationManager()
	{
		return m_localizationManager;
	}
}
