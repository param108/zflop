////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import java.io.PrintWriter;
import macromedia.asc.parser.CallExpressionNode;
import macromedia.asc.parser.GetExpressionNode;
import macromedia.asc.parser.SelectorNode;
import macromedia.asc.parser.SetExpressionNode;
import macromedia.asc.parser.ThisExpressionNode;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
import flash.swf.tools.as3.PrettyPrinter;

public class PrefixedPrettyPrinter extends PrettyPrinter
{
    private String prefix;

    public PrefixedPrettyPrinter(String prefix, PrintWriter out)
    {
        super(out);
        this.prefix = prefix;
    }

    public Value evaluate(Context cx, CallExpressionNode node)
    {
        if (!node.is_new)
        {
            out.print(prefix + ".");
        }

        super.evaluate(cx, node);

        return null;
    }

	public Value evaluate(Context cx, GetExpressionNode node)
	{
        if ((node.base == null) && !isStatic(cx, node))
        {
            out.print(prefix + ".");
        }

        super.evaluate(cx, node);

		return null;
	}

	public Value evaluate(Context cx, SetExpressionNode node)
	{
        if ((node.base == null) && !isStatic(cx, node))
        {
            out.print(prefix + ".");
        }

        super.evaluate(cx, node);

		return null;
	}

    private boolean isStatic(Context cx, SelectorNode node)
    {
        boolean result = false;
        ReferenceValue ref = node.ref;

        if (ref != null)
        {
            if (ref.getType(cx).getName().toString().equals("Class") &&
                (ref.slot != null) && (ref.slot.getObjectValue() != null))
            {
                result = true;
            }
        }

        return result;
    }

	public Value evaluate(Context cx, ThisExpressionNode node)
    {
        out.print(prefix);
        return null;
    }
}
