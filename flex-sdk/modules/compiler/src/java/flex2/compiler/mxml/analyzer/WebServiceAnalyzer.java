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
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.dom.OperationNode;
import flex2.compiler.mxml.dom.RequestNode;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.QName;

/**
 * @author Clement Wong
 */
public class WebServiceAnalyzer extends AnalyzerAdapter
{
	public WebServiceAnalyzer(CompilationUnit unit, Configuration configuration)
	{
		super(unit, configuration);
	}

	public void analyze(OperationNode node)
	{
		if (node.getAttribute("name") == null)
		{
			log(node, new OperationRequiresName());
		}
		super.analyze(node);
	}

    public void analyze(RequestNode node)
    {
		int attrCount = node.getAttributeCount();
		if (attrCount > 1 ||
				(attrCount == 1 && !((QName)(node.getAttributeNames().next())).getLocalPart().equals("format")))
        {
	        log(node, new RequestRequiresFormat());
        }
        super.analyze(node);
    }

    public void analyze(Node node)
    {
        super.analyze(node);
    }

	// error messages

	public static class OperationRequiresName extends CompilerMessage.CompilerError
	{
		public OperationRequiresName()
		{
			super();
		}
	}

	public static class RequestRequiresFormat extends CompilerMessage.CompilerError
	{
		public RequestRequiresFormat()
		{
			super();
		}
	}
}
