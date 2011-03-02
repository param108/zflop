////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep.init;

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.rep.MovieClip;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.lang.StandardDefs;

/**
 * TODO remove when you-know-what finally happens
 */
public class VisualChildInitializer extends ValueInitializer
{
	public VisualChildInitializer(MovieClip movieClip)
	{
		super(movieClip, movieClip.getXmlLineNumber());
	}

	public Type getLValueType()
	{
		return ((Model)value).getType();
	}

	public String getAssignExpr(String lvalueBase)
	{
		if (StandardDefs.isRepeater(getLValueType()))
		{
			//	parent must have property mx_internal::childRepeaters.
			/**
			 * TODO: uncomment mx_internal namespace argument, once bug ??????
			 * (user namespaces not showing up in SymbolTable property info) is fixed
			 */
			assert ((Model)value).getParent().getType().getProperty(
					/* StandardDefs.NAMESPACE_MX_INTERNAL_URI, */ StandardDefs.PROP_CONTAINER_CHILDREPEATERS) != null :
				"Repeater parent lacks childRepeaters[] property";

			String cr = lvalueBase + "." + StandardDefs.NAMESPACE_MX_INTERNAL_LOCALNAME + "::" + StandardDefs.PROP_CONTAINER_CHILDREPEATERS;
			return "(" + cr + " ? " + cr + " : (" + cr + "=[])).push(" + getValueExpr() + ")";
		}
		else
		{
			return lvalueBase + ".addChild(" + getValueExpr() + ")";
		}
	}
}
