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

package flex2.compiler.as3.binding;

import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Paul Reilly
 */
public class ClassInfo extends Info
{
    private String className;
    private List variables; // List<QName>
    private ClassInfo baseClassInfo;
    private String baseClassName;
    private MultiName baseClassMultiName;

    public ClassInfo(String className)
    {
        this.className = className;

        int lastIndex = className.lastIndexOf(":");

        if (lastIndex > 0)
        {
            addImport(className.substring(0, lastIndex));
        }
    }

    public void addVariable(QName variableName)
    {
        if (variables == null)
        {
            variables = new ArrayList();
        }

        variables.add(variableName);
    }

    public boolean definesFunction(String functionName, boolean inherited)
    {
        boolean result = definesFunction(functionName);

        if (!result && inherited && (baseClassInfo != null))
        {
            result = baseClassInfo.definesFunction(functionName, inherited);
        }

        return result;
    }

    public boolean definesGetter(String getterName, boolean inherited)
    {
        boolean result = super.definesGetter(getterName);

        if (!result && inherited && (baseClassInfo != null))
        {
            result = baseClassInfo.definesGetter(getterName, inherited);
        }

        return result;        
    }

    public boolean definesSetter(String setterName, boolean inherited)
    {
        boolean result = super.definesSetter(setterName);

        if (!result && inherited && (baseClassInfo != null))
        {
            result = baseClassInfo.definesSetter(setterName, inherited);
        }

        return result;
    }

    public String getClassName()
    {
        return className;
    }

    public ClassInfo getBaseClassInfo()
    {
        return baseClassInfo;
    }

    public String getBaseClassName()
    {
        return baseClassName;
    }

    public MultiName getBaseClassMultiName()
    {
        if (baseClassMultiName == null)
        {
            baseClassMultiName = getMultiName(baseClassName);
        }

        return baseClassMultiName;
    }

    public boolean definesVariable(String variableName)
    {
        boolean result = false;

        if (variables != null)
        {
            Iterator iterator = variables.iterator();

            while ( iterator.hasNext() )
            {
                QName qName = (QName) iterator.next();
                if ( variableName.equals( qName.getLocalPart() ) )
                {
                    result = true;
                }
            }
        }

        if (!result && (baseClassInfo != null))
        {
            result = baseClassInfo.definesVariable(variableName);
        }

        return result;
    }

    public boolean extendsClass(String className)
    {
        boolean result = this.className.equals(className);

        if (!result && baseClassInfo != null)
        {
            result = baseClassInfo.extendsClass(className);
        }

        return result;
    }

    public boolean implementsInterface(String namespace, String interfaceName)
    {
        boolean result = super.implementsInterface(namespace, interfaceName);

        if (!result && (baseClassInfo != null))
        {
            result = baseClassInfo.implementsInterface(namespace, interfaceName);
        }

        return result;
    }

    public void setBaseClassInfo(ClassInfo baseClassInfo)
    {
        assert baseClassInfo != null;
        this.baseClassInfo = baseClassInfo;
    }

    public void setBaseClassName(String baseClassName)
    {
        this.baseClassName = baseClassName;
    }
}
