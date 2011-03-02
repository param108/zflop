////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
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
public interface Inspectable
{
	String INSPECTABLE      = "Inspectable";
	String ENUMERATION      = "enumeration";
	String IS_DEFAULT       = "isDefault";
	String CATEGORY         = "category";
	String IS_VERBOSE       = "isVerbose";
	String TYPE             = "type";
	String ARRAY_TYPE       = "arrayType";
	String OBJECT_TYPE      = "objectType";
	String ENVIRONMENT      = "environment";
	String FORMAT           = "format";
	String DEFAULT_VALUE    = "defaultValue";

	/**
	 * enumeration
	 */
	String[] getEnumeration();

	/**
	 * default value
	 */
	String getDefaultValue();

	/**
	 * default?
	 */
	boolean isDefault();

	/**
	 * category
	 */
	String getCategory();

	/**
	 * verbose?
	 */
	boolean isVerbose();

	/**
	 * type
	 */
    String getType();

	/**
	 * object type
	 */
	String getObjectType();

	/**
	 * array type
	 */
	String getArrayType();

	/**
	 * environment
	 */ 
	String getEnvironment();

	/**
	 * format
	 */ 
	String getFormat();
}
