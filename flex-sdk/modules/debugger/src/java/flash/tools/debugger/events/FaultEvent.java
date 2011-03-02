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

package flash.tools.debugger.events;

/**
 * An event type that signals a problem situation within the Player.
 * Under normal conditions the Player will suspend execution, resulting
 * in a following BreakEvent to be fired.  However, if this occurs
 * while a getter or setter is executing, then the player will *not*
 * suspend execution.
 */
public abstract class FaultEvent extends DebugEvent
{
	private String stackTrace = ""; //$NON-NLS-1$

	public FaultEvent(String info)
	{
		super(getFirstLine(info));
		int newline = info.indexOf('\n');
		if (newline != -1)
			stackTrace = info.substring(newline+1);
	}

	public FaultEvent()
	{
		super();
	}

	public abstract String name();

	private static String getFirstLine(String s) {
		int newline = s.indexOf('\n');
		if (newline == -1)
			return s;
		else
			return s.substring(0, newline);
	}

	/**
	 * Returns the callstack in exactly the format that it came back
	 * from the player.  That is, as a single string of the following
	 * form:
	 *
	 * <pre>
	 *		at functionName()[filename:lineNumber]
	 *		at functionName()[filename:lineNumber]
	 *		...
	 * </pre>
	 *
	 * Each line has a leading tab character.
	 *
	 * @return callstack, or an empty string; never <code>null</code>
	 */
	public String stackTrace()
	{
		return stackTrace;
	}
}
