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

package flex2.tools;

import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;

/**
 * Used to setup the "digest." prefix for the optioins in DigestConfiguration.
 * 
 * @author dloverin
 */
public class DigestRootConfiguration
{
	public DigestRootConfiguration()
	{
        digestConfiguration = new DigestConfiguration(this);
	}
	
	//
    // 'digest.*' options
    //
	
	private DigestConfiguration digestConfiguration;
	
    public DigestConfiguration getDigestConfiguration()
    {
        return digestConfiguration;
    }
   	
	//
	// 'version' option
	//
	
    // dummy, just a trigger for version info
    public void cfgVersion(ConfigurationValue cv, boolean dummy)
    {
        // intercepted upstream in order to allow version into to be printed even when required args are missing
    }

	//
	// 'help' option
	//
	
    // dummy, just a trigger for help text
	public void cfgHelp(ConfigurationValue cv, String[] keywords)
	{
    }

    public static ConfigurationInfo getHelpInfo()
    {
        return new ConfigurationInfo( -1, "keyword" )
        {
            public boolean isGreedy()
			{
				return true;
			}

            public boolean isDisplayed()
			{
				return false;
			}
        };
    }
}
