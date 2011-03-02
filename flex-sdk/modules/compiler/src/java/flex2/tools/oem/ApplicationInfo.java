////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem;

/**
 * 
 * @author Clement Wong
 * @version 3.0
 */
public interface ApplicationInfo
{
	/**
	 * 
	 * @return
	 */
	String[] getSymbolNames();
	
	/**
	 * 
	 * @return
	 */
	int getSWFVersion();
}
