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

/**
 * Reasons for which the Flash Player will suspend itself
 */
public interface SuspendReason
{
	public static final int Unknown			= 0;
	
	/** We hit a breakpoint */
	public static final int Breakpoint  	= 1;
	
	/** A watchpoint was triggered */
	public static final int Watch			= 2;
	
	/** A fault occurred */
	public static final int Fault			= 3;

	public static final int StopRequest		= 4;

	/** A step completed */
	public static final int Step			= 5;

	public static final int HaltOpcode		= 6;
	
	/**
	 * Either a new SWF was loaded, or else one or more scripts (ABCs)
	 * from an existing SWF were loaded.
	 */
	public static final int ScriptLoaded	= 7;
}
