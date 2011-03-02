////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.swc;

import flex2.compiler.io.VirtualFile;

import java.io.IOException;
import java.io.InputStream;

import flash.util.FileUtils;

/**
 * A file within a SWC.  Only grabs the file from the archive when absolutely necessary.  It uses the values
 * from catalog.xml for name and lastModified
 *
 * @author Brian Deitte
 */
public class SwcFile implements VirtualFile
{
    private String path;
    private long lastModified;
    private SwcArchive archive;
    private VirtualFile virtualFile;
    private Swc swc;

    public SwcFile(String path, long lastModified, Swc swc, SwcArchive archive)
    {
        this.path = path;
        this.lastModified = lastModified;
        this.archive = archive;
        this.swc = swc;
    }
    
    private void createVirtualFile()
    {
        if (virtualFile == null)
        {
            virtualFile = archive.getFile(path);
            if (virtualFile == null)
            {
                throw new SwcException.MissingFile(path);
            }
            // make sure the assumptions we may have made about the virtualFile are correct
            assert virtualFile.getName().equals(getName());
            assert virtualFile.hashCode() == hashCode();

            // preilly: Don't assert that the lastModified from the
            // swc, a zip file, is the same as the one in the catalog,
            // because Java's java.util.ZipEntry's getTime() and
            // setTime() are not symmetric.  See the private methods
            // javaToDosTime() and dosToJavaTime() in ZipEntry.
        }
    }

    public String getName()
    {
        return swc.getLocation() + "$" + path;
    }

	public String getNameForReporting()
	{
		return getName();
	}

    public String getURL()
    {
        createVirtualFile();
        return virtualFile.getURL();
    }

    public String getParent()
    {
        createVirtualFile();
        return virtualFile.getParent();
    }

    public Swc getSwc()
    {
        return swc;
    }

    public boolean isDirectory()
    {
        return false;  // ?
    }

    public long size()
    {
        createVirtualFile();
        return virtualFile.size();
    }

    public String getMimeType()
    {
        createVirtualFile();
        return virtualFile.getMimeType();
    }

    public InputStream getInputStream() throws IOException
    {
        createVirtualFile();
        return virtualFile.getInputStream();
    }

	public byte[] toByteArray() throws IOException
	{
		throw new SwcException.UnsupportedOperation("toByteArray()", this.getClass().getName());
	}
	
    public long getLastModified()
    {
        return lastModified;
    }

    public VirtualFile resolve(String relative)
    {
        createVirtualFile();
        int sep = path.lastIndexOf( "/" );
        if (sep != -1)
        {
            relative = FileUtils.addPathComponents( path.substring( 0, sep ), relative, '/' );
        }

        String swcLocation = getSwcLocation(relative);

        if ((swcLocation != null) && swcLocation.equals(swc.getLocation()))
        {
            String filePath = getFilePath(relative);
            relative = filePath;
        }

        return swc.getFile(relative); // a relative reference to a swc file is always internally resolved
    }

    public void close()
    {
        createVirtualFile();
        virtualFile.close();
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof SwcFile)
        {
            createVirtualFile();
            return virtualFile.equals(((SwcFile)obj).virtualFile);
        }
        return false;
    }

    public int hashCode()
    {
        return getName().hashCode();
    }

	public boolean isTextBased()
	{
		return false;
	}

	/**
	 * Returns the SWC location from a SWC VirtualFile's name
	 */
	public static String getSwcLocation(String name)
	{
		int ind = name.indexOf('$');
		if (ind != -1)
		{
			return name.substring(0, ind);
		}
		return null;

	}

	/**
	 * Returns the file path from a SWC VirtualFile's name
	 */
	public static String getFilePath(String name)
	{
		int ind = name.indexOf('$');
		if (ind != -1)
		{
			return name.substring(ind + 1);
		}
		return null; 
	}

	public String toString()
	{
		return getName();
	}
}
