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
 * The <code>Logger</code> interface is the Flex compiler logging mechanism for OEM applications.
 * You implement this interface and provide an instance of the implementation to the 
 * <code>Application.setLogger()</code> and/or <code>Library.setLogger()</code> methods.
 * 
 * <p>
 * The Flex compiler API exposes warnings and errors as <code>Message</code> objects.
 * You can use the <code>Message.getClass().getName()</code> method to differentiate
 * between message types programmatically.
 * 
 * <p>
 * The compiler utilizes some third-party libraries that use error-code-based
 * logging systems. As a result, the <code>log()</code> method also supports error codes.
 * 
 * @version 2.0.1
 * @author Clement Wong
 */
public interface Logger
{
    // C: Ideally, errorCode and source should be in Message...
    // void log(Message message);
    
    /**
     * Logs a compiler message.
     *  
     * @param message An object that implements the <code>flex2.tools.oem.Message</code> interface.
     * @param errorCode Error code. -1 if an error code is not available.
     * @param source Source code line number specified by the <code>message.getLine()</code> method.
     *               <code>null</code> if the compiler message is not associated with any source file.
     */
    void log(Message message, int errorCode, String source);
}
