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

public class ConditionTypeNotSupported extends CompilerWarning
{
    public String condition;

    public ConditionTypeNotSupported(String path, int line, String condition)
    {
        this.path = path;
        this.line = line;
        this.condition = condition;
    }
}
