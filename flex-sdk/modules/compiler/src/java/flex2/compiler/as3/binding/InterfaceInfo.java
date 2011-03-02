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

/**
 * @author Paul Reilly
 */
public class InterfaceInfo extends Info
{
    private String interfaceName;
    private InterfaceInfo baseInterfaceInfo;
    private String baseInterfaceName;
    private MultiName baseInterfaceMultiName;

    public InterfaceInfo(String interfaceName)
    {
        this.interfaceName = interfaceName;

        int lastIndex = interfaceName.lastIndexOf(":");

        if (lastIndex > 0)
        {
            addImport(interfaceName.substring(0, lastIndex));
        }
    }

    public boolean definesFunction(String functionName, boolean inherited)
    {
        boolean result = super.definesFunction(functionName);

        if (!result && inherited && (baseInterfaceInfo != null))
        {
            result = baseInterfaceInfo.definesFunction(functionName, inherited);
        }

        return result;
    }

    public boolean definesGetter(String getterName)
    {
        boolean result = super.definesGetter(getterName);

        if (!result && (baseInterfaceInfo != null))
        {
            result = baseInterfaceInfo.definesGetter(getterName);
        }

        return result;        
    }

    public boolean definesSetter(String setterName, boolean inherited)
    {
        boolean result = super.definesSetter(setterName);

        if (!result && inherited && (baseInterfaceInfo != null))
        {
            result = baseInterfaceInfo.definesSetter(setterName, inherited);
        }

        return result;
    }

    public String getInterfaceName()
    {
        return interfaceName;
    }

    public String getBaseInterfaceName()
    {
        return baseInterfaceName;
    }

    public MultiName getBaseInterfaceMultiName()
    {
        if (baseInterfaceMultiName == null)
        {
            baseInterfaceMultiName = getMultiName(baseInterfaceName);
        }

        return baseInterfaceMultiName;
    }

    public boolean extendsInterface(String namespace, String interfaceName)
    {
        boolean result = false;

        if (baseInterfaceInfo != null)
        {
            result = baseInterfaceInfo.getInterfaceName().equals(namespace + ":" + interfaceName);

            if (!result)
            {
                result = baseInterfaceInfo.extendsInterface(namespace, interfaceName);
            }
        }

        return result;
    }

    public boolean implementsInterface(String namespace, String interfaceName)
    {
        boolean result = super.implementsInterface(namespace, interfaceName);

        if (!result && (baseInterfaceInfo != null))
        {
            result = baseInterfaceInfo.implementsInterface(namespace, interfaceName);
        }

        return result;
    }

    public void setBaseInterfaceInfo(InterfaceInfo baseInterfaceInfo)
    {
        assert baseInterfaceInfo != null;
        this.baseInterfaceInfo = baseInterfaceInfo;
    }

    public void setBaseInterfaceName(String baseInterfaceName)
    {
        this.baseInterfaceName = baseInterfaceName;
    }

    public String toString()
    {
        return ("InterfaceInfo: " + interfaceName);
    }
}
