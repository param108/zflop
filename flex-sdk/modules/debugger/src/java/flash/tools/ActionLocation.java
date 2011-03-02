////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools;

import flash.swf.types.ActionList;
import flash.swf.actions.ConstantPool;
import flash.swf.actions.DefineFunction;

/**
 * ActionLocation record.  Used to contain
 * information regarding a specific location
 * within an action record.  
 * 
 * at and actions are typically guaranteed to 
 * be filled out.  The others are optional.
 * @see SwfActionContainer
 */
public class ActionLocation
{
	public ActionLocation()						{ init(-1, null, null, null, null); }
	public ActionLocation(ActionLocation base)	{ init(base.at, base.actions, base.pool, base.className, base.function); }

	void init(int p1, ActionList p2, ConstantPool p3, String p4, DefineFunction p5)
	{
		at = p1;
		actions = p2;
		pool = p3;
		className = p4;
		function = p5;
	}

	public int				at = -1;
	public ActionList		actions;
	public ConstantPool		pool;
	public String			className;
	public DefineFunction	function;
}

