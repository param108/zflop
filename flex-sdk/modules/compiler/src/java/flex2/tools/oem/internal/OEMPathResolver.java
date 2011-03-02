////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem.internal;

import java.io.File;

import flex2.compiler.common.SinglePathResolver;
import flex2.compiler.io.LocalFile;
import flex2.compiler.io.VirtualFile;
import flex2.tools.oem.PathResolver;

/**
 * 
 * @version 3.0
 * @author Clement Wong
 */
public class OEMPathResolver implements SinglePathResolver
{
	public OEMPathResolver(PathResolver r)
	{
		resolver = r;
	}
	
	private PathResolver resolver;
	
	public VirtualFile resolve(String relative)
	{
		File f = resolver != null ? resolver.resolve(relative) : null;
		return f != null ? new LocalFile(f) : null;
	}
}
