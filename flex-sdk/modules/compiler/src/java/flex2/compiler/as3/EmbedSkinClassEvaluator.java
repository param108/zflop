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

package flex2.compiler.as3;

import flash.swf.tools.as3.EvaluatorAdapter;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Transcoder;
import flex2.compiler.as3.reflect.MetaData;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.NameFormatter;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.parser.Node;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

/**
 * Evaluator that is meant to be used during the parse1 phase to
 * insure that skin classes are parsed, so the EmbedEvaluator, which
 * runs in parse2 phase, can look up information about the skin class.
 *
 * @author Paul Reilly
 */
class EmbedSkinClassEvaluator extends EvaluatorAdapter
{
    private CompilationUnit unit;

    EmbedSkinClassEvaluator(CompilationUnit unit)
    {
        this.unit = unit;
    }

    public Value evaluate(Context context, MetaDataNode node)
    {
        if ("Embed".equals(node.id))
        {
            MetaData metaData = new MetaData(node);
            int len = metaData.count();

            for (int i = 0; i < len; i++)
            {
                String key = metaData.getKey(i);

                if ((key != null) && key.equals(Transcoder.SKINCLASS))
                {
                    String skinClass = metaData.getValue(i);
                    unit.inheritance.add(new MultiName(NameFormatter.toColon(skinClass)));
                }
            }
        }

        return null;
    }
}
