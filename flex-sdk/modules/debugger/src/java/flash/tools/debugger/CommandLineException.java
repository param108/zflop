////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger;

import java.io.IOException;

/**
 * Represents an error that occurred while invoking a command-line
 * program.  Saves the text error message that was reported
 * by the command-line program.
 * 
 * @author mmorearty
 */
public class CommandLineException extends IOException
{
	private String[] m_commandLine;
	private String m_commandOutput;
	private int m_exitValue;

	/**
	 * @param detailMessage
	 *            the detail message, e.g. "Program failed" or whatever
	 * @param commandLine
	 *            the command and arguments that were executed, e.g.
	 *            <code>{ "ls", "-l" }</code>
	 * @param commandOutput
	 *            the text error message that was reported by the command-line
	 *            program. It is common for this message to be more than one
	 *            line.
	 * @param exitValue
	 *            the exit value that was returned by the command-line program.
	 */
	public CommandLineException(String detailMessage, String[] commandLine, String commandOutput, int exitValue)
	{
		super(detailMessage);

		m_commandLine = commandLine;
		m_commandOutput = commandOutput;
		m_exitValue = exitValue;
	}

	public String[] getCommandLine()
	{
		return m_commandLine;
	}

	/**
	 * @return command line message, often multi-line, never <code>null</code>
	 */
	public String getCommandOutput()
	{
		return m_commandOutput;
	}

	/**
	 * @return the exit value that was returned by the command-line program.
	 */
	public int getExitValue()
	{
		return m_exitValue;
	}
}
