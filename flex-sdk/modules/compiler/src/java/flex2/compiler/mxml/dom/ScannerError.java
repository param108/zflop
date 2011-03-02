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

package flex2.compiler.mxml.dom;

/**
 * @author Clement Wong
 */
public class ScannerError extends Error
{
	ScannerError(int line, int col, String reason)
	{
		this.line = line;
		this.col = col;
		this.reason = reason;
	}

	private int line;
	private int col;
	private String reason;

	public int getLineNumber()
	{
		return line;
	}

	public int getColumnNumber()
	{
		return col;
	}

	public String getReason()
	{
		return reason;
	}
}
