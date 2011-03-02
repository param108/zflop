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

package flex2.tools.oem;

import java.io.File;

/**
 * @version 3.0
 * @author Clement Wong
 * @author Brian Deitte
 */
public interface PathResolver
{
	/**
	 * 
	 * @param relative
	 * @return
	 */
	File resolve(String relative);
}
