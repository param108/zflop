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
 * @author Clement Wong
 */
public interface Property
{
	/**
	 * Property name
	 */
	String getName();

	/**
	 * Type.
	 * 
	 * If this is a getter, the returned value is the getter's return type.
	 * If this is a setter, the returned value is the type of the input argument of the setter.
	 */
	Type getType();

	/**
	 * Is this read only?
	 */
	boolean readOnly();

	/**
	 * Is this write only?
	 */
	boolean writeOnly();

	/**
	 *
	 */
	public boolean hasStatic();

	/**
	 * Does this property override supertype's property?
	 */
	public boolean hasOverride();

	/**
	 *
	 */
	public boolean hasPrivate();

	/**
	 *
	 */
	public boolean hasPublic();

	/**
	 *
	 */
	public boolean hasProtected();

	/**
	 *
	 */
	public boolean hasInternal();

	/**
	 *
	 */
	public boolean hasNamespace(String nsValue);

	// metadata

	/**
	 * [Inspectable]
	 */
	Inspectable getInspectable();

	/**
	 * [CollapseWhiteSpace]
	 */
	boolean collapseWhiteSpace();

	/**
	 * [Deprecated]
	 */
	Deprecated getDeprecated();

	/**
	 * [ChangeEvent]
	 */
	boolean hasChangeEvent(String name);

	/**
	 * [ArrayElementType]
	 *
	 * @return null if the array element type metadata is not available or if the array element type is not specified.
	 */
	String getArrayElementType();

	/**
	 * [InstanceType]
	 *
	 * @return null if the instance type metadata is not available or if the instance type is not specified.
	 */
	String getInstanceType();

	/**
	 * [PercentProxy]
	 */
	String getPercentProxy();
}
