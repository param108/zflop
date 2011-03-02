////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import flex2.compiler.mxml.rep.BindingExpression;
import java.io.PrintWriter;
import java.io.StringWriter;
import macromedia.asc.parser.ArgumentListNode;

public class FunctionReturnWatcher extends EvaluationWatcher
{
    private String functionName;
    private boolean isNew;
    private ArgumentListNode args;

    public FunctionReturnWatcher(int id, BindingExpression bindingExpression,
                                 String functionName, ArgumentListNode args)
    {
        super(id, bindingExpression);
        this.functionName = functionName;
        this.args = args;
        isNew = false;
    }

    public boolean shouldWriteSelf()
    {
        return !isNew && (super.shouldWriteSelf() || !getChangeEvents().isEmpty());
    }

    public String getEvaluationPart()
    {
        String result = "";

        if (args != null)
        {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            PrefixedPrettyPrinter prettyPrinter = new PrefixedPrettyPrinter("target", printWriter);
        
            prettyPrinter.evaluate(null, args);
            result = stringWriter.toString();
        }

        return result;
    }

    public String getFunctionName()
    {
        return functionName;
    }
}
