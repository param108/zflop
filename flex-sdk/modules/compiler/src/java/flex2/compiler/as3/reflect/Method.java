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

package flex2.compiler.as3.reflect;

import flex2.compiler.SymbolTable;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.MemberExpressionNode;
import macromedia.asc.parser.ParameterListNode;
import macromedia.asc.semantics.ReferenceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Clement Wong
 */
public final class Method implements flex2.compiler.abc.Method
{
	Method(FunctionDefinitionNode function, Class declaringClass)
	{
		name = functionName(function);

		returnTypeName = functionTypeName(function);

		if (function.attrs != null)
		{
			attributes = new Attributes(function.attrs);
		}

		ParameterListNode params = function.fexpr.signature.parameter;
		int size = params != null ? params.items.size() : 0;
		if (size != 0)
		{
			parameterNames = new String[size];
			parameterTypeNames = new String[size];
			parameterHasDefault = new boolean[size];
			TypeTable.createParameters(params, parameterNames, parameterTypeNames, parameterHasDefault);
		}

		/*
		size = function.fexpr.fexprs.size();
		if (size != 0)
		{
			methods = new QNameMap(); // QNameMap<QName, Method>
			getters = new QNameMap(); // QNameMap<QName, Method>
			setters = new QNameMap(); // QNameMap<QName, Method>
			TypeTable.createMethods(function.fexpr.fexprs, methods, getters, setters, internalNamespace, metadata, declaringClass);
		}
        */

		this.metadata = TypeTable.createMetaData(function);

		this.declaringClass = declaringClass;
	}

	private Attributes attributes;
	private String name;
	private String returnTypeName;
	private String[] parameterNames;
	private String[] parameterTypeNames;
	private boolean[] parameterHasDefault;
	private List metadata; // List<MetaData>
	private Class declaringClass;

	public flex2.compiler.abc.Attributes getAttributes()
	{
		return attributes;
	}

	public String getName()
	{
		return name;
	}

	public String getReturnTypeName()
	{
		return returnTypeName;
	}

	public String[] getParameterNames()
	{
		return parameterNames;
	}

	public String[] getParameterTypeNames()
	{
		return parameterTypeNames;
	}

	public boolean[] getParameterHasDefault()
	{
		return parameterHasDefault;
	}

	public List getMetaData() // List<flex2.compiler.abc.MetaData>
	{
		return metadata != null ? metadata : Collections.EMPTY_LIST;
	}

	public List getMetaData(String id) // List<flex2.compiler.abc.MetaData>
	{
		if (metadata == null)
		{
			return null;
		}
		else
		{
			List list = null;
			for (int i = 0, length = metadata.size(); i < length; i++)
			{
				if (id.equals(((MetaData) metadata.get(i)).getID()))
				{
					if (list == null)
					{
						list = new ArrayList();
					}
					list.add(metadata.get(i));
				}
			}
			return list;
		}
	}

	public flex2.compiler.abc.Class getDeclaringClass()
	{
		return declaringClass;
	}

	public static String functionName(FunctionDefinitionNode function)
	{
		return function.fexpr.identifier.name;
	}

	public static String functionTypeName(FunctionDefinitionNode function)
	{
		if (function.fexpr.signature.type != null)
		{
			return TypeTable.convertName(function.fexpr.signature.type.getName().toString());
		}
		else if (function.fexpr.signature.result != null)
		{
			ReferenceValue typeref = ((MemberExpressionNode) function.fexpr.signature.result).ref;
			if (typeref != null)
			{
				return TypeTable.convertName(typeref);
			}
			else
			{
				return SymbolTable.NOTYPE;
			}
		}
		else
		{
			return SymbolTable.NOTYPE;
		}
	}
}
