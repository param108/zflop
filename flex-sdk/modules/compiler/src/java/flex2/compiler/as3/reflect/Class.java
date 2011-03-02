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

package flex2.compiler.as3.reflect;

import flex2.compiler.SymbolTable;
import flex2.compiler.util.QName;
import flex2.compiler.util.QNameMap;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.TypeValue;
import macromedia.asc.util.ObjectList;

import java.util.*;

/**
 * @author Clement Wong
 */
public final class Class implements flex2.compiler.abc.Class
{
	Class(ClassDefinitionNode clsdef, TypeTable typeTable)
	{
		name = TypeTable.convertName(clsdef.cframe.builder.classname.toString());
        if (clsdef instanceof BinaryClassDefNode)
        {
	        String choice1 = null, choice2 = null;

	        if (clsdef.baseref != null)
	        {
		        choice1 = TypeTable.convertName(clsdef.baseref);
	        }

	        if (clsdef.cframe.baseclass != null)
	        {
                choice2 = TypeTable.convertName(clsdef.cframe.baseclass.builder.classname.toString());
	        }

	        if (choice1 == null || choice1.equals(choice2))
	        {
		        superTypeName = choice2;
	        }
	        else if (choice2 == null)
	        {
		        superTypeName = choice1;
	        }
	        else
	        {
		        assert false : name + " extends " + choice1 + " _or_ " + choice2 + "??";
	        }

	        isInterface = clsdef instanceof BinaryInterfaceDefinitionNode;
        }
		else // if (clsdef instanceof ClassDefinitionNode)
        {
	        if (clsdef.cframe.baseclass != null)
	        {
                superTypeName = TypeTable.convertName(clsdef.cframe.baseclass.builder.classname.toString());
	        }

	        isInterface = clsdef instanceof InterfaceDefinitionNode;
        }

		if (superTypeName == null)
		{
			superTypeName = SymbolTable.OBJECT.equals(name) ? null : SymbolTable.OBJECT;
		}

		if (clsdef.attrs != null)
		{
			attributes = new Attributes(clsdef.attrs);
		}

		int size = (clsdef.interfaces == null) ? 0 : clsdef.interfaces.values.size();
		if (size != 0)
		{
			interfaceNames = new String[size];
			for (int i = 0; i < size; i++)
			{
                ReferenceValue referenceValue = (ReferenceValue) clsdef.interfaces.values.get(i);
				if (referenceValue == null || referenceValue.slot == null)
				{
					continue;
				}

                TypeValue typeValue = (TypeValue) referenceValue.slot.getValue();
                if (typeValue != null)
                {
                    interfaceNames[i] = TypeTable.convertName( typeValue.name.toString() );
                }
                else
                {
	                assert false : "There is an interface without a TypeValue...";
                }
			}
		}

		this.metadata = TypeTable.createMetaData(clsdef);
		this.typeTable = typeTable;

		processDefinitions(clsdef.statements.items);
		processDefinitions(clsdef.staticfexprs);
		processDefinitions(clsdef.instanceinits);
	}

	private void processDefinitions(ObjectList items)
	{
		for (int i = 0, length = items == null ? 0 : items.size(); i < length; i++)
		{
			Node stmt = (Node) items.get(i);

			if (stmt instanceof VariableDefinitionNode)
			{
				VariableDefinitionNode var = (VariableDefinitionNode) stmt;
				if (variables == null)
				{
					variables = new QNameMap(); // HashMap<QName, Variable>
				}
				TypeTable.createVariable(var, variables, this);
			}
			else if (stmt instanceof FunctionDefinitionNode || stmt instanceof FunctionCommonNode)
			{
				FunctionDefinitionNode f = (stmt instanceof FunctionCommonNode) ? ((FunctionCommonNode) stmt).def : (FunctionDefinitionNode) stmt;
				if (f == null)
				{
					continue;
				}

				switch (f.name.kind)
				{
				case Tokens.GET_TOKEN:
					if (getters == null)
					{
						getters = new QNameMap(); // HashMap<String || QName, Method>
					}
					TypeTable.createMethod(f, getters, this);
					break;
				case Tokens.SET_TOKEN:
					if (setters == null)
					{
						setters = new QNameMap(); // HashMap<String || QName, Method>
					}
					TypeTable.createMethod(f, setters, this);
					break;
				// case Tokens.EMPTY_TOKEN:
				default:
					if (f.ref != null && "$construct".equals(f.ref.name))
					{
						continue;
					}

					if (methods == null)
					{
						methods = new QNameMap(); // HashMap<String || QName, Method>
					}
					TypeTable.createMethod(f, methods, this);
					break;
				}
			}
			else if (stmt instanceof NamespaceDefinitionNode)
			{
			}
		}
	}

	private String name;
	private String superTypeName;
	private String[] interfaceNames;
	private QNameMap variables; // Map<QName, Variable>
	private QNameMap methods; // Map<QName, Method>
	private QNameMap getters; // Map<QName, Method>
	private QNameMap setters; // Map<QName, Method>
	private Attributes attributes;
	private List metadata; // List<MetaData>
    private TypeTable typeTable;
	private boolean isInterface;

	public flex2.compiler.abc.Variable getVariable(String[] namespaces, String name, boolean inherited)
	{
        flex2.compiler.abc.Variable variable = null;
        int i = 0;

        while (i < namespaces.length)
        {
            if (variables != null)
            {
                variable = (flex2.compiler.abc.Variable) variables.get(namespaces[i], name);
                if (variable != null)
                {
                    break;
                }
            }
            i++;
        }

        if ((variable == null) && inherited)
        {
            flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

            if (superType != null)
            {
                variable = superType.getVariable(namespaces, name, true);
            }
        }

		return variable;
	}

	public QName[] getVariableNames()
	{
		return getQNameArray(variables);
	}

	public flex2.compiler.abc.Method getMethod(String[] namespaces, String name, boolean inherited)
	{
        flex2.compiler.abc.Method method = null;
        int i = 0;

        while (i < namespaces.length)
        {
            if (methods != null)
            {
                method = (flex2.compiler.abc.Method) methods.get(namespaces[i], name);
                if (method != null)
                {
                    break;
                }
            }
            i++;
        }

        if ((method == null) && inherited)
        {
            flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

            if (superType != null)
            {
                method = superType.getMethod(namespaces, name, true);
            }
        }

		return method;
	}

	public QName[] getMethodNames()
	{
		return getQNameArray(methods);
	}

	public flex2.compiler.abc.Method getGetter(String[] namespaces, String name, boolean inherited)
	{
        flex2.compiler.abc.Method getter = null;
        int i = 0;

        while (i < namespaces.length)
        {
            if (getters != null)
            {
                getter = (flex2.compiler.abc.Method) getters.get(namespaces[i], name);
                if (getter != null)
                {
                    break;
                }
            }
            i++;
        }

        if ((getter == null) && inherited)
        {
            flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

            if (superType != null)
            {
                getter = superType.getGetter(namespaces, name, true);
            }
        }

		return getter;
	}

	public QName[] getGetterNames()
	{
		return getQNameArray(getters);
	}

	public flex2.compiler.abc.Method getSetter(String[] namespaces, String name, boolean inherited)
	{
        flex2.compiler.abc.Method setter = null;
        int i = 0;

        while (i < namespaces.length)
        {
            if (setters != null)
            {
                setter = (flex2.compiler.abc.Method) setters.get(namespaces[i], name);
                if (setter != null)
                {
                    break;
                }
            }
            i++;
        }

        if ((setter == null) && inherited)
        {
            flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

            if (superType != null)
            {
                setter = superType.getSetter(namespaces, name, true);
            }
        }

		return setter;
	}

	public QName[] getSetterNames()
	{
		return getQNameArray(setters);
	}

	public flex2.compiler.abc.Namespace getNamespace(String nsName)
	{
		return null;
	}

	public String getName()
	{
		return name;
	}

	public String getSuperTypeName()
	{
		return superTypeName;
	}

	public String[] getInterfaceNames()
	{
		return interfaceNames;
	}

	public flex2.compiler.abc.Attributes getAttributes()
	{
		return attributes;
	}

	public List getMetaData(String id, boolean inherited)
	{
		return getMetaData(id, inherited, new ArrayList(inherited ? 10 : (metadata != null) ? metadata.size() : 1));
	}

	private List getMetaData(String id, boolean inherited, List list)
	{
		if (metadata != null)
		{
			for (int i = 0, length = metadata.size(); i < length; i++)
			{
				if (id.equals(((MetaData) metadata.get(i)).getID()))
				{
					list.add(metadata.get(i));
				}
			}
		}

		if (inherited)
		{
			flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

			if (superType != null)
			{
				if (superType instanceof Class)
				{
					((Class)superType).getMetaData(id, true, list);
				}
				else
				{
					list.addAll(superType.getMetaData(id, true));
				}
			}
		}

		return list;
	}

	public boolean implementsInterface(String interfaceName)
	{
	    boolean result = false;

	    if (interfaceNames != null)
	    {
	        int size = interfaceNames.length;

	        for (int i = 0; i < size; i++)
	        {
	            if (interfaceName.equals(interfaceNames[i]))
	            {
	                result = true;
	            }
	            else
	            {
	                Class interfaceType = (Class) typeTable.getClass(interfaceNames[i]);

	                if (interfaceType.isAssignableTo(interfaceName))
	                {
	                    result = true;
	                }
	            }
	        }
	    }

	    if (!result)
	    {
	        flex2.compiler.abc.Class superType = typeTable.getClass(superTypeName);

	        if (superType != null)
	        {
	            result = superType.implementsInterface(interfaceName);
	        }
	    }

	    return result;
	}

	public boolean isSubclassOf(String baseName)
	{
		if (SymbolTable.NOTYPE.equals(baseName))
		{
			return false;
		}
		else
		{
			return isAssignableTo(baseName);
		}
	}

	public boolean isInterface()
	{
		return isInterface;
	}

    public boolean isAssignableTo(String baseName)
    {
        if (SymbolTable.NOTYPE.equals(baseName) || getName().equals(baseName))
        {
            return true;
        }

        String superTypeName = getSuperTypeName();

        if (superTypeName != null && superTypeName.equals(baseName))
        {
            return true;
        }

        String[] interfaceNames = getInterfaceNames();

        for (int i = 0, length = (interfaceNames == null) ? 0 : interfaceNames.length; i < length; i++)
        {
            if (baseName != null && baseName.equals(interfaceNames[i]))
            {
                return true;
            }
        }

        if (superTypeName != null)
        {
            Class superType = (Class) typeTable.getClass(superTypeName);

            if ( superType.isAssignableTo(baseName) )
            {
                return true;
            }
        }

        for (int i = 0, length = (interfaceNames == null) ? 0 : interfaceNames.length; i < length; i++)
        {
            if (interfaceNames[i] != null)
            {
                Class interfaceType = (Class) typeTable.getClass(interfaceNames[i]);

                if (interfaceType.isAssignableTo(baseName))
                {
                    return true;
                }
            }
        }

        return false;
    }

	private QName[] getQNameArray(Map m)
	{
		if (m != null)
		{
			QName[] qNames = new QName[m.size()];
			int index = 0;

			for (Iterator i = m.keySet().iterator(); i.hasNext(); index++)
			{
				qNames[index] = (QName) i.next();
			}

			return qNames;
		}
		else
		{
			return null;
		}
	}

	public void setTypeTable(Object typeTable)
	{
		this.typeTable = (TypeTable) typeTable;
	}
}
