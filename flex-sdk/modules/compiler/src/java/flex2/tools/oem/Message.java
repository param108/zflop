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

/**
 * The <code>Message</code> interface lets you discover
 * the following information about a compiler message.
 * 
 * <pre>
 * 1. getLevel()    - Error level: error, warning or info.
 * 2. getPath()     - Location.
 * 3. getLine()     - Line number.
 * 4. getColumn()   - Column number.
 * 5. toString()    - Message string.
 * </pre>
 *
 * The <code>Message.toString()</code> method returns the message text.
 * 
 * @version 2.0.1
 * @author Clement Wong
 */
public interface Message
{
    /**
     * Indicates that the severity of this <code>Message</code> is "<code>error</code>".
     */
    String ERROR = "error";
    
    /**
     * Indicates that the severity of this <code>Message</code> is "<code>warning</code>".
     */
    String WARNING = "warning";
    
    /**
     * Indicates that the severity of this <code>Message</code> is "<code>info</code>".
     */
    String INFO = "info";
    
    /**
     * Returns the compiler message severity level.
     * 
     * @return One of <code>Message.ERROR</code>, <code>Message.WARNING</code>, or <code>Message.INFO</code>.
     * @see #ERROR
     * @see #WARNING
     * @see #INFO
     */
    String getLevel();

    /**
     * Returns the location of the file that contains the error.
     * 
     * @return String; <code>null</code> if the path is not applicable.
     */
    String getPath();
    
    /**
     * Returns the line number of the file that contains the error.
     * 
     * @return An integer; <code>-1</code> if the line number is not applicable.
     */
    int getLine();
    
    /**
     * Returns the column number of the file that contains the error.
     * 
     * @return An integer; <code>-1</code> if the column number is not applicable.
     */
    int getColumn();
    
    /**
     * Returns the message.
     * 
     * @return A string.
     */
    String toString();
}
