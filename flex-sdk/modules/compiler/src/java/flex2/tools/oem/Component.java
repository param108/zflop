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
 * @author Brian Deitte
 * @version 3.0
 */
public interface Component
{
	/**
	 * 
	 * @return
	 */
	String getClassName();
	
	/**
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * 
	 * @return
	 */
	String getUri();
	
	/**
	 * 
	 * @return
	 */
	String getIcon();
	
	/**
	 * 
	 * @return
	 */
	String getDocs();
	
	/**
	 * 
	 * @return
	 */
	String getPreview();
	
	/**
	 * 
	 * @return
	 */
	String getLocation();
}
