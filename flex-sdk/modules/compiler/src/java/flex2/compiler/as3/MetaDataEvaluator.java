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

package flex2.compiler.as3;

import flash.swf.tools.as3.EvaluatorAdapter;
import flex2.compiler.util.CompilerMessage.CompilerError;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

public class MetaDataEvaluator extends EvaluatorAdapter
{
	public Value evaluate(Context context, MetaDataNode metaDataNode)
	{
        if (metaDataNode.def == null)
        {
            context.localizedError2(metaDataNode.pos(), new MetaDataRequiresDefinition());
        }

        return null;
    }

	public static class MetaDataRequiresDefinition extends CompilerError {}
}
