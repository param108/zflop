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

package flex2.compiler.mxml.reflect;

/**
 * The facade for the MXML compiler to AS3 type. More API methods can be added but they must be meaningful to
 * MXML parsing, semantic analysis and code generation.
 *
 * @author Clement Wong
 */
public interface Type
{
	/**
	 * Type name. AS3-compatible fully-qualified class name.
	 */
	String getName();

	/**
	 * return type table
	 */
	TypeTable getTypeTable();

	/**
	 * Super type
	 */
	Type getSuperType();

	/**
	 * Interfaces
	 */
	Type[] getInterfaces();

	/**
	 * Property = variable | [getter]/[setter]
	 * Searches all standard namespaces: public, protected, internal, private
	 */
	Property getProperty(String name);

	/**
	 * Property = variable | [getter]/[setter]
	 * Searches specified namespace
	 */
	Property getProperty(String namespace, String name);

	/**
	 * Property = variable | [getter]/[setter]
	 * Searches specified namespaces
	 */
	Property getProperty(String[] namespaces, String name);

	/**
	 *
	 */
	public boolean hasStaticMember(String name);

	/**
	 * [Event]
	 */
	Event getEvent(String name);

	/**
	 * [Effect]
	 */
	Effect getEffect(String name);

	/**
	 * [Style]
	 */
	Style getStyle(String name);

    /**
     * [Frame(loaderClass=...)]
     * Might support other Frame stuff in the future, requiring some refactoring.
     */
    public String getLoaderClass();

	/**
	 * [Obsolete]
	 */
	boolean hasObsolete(String name);

	/**
	 * [DefaultProperty]
	 * Note: returns name as given in metadata - may or may not correctly specify a public property 
	 */
	Property getDefaultProperty();

	/**
	 * [MaxChildren]
	 */
	int getMaxChildren();

	/**
	 * Dynamic type
	 */
	boolean hasDynamic();

	/**
	 * Return true if this type is assignable to 'baseType'.
	 */
	boolean isAssignableTo(Type baseType);

	/**
	 * Return true if this type is assignable to 'baseName'.
	 */
	boolean isAssignableTo(String baseName);

	/**
	 * Return true if this type is a subclass of 'baseType'.
	 */
	boolean isSubclassOf(Type baseType);

	/**
	 * Return true if this type is a subclass of 'baseName'.
	 */
	boolean isSubclassOf(String baseName);
}
