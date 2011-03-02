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

package flex2.compiler.config;

/**
 *	Filter the set of configuration variables in use. 
 *  
 *  @author dloverin
 */
public interface ConfigurationFilter
{
	/**
	 * Decide if a given configuration option should be removed from the set
	 * of configuration options.  
	 * 
	 * @param name full name of the configuration option
	 * @return true if the option should be in the configuration,
	 * 		   false if the option is excluded.
	 */
	boolean select(String name);

}
