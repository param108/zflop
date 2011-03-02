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

package flex2.tools;

import flash.util.Trace;
import flex2.compiler.common.Configuration;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.config.ConfigurationInfo;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;

public class ToolsConfiguration extends Configuration
{
    public ToolsConfiguration()
    {
        this.licenses = new LicensesConfiguration();
    }

	private LicensesConfiguration licenses;

	public LicensesConfiguration getLicensesConfiguration()
	{
		return licenses;
	}

    private VirtualFile licenseFile;

    public VirtualFile getLicenseFile()
    {
        return licenseFile;
    }

    private void processDeprecatedOptions(ConfigurationBuffer configurationBuffer)
    {
		for (Iterator i = configurationBuffer.getVarIterator(); i.hasNext(); )
		{
			String var = (String) i.next();
			ConfigurationInfo info = configurationBuffer.getInfo(var);
			if (info.isDeprecated() && configurationBuffer.getVar(var) != null)
			{
				CompilerMessage.CompilerWarning warning = info.getDeprecatedMessage();
				String replacement = info.getDeprecatedReplacement();
				String since = info.getDeprecatedSince();
				
				if (warning != null)
				{
					ThreadLocalToolkit.log(warning);
				}
				else
				{
					ThreadLocalToolkit.log(new DeprecatedConfigurationOption(var, replacement, since));
				}
			}
		}		
    }
    
    public static class DeprecatedConfigurationOption extends CompilerMessage.CompilerWarning
    {
    	public DeprecatedConfigurationOption(String var, String replacement, String since)
    	{
    		this.var = var;
    		this.replacement = replacement;
    		this.since = since;
    	}
    	
    	public final String var, replacement, since;
    }

	public void validate(ConfigurationBuffer configurationBuffer) throws flex2.compiler.config.ConfigurationException
    {
		// process the merged configuration buffer. right, can't just process the args.
		processDeprecatedOptions(configurationBuffer);

		// If license.jar is present, call flex.license.OEMLicenseService.getLicenseFilename().
		try
		{
			Class oemLicenseServiceClass = Class.forName("flex.license.OEMLicenseService");
			Method method = oemLicenseServiceClass.getMethod("getLicenseFilename", null);
			String licenseFileName = (String)method.invoke(null, null);
			licenseFile = configResolver.resolve(licenseFileName);
		}
		catch (Exception e)
		{
		}

        if (Trace.license)
        {
            final String file = (licenseFile != null) ? licenseFile.getName() : "";
            Trace.trace("ToolsConfiguration.validate: licenseFile = '" + file + "'");
        }
        
	    // validate the -AS3, -ES and -strict settings
	    boolean strict = "true".equalsIgnoreCase(configurationBuffer.peekSimpleConfigurationVar(CompilerConfiguration.STRICT));
	    boolean as3 = "true".equalsIgnoreCase(configurationBuffer.peekSimpleConfigurationVar(CompilerConfiguration.AS3));
	    boolean es = "true".equalsIgnoreCase(configurationBuffer.peekSimpleConfigurationVar(CompilerConfiguration.ES));

	    if ((as3 && es) || (!as3 && !es))
	    {
		    throw new BadAS3ESCombination(as3, es);
	    }
	    else if (strict && es)
	    {
		    ThreadLocalToolkit.log(new BadESStrictCombination(es, strict));
	    }
        
        // if we're saving signatures to files and the directory is unset, use the default.
        final CompilerConfiguration compConfig = getCompilerConfiguration();
        if (compConfig.getKeepGeneratedSignatures() && (compConfig.getSignatureDirectory() == null))
        {
            // the setter is non-trivial and resolves the path -- this resolves the default path
            compConfig.setSignatureDirectory(null);
        }
    }

	public static class BadAS3ESCombination extends ConfigurationException
	{
	    public BadAS3ESCombination(boolean as3, boolean es)
	    {
	        super("");
		    this.as3 = as3;
		    this.es = es;
	    }

		public final boolean as3, es;
	}

	public static class BadESStrictCombination extends ConfigurationException
	{
	    public BadESStrictCombination(boolean es, boolean strict)
	    {
	        super("");
		    this.es = es;
		    this.strict = strict;
	    }

		public final boolean es, strict;

		public String getLevel()
		{
		    return WARNING;
		}
	}
    
    //
    // 'warnings' option
    //
    
    private boolean warnings = true;
    
    public boolean getWarnings()
    {
        return warnings;
    }

    public void cfgWarnings(ConfigurationValue cv, boolean b)
    {
        warnings = b;
    }

    public static ConfigurationInfo getWarningsInfo()
    {
        return new ConfigurationInfo();
    }
    
}

