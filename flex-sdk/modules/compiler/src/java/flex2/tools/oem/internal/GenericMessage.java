////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.tools.oem.internal;

import flex2.tools.oem.Message;

/**
 * @version 2.0.1
 * @author Clement Wong
 */
class GenericMessage implements Message
{
	GenericMessage(Message message)
	{
		this(message.getLevel(), message.getPath(), message.getLine(), message.getColumn(), message.toString());
	}
	
	GenericMessage(String level, String path, int line, int col, String message)
	{
		this.level = level;
		this.path = path;
		this.line = line;
		this.col = col;
		this.message = message;
	}
	
	private String level, path, message;
	private int line, col;
	
	public int getColumn()
	{
		return col;
	}

	public String getLevel()
	{
		return level;
	}

	public int getLine()
	{
		return line;
	}

	public String getPath()
	{
		return path;
	}
	
	public String toString()
	{
		return message;
	}
}
