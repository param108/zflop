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

import java.util.HashMap;
import java.util.Map;

/**
 * NoResponseException is thrown when the Player does
 * not respond to the command that was issued.
 * 
 * The field m_waitedFor contains the number of
 * milliseconds waited for the response.
 */
public class NoResponseException extends PlayerDebugException
{
	/**
	 * Number of milliseconds that elapsed causing the timeout
	 * -1 means unknown.
	 */
	public int m_waitedFor;

	public NoResponseException(int t) 
	{
		m_waitedFor = t;
	}

	public String getMessage()
	{
		Map args = new HashMap();
		String formatString;
		if (m_waitedFor != -1 && m_waitedFor != 0)
		{
			formatString = "key2"; //$NON-NLS-1$
			args.put("time", Integer.toString(m_waitedFor)); //$NON-NLS-1$
		}
		else
		{
			formatString = "key1"; //$NON-NLS-1$
		}
		return Bootstrap.getLocalizationManager().getLocalizedTextString(formatString, args);
	}
}
