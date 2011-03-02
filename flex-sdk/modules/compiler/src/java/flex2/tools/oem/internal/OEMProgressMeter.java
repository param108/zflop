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

package flex2.tools.oem.internal;

import flex2.tools.oem.ProgressMeter;

/**
 * @version 2.0.1
 * @author Clement Wong
 */
public class OEMProgressMeter implements ProgressMeter
{
	public void end()
	{
		System.out.println("progress meter: end");
	}

	public void percentDone(int n)
	{
		System.out.println(n + "%");
	}

	public void start()
	{
		System.out.println("progress meter: start");
	}
}
