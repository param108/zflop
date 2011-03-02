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

package flex2.tools;

import flash.localization.LocalizationManager;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.CompilerMessage.CompilerError;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class Tool
{
    public static Map getLicenseMapFromFile(String fileName) throws flex2.compiler.config.ConfigurationException
    {
        Map result = null;
        FileInputStream in = null;

        try
        {
            in = new FileInputStream(fileName);
            Properties properties = new Properties();
            properties.load(in);
            Enumeration enumeration = properties.propertyNames();

            if ( enumeration.hasMoreElements() )
            {
                result = new HashMap();

                while ( enumeration.hasMoreElements() )
                {
                    String propertyName = (String) enumeration.nextElement();
                    result.put(propertyName, properties.getProperty(propertyName));
                }
            }
        }
        catch (IOException ioException)
        {
        	LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
            throw new flex2.compiler.config.ConfigurationException(l10n.getLocalizedTextString(new FailedToLoadLicenseFile(fileName)));
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }

        return result;
    }

    public static class FailedToLoadLicenseFile extends CompilerError
	{
		public String fileName;

		public FailedToLoadLicenseFile(String fileName)
		{
			super();
			this.fileName = fileName;
		}
	}
}
