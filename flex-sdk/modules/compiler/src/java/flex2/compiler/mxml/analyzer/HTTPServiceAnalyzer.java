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

package flex2.compiler.mxml.analyzer;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.AnalyzerAdapter;
import flex2.compiler.mxml.dom.RequestNode;
import flex2.compiler.util.CompilerMessage;

/**
 * @author Clement Wong
 */
public class HTTPServiceAnalyzer extends AnalyzerAdapter
{
	public HTTPServiceAnalyzer(CompilationUnit unit, Configuration configuration)
	{
		super(unit, configuration);
	}

	public void analyze(RequestNode node)
	{
		if (node.getAttributeCount() > 0)
		{
			log(node, new RequestNoAttributes());
		}
		super.analyze(node);
	}

	// error messages

	public static class RequestNoAttributes extends CompilerMessage.CompilerError
	{
		public RequestNoAttributes()
		{
			super();
		}
	}
}
