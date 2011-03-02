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

package flex2.compiler.css;

import flex2.compiler.util.CompilerMessage;
import flex2.compiler.Source;

public class StyleConflictException extends CompilerMessage.CompilerError
{
	public String style;
	public String source;

	public StyleConflictException(String style, Source source)
	{
		this.style = style;
		this.source = source != null ? source.getNameForReporting() : "";
	}

}
