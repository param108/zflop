////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.common;

import flex2.compiler.io.VirtualFile;

/**
 * A resolve that is used in PathResolver.  The most used and obvious implementor of this interface is VirtualFile.
 * There are a few other useful implementations, however, all of which use VirtualFile within them.
 *
 * @author Brian Deitte
 */
public interface SinglePathResolver
{
    // Brian: open to suggestions for a better name for this interface

    /**
     * Return an instance of this interface which represents the specified relative path.
     */
    VirtualFile resolve(String relative);
}
