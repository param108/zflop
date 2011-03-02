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

package flex2.compiler.css;

import flex2.compiler.util.CompilerMessage.CompilerWarning;

public class SelectorTypeNotSupported extends CompilerWarning
{
    public String selector;

    public SelectorTypeNotSupported(String path, int line, String selector)
    {
        this.path = path;
        this.line = line;
        this.selector = selector;
    }
}
