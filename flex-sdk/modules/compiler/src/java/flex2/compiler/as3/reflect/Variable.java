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
import macromedia.asc.parser.AttributeListNode;
import macromedia.asc.parser.Tokens;
import macromedia.asc.parser.VariableBindingNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Clement Wong
 */
public final class Variable implements flex2.compiler.abc.Variable
{
	Variable(AttributeListNode attrs, VariableBindingNode binding, List metadata, // List<MetaData>
			 Class declaringClass)
	{
		name = variableName(binding);

		type = variableTypeName(binding);

		if (attrs != null)
		{
			attributes = new Attributes(attrs);

            // preilly: The AttributeListNode's hasConst is never
            // used, so we do the following to check for const's.
            if (binding.kind == Tokens.CONST_TOKEN)
            {
                attributes.setHasConst();
            }
		}

		this.metadata = metadata;

		this.declaringClass = declaringClass;
	}

	private Attributes attributes;
	private String name;
	private String type;
	private List metadata;
	private Class declaringClass;

	public flex2.compiler.abc.Attributes getAttributes()
	{
		return attributes;
	}

	public String getName()
	{
		return name;
	}

	public String getTypeName()
	{
		return type;
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

	public static String variableName(VariableBindingNode binding)
	{
		return binding.variable.identifier.name;
	}

	public static String variableTypeName(VariableBindingNode binding)
	{
		if (binding.variable.identifier.ref != null)
		{
			return TypeTable.convertName(binding.variable.identifier.ref.slot.getType().getName().toString());
		}
		else if (binding.typeref != null)
		{
			return TypeTable.convertName(binding.typeref);
		}
		else
		{
			return SymbolTable.NOTYPE;
		}
	}
}
