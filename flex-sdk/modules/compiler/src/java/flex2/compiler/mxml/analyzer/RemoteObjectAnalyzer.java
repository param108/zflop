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
import flex2.compiler.mxml.dom.ArgumentsNode;
import flex2.compiler.mxml.dom.MethodNode;
import flex2.compiler.util.CompilerMessage;

/**
 * @author Clement Wong
 */
public class RemoteObjectAnalyzer extends AnalyzerAdapter
{
    public RemoteObjectAnalyzer(CompilationUnit unit, Configuration configuration)
    {
        super(unit, configuration);
	}

	public void analyze(MethodNode node)
	{
		if (node.getAttribute("name") == null)
		{
			log(node, new MethodRequiresName());
		}
		super.analyze(node);
	}

    public void analyze(ArgumentsNode node)
    {
        if (node.getAttributeCount() > 0)
        {
	        log(node, new ArgumentsNoAttributes());
        }
        super.analyze(node);
    }

	// error messages

	public static class MethodRequiresName extends CompilerMessage.CompilerError
	{
		public MethodRequiresName()
		{
			super();
		}
	}

	public static class ArgumentsNoAttributes extends CompilerMessage.CompilerError
	{
		public ArgumentsNoAttributes()
		{
			super();
		}
	}
}
