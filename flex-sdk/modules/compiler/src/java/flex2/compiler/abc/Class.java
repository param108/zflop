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

package flex2.compiler.abc;

import flex2.compiler.util.QName;

import java.util.List;

/**
 * @author Clement Wong
 */
public interface Class
{
	Variable getVariable(String[] namespaces, String name, boolean inherited);

	QName[] getVariableNames();

	Method getMethod(String[] namespaces, String name, boolean inherited);

	QName[] getMethodNames();

	Method getGetter(String[] namespaces, String name, boolean inherited);

	QName[] getGetterNames();

	Method getSetter(String[] namespaces, String name, boolean inherited);

	QName[] getSetterNames();

	Namespace getNamespace(String nsName);

	String getName();

	String getSuperTypeName();

	String[] getInterfaceNames();

	Attributes getAttributes();

	/**
	 * all metadata [name] defined on this class. superclasses are scanned if (inherited)
	 */
	List getMetaData(String name, boolean inherited);

	/**
	 * Super classes are scanned.
	 */
    boolean implementsInterface(String interfaceName);

    boolean isSubclassOf(String baseName);

	boolean isInterface();

	// C: bad design. will revisit when i get a chance...
	void setTypeTable(Object obj);
}

