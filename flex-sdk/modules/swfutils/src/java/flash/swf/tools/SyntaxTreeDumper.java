////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.swf.tools;

import java.io.PrintWriter;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.semantics.QName;
import macromedia.asc.util.Context;

public class SyntaxTreeDumper implements Evaluator
{
    private int indent;
    private PrintWriter out;

    public SyntaxTreeDumper(PrintWriter out)
    {
        this(out, 0);
    }

    public SyntaxTreeDumper(PrintWriter out, int indent)
    {
        this.out = out;
        this.indent = indent;
    }

	public boolean checkFeature(Context cx, Node node)
	{
		return true;
	}

	public Value evaluate(Context cx, Node node)
	{
        output("<Node position=\"" + node.pos() + "\"/>");
		return null;
	}

    public Value evaluate(Context cx, ApplyTypeExprNode node)
    {
        output("<ApplyTypeExprNode position=\"" + node.pos() + "\">");
        indent++;
        
        output("<expr>");
        if (node.expr != null)
        {
            indent++;
            node.expr.evaluate(cx, this);
            indent--;
        }
        output("</expr>");
        
        output("<typeArgs>");
        if (node.typeArgs != null)
        {
            indent++;
            node.typeArgs.evaluate(cx, this);
            indent--;
        }
        output("</typeArgs>");
        
        indent--;
        output("</ApplyTypeExprNode>");
        return null;
    }
    
	public Value evaluate(Context cx, IdentifierNode node)
	{
        output("<IdentifierNode name=\"" + node.name + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, IncrementNode node)
	{
        output("<IncrementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</IncrementNode>");
		return null;
	}

	public Value evaluate(Context cx, ThisExpressionNode node)
	{
        output("<ThisExpressionNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, QualifiedIdentifierNode node)
	{
        if (node.qualifier != null)
        {
            output("<QualifiedIdentifierNode name=\"" + node.name + "\">");
            indent++;
            node.qualifier.evaluate(cx, this);
            indent--;
            output("</QualifiedIdentifierNode>");
        }
        else
        {
            output("<QualifiedIdentifierNode name=\"" + node.name + "\"/>");
        }
		return null;
	}

    public Value evaluate(Context cx, QualifiedExpressionNode node)
    {
        if( node.ref == null)
        {
            evaluate(cx,(QualifiedIdentifierNode)node);
            node.expr.evaluate(cx,this);
        }
        return node.ref;
    }

	public Value evaluate(Context cx, LiteralBooleanNode node)
	{
        output("<LiteralBooleanNode value=\"" + node.value + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, LiteralNumberNode node)
	{
        output("<LiteralNumberNode value=\"" + node.value + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, LiteralStringNode node)
	{
        if (node.value.length() > 0)
        {
            output("<LiteralStringNode value=\"" + node.value + "\"/>");
        }
		return null;
	}

	public Value evaluate(Context cx, LiteralNullNode node)
	{
        output("<LiteralNullNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, LiteralRegExpNode node)
	{
        output("<LiteralRegExpNode value=\"" + node.value + "\" position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, LiteralXMLNode node)
	{
        output("<LiteralXMLNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.list != null)
        {
            node.list.evaluate(cx, this);
        }
        indent--;
        output("</LiteralXMLNode>");
		return null;
	}

	public Value evaluate(Context cx, FunctionCommonNode node)
	{
        output("<FunctionCommonNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.signature != null)
        {
            node.signature.evaluate(cx, this);
        }
        if (node.body != null)
        {
            node.body.evaluate(cx, this);
        }
        indent--;
        output("</FunctionCommonNode>");
		return null;
	}

	public Value evaluate(Context cx, ParenExpressionNode node)
	{
        output("<ParenExpressionNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, ParenListExpressionNode node)
	{
        output("<ParenListExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</ParenListExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, LiteralObjectNode node)
	{
        output("<LiteralObjectNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.fieldlist != null)
        {
            node.fieldlist.evaluate(cx, this);
        }
        indent--;
        output("</LiteralObjectNode>");
		return null;
	}

	public Value evaluate(Context cx, LiteralFieldNode node)
	{
        output("<LiteralFieldNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.name != null)
        {
            node.name.evaluate(cx, this);
        }
        if (node.value != null)
        {
            node.value.evaluate(cx, this);
        }
        indent--;
        output("</LiteralFieldNode>");
		return null;
	}

	public Value evaluate(Context cx, LiteralArrayNode node)
	{
        output("<LiteralArrayNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.elementlist != null)
        {
            node.elementlist.evaluate(cx, this);
        }
        indent--;
        output("</LiteralArrayNode>");
		return null;
	}

	public Value evaluate(Context cx, SuperExpressionNode node)
	{
        output("<SuperExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</SuperExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, MemberExpressionNode node)
	{
        output("<MemberExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.base != null)
        {
            node.base.evaluate(cx, this);
        }
        if (node.selector != null)
        {
            node.selector.evaluate(cx, this);
        }
        indent--;
        output("</MemberExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, InvokeNode node)
	{
        output("<InvokeNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.args != null)
        {
            node.args.evaluate(cx, this);
        }
        indent--;
        output("</InvokeNode>");
		return null;
	}

	public Value evaluate(Context cx, CallExpressionNode node)
	{
        output("<CallExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.args != null)
        {
            node.args.evaluate(cx, this);
        }
        indent--;
        output("</CallExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, DeleteExpressionNode node)
	{
        output("<DeleteExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</DeleteExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, GetExpressionNode node)
	{
        output("<GetExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</GetExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, SetExpressionNode node)
	{
        output("<SetExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.args != null)
        {
            node.args.evaluate(cx, this);
        }
        indent--;
        output("</SetExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, UnaryExpressionNode node)
	{
        output("<UnaryExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</UnaryExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, BinaryExpressionNode node)
	{
        output("<BinaryExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.lhs != null)
        {
            node.lhs.evaluate(cx, this);
        }
        if (node.rhs != null)
        {
            node.rhs.evaluate(cx, this);
        }
        indent--;
        output("</BinaryExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, ConditionalExpressionNode node)
	{
        output("<ConditionalExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.condition != null)
        {
            node.condition.evaluate(cx, this);
        }
        if (node.thenexpr != null)
        {
            node.thenexpr.evaluate(cx, this);
        }
        if (node.elseexpr != null)
        {
            node.elseexpr.evaluate(cx, this);
        }
        indent--;
        output("</ConditionalExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, ArgumentListNode node)
	{
        output("<ArgumentListNode position=\"" + node.pos() + "\">");
        indent++;
        // for (Node n : node.items)
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            Node n = (Node) node.items.get(i);
            n.evaluate(cx, this);
        }
        indent--;
        output("</ArgumentListNode>");
		return null;
	}

	public Value evaluate(Context cx, ListNode node)
	{
        output("<ListNode position=\"" + node.pos() + "\">");
        indent++;
        // for (Node n : node.items)
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            Node n = (Node) node.items.get(i);
            n.evaluate(cx, this);
        }
        indent--;
        output("</ListNode>");
		return null;
	}

	// Statements

	public Value evaluate(Context cx, StatementListNode node)
	{
        output("<StatementListNode position=\"" + node.pos() + "\">");
        indent++;
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            Node n = (Node) node.items.get(i);
            if (n != null)
            {
                n.evaluate(cx, this);
            }
        }
        indent--;
        output("</StatementListNode>");
		return null;
	}

	public Value evaluate(Context cx, EmptyElementNode node)
	{
        //output("<EmptyElementNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, EmptyStatementNode node)
	{
        //output("<EmptyStatementNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, ExpressionStatementNode node)
	{
        output("<ExpressionStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</ExpressionStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, SuperStatementNode node)
	{
        output("<SuperStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.call.args != null)
        {
            node.call.args.evaluate(cx, this);
        }
        indent--;
        output("</SuperStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, LabeledStatementNode node)
	{
        output("<LabeledStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.label != null)
        {
            node.label.evaluate(cx, this);
        }
        if (node.statement != null)
        {
            node.statement.evaluate(cx, this);
        }
        indent--;
        output("</LabeledStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, IfStatementNode node)
	{
        output("<IfStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.condition != null)
        {
            node.condition.evaluate(cx, this);
        }
        if (node.thenactions != null)
        {
            node.thenactions.evaluate(cx, this);
        }
        if (node.elseactions != null)
        {
            node.elseactions.evaluate(cx, this);
        }
        indent--;
        output("</IfStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, SwitchStatementNode node)
	{
        output("<SwitchStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }
        indent--;
        output("</SwitchStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, CaseLabelNode node)
	{
        output("<CaseLabelNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.label != null)
        {
            node.label.evaluate(cx, this);
        }
        indent--;
        output("</CaseLabelNode>");
		return null;
	}

	public Value evaluate(Context cx, DoStatementNode node)
	{
        output("<DoStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }
        indent--;
        output("</DoStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, WhileStatementNode node)
	{
        output("<WhileStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.statement != null)
        {
            node.statement.evaluate(cx, this);
        }
        indent--;
        output("</WhileStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, ForStatementNode node)
	{
        output("<ForStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.initialize != null)
        {
            node.initialize.evaluate(cx, this);
        }
        if (node.test != null)
        {
            node.test.evaluate(cx, this);
        }
        if (node.increment != null)
        {
            node.increment.evaluate(cx, this);
        }
        if (node.statement != null)
        {
            node.statement.evaluate(cx, this);
        }
        indent--;
        output("</ForStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, WithStatementNode node)
	{
        output("<WithStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        if (node.statement != null)
        {
            node.statement.evaluate(cx, this);
        }
        indent--;
        output("</WithStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, ContinueStatementNode node)
	{
        output("<ContinueStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.id != null)
        {
            node.id.evaluate(cx, this);
        }
        indent--;
        output("</ContinueStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, BreakStatementNode node)
	{
        output("<BreakStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.id != null)
        {
            node.id.evaluate(cx, this);
        }
        indent--;
        output("</BreakStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, ReturnStatementNode node)
	{
        output("<ReturnStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</ReturnStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, ThrowStatementNode node)
	{
        output("<ThrowStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</ThrowStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, TryStatementNode node)
	{
        output("<TryStatementNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.tryblock != null)
        {
            node.tryblock.evaluate(cx, this);
        }
        if (node.catchlist != null)
        {
            node.catchlist.evaluate(cx, this);
        }
        if (node.finallyblock != null)
        {
            node.finallyblock.evaluate(cx, this);
        }
        indent--;
        output("</TryStatementNode>");
		return null;
	}

	public Value evaluate(Context cx, CatchClauseNode node)
	{
        output("<CatchClauseNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.parameter != null)
        {
            node.parameter.evaluate(cx, this);
        }
        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }
        indent--;
        output("</CatchClauseNode>");
		return null;
	}

	public Value evaluate(Context cx, FinallyClauseNode node)
	{
        output("<FinallyClauseNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }
        indent--;
        output("</FinallyClauseNode>");
		return null;
	}

	public Value evaluate(Context cx, UseDirectiveNode node)
	{
        output("<UseDirectiveNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, IncludeDirectiveNode node)
	{
        output("<IncludeDirectiveNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	// Definitions

	public Value evaluate(Context cx, ImportDirectiveNode node)
	{
        output("<ImportDirectiveNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.name != null)
        {
            node.name.evaluate(cx, this);
        }
        indent--;
        output("</ImportDirectiveNode>");
		return null;
	}

	public Value evaluate(Context cx, AttributeListNode node)
	{
        StringBuffer buffer = new StringBuffer("<AttributeListNode");
        if (node.hasIntrinsic)
        {
            buffer.append(" intrinsic='true'");
        }
        if (node.hasStatic)
        {
            buffer.append(" static='true'");
        }
        if (node.hasFinal)
        {
            buffer.append(" final='true'");
        }
        if (node.hasVirtual)
        {
            buffer.append(" virtual='true'");
        }
        if (node.hasOverride)
        {
            buffer.append(" override='true'");
        }
        if (node.hasDynamic)
        {
            buffer.append(" dynamic='true'");
        }
        if (node.hasNative)
        {
            buffer.append(" native='true'");
        }
        if (node.hasPrivate)
        {
            buffer.append(" private='true'");
        }
        if (node.hasProtected)
        {
            buffer.append(" protected='true'");
        }
        if (node.hasPublic)
        {
            buffer.append(" public='true'");
        }
        if (node.hasInternal)
        {
            buffer.append(" internal='true'");
        }
        if (node.hasConst)
        {
            buffer.append(" const='true'");
        }
        if (node.hasFalse)
        {
            buffer.append(" false='true'");
        }
        if (node.hasPrototype)
        {
            buffer.append(" prototype='true'");
        }
        buffer.append(" position=\"" + node.pos() + "\">");
        output( buffer.toString() );
        indent++;
        // for (Node n : node.items)
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            Node n = (Node) node.items.get(i);
            n.evaluate(cx, this);
        }
        indent--;
        output("</AttributeListNode>");
		return null;
	}

	public Value evaluate(Context cx, VariableDefinitionNode node)
	{
        output("<VariableDefinitionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.list != null)
        {
            node.list.evaluate(cx, this);
        }
        indent--;
        output("</VariableDefinitionNode>");
		return null;
	}

	public Value evaluate(Context cx, VariableBindingNode node)
	{
        output("<VariableBindingNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.variable != null)
        {
            node.variable.evaluate(cx, this);
        }
        if (node.initializer != null)
        {
            node.initializer.evaluate(cx, this);
        }
        indent--;
        output("</VariableBindingNode>");
		return null;
	}

	public Value evaluate(Context cx, UntypedVariableBindingNode node)
	{
        output("<UntypedVariableBindingNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        if (node.initializer != null)
        {
            node.initializer.evaluate(cx, this);
        }
        indent--;
        output("</UntypedVariableBindingNode>");
		return null;
	}

	public Value evaluate(Context cx, TypedIdentifierNode node)
	{
        output("<TypedIdentifierNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        if (node.type != null)
        {
            node.type.evaluate(cx, this);
        }
        indent--;
        output("</TypedIdentifierNode>");
		return null;
	}

	public Value evaluate(Context cx, BinaryFunctionDefinitionNode node)
	{
		return evaluate(node, cx, "BinaryFunctionDefinitionNode");
	}

	public Value evaluate(Context cx, FunctionDefinitionNode node)
	{
		return evaluate(node, cx, "FunctionDefinitionNode");
	}

    private Value evaluate(FunctionDefinitionNode node, Context cx, String name)
    {
        if ((node.name != null) && (node.name.identifier.name != null))
        {
            output("<" + name + " name=\"" + node.name.identifier.name + "\">");
        }
        else
        {
            output("<" + name + ">");
        }
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.fexpr != null)
        {
            node.fexpr.evaluate(cx, this);
        }
        indent--;
        output("</" + name + ">");
		return null;
    }

	public Value evaluate(Context cx, FunctionNameNode node)
	{
        output("<FunctionNameNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        indent--;
        output("</FunctionNameNode>");
		return null;
	}

	public Value evaluate(Context cx, FunctionSignatureNode node)
	{
        output("<FunctionSignatureNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.parameter != null)
        {
            node.parameter.evaluate(cx, this);
        }
        if (node.result != null)
        {
            node.result.evaluate(cx, this);
        }
        indent--;
        output("</FunctionSignatureNode>");
		return null;
	}

	public Value evaluate(Context cx, ParameterNode node)
	{
        if ((0 <= node.kind) && (node.kind < Tokens.tokenClassNames.length))
        {
            output("<ParameterNode kind=\"" + Tokens.tokenClassNames[node.kind] + "\">");
        }
        else
        {
            output("<ParameterNode kind=\"" + node.kind + "\">");
        }
        indent++;
        if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        if (node.type != null)
        {
            node.type.evaluate(cx, this);
        }
        indent--;
        output("</ParameterNode>");
		return null;
	}

	public Value evaluate(Context cx, RestExpressionNode node)
	{
        output("<RestExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</RestExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, RestParameterNode node)
	{
        output("<RestParameterNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.parameter != null)
        {
            node.parameter.evaluate(cx, this);
        }
        indent--;
        output("</RestParameterNode>");
		return null;
	}

    public Value evaluate(Context cx, BinaryClassDefNode node)
    {
        return evaluate(node, cx, "BinaryClassDefNode");
    }

	public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node)
	{
		return evaluate(node, cx, "BinaryInterfaceDefinitionNode");
	}

	public Value evaluate(Context cx, ClassDefinitionNode node)
	{
        return evaluate(node, cx, "ClassDefinitionNode");
    }

    private Value evaluate(ClassDefinitionNode node, Context cx, String name)
    {
        if ((node.name != null) && (node.name.name != null))
        {
            output("<" + name + " name=\"" + node.name.name + "\">");
        }
        else if ((node.cframe != null) && (node.cframe.builder != null))
        {
            output("<" + name + " name=\"" + node.cframe.builder.classname + "\">");
        }
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.name != null)
        {
            node.name.evaluate(cx, this);
        }
        if (node.baseclass != null)
        {
            node.baseclass.evaluate(cx, this);
        }
        if (node.interfaces != null)
        {
            node.interfaces.evaluate(cx, this);
        }

	    if (node.fexprs != null)
	    {
		    for (int i = 0, size = node.fexprs.size(); i < size; i++)
		    {
			    Node fexpr = (Node) node.fexprs.get(i);
			    fexpr.evaluate(cx, this);
		    }
	    }

	    if (node.staticfexprs != null)
	    {
		    for (int i = 0, size = node.staticfexprs.size(); i < size; i++)
		    {
			    Node staticfexpr = (Node) node.staticfexprs.get(i);
			    staticfexpr.evaluate(cx, this);
		    }
	    }
	    if (node.instanceinits != null)
	    {
		    for (int i = 0, size = node.instanceinits.size(); i < size; i++)
		    {
			    Node instanceinit = (Node) node.instanceinits.get(i);
			    instanceinit.evaluate(cx, this);
		    }
	    }

	    if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }

        indent--;
        output("</" + name + ">");
        return null;
    }

    public Value evaluate(Context cx, InterfaceDefinitionNode node)
	{
        if ((node.name != null) && (node.name.name != null))
        {
            output("<InterfaceDefinitionNode name=\"" + node.name.name + "\">");
        }
        else if ((node.cframe != null) && (node.cframe.builder != null))
        {
            output("<InterfaceDefinitionNode name=\"" + node.cframe.builder.classname + "\">");
        }
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.name != null)
        {
            node.name.evaluate(cx, this);
        }
        if (node.interfaces != null)
        {
            node.interfaces.evaluate(cx, this);
        }
        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }
        indent--;
        output("</InterfaceDefinitionNode>");
		return null;
	}

	public Value evaluate(Context cx, ClassNameNode node)
	{
        output("<ClassNameNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.pkgname != null)
        {
            node.pkgname.evaluate(cx, this);
        }
        if (node.ident != null)
        {
            node.ident.evaluate(cx, this);
        }
        indent--;
        output("</ClassNameNode>");
		return null;
	}

	public Value evaluate(Context cx, InheritanceNode node)
	{
        output("<InheritanceNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.baseclass != null)
        {
            node.baseclass.evaluate(cx, this);
        }
        if (node.interfaces != null)
        {
            node.interfaces.evaluate(cx, this);
        }
        indent--;
        output("</InheritanceNode>");
		return null;
	}

	public Value evaluate(Context cx, NamespaceDefinitionNode node)
	{
        output("<NamespaceDefinitionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.attrs != null)
        {
            node.attrs.evaluate(cx, this);
        }
        if (node.name != null)
        {
            node.name.evaluate(cx, this);
        }
        if (node.value != null)
        {
            node.value.evaluate(cx, this);
        }
        indent--;
        output("</NamespaceDefinitionNode>");
		return null;
	}
    
    public Value evaluate(Context cx, ConfigNamespaceDefinitionNode node)
    {
        output("<ConfigNamespaceDefinitionNode />");
        return null;
    }

	public Value evaluate(Context cx, PackageDefinitionNode node)
	{
        output("<PackageDefinitionNode />");
		return null;
	}

	public Value evaluate(Context cx, PackageIdentifiersNode node)
	{
        output("<PackageIdentifiersNode position=\"" + node.pos() + "\">");
        indent++;
        for (int i = 0, size = node.list.size(); i < size; i++)
        {
            IdentifierNode n = (IdentifierNode) node.list.get(i);
            n.evaluate(cx, this);
        }
        indent--;
        output("</PackageIdentifiersNode>");
		return null;
	}

	public Value evaluate(Context cx, PackageNameNode node)
	{
        output("<PackageNameNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.id != null)
        {
            node.id.evaluate(cx, this);
        }
        indent--;
        output("</PackageNameNode>");
		return null;
	}

	public Value evaluate(Context cx, ProgramNode node)
	{
        output("<ProgramNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.pkgdefs != null)
        {
            // for (PackageDefinitionNode n : node.pkgdefs)
            for (int i = 0, size = node.pkgdefs.size(); i < size; i++)
            {
                PackageDefinitionNode n = (PackageDefinitionNode) node.pkgdefs.get(i);
                n.evaluate(cx, this);
            }
        }

        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }

        if (node.fexprs != null)
        {
            // for (FunctionCommonNode n : node.fexprs)
            for (int i = 0, size = node.fexprs.size(); i < size; i++)
            {
                FunctionCommonNode n = (FunctionCommonNode) node.fexprs.get(i);
                n.evaluate(cx, this);
            }
        }

        if (node.clsdefs != null)
        {
            // for (FunctionCommonNode n : node.clsdefs)
            for (int i = 0, size = node.clsdefs.size(); i < size; i++)
            {
                ClassDefinitionNode n = (ClassDefinitionNode) node.clsdefs.get(i);
                n.evaluate(cx, this);
            }
        }

        indent--;
        output("</ProgramNode>");
		return null;
	}

	public Value evaluate(Context cx, ErrorNode node)
	{
        output("<ErrorNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, ToObjectNode node)
	{
        output("<ToObjectNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, LoadRegisterNode node)
	{
        output("<LoadRegisterNode position=\"" + node.pos() + "\"/>");
		return null;
	}

	public Value evaluate(Context cx, StoreRegisterNode node)
	{
        output("<StoreRegisterNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</StoreRegisterNode>");
		return null;
	}

	public Value evaluate(Context cx, BoxNode node)
	{
        output("<BoxNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</BoxNode>");
		return null;
	}

	public Value evaluate(Context cx, CoerceNode node)
	{
        output("<CoerceNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
        indent--;
        output("</CoerceNode>");
		return null;
	}

	public Value evaluate(Context cx, PragmaNode node)
	{
        output("<PragmaNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.list != null)
        {
            node.list.evaluate(cx, this);
        }
        indent--;
        output("</PragmaNode>");
		return null;
	}

	public Value evaluate(Context cx, PragmaExpressionNode node)
	{
        output("<PragmaExpressionNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        indent--;
        output("</PragmaExpressionNode>");
		return null;
	}

	public Value evaluate(Context cx, ParameterListNode node)
	{
        output("<ParameterListNode position=\"" + node.pos() + "\">");
        indent++;
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            // ParameterNode param = node.items.get(i);
            ParameterNode param = (ParameterNode) node.items.get(i);
            if (param != null)
            {
                param.evaluate(cx, this);
            }
        }
        indent--;
        output("</ParameterListNode>");
		return null;
	}

	public Value evaluate(Context cx, MetaDataNode node)
	{
        output("<MetaDataNode id=\"" + node.id + "\">");
        indent++;
        if (node.data != null)
        {
            MetaDataEvaluator mde = new MetaDataEvaluator();
            node.evaluate(cx, mde);
        }
        indent--;
        output("</MetaDataNode>");
		return null;
	}

	public Value evaluate(Context context, DefaultXMLNamespaceNode node)
	{
        output("<DefaultXMLNamespaceNode position=\"" + node.pos() + "\"/>");
        return null;
    }

    public Value evaluate(Context cx, DocCommentNode node)
    {
        output("<DocCommentNode position=\"" + node.pos() + "\"/>");
        return null;
    }

    public Value evaluate(Context cx, ImportNode node)
    {
        String id = node.filespec.value;
        QName qname = new QName(cx.publicNamespace(), id);
        output("<ImportNode value=" + qname + "/>");
        return null;
    }

    public Value evaluate(Context cx, BinaryProgramNode node)
    {
        output("<BinaryProgramNode position=\"" + node.pos() + "\">");
        indent++;
        if (node.pkgdefs != null)
        {
            // for (PackageDefinitionNode n : node.pkgdefs)
            for (int i = 0, size = node.pkgdefs.size(); i < size; i++)
            {
                PackageDefinitionNode n = (PackageDefinitionNode) node.pkgdefs.get(i);
                n.evaluate(cx, this);
            }
        }

        if (node.statements != null)
        {
            node.statements.evaluate(cx, this);
        }

        if (node.fexprs != null)
        {
            // for (FunctionCommonNode n : node.fexprs)
            for (int i = 0, size = node.fexprs.size(); i < size; i++)
            {
                FunctionCommonNode n = (FunctionCommonNode) node.fexprs.get(i);
                n.evaluate(cx, this);
            }
        }

        if (node.clsdefs != null)
        {
            // for (FunctionCommonNode n : node.clsdefs)
            for (int i = 0, size = node.clsdefs.size(); i < size; i++)
            {
                ClassDefinitionNode n = (ClassDefinitionNode) node.clsdefs.get(i);
                n.evaluate(cx, this);
            }
        }

        indent--;
        output("</BinaryProgramNode>");
        return null;
    }

    public Value evaluate(Context cx, RegisterNode node)
    {
        output("<RegisterNode position=\"" + node.pos() + "\"/>");
        return null;
    }

	public Value evaluate(Context cx, HasNextNode node)
	{
		return null;
	}
	
	public Value evaluate(Context cx, TypeExpressionNode node)
	{
		output("<TypeExpressionNode position=\"" + node.pos() + "\">");
		node.expr.evaluate(cx, this);
		output("</TypeExpressionNode>");
		return null;
	}
	
    public Value evaluate(Context cx, UseNumericNode node)
    {
    	output("<UseNumericNode position=\"" + node.pos() + "\"/>");
    	return null;
    }

    public Value evaluate(Context cx, UsePrecisionNode node)
    {
    	output("<UsePrecisionNode position=\"" + node.pos() + "\"/>");
    	return null;
    }
    
    public Value evaluate(Context cx, UseRoundingNode node)
    {
    	output("<UseRoundingNode position=\"" + node.pos() + "\"/>");
    	return null;
    }
	
    private String indent()
    {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < indent; i++)
        {
            buffer.append("  ");
        }

        return buffer.toString();
    }

    private void output(String tag)
    {
        try
        {
            out.println(indent() + tag);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
}
