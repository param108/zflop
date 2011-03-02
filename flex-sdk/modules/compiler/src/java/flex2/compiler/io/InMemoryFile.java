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

import flash.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Not done yet...                                             
 * 
 * @author Clement Wong
 */
public class InMemoryFile implements VirtualFile
{
	public InMemoryFile(InputStream stream, long size, String name, String mimeType, long lastModified)
		throws IOException
    {
        this(FileUtils.toByteArray(stream, (int) size), name, mimeType, lastModified);
    }

	public InMemoryFile(InputStream stream, String name, String mimeType, long lastModified)
    {
        this(FileUtils.toByteArray(stream), name, mimeType, lastModified);
    }

	public InMemoryFile(byte[] data, String name, String mimeType, long lastModified)
	{
		this.bytes = data;
		init(name, mimeType, lastModified);
	}

	private void init(String name, String mimeType, long lastModified)
	{
		this.name = name;
		this.mimeType = mimeType;
	    this.lastModified = lastModified;
	}

    private byte[] bytes;
	private String name;
	private String mimeType;
	private long lastModified;

	public String getName()
	{
		return name;
	}

	public String getNameForReporting()
	{
		return name;
	}

	public String getURL()
	{
		return "memory://" + name;
	}

	public String getParent()
	{
		return null;
    }

    public boolean isDirectory()
    {
        return false;
    }

    /**
	 * Return file size...
	 */
	public long size()
	{
		return bytes.length;
	}

	/**
	 * Return mime type
	 */
	public String getMimeType()
	{
		return mimeType;
	}

	/**
	 * Return input stream...
	 */
	public InputStream getInputStream() throws IOException
	{
		return new ByteArrayInputStream(bytes);
	}

	public byte[] toByteArray() throws IOException
	{
		return bytes;
	}

	/**
	 * Return last time the underlying source is modified.
	 */
	public long getLastModified()
	{
		return lastModified;
	}

    /**
	 * Return an instance of this interface which represents the specified relative path.
	 */
	public VirtualFile resolve(String relative)
	{
		return null;
	}

	/**
	 * Signal the hosting environment that this instance is no longer used.
	 */
	public void close()
	{
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof InMemoryFile)
		{
			return (this == obj) || getName().equals(((InMemoryFile) obj).getName());
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

	public boolean isTextBased()
	{
		return false;
	}
}
