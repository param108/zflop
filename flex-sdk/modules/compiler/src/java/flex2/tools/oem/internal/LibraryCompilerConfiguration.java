////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem.internal;

import java.util.HashMap;
import java.util.Map;

import flex2.compiler.common.Configuration;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.tools.ToolsConfiguration;

/**
 * @version 2.0.1
 * @author Clement Wong
 */
public class LibraryCompilerConfiguration extends ToolsConfiguration
{
	public static Map getAliases()
    {
        Map map = new HashMap();
	    map.putAll(Configuration.getAliases());
	    map.remove("o");
		return map;
    }

    public void validate( ConfigurationBuffer cfgbuf ) throws flex2.compiler.config.ConfigurationException
    {
    	super.validate(cfgbuf);
    }

    //
    // 'generate-link-report' option
    //
    
	private boolean generateLinkReport;
	
	public boolean generateLinkReport()
	{
		return generateLinkReport;
	}

	public void keepLinkReport(boolean b)
	{
		generateLinkReport = b;
	}

    //
    // 'load-config' option
    //
    
    // dummy, ignored - pulled out of the buffer
    public void cfgLoadConfig(ConfigurationValue cv, String filename) throws flex2.compiler.config.ConfigurationException
    {
    }

    public static ConfigurationInfo getLoadConfigInfo()
    {
        return new ConfigurationInfo( 1, "filename" )
        {
            public boolean allowMultiple()
            {
                return true;
            }
        };
    }
    
	//
	// compute-digest option
	//
	
	private boolean computeDigest = true;
	
	public boolean getComputeDigest()
	{
		return computeDigest;
	}
	
	/**
	 * compute-digest option
	 * 
	 * @param cv
	 * @param b
	 */
	public void cfgComputeDigest(ConfigurationValue cv, boolean b)
	{
		computeDigest = b;
	}
}
