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

package flex2.compiler.as3.genext;

import macromedia.asc.parser.Node;
import macromedia.asc.util.Context;
import flash.swf.tools.as3.EvaluatorAdapter;

/**
 * 
 */
public class PositionResetEvaluator extends EvaluatorAdapter
{
    private int position;

	public PositionResetEvaluator(int position)
	{
        this.position = position;
	}

	public boolean checkFeature(Context cx, Node node)
	{
        node.pos(position);
		return true;
	}
}
