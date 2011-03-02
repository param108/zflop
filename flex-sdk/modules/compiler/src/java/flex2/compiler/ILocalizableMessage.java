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

package flex2.compiler;

/**
 * @author Roger Gonzalez
 *
 * This is a marker interface for all localizable messages.
 * Please make all localized errors and exceptions implement this!
 */
public interface ILocalizableMessage extends flex2.tools.oem.Message
{
    void setPath(String path);
    void setLine(int line); 
    void setColumn(int column);
    Exception getExceptionDetail();
    boolean isPathAvailable();
}
