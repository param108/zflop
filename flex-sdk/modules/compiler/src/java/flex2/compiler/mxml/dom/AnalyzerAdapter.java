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

package flex2.compiler.mxml.dom;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
public abstract class AnalyzerAdapter implements Analyzer
{
	public AnalyzerAdapter(CompilationUnit unit, Configuration configuration)
	{
		this.unit = unit;
		this.configuration = configuration;
	}

	protected CompilationUnit unit;
	protected Configuration configuration;
	private Node currentNode;

	public void prepare(Node node)
	{
		currentNode = node;
	}

	public void analyze(CDATANode node)
	{
		traverse(node);
	}

	public void analyze(StyleNode node)
	{
		traverse(node);
	}

	public void analyze(ScriptNode node)
	{
		traverse(node);
	}

	public void analyze(MetaDataNode node)
	{
		traverse(node);
	}

	public void analyze(ModelNode node)
	{
		traverse(node);
	}

	public void analyze(XMLNode node)
	{
		traverse(node);
	}
    
    public void analyze(XMLListNode node)
    {
        traverse(node);
    }

	public void analyze(ArrayNode node)
	{
		traverse(node);
	}

	public void analyze(BindingNode node)
	{
		traverse(node);
	}

	public void analyze(StringNode node)
	{
		traverse(node);
	}

	public void analyze(NumberNode node)
	{
		traverse(node);
	}

    public void analyze(IntNode node)
    {
        traverse(node);
    }

    public void analyze(UIntNode node)
    {
        traverse(node);
    }

    public void analyze(BooleanNode node)
	{
		traverse(node);
	}

	public void analyze(ClassNode node)
	{
		traverse(node);
	}

	public void analyze(FunctionNode node)
	{
		traverse(node);
	}

	public void analyze(WebServiceNode node)
	{
		traverse(node);
	}

	public void analyze(HTTPServiceNode node)
	{
		traverse(node);
	}

	public void analyze(RemoteObjectNode node)
	{
		traverse(node);
	}

	public void analyze(OperationNode node)
	{
		traverse(node);
	}

	public void analyze(RequestNode node)
	{
		traverse(node);
	}

	public void analyze(MethodNode node)
	{
		traverse(node);
	}

	public void analyze(ArgumentsNode node)
	{
		traverse(node);
	}

 	public void analyze(InlineComponentNode node)
 	{
 		traverse(node);
 	}

	public void analyze(Node node)
	{
		traverse(node);
	}

	private void traverse(Node node)
	{
		for (int i = 0, count = node.getChildCount(); i < count; i++)
		{
			Node n = (Node) node.getChildAt(i);
			n.analyze(this);
		}
	}

	protected int getLineNumber()
	{
		return currentNode.beginLine;
	}

	protected void logInfo(String message)
	{
		logInfo(currentNode, message);
	}

	protected void log(CompilerMessage msg)
	{
		log(currentNode, msg);
	}

	protected void logInfo(int line, String message)
	{
		logInfo(currentNode, line, message);
	}

	protected void log(int line, CompilerMessage msg)
	{
		log(currentNode, line, msg);
	}

	protected void logDebug(String message)
	{
		logDebug(currentNode, message);
	}

	protected void logDebug(int line, String message)
	{
		logDebug(currentNode, line, message);
	}

	protected void logWarning(String message)
	{
		logWarning(currentNode, message);
	}

	protected void logWarning(int line, String message)
	{
		logWarning(currentNode, line, message);
	}

	protected void logError(String message)
	{
		logError(currentNode, message);
	}

	protected void logError(int line, String message)
	{
		logError(currentNode, line, message);
	}

	protected void logInfo(Node node, String message)
	{
		ThreadLocalToolkit.logInfo(unit.getSource().getNameForReporting(), node.beginLine, message);
	}

	protected void log(Node node, CompilerMessage msg)
	{
		msg.path = unit.getSource().getNameForReporting();
		msg.line = node.beginLine;
		ThreadLocalToolkit.log(msg);
	}

	protected void logInfo(Node node, int line, String message)
	{
		ThreadLocalToolkit.logInfo(unit.getSource().getNameForReporting(), (line == 0) ? node.beginLine : line, message);
	}

	protected void log(Node node, int line, CompilerMessage msg)
	{
		msg.path = unit.getSource().getNameForReporting();
		msg.line = (line == 0) ? node.beginLine : line;
		ThreadLocalToolkit.log(msg);
	}

	protected void logDebug(Node node, String message)
	{
		ThreadLocalToolkit.logDebug(unit.getSource().getNameForReporting(), node.beginLine, message);
	}

	protected void logDebug(Node node, int line, String message)
	{
		ThreadLocalToolkit.logDebug(unit.getSource().getNameForReporting(), (line == 0) ? node.beginLine : line, message);
	}

	protected void logWarning(Node node, String message)
	{
		ThreadLocalToolkit.logWarning(unit.getSource().getNameForReporting(), node.beginLine, message);
	}

	protected void logWarning(Node node, int line, String message)
	{
		ThreadLocalToolkit.logWarning(unit.getSource().getNameForReporting(), (line == 0) ? node.beginLine : line, message);
	}

	protected void logError(Node node, String message)
	{
		ThreadLocalToolkit.logError(unit.getSource().getNameForReporting(), node.beginLine, message);
	}

	protected void logError(Node node, int line, String message)
	{
		ThreadLocalToolkit.logError(unit.getSource().getNameForReporting(), (line == 0) ? node.beginLine : line, message);
	}

	protected String getLocalizedMessage(CompilerMessage msg)
	{
		return ThreadLocalToolkit.getLocalizationManager().getLocalizedTextString(msg);
	}

	/**
	 * If a node's content is text, return the single CDATANode containing it.
	 * If nodes contains something other than a single CDATANode, return null.
	 * If allowNonText is false and nodes contains non-CDATA nodes, raise an error.
	 * If the node's content is mixed, we always generate an error currently, since mixed content is never ok in MXML.
	 */
	protected CDATANode getTextContent(Collection nodes, boolean allowNonText)
	{
		if (!nodes.isEmpty())
		{
			Iterator iter = nodes.iterator();
			Node first = (Node)iter.next();

			if (first instanceof CDATANode)
			{
				if (!iter.hasNext())
				{
					return (CDATANode)first;
				}
				else
				{
					Node second = (Node)iter.next();
					assert !(second instanceof CDATANode) : "internal error: multiple CDATA children";

					log(second, new MixedContentNotAllowed());
				}
			}
			else if (!allowNonText)
			{
				log(first.beginLine, new ChildElementsNotAllowed());
			}
		}

		return null;
	}

	// error messages

	public static class CouldNotResolveToComponent extends CompilerMessage.CompilerError
	{
		public CouldNotResolveToComponent(String tag)
		{
			super();
			this.tag = tag;
		}

		public final String tag;
	}

	public static class MixedContentNotAllowed extends CompilerMessage.CompilerError
	{
		public MixedContentNotAllowed()
		{
			super();
		}
	}

	public static class ChildElementsNotAllowed extends CompilerMessage.CompilerError
	{
		public ChildElementsNotAllowed()
		{
			super();
		}
	}
}
