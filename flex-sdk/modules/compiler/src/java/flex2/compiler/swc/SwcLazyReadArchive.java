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

package flex2.compiler.swc;

import flex2.compiler.io.VirtualFile;
import flex2.compiler.io.VirtualZipFile;
import flex2.compiler.io.ZipFileHolder;
import flex2.compiler.swc.zip.ZipEntry;
import flex2.compiler.swc.zip.ZipFile;
import flex2.compiler.util.MimeMappings;

import java.util.Enumeration;
import java.io.File;
import java.io.IOException;

import flash.util.Trace;

/**
 * This SwcArchive works like the default SwcDynamicArchive except in its loading and close.  It holds on to the ZipFile
 * for its VirtualFiles and only loads the bytes when requested.
 *
 * @author Brian Deitte
 */
public class SwcLazyReadArchive extends SwcDynamicArchive
{
	private ZipFile zipFile;

    public SwcLazyReadArchive( String path )
    {
	    super(path);
    }

    public void load()
    {
        try
        {
	        File file = new File(path);
	        zipFile = new ZipFile(file);
	        ZipFileHolder holder = new ZipFileHolder(zipFile, path);
	        Enumeration e = zipFile.getEntries();
            while (e.hasMoreElements())
            {
	            ZipEntry ze = (ZipEntry) e.nextElement();
	            VirtualFile f = new VirtualZipFile( holder,  MimeMappings.getMimeType(ze.getName()),
			            path + "$" + ze.getName(), ze.getName());
                files.put( ze.getName(), f );
            }
        }
        catch (SwcException.UnknownZipFormat e)
        {
        	throw new SwcException.NotASwcFile(path);
        }
        catch (SwcException e)
        {
	        throw e;
        }
        catch (Exception e)
        {
            throw new SwcException.FilesNotRead( e.getMessage() );
        }
    }

	public void close()
	{
		try
		{
			zipFile.close();
		}
		catch(IOException ioe)
		{
			// ignore, not a problem if we can't close
			if (Trace.error)
			    ioe.printStackTrace();
		}
	}
}
