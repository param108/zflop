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

package flash.tools.debugger.concrete;

import java.io.File;

/**
 * @author mmorearty
 */
public class ActiveXPlayer extends AbstractPlayer
{
	public ActiveXPlayer(File iexploreExe, File path)
	{
		super(iexploreExe, path);
	}

	/*
	 * @see flash.tools.debugger.Player#getType()
	 */
	public int getType()
	{
		return ACTIVEX;
	}
}
