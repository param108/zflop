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

package flex2.compiler.util;

import flash.util.Trace;
import flex2.compiler.common.SinglePathResolver;
import flex2.compiler.io.NetworkFile;
import flex2.compiler.io.VirtualFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class URLPathResolver implements SinglePathResolver
{
    private static final URLPathResolver singleton = new URLPathResolver();

    private URLPathResolver()
    {
    }

    public static final URLPathResolver getSingleton()
    {
        return singleton;
    }

    public VirtualFile resolve(String uri)
    {
        VirtualFile location = null;

		try
		{
			URL url = new URL(uri);
            if (url != null)
            {
                location = new NetworkFile(url);
            }            
		}
		catch (SecurityException securityException)
		{
	    }
	    catch (MalformedURLException malformedURLException)
		{
		}
        catch (IOException ioException)
        {
        }

        if ((location != null) && Trace.pathResolver)
        {
            Trace.trace("URLPathResolver.resolve: resolved " + uri + " to " + location.getName());
        }

        return location;
    }
}
