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
 * The <code>ProgressMeter</code> interface lets you get periodic updates from the compiler
 * about the compilation progress.
 * 
 * <p>
 * Providing a progress meter to the compiler is optional. If you want to
 * know about compilation progress, you must implement this interface and provide an
 * instance of the implementation to the <code>Application.setProgressMeter()</code> and/or
 * <code>Library.setProgressMeter()</code> methods.
 * 
 * @version 2.0.1
 * @author Clement Wong
 */
public interface ProgressMeter
{
    /**
     * Notifies the caller that the compilation has begun. 
     */
    void start();
    
    /**
     * Notifies the caller that the compilation has ended.
     */
    void end();
    
    /**
     * Notifies the caller of the percentage of compilation done by the compiler.
     * @param n An integer. Valid values are <code>0</code> through <code>100</code>.
     */
    void percentDone(int n);
}
