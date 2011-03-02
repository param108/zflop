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

package flex2.compiler.as3;

import flex2.compiler.CompilationUnit;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.css.StyleConflictException;
import flex2.compiler.as3.reflect.MetaData;
import flex2.compiler.as3.reflect.NodeMagic;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
import flash.swf.tools.as3.EvaluatorAdapter;
import flash.util.Trace;

/**
 * @author Paul Reilly
 */
class StyleEvaluator extends EvaluatorAdapter
{
	private CompilationUnit unit;

	StyleEvaluator(CompilationUnit unit)
	{
		this.unit = unit;
	}

	public Value evaluate(Context context, MetaDataNode node)
	{
		if ("Style".equals(node.id))
		{
			if (NodeMagic.isClassDefinition(node))
			{
				processStyle(context, node);
			}
			else
			{
				context.localizedError2(node.pos(), new StyleMustAnnotateAClass());
			}
		}

		return null;
	}

    private void processStyle(Context context, MetaDataNode metaDataNode)
    {
        MetaData metaData = new MetaData(metaDataNode);
        String styleName = metaData.getValue("name");
	    String typeName = metaData.getValue("type");

        if (styleName == null)
        {
            // preilly: we should report this earlier in the process.
	        context.localizedError2(metaDataNode.pos(), new StyleHasMissingName());
        }

	    if (typeName != null)
	    {
		    unit.expressions.add(NameFormatter.toMultiName(typeName));
	    }

		registerStyle(context, metaDataNode, styleName, metaData);
    }

	/**
	 * add style into unit-wide list
	 */
	private void registerStyle(Context context, MetaDataNode metaDataNode, String name, MetaData md)
	{
		try
		{
			unit.styles.addStyle(name, md, unit.getSource());
		}
		catch (StyleConflictException e)
		{
			context.localizedWarning2(metaDataNode.pos(), e);

			if (Trace.error)
				e.printStackTrace();
		}

	}

	// error messages

	public static class StyleMustAnnotateAClass extends CompilerMessage.CompilerError
	{
		public StyleMustAnnotateAClass()
		{
			super();
		}
	}

	public static class StyleHasMissingName extends CompilerMessage.CompilerError
	{
		public StyleHasMissingName()
		{
			super();
		}
	}
}
