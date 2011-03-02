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

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;

/**
 * wraps array coercion logic, basically
 */
public class TypeCompatibility
{
	public static final int Ok = 0;									//	rvalue type is assignable to lavlue type
	public static final int OkCoerceToArray = 1;					//	rvalue type is assignable to lvalue's array elem type
	public static final int ErrRTypeNotAssignableToLType = 2;		//	rvalue type is not assignable to lvalue type
	public static final int ErrLTypeNotMultiple = 3;				//	multiple rvalues, and lvalue type is not > Array
	public static final int ErrSingleRValueNotArrayOrArrayElem = 4;	//	rvalue type is not assignable to either lvalue type or lvalue's array elem type
	public static final int ErrMultiRValueNotArrayElem = 5;			//	multiple rvalues, and rvalue type is not assignable to lvalue's array elem type

	/**
	 * Check type compatibility modulo Array coercion, and return correctness result and necessary action.
	 * In a nutshell, the algorithm allows implicit array coercion if the lvalue type is either (a) exactly Array, or
	 * (b) a supertype of Array. The only variation between (a) and (b) comes when the rvalue is a singleton. In
	 * (a), a single rvalue is coerced to [rvalue]; in (b) it's left alone.
	 *
	 * <p>Algorithm:<pre>
	 * 	1. if lvalue type is <strong>exactly</strong> Array, then
	 * 		if rvalue is a singleton, then
	 * 			rvalue must either be a subtype of Array (ok), or
	 * 			rvalue must be a subtype of lvalue's array element type (coerce)
	 * 		else (rvalue is one of a collection)
	 * 			rvalue must be a subtype of lvalue's array element type (coerce)
	 * 	2. else if lvalue type is a <strong>supertype</strong> of Array, then
	 * 		if rvalue is a singleton, then
	 * 			rvalue must be a subtype of lvalue type (ok)
	 * 		if rvalue is one of a collection, then
	 * 			rvalue must be a subtype of lvalue's array element type (coerce)
	 * 	3. else (lvalue type is not Array or a supertype of Array), then
	 * 		rvalue must be a singleton, and rvalue type must be a subtype of lvalue type (ok)
	 * </pre>
	 */
	public static int check(Type lvalueType, Type lvalueArrayElemType, Type rvalueType, boolean rvalueIsSingleton)
	{
		TypeTable typeTable = lvalueType.getTypeTable();
		//	assert typeTable == rvalueType.getTypeTable();

		if (typeTable.arrayType.equals(lvalueType))
		{
			if (rvalueIsSingleton)
			{
				return rvalueType.isAssignableTo(typeTable.arrayType) ? Ok :
						checkSingleton(typeTable, lvalueArrayElemType, rvalueType) ? OkCoerceToArray :
						ErrSingleRValueNotArrayOrArrayElem;
			}
			else
			{
				return checkSingleton(typeTable, lvalueArrayElemType, rvalueType) ? OkCoerceToArray :
					ErrMultiRValueNotArrayElem;
			}
		}
		else if (typeTable.arrayType.isAssignableTo(lvalueType))
		{
			if (rvalueIsSingleton)
			{
				return checkSingleton(typeTable, lvalueType, rvalueType) ? Ok :
					ErrRTypeNotAssignableToLType;
			}
			else
			{
				return checkSingleton(typeTable, lvalueArrayElemType, rvalueType) ? OkCoerceToArray :
					ErrMultiRValueNotArrayElem;
			}
		}
		else
		{
			return !rvalueIsSingleton ? ErrLTypeNotMultiple :
					checkSingleton(typeTable, lvalueType, rvalueType) ? Ok :
					ErrRTypeNotAssignableToLType;
		}
	}

	/**
	 * Note that there is no special 'OkCoerceToFactory'. This is a somewhat cheap way to avoid handling the interplay
	 * between array and factory coercion here. It's possible because caller handles this case in a very simple way currently.
	 * TODO should revisit.
	 */
	private static boolean checkSingleton(TypeTable typeTable, Type ltype, Type rtype)
	{
		return rtype.isAssignableTo(ltype) || (StandardDefs.isIFactory(ltype) && rtype.equals(typeTable.classType));
	}
}
