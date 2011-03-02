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

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Clement Wong
 */
public class TextFile implements VirtualFile
{
	public TextFile(String text, String name, String parent, String mimeType)
	{
        this(text, name, name, parent, mimeType, System.currentTimeMillis());
	}

	public TextFile(String text, String name, String parent, String mimeType, long lastModified)
	{
		init(text, name, name, parent, mimeType, lastModified);
	}

	// This is for creating InlineComponent Source object
	public TextFile(String text, String name, String nameForReporting, String parent, String mimeType, long lastModified)
	{
        init(text, name, nameForReporting, parent, mimeType, lastModified);
	}

    private void init(String text, String name, String nameForReporting, String parent, String mimeType, long lastModified)
    {
	    this.text = text;
	    this.size = text == null ? 0 : text.length();
        this.name = name;
        this.nameForReporting = nameForReporting;
	    this.parent = parent;
        this.mimeType = mimeType;
        this.lastModified = lastModified;
    }

    private String text;
	private String name;
	private String nameForReporting;
	private String parent;
	private String mimeType;
	private long lastModified;
	private long size;

	public String getName()
	{
		return name;
	}

	public String getNameForReporting()
	{
		return nameForReporting;
	}

	public String getURL()
	{
		return "memory://" + name;
	}

	public String getParent()
	{
		return parent;
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
		return size;
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
		throw new UnsupportedOperationException();
	}

	public byte[] toByteArray() throws IOException
	{
		throw new UnsupportedOperationException();
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
		text = null;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof TextFile)
		{
			return (this == obj) || getName().equals(((TextFile) obj).getName());
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
		return true;
	}
	
	public String toString()
	{
		return text;
	}
}

