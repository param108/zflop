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

package flash.tools.debugger;

/**
 * A simple interface to report progress on some operation.
 * 
 * @author mmorearty
 */
public interface IProgress
{
	/**
	 * Reports how much work has been done.
	 * 
	 * @param current
	 *            how much progress has been made toward the total
	 * @param total
	 *            the total amount of work
	 */
	public void setProgress(int current, int total);
}
