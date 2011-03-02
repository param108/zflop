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

package flex2.compiler.common;

import flash.util.FileUtils;
import flash.util.Trace;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.io.FileUtil;
import flex2.compiler.io.LocalFile;

import java.io.File;

/**
 * Resolves absolute file paths.  This resolver explicitly does not resolve relative
 * paths, because it is included in the ThreadLocalToolkit's PathResolver.  The
 * ThreadLocalToolkit's PathResolver is used to resolve things like @Embed assets and we
 * don't want paths which are relative to the current working directory and not relative
 * to the containing Mxml document to be resolved.  For example, if we have:
 *
 *   C:/foo/bar.mxml
 *
 * with:
 *
 *   <mx:Image source="@Embed(source='image.jpg')"/>
 *
 * and:
 *
 *   C:/foo/image.jpg
 *   C:/image.jpg
 *
 * When the current working directory is C:/, we don't want resolve() to return
 * C:/image.jpg.
 * 
 * @author Brian Deitte
 */
public class LocalFilePathResolver implements SinglePathResolver
{
    private static final LocalFilePathResolver singleton = new LocalFilePathResolver();

    private LocalFilePathResolver()
    {
    }

    public static LocalFilePathResolver getSingleton()
    {
        return singleton;
    }

    public VirtualFile resolve( String pathStr )
    {
        File path = FileUtil.openFile(pathStr);
        VirtualFile virt = null;

        if (path != null && FileUtils.exists(path) && FileUtils.isAbsolute(path))
        {
            virt = new LocalFile(path);
        }

        if ((virt != null) && Trace.pathResolver)
        {
            Trace.trace("LocalFilePathResolver.resolve: resolved " + pathStr + " to " + virt.getName());
        }

        return virt;
    }
}
