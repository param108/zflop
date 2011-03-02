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
import java.io.ByteArrayInputStream;

import flash.util.FileUtils;
import flex2.compiler.swc.zip.ZipEntry;

/**
 * VirtualFile implementation used by SwcLazyReadArchive
 */
public class VirtualZipFile implements VirtualFile
{
	private String mimeType;
	private String name;
	private String nameInZip;
	private byte[] bytes;
	private ZipFileHolder zipFile;

	public VirtualZipFile(ZipFileHolder zipFileHolder, String mimeType, String name, String nameInZip)
	{
		this.zipFile = zipFileHolder;
		this.mimeType = mimeType;
		this.name = name;
		this.nameInZip = nameInZip;
	}

	public void close()
	{
		zipFile.close();
	}

	public InputStream getInputStream() throws IOException
	{
		return new ByteArrayInputStream(toByteArray());
	}

	/**
	 * The bytes are only loaded as they are needed.
	 */
	public byte[] toByteArray() throws IOException
	{
		if (bytes == null)
		{
			InputStream stream = zipFile.getZipFile().getInputStream(getEntry());
		    this.bytes = FileUtils.toByteArray(stream);
		}
		return bytes;
	}

	public long getLastModified()
	{
		return getEntry().getTime();
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public String getName()
	{
		return name;
	}

	public String getNameForReporting()
	{
		return getName();
	}

	public String getParent()
	{
		return zipFile.getPath();
	}

    public boolean isDirectory()
    {
        return false;
    }

    public String getURL()
	{
        return "jar:file://" + getName().replaceAll("\\$", "!/");
	}

	public long size()
	{
		return getEntry().getSize();
	}

	public VirtualFile resolve(String relative)
	{
		return null;
	}

	public boolean equals(Object obj)
	{
		return obj == this;
	}

    public int hashCode()
    {
        return getName().hashCode();
    }

	public boolean isTextBased()
	{
		return false;
	}

	private ZipEntry getEntry()
	{
		return zipFile.getEntry(nameInZip);
	}
}
