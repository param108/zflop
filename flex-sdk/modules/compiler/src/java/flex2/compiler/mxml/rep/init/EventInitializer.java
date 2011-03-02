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

import flex2.compiler.mxml.gen.CodeFragmentList;
import flex2.compiler.mxml.gen.TextGen;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.rep.EventHandler;
import flex2.compiler.util.NameFormatter;
import org.apache.commons.collections.iterators.SingletonIterator;

import java.util.Iterator;

/**
 *
 */
public class EventInitializer implements Initializer
{
	private final EventHandler handler;

	public EventInitializer(EventHandler handler)
	{
		this.handler = handler;
	}

	public String getName()
	{
		return handler.getName();
	}

	public Type getLValueType()
	{
		return handler.getType();
	}

	public int getLineRef()
	{
		return handler.getXmlLineNumber();
	}

	public boolean isBinding()
	{
		return false;
	}

	public String getValueExpr()
	{
		return handler.getDocumentFunctionName();
	}

	public String getAssignExpr(String lvalueBase)
	{
		return lvalueBase + ".addEventListener(" + TextGen.quoteWord(getName()) + ", " + getValueExpr() + ")";
	}

	public boolean hasDefinition()
	{
		return true;
	}

	public Iterator getDefinitionsIterator()
	{
		return new SingletonIterator(getDefinitionBody());
	}

	protected CodeFragmentList getDefinitionBody()
	{
		int line = getLineRef();
		CodeFragmentList list = new CodeFragmentList();

		//	TODO public only for UIObjectDescriptor, which takes names rather than function refs
		list.add("/**", line);
		list.add(" * @private", line);
		list.add(" **/", line);
		list.add("public function ", handler.getDocumentFunctionName(), "(event:", NameFormatter.toDot(handler.getType().getName()), "):void", line);
		list.add("{", line);
		list.add("\t", handler.getEventHandlerText(), line);
		list.add("}", line);

		return list;
	}
}
