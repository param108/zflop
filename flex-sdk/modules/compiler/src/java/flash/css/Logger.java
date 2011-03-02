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

package flash.css;

/**
 * @author Clement Wong
 */
public interface Logger
{
	void logError(String path, int line, String message);

	void logError(String message);

	void logWarning(String path, int line, String message);

	void logWarning(String message);
}
