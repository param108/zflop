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

public class ProcessListener extends Thread
{
	PlayerSessionManager	m_mgr;
	Process					m_process;
	volatile boolean		m_done;

	public ProcessListener(PlayerSessionManager mgr, Process process)
	{
		super("DJAPI ProcessListener"); //$NON-NLS-1$
		setDaemon(true);
		m_mgr = mgr;
		m_process = process;
		m_done = false;
	}

	public void run()
	{
		while (!m_done)
		{
			try
			{
				m_process.waitFor();
				m_done = true;
				m_mgr.setProcessDead(m_process.exitValue());
			}
			catch (InterruptedException e)
			{
				// this will happen if finish() calls Thread.interrupt()
			}
		}
	}

	public void finish()
	{
		m_done = true;
		this.interrupt(); // wake up the listening thread
	}
}
