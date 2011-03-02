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

package flex2.compiler.io;

import flex2.compiler.swc.zip.ZipFile;
import flex2.compiler.swc.zip.ZipEntry;

import java.io.IOException;

import flash.util.Trace;

/**
 * Holds a ZipFile or opens one on request based on the given path.
 *
 * @author Brian Deitte
 */
public class ZipFileHolder
{
	private ZipFile file;
	private String path;

	public ZipFileHolder(ZipFile file, String path)
	{
		this.file = file;
		this.path = path;
	}

	public ZipFile getZipFile()
	{
		if (file == null)
		{
			try
			{
				file = new ZipFile(path);
			}
			catch (IOException ioe)
			{
				// this should never happen
				throw new RuntimeException("An unexpected error occured when accessing " + path, ioe);
			}
		}
		return file;
	}

	public ZipEntry getEntry(String name)
	{
		return getZipFile().getEntry(name);
	}

	public String getPath()
	{
		return path;
	}

	public void close()
	{
		if (file != null)
		{
			try
			{
				file.close();
			}
			catch(IOException ioe)
			{
				// normally ignore issues with close
				if (Trace.error)
				    ioe.printStackTrace();
			}
			file = null;
		}
	}
}
