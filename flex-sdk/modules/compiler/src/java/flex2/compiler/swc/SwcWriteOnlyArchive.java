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

package flex2.compiler.swc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import flex2.compiler.io.FileUtil;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.swc.zip.ZipEntry;
import flex2.compiler.swc.zip.ZipOutputStream;
import flex2.compiler.util.MimeMappings;

public class SwcWriteOnlyArchive implements SwcArchive
{
    public SwcWriteOnlyArchive( String name, OutputStream out )
    {
        this.out = out;
        this.name = name;
    }

    public String getLocation()
    {
        return name;
    }

    public void load()
    {
    	throw new UnsupportedOperationException();
    }

    public void save() throws Exception
    {
        ZipOutputStream zos = null;
	    try
	    {
            zos = new ZipOutputStream(out);

            for (Iterator it = files.values().iterator(); it.hasNext(); )
            {
                VirtualFile f = (VirtualFile) it.next();

                ZipEntry entry = new ZipEntry( f.getName() );
                entry.setTime(f.getLastModified());
                zos.putNextEntry( entry );

                BufferedInputStream in = new BufferedInputStream(f.getInputStream());
                FileUtil.streamOutput(in, zos);
                zos.closeEntry();
            }

            zos.flush();
        }
        finally
        {
            try { if (zos != null) zos.close(); } catch(IOException ioe) {}
        }
    }

	public void close()
	{
	}

    public Map getFiles()
    {
        return files;
    }

    public VirtualFile getFile( String path )
    {
        return (VirtualFile) files.get( path );
    }

    public void putFile( VirtualFile file )
    {
        files.put( file.getName(), file );
    }

    public void putFile( String path, byte[] data, long lastModified )
    {
        InMemoryFile file = new InMemoryFile( data, path, MimeMappings.getMimeType(path), lastModified );
        files.put( file.getName(), file );
    }

    public long getLastModified()
    {
        return 0;
    }

    protected final OutputStream out;
    protected final String name;
    protected Map files = new HashMap();
}
