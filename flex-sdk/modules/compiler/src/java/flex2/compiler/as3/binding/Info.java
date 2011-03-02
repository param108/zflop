////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Paul Reilly
 */
abstract class Info
{
    private Set imports; // List<String>
    private Map qualifiedImports; // Map<String, String>
    private List interfaceNames; // List<String>
    private List interfaceMultiNames; // List<MultiName>
    private List interfaceInfoList; // List<InterfaceInfo>
    private List functions; // List<QName>
    private List getters; // List<QName>
    private List setters; // List<QName>

    public Info()
    {
    }

    public void addFunction(QName functionName)
    {
        assert functionName != null;

        if (functions == null)
        {
            functions = new ArrayList();
        }

        functions.add(functionName);
    }

    public void addGetter(QName getterName)
    {
        assert getterName != null;

        if (getters == null)
        {
            getters = new ArrayList();
        }

        getters.add(getterName);
    }

    void addImport(String importName)
    {
        assert importName != null;

        if (imports == null)
        {
            imports = new TreeSet();
        }

        imports.add(importName);
    }

    public void addInterfaceMultiName(String[] namespaces, String interfaceName)
    {
        assert namespaces != null && interfaceName != null;

        if (interfaceMultiNames == null)
        {
            interfaceMultiNames = new ArrayList();
        }

        interfaceMultiNames.add( new MultiName(namespaces, interfaceName) );
    }

    public void addInterfaceMultiName(String namespace, String interfaceName)
    {
        assert namespace != null && interfaceName != null;

        if (interfaceMultiNames == null)
        {
            interfaceMultiNames = new ArrayList();
        }

        interfaceMultiNames.add( new MultiName(namespace, interfaceName) );
    }

    void addInterfaceName(String interfaceName)
    {
        assert interfaceName != null;

        if (interfaceNames == null)
        {
            interfaceNames = new ArrayList();
        }

        interfaceNames.add(interfaceName);
    }

    public void addInterfaceInfo(InterfaceInfo interfaceInfo)
    {
        assert interfaceInfo != null;

        if (interfaceInfoList == null)
        {
            interfaceInfoList = new ArrayList();
        }

        interfaceInfoList.add(interfaceInfo);
    }

    void addQualifiedImport(String localPart, String namespace)
    {
        assert (localPart != null) && (localPart.length() > 0) && (namespace != null);

        if (qualifiedImports == null)
        {
            qualifiedImports = new TreeMap();
        }

        qualifiedImports.put(localPart, namespace);
    }

    public void addSetter(QName setterName)
    {
        if (setters == null)
        {
            setters = new ArrayList();
        }

        setters.add(setterName);
    }

    boolean definesFunction(String functionName)
    {
        boolean result = false;

        if (functions != null)
        {
            Iterator iterator = functions.iterator();

            while ( iterator.hasNext() )
            {
                QName qName = (QName) iterator.next();
                if ( functionName.equals( qName.getLocalPart() ) )
                {
                    result = true;
                }
            }
        }

        return result;
    }

    boolean definesGetter(String getterName)
    {
        boolean result = false;

        if (getters != null)
        {
            Iterator iterator = getters.iterator();

            while ( iterator.hasNext() )
            {
                QName qName = (QName) iterator.next();
                if ( getterName.equals( qName.getLocalPart() ) )
                {
                    result = true;
                }
            }
        }

        return result;
    }

    boolean definesSetter(String setterName)
    {
        boolean result = false;

        if (setters != null)
        {
            Iterator iterator = setters.iterator();

            while ( iterator.hasNext() )
            {
                QName qName = (QName) iterator.next();
                if ( setterName.equals( qName.getLocalPart() ) )
                {
                    result = true;
                }
            }
        }

        return result;
    }

    public List getFunctionNames()
    {
        return functions;
    }

    public Set getImports()
    {
        return imports;
    }

    List getInterfaceMultiNames()
    {
        if (interfaceMultiNames == null)
        {
            interfaceMultiNames = new ArrayList();

            if (interfaceNames != null)
            {
                Iterator iterator = interfaceNames.iterator();

                while ( iterator.hasNext() )
                {
                    String interfaceName = (String) iterator.next();

                    MultiName interfaceMultiName = getMultiName(interfaceName);

                    interfaceMultiNames.add(interfaceMultiName);
                }
            }
        }

        return interfaceMultiNames;
    }

    public MultiName getMultiName(String name)
    {
		assert name != null : "Info.getMultiName called on null";

		MultiName result;

        int lastIndex = name.lastIndexOf(":");

        if (lastIndex < 0)
        {
            lastIndex = name.lastIndexOf(".");
        }

        if (lastIndex > 0)
        {
            result = new MultiName(new String[] {name.substring(0, lastIndex)},
                                   name.substring(lastIndex + 1));
        }
        else if ((qualifiedImports != null) && qualifiedImports.containsKey(name))
        {
            result = new MultiName(new String[] {(String) qualifiedImports.get(name)}, name);
        }
        else if (imports != null)
        {
            String[] namespaces = new String[imports.size() + 1];
            imports.toArray(namespaces);
            namespaces[imports.size()] = "";
            result = new MultiName(namespaces, name);
        }
        else
        {
            result = new MultiName(name);
        }

        return result;
    }

    boolean implementsInterface(String namespace, String interfaceName)
    {
        boolean result = false;

        assert (((interfaceMultiNames == null) && (interfaceInfoList == null)) ||
                ((interfaceMultiNames != null) && (interfaceInfoList != null) &&
                 (interfaceInfoList.size() == interfaceMultiNames.size()))) :
                "Info.implementsInterface: interfaceInfoList = " + interfaceInfoList +
                ", interfaceMultiNames = " + interfaceMultiNames;

        if (interfaceInfoList != null)
        {
            Iterator iterator = interfaceInfoList.iterator();

            while ( iterator.hasNext() )
            {
                InterfaceInfo interfaceInfo = (InterfaceInfo) iterator.next();

                if (interfaceInfo.getInterfaceName().equals(namespace + ":" + interfaceName))
                {
                    result = true;
                }
                else if (interfaceInfo.extendsInterface(namespace, interfaceName))
                {
                    result = true;
                }
                else if (interfaceInfo.implementsInterface(namespace, interfaceName))
                {
                    result = true;
                }
            }
        }

        return result;
    }
}
