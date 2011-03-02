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

package flex2.compiler.config;

import java.util.List;
import java.util.LinkedList;

/**
 * @author Roger Gonzalez
 */
public class ConfigurationValue
{
    protected ConfigurationValue( ConfigurationBuffer buffer, String var, List args, String source, int line, String context )
    {
        this.buffer = buffer;
        this.var = var;
        this.args = new LinkedList( args );
        this.source = source;
        this.line = line;
        this.context = context;
    }

    /**
     * getArgs
     *
     * @return list of values provided, in schema order
     */
    public final List getArgs()
    {
        return args;
    }

    /**
     * getBuffer
     *
     * @return a handle to the associated buffer holding this value
     */
    public final ConfigurationBuffer getBuffer()
    {
        return buffer;
    }

    /**
     * getSource
     *
     * @return a string representing the origin of this value, or null if unknown
     */
    public final String getSource()
    {
        return source;
    }

    /**
     * getLine
     *
     * @return the line number of the origin of this value, or -1 if unknown
     */
    public final int getLine()
    {
        return line;
    }

    /**
     * getVar
     *
     * @return the full name of this configuration variable in the hierarchy
     */
    public final String getVar()
    {
        return var;
    }

    /**
     * getContext
     *
     * @return the path of the enclosing context where the variable was set
     * (i.e. the directory where the config file was found)
     */
    public final String getContext()
    {
        return context;
    }

    private final ConfigurationBuffer buffer;
    private final String var;
    private final List args;
    private final String source;
    private final int line;
    private final String context;
}
