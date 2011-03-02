////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.config;

import java.util.Properties;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * @author Roger Gonzalez
 */
public class SystemPropertyConfigurator
{
    /**
     * Opportunistically find some configuration settings in system properties.
     * @param buffer the intermediate config buffer
     * @param prefix an optional prefix to add to the variable, pass null if no prefix
     * @throws ConfigurationException
     */
    public static void load( final ConfigurationBuffer buffer, String prefix ) throws ConfigurationException
    {
        try
        {
            Properties props = System.getProperties();

            for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
            {
                String propname = (String) e.nextElement();

                if (!propname.startsWith( prefix + "."))
                {
                    String value = System.getProperty( propname );
                    buffer.setToken( propname, value );
                    continue;
                }

                String varname = propname.substring( prefix.length() + 1 );

                if (!buffer.isValidVar( varname ))
                    continue;

                String value = System.getProperty( propname );

                List args = new LinkedList();
                StringTokenizer t = new StringTokenizer( value, "," );

                while (t.hasMoreTokens())
                {
                    String token = t.nextToken();
                    args.add( token );
                }
                buffer.setVar( varname, args, "system properties", -1 );
            }
        }
        catch (SecurityException se)
        {
            // just ignore, this is an optional for loading configuration   
        }
    }
}
