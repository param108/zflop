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

package flex2.linker;

import flash.swf.Movie;

/**
 * Post-link step
 *
 * @author Clement Wong
 */
public interface PostLink
{
	void run(Movie movie);
	
	void run(ConsoleApplication app);
}
