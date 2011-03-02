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

package flex2.compiler.mxml.builder;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.InlineComponentNode;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.Model;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;

/**
 * @author Paul Reilly
 */
class InlineComponentBuilder extends Builder
{
	InlineComponentBuilder(CompilationUnit unit,
						   TypeTable typeTable,
						   Configuration configuration,
						   MxmlDocument document,
						   boolean topLevel)
	{
		super(unit, typeTable, configuration, document);
		this.topLevel = topLevel;
	}

	protected boolean topLevel;
	Model rvalue;

	public void analyze(InlineComponentNode node)
	{
		QName classQName = node.getClassQName();

		rvalue = factoryFromClass(NameFormatter.toDot(classQName), node.beginLine);

		if (node.getAttribute("id") != null || topLevel)
		{
			registerModel(node, rvalue, topLevel);
		}
	}

	public Model getRValue()
	{
		return rvalue;
	}
}
