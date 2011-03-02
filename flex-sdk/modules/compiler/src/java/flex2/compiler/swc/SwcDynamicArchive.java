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
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.FileUtil;
import flex2.compiler.swc.zip.ZipEntry;
import flex2.compiler.swc.zip.ZipFile;
import flex2.compiler.swc.zip.ZipOutputStream;
import flex2.compiler.util.MimeMappings;

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;

import flash.util.FileUtils;

/**
 * This SwcArchive implementation is somewhat inefficient because it holds a full snapshot of the archive
 * in memory all the time, but it has the ability to be used for both reading and writing.
 *
 * @author Roger Gonzalez
 */
public class SwcDynamicArchive implements SwcArchive
{
    public SwcDynamicArchive( String path )
    {
        this.path = path;
    }

    public String getLocation()
    {
        return path;
    }

    public void load()
    {
	    ZipFile zipFile = null;
        try
        {
	        zipFile = new ZipFile(path);
	        Enumeration e = zipFile.getEntries();
	        while (e.hasMoreElements())
	        {
		        ZipEntry ze = (ZipEntry)e.nextElement();
                InputStream inputStream = zipFile.getInputStream(ze);
                VirtualFile f = new InMemoryFile( inputStream, ze.getSize(), path + "$" + ze.getName(),
		                MimeMappings.getMimeType(ze.getName()), ze.getTime() );

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
        finally
        {
            try
            {
                if (zipFile != null)
                    zipFile.close();
            }
            catch(IOException ioe)
            {
                // ignore
            }
        }
    }

    public void save() throws Exception
    {
        String tmpPath = path + ".tmp";
        ZipOutputStream zos = null;
	    try
	    {
            zos = new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( FileUtil.openFile(tmpPath, true) )));

            for (Iterator it = files.entrySet().iterator(); it.hasNext(); )
            {
            	Map.Entry mapEntry = (Map.Entry)it.next();
                VirtualFile f = (VirtualFile) mapEntry.getValue();

                ZipEntry entry = new ZipEntry( (String)mapEntry.getKey() );
                entry.setTime(f.getLastModified());
                zos.putNextEntry( entry );

                BufferedInputStream in = new BufferedInputStream(f.getInputStream());
                FileUtil.streamOutput(in, zos);
                zos.closeEntry();
            }

            zos.close();
            zos = null;

            File tmpFile = new File(tmpPath);
            File file = new File(path);
            if (!FileUtils.renameFile( tmpFile, file ))
            {
                throw new SwcException.SwcNotRenamed( tmpFile.getAbsolutePath(), file.getAbsolutePath() );
            }
        }
        finally
        {
            try
            {
                if (zos != null)
                    zos.close();
            }
            catch(IOException ioe)
            {
                // ignore
            }
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

    protected final String path;
    protected Map files = new HashMap();
}
