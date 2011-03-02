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
import flex2.compiler.util.QName;
import flex2.compiler.util.QNameList;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.TypeInfo;
import macromedia.asc.util.ObjectList;

import java.util.*;

/**
 * @author Clement Wong
 */
public class TypeTable
{
	public TypeTable(SymbolTable symbolTable)
	{
		this.symbolTable = symbolTable;
	}

	private SymbolTable symbolTable;

	public flex2.compiler.abc.Class getClass(String className)
	{
		return symbolTable.getClass(className);
	}

	// C: TypeTable should not expose SymbolTable, if possible.
    public SymbolTable getSymbolTable()
    {
        return symbolTable;
    }

	public static String convertName(ReferenceValue typeref)
	{
		ObjectValue ns = (ObjectValue) typeref.namespaces.first();
		if( ns != null && ns.name.length() > 0 )
		{
			StringBuffer value = new StringBuffer(ns.name.length() + 1 + typeref.name.length());
		    value.append(ns.name).append(':').append(typeref.name);
			return value.toString();
		}
		else
		{
			return typeref.name;
		}
	}

	public static String convertName(String name)
	{
		// C: Warning: This doesn't handle the asc debug name full syntax...

		/*
		int dollarSign = -1, colon = -1;
		String pkg_name = null, def_name = null;

		if ((colon = name.indexOf(':', dollarSign + 1)) > 0)
		{
			def_name = name.substring(colon + 1);

			if (dollarSign == -1)
			{
				pkg_name = name.substring(0, colon);
			}
		}
		else
		{
			def_name = name;
		}

		return (pkg_name == null) ? def_name : pkg_name + ":" + def_name;
		*/

		return name.intern();
	}

	public final Map createClasses(ObjectList clsdefs, QNameList toplevelDefinitions)
	{
		Map classes = new HashMap();
		for (int i = 0, size = clsdefs.size(); i < size; i++)
		{
			ClassDefinitionNode clsdef = (ClassDefinitionNode) clsdefs.get(i);
			macromedia.asc.semantics.QName qName = clsdef.cframe.builder.classname;

			if (toplevelDefinitions.contains(qName.ns.name, qName.name))
			{
				createClass(clsdef, classes);
			}
		}
		return classes;
	}

	// ClassDefinitionNode clsdefs, Map<QName, Class>
	final void createClass(ClassDefinitionNode clsdef, Map classes)
	{
		flex2.compiler.abc.Class cls = new Class(clsdef, this);
		classes.put(cls.getName(), cls);
	}

	// FunctionDefinitionNode, Map<QName, Method>
	static void createMethod(FunctionDefinitionNode f, Map methods, Class declaringClass)
	{
		flex2.compiler.abc.Method method = new Method(f, declaringClass);

        String namespace = getNamespace(method.getAttributes());

        if (methods.put(new QName(namespace, method.getName()), method) != null)
		{
			// ThreadLocalToolkit.logWarning("duplicate method... " + method.getName() + " in " + declaringClass.getName());
		}
	}

	// ObjectList<Node>, Map<QName, Variable>, List<MetaData>
	static void createVariable(VariableDefinitionNode var, Map variables, Class declaringClass)
	{
		List meta = createMetaData(var); // List<MetaData>

		// for (Node n : var.list.items)
		for (int k = 0, len = var.list.items.size(); k < len; k++)
		{
			VariableBindingNode binding = (VariableBindingNode) var.list.items.get(k);
			Variable variable = new Variable(var.attrs, binding, meta, declaringClass);
            String namespace = getNamespace(variable.getAttributes());
            variables.put(new QName(namespace, variable.getName()), variable);
		}
	}

	static void createParameters(ParameterListNode params, String[] names, String[] typeNames, boolean[] hasDefault)
	{
		for (int i = 0, size = params.size(); i < size; i++)
		{
			ParameterNode item = (ParameterNode)params.items.get(i);

			//	name
			names[i] = item.ref != null ? item.ref.name : "";

			//	has default
			hasDefault[i] = item.init != null;

			//	type name - could get from item.typeref?
			if (params.types != null && i < params.types.size())
			{
				TypeInfo paramType = (TypeInfo) params.types.get(i);
				typeNames[i] = convertName(paramType.getName().toString());
			}
			else
			{
				ParameterNode param = (ParameterNode) params.items.get(i);
				if (param != null && param.typeref != null)
				{
					typeNames[i] = convertName(param.typeref);
				}
				else if (param != null)
				{
					typeNames[i] = SymbolTable.NOTYPE;
				}
				else
				{
					assert false : "Expected ParameterNode...";
				}
			}
		}
	}

	// static List createMetaData(List metadata, DefinitionNode def) // List<MetaData>, List<MetaDataNode>
	static List createMetaData(DefinitionNode def) // List<MetaDataNode>
	{
		List list = null;

		for (int i = 0, size = def.metaData != null ? def.metaData.items.size() : 0; i < size; i++)
		{
			MetaDataNode n = (MetaDataNode) def.metaData.items.get(i);
			if (list == null)
			{
				list = new ArrayList();
			}
			list.add(new MetaData(n));
		}

		return list;
	}

    private static String getNamespace(flex2.compiler.abc.Attributes attributes)
    {
        String result = QName.DEFAULT_NAMESPACE;

        if (attributes != null)
        {
            Iterator iterator = attributes.getNamespaces();

            if ((iterator != null) && iterator.hasNext())
            {
                result = (String) iterator.next();
            }
        }

        return result;
    }
}
