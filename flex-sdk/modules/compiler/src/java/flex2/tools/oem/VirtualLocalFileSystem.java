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

package flex2.tools.oem;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import flash.util.FileUtils;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.VirtualFile;

/**
 * The <code>VirtualLocalFileSystem</code> class serves as a factory for creating <code>VirtualLocalFile</code> instances.
 * It also helps <code>VirtualLocalFile</code> instances resolve relative paths that might point to other
 * <code>VirtualLocalFile</code> instances or "real" files in the file system.
 * 
 * @see flex2.tools.oem.VirtualLocalFile
 * @version 2.0.1
 * @author Clement Wong
 */
public class VirtualLocalFileSystem
{
    /**
     * Constructor.
     */
    public VirtualLocalFileSystem()
    {
        files = new HashMap();
    }
    
    private final Map files;

    /**
     * Creates a <code>VirtualLocalFile</code> instance.
     *
     * @param name A canonical path. The name must end with a file extension; for example: <code>.mxml</code> or <code>.as</code>.
     * @param text Source code.
     * @param parent The parent directory of this <code>VirtualLocalFile</code> object.
     * @param lastModified The last modified time for the virtual local file.
     * @return A <code>VirtualLocalFile</code>.
     */
    public final VirtualLocalFile create(String name, String text, File parent, long lastModified)
    {
        VirtualLocalFile f = new VirtualLocalFile(name, text, parent, lastModified, this);
        files.put(name, f);
        return f;
    }
    
    /**
     * Updates a <code>VirtualLocalFile</code> with the specified text and timestamp.
     * 
     * @param name A canonical path. The name must end with a file extension; for example: <code>.mxml</code> or <code>.as</code>.
     * @param text Source code.
     * @param lastModified The last modified time.
     * @return <code>true</code> if the <code>VirtualLocalFile</code> was successfully updated; <code>false</code> if a 
     * <code>VirtualLocalFile</code> was not found.
     */
    public final boolean update(String name, String text, long lastModified)
    {
        VirtualLocalFile f = (VirtualLocalFile) files.get(name);
        if (f != null)
        {
            f.text = text;
            f.lastModified = lastModified;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    final VirtualFile resolve(VirtualLocalFile base, String name)
    {
        // if 'name' is a full name and the VirtualFile instance exists, return it.
        VirtualLocalFile f = (VirtualLocalFile) files.get(name);
        if (f != null)
        {
            return f;
        }
        
        // if 'name' is relative to 'base', resolve 'name' into a full name.
        String fullName = constructName(base, name);
        
        // try to lookup again.
        f = (VirtualLocalFile) files.get(fullName);
        if (f != null)
        {
            return f;
        }

        // it's not in the HashMap. let's locate it in the file system.
        File absolute = FileUtil.openFile(name);
        if (absolute != null && FileUtils.exists(absolute))
        {
            return new LocalFile(absolute);
        }
        
        File relative = FileUtil.openFile(fullName);
        if (relative != null && FileUtils.exists(relative))
        {
            return new LocalFile(relative);
        }
        
        return null;
    }
    
    private String constructName(VirtualLocalFile base, String relativeName)
    {
        return FileUtil.getCanonicalPath(FileUtil.openFile(base.getParent() + File.separator + relativeName));
    }
}
