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

/**
 * @author Clement Wong
 */
public interface Analyzer
{
	void analyze(CDATANode node);

	void analyze(StyleNode node);

	void analyze(ScriptNode node);

	void analyze(MetaDataNode node);

	void analyze(ModelNode node);

	void analyze(XMLNode node);
    
    void analyze(XMLListNode node);

	void analyze(ArrayNode node);

	void analyze(BindingNode node);

	void analyze(StringNode node);

	void analyze(NumberNode node);

    void analyze(IntNode node);

    void analyze(UIntNode node);

    void analyze(BooleanNode node);

	void analyze(ClassNode node);

	void analyze(FunctionNode node);

	void analyze(WebServiceNode node);

	void analyze(HTTPServiceNode node);

	void analyze(RemoteObjectNode node);

	void analyze(OperationNode node);

	void analyze(RequestNode node);

	void analyze(MethodNode node);

	void analyze(ArgumentsNode node);

	void analyze(InlineComponentNode node);

	void analyze(Node node);

	void prepare(Node node);
}

