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

import java.util.Iterator;

/**
 * 
 * @author Clement Wong
 * @version 3.0
 */
public interface LibraryInfo
{
	/**
	 * 
	 * @param namespaceURI
	 * @param name
	 * @return
	 */
	Component getComponent(String namespaceURI, String name);

	/**
	 * 
	 * @param definition
	 * @return
	 */
	Component getComponent(String definition);
	
	/**
	 * 
	 * @return
	 */
	Iterator getComponents();
	
	/**
	 * 
	 * @param definition
	 * @return
	 */
	Script getScript(String definition);
	
	/**
	 * 
	 * @return
	 */
	Iterator getScripts();
	
	/**
	 * 
	 * @return
	 */
	String[] getDefinitionNames();
	
	/**
	 * 
	 * @return
	 */
	Iterator getFiles();
}
