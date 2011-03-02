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

package flex2.compiler.util;

/**
 * @version 2.0.1
 * @author Clement Wong
 */
public class CompilerControl
{
	public static final int RUN = 1;
	public static final int PAUSE = 2;
	public static final int STOP = 4;
	
	public CompilerControl()
	{
		run();
	}
	
	private int status;
	
	public void run()
	{
		status = RUN;
	}
	
	public void pause()
	{
		status = PAUSE;
	}
	
	public void stop()
	{
		status = STOP;
	}
	
	public int getStatus()
	{
		return status;
	}
}
