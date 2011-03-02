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

package flex2.compiler.swc;

import flex2.compiler.io.VirtualFile;

import java.util.Map;

/**
 * SwcArchive stores information on the files in a SWC
 *
 * @author Roger Gonzalez
 */
public interface SwcArchive
{
    public String getLocation();
    public void load();
    public void save() throws Exception;
	public void close();

    public Map getFiles();
    public VirtualFile getFile( String path );
    public void putFile( VirtualFile file );
    public void putFile( String path, byte[] data, long lastModified );

    public long getLastModified();
}
