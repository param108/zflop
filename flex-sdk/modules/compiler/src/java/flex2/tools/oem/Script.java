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
 * @author Roger Gonzalez
 * @version 3.0
 */
public interface Script
{
	/**
	 * 
	 */
	Object INHERITANCE = "Inheritance";
	
	/**
	 * 
	 */
	Object NAMESPACE = "Namespace";
	
	/**
	 * 
	 */
	Object SIGNATURE = "Signature";
	
	/**
	 * 
	 */
	Object EXPRESSION = "Expression";
	
	/**
	 * 
	 * @return
	 */
	String getLocation();
	
	/**
	 * 
	 * @return
	 */
	long getLastModified();
	
	/**
	 * 
	 * @return
	 */
	String[] getDefinitionNames();
	
	/**
	 * 
	 * @return
	 */
	String[] getPrerequisites();
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	String[] getDependencies(Object type);
	
	/**
	 * 
	 * @return
	 */
	byte[] getBytecodes();
}
