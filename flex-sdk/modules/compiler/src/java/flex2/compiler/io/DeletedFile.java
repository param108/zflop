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

package flex2.compiler.io;

import java.io.IOException;
import java.io.InputStream;

import flex2.compiler.util.MimeMappings;

/**
 * @author Clement Wong
 */
public class DeletedFile implements VirtualFile
{
	public DeletedFile(String name)
	{
		this.name = name;
	}

	private String name;
	private String mimeType;
	         
	public String getName()
	{
		return name;
	}

	public String getNameForReporting()
	{
		return getName();
	}

	public String getURL()
	{
		return null;
	}

	public String getParent()
	{
		return null;
	}

	public long size()
	{
		return 0;
	}

    public boolean isDirectory()
    {
        return false;
    }

    public String getMimeType()
	{
		if (mimeType == null)
		{
            mimeType = MimeMappings.getMimeType(getName());
		}
		return mimeType;
	}

	public InputStream getInputStream() throws IOException
	{
		return null;
	}

	public byte[] toByteArray() throws IOException
	{
		throw new UnsupportedOperationException("toByteArray() not supported in " + this.getClass().getName());
	}

	public long getLastModified()
	{
		return -1;
	}

	public void close()
	{
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof DeletedFile)
		{
			return (this == obj) || getName().equals(((DeletedFile) obj).getName());
		}
		else
		{
			return false;
		}
	}

	public int hashCode()
	{
		return getName().hashCode();
	}

	public VirtualFile resolve(String relative)
	{
		return null;
	}

	public boolean isTextBased()
	{
		return false;
	}
}
