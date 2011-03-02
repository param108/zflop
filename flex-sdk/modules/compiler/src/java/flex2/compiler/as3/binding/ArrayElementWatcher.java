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

public class ArrayElementWatcher extends EvaluationWatcher
{
    private ArgumentListNode args;

    public ArrayElementWatcher(int id, BindingExpression bindingExpression, ArgumentListNode args)
    {
        super(id, bindingExpression);
        this.args = args;
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

    public boolean getShouldWriteChildren()
    {
        return shouldWriteSelf();
    }

    public boolean shouldWriteSelf()
    {
        boolean result = true;
        Watcher parent = getParent();

        if ((parent != null) && ((parent instanceof XMLWatcher) || !parent.shouldWriteSelf()))
        {
            result = false;
        }

        return result;
    }
}
