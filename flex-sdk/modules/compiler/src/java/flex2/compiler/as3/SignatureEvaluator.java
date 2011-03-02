////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
import flash.localization.LocalizationManager;
import flash.swf.tools.as3.EvaluatorAdapter;
import flex2.compiler.SymbolTable;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.util.QName;


//     _____ _                   _                  _____           _             _
//    /  ___(_)                 | |                |  ___|         | |           | |
//    \ `--. _  __ _ _ __   __ _| |_ _   _ _ __ ___| |____   ____ _| |_   _  __ _| |_ ___  _ __
//     `--. \ |/ _` | '_ \ / _` | __| | | | '__/ _ \  __\ \ / / _` | | | | |/ _` | __/ _ \| '__|
//    /\__/ / | (_| | | | | (_| | |_| |_| | | |  __/ |___\ V / (_| | | |_| | (_| | || (_) | |
//    \____/|_|\__, |_| |_|\__,_|\__|\__,_|_|  \___\____/ \_/ \__,_|_|\__,_|\__,_|\__\___/|_|
//              __/ |
//             |___/

//   _        __ __            _  __ __       _______ _     __
//  |_)| ||  |_ (_   _ __  _| |_|(_ (_ | ||V||_)|  | / \|\|(_
//  | \|_||__|____) (_|| |(_| | |__)__)|_|| ||  | _|_\_/| |__)
//
// GENERAL
// * Packages can only have one top level definition in mxmlc, but not asc/authoring (therefore,
//   this evaluator might not be sufficient for ASC in the general case).
//
// * Ignoring all private variables and functions, method bodies, and defs outside the main package.
//
// * Includes are inlined into the source berore generating a signature
//
// * Definition(Node)s that are not in packages are not evaluated -- none of them should be externally visible
//
// * Peephole sorting: Attributes, CLASS IMPLEMENTS lists, INTERFACE EXTENDS lists, MetaData values,
//   and USE NAMESPACE (N1,N2) lists are sorted canonically (case-sensitive for stability).
//   (these use cases affect about 20% of the Flex SDK!)
//
// * Namespace values (e.g. http://www.adobe.com/2006/flex/mx/internal) DO affect dependent files
//   and therefore the signature; the value appears in dependent external definitions.
//
// * This evaluator does not support top-level ExpressionStatements -- statically executable code
//   (Flash's linker does this a lot, mxmlc does not even support it); another limitation is that
//   we only consider the first package, whereas ASC allows multiple packages in a file
//   (this stuff is easily fixable).
//
// * The values of MetaData are part of the signature since they can affect an arbitrary set of files.
//
// * The values of initializers and defaults are omitted, but their existence is significant for
//   type checking; therefore "=..." is emitted in their place.
//
//   One consideration on whether to mark that there's a default value is that the number of
//   expected arguments change when you add or remove a default value from a method parameter.
//   It may be necessary for the signature to reflect that.
//
// * If a variables or function has no access specifiers, the signature will contain "internal". 
//
// IMPORT and USE directives
// * Since use namespace directives, and imports, are block scoped, their existence in function
//   bodies is not meaningful externally. Function bodies are entirely omitted.
//
// * Class and package level imports can affect signatures -- type declarations for external fields
//   and functions are multinames containing the set of imports; though the type name of an import
//   doesn't change, you could swap an import out for another which provides the same type name
//   but different definition, which requires a deep recompile.
//
// * ALL imports that are written in the original source are assumed to be in-use;
//   in other words, even unused imports are part of the signature.
//
// * Imports and use directives within functions are block scoped and not part of the signature
//

//   _  _  __ _____ _     __  _  _______   ______ _______ _     __
//  |_)/ \(_ (_  | |_)|  |_  / \|_)|  | |V| |  _/|_||  | / \|\|(_
//  |  \_/__)__)_|_|_)|__|__ \_/|  | _|_| |_|_/__| || _|_\_/| |__)
//
// * What other things can be canonical/sorted -- e.g. the list of implements (which is implemented)?
//
// * Semicolons would make signatures prettier.
//
// * non-human readable version: smaller files, most memory, faster CRC.
//   Excludes indentation, new-lines, semicolons.
//
// * Is (x) the same as (x:*)? The difference should probably be reflectted in the signature for
//   type checking and warning purposes. If no difference, then I can make both map to same sig.
//
// * Do I need to mark the existence of a variable initializer, or can I omit "=..."?
//
// * Do I need to print return values? as long as type checking continues to work on dependent
//   files, the return type is unused in dependent files -- we would only be recompiling them
//   to do type checking.
//
// * I already have a rule system that determines what gets included in a signature,
//   it'd be useful to to generate multiple signatures in one pass
//   (e.g. just protected methods, just public methods)
//
// * Unused imports could be excluded from signatures (assumes the files can compile and link).
//

//     ___ _  __   _
//    |_ _/ \|  \ / \
//     | ( o ) o | o )
//     |_|\_/|__/ \_/
//
// TODO handle TypeExpressionNode -- especially ! and ? chars
// TODO handle ES4 initializer lists -- see flashfarm changelist 296802
//
// TODO INTELLIGENT SORTING
// * Do (static) variables with initializers need to be ordered? Do I need to emit the
//   initializer and/or a placeholder equals sign to show that there is an initializer?
//
// * separate a signature out into a linked list of string buffers, and update each buffer separately
//   each buffer represents a different element of a file, e.g. imports
//   concatenation of buffers is a signature.
//
// * You need a tree structure of string buffers. Create a top-level list of string buffers, create
//   a class such as ClassStringBuffer, which will contain ImportStringBuffer, which will contain a
//   sorted list of string buffers. Etc.
//
//   The hardest part of this is figuring out what to do with the top level of string buffers.
//   You could have a PackageStringBuffer, but still, what would it contain? Probably the same
//   things as a ClassStringBuffer, except there's shouldn't really be more than one function,
//   class,  or variable declaration since its top-level, but imports, namespaces, etc. should be
//   in there. It's the catchall which I'm not sure what to do with -- perhaps each specialized
//   string buffer will contain a catchall string buffer, and the catchall will register itself as
//   the current "buffer" for evaluators, and when the specialized string buffer is completed, the
//   catchall buffer will "pop" itself off the "stack", so the next catchall buffer takes over...
//   all the way up through package.
//
// * Don't forget to move metadata with definitions (variables/functions/classes/packages (others?)
//




/**
 * Evaluates an AS3 syntax tree and emits a file signature.
 * 
 * This class is not meant to be reused -- always create a new instance when you need it.
 * 
 * <b>IMPORTANT:</b> If you use this evaluator, you must catch SignatureAssertionRuntimeException.
 * 
 * @see SignatureAssertionRuntimeException
 * @author Jono Spiro
 */
public class SignatureEvaluator extends EvaluatorAdapter implements Tokens
{
    
//    ______ _      _     _
//    |  ___(_)    | |   | |
//    | |_   _  ___| | __| |___
//    |  _| | |/ _ \ |/ _` / __|
//    | |   | |  __/ | (_| \__ \
//    \_|   |_|\___|_|\__,_|___/

    public static String NEWLINE = System.getProperty("line.separator");
    
    /**
     * Enables stdout printing of what's going on.
     */
    private final static boolean DEBUG = false;
    
    /**
     * This is where a finished signature ends up.
     */
    private final StringBuffer out;
    
    /**
     * The current indent distance.
     */
    private int indent;
    
    /**
     * Used to determine which nodes may be evaluated and other things
     * about what the signatures will contain.
     */
    private final SignatureRules signatureRules;
    
    /**
     * Stores a cache of expensively computed attributes about DefinitionNodes;
     * used alongside SignatureRules, in checkFeature().
     */
    private final AttributeInfoCache attributeInfoCache = new AttributeInfoCache();

    
//     _____                 _                   _
//    /  __ \               | |                 | |
//    | /  \/ ___  _ __  ___| |_ _ __ _   _  ___| |_ ___  _ __ ___
//    | |    / _ \| '_ \/ __| __| '__| | | |/ __| __/ _ \| '__/ __|
//    | \__/\ (_) | | | \__ \ |_| |  | |_| | (__| || (_) | |  \__ \
//     \____/\___/|_| |_|___/\__|_|   \__,_|\___|\__\___/|_|  |___/
    
    /**
     * Initial buffer size of 8k.
     * 
     * @see SignatureEvaluator(int suggestedBufferSize)
     */
    public SignatureEvaluator()
    {
        this(8192);
    }
    
    /** 
     * Save the StringBuffer some work and guess how big your signature might be.
     * Uses the default SignatureRules (rules for determining what to include in a signature).
     * 
     * @see SignatureEvaluator(int suggestedBufferSize, SignatureRules signatureRules)
     */
    public SignatureEvaluator(int suggestedBufferSize)
    {
        this(suggestedBufferSize, new SignatureRules());
    }
    
    /** 
     * Save the StringBuffer some work and guess how big your signature might be.
     * Uses a custom SignatureRules (rules for determining what to include in a signature).
     */
    public SignatureEvaluator(int suggestedBufferSize, SignatureRules signatureRules)
    {
        this.out = new StringBuffer(suggestedBufferSize);
        this.signatureRules = signatureRules;
    }
    
    
    
//    ___  ___               _                ______                _   _
//    |  \/  |              | |               |  ___|              | | (_)
//    | .  . | ___ _ __ ___ | |__   ___ _ __  | |_ _   _ _ __   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \ '_ ` _ \| '_ \ / _ \ '__| |  _| | | | '_ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/ | | | | | |_) |  __/ |    | | | |_| | | | | (__| |_| | (_) | | | \__ \
//    \_|  |_/\___|_| |_| |_|_.__/ \___|_|    \_|  \__,_|_| |_|\___|\__|_|\___/|_| |_|___/

    public String getSignature()
    {
        return out.toString();
    }

    
    public String toString()
    {
        return getSignature();
    }
    
    
    private String indentCache;
    private int lastIndent; 
    
    /**
     * Returns the current indentation.
     */
    private String indent()
    {
        assert (indent >= 0);
        if (lastIndent != indent)
        {
            indentCache = "";
            for (int i = 0; i < indent; i++)
            {
                indentCache += "    ";
            }
        }
        return indentCache;
    }
    
    /**
     * Prints to the final signature.
     */
    private void printLn(Object str)
    {
        out.append(str).append(NEWLINE);
        
        if(DEBUG)
            System.out.println(str);
    }


    /**
     * Prints to the final signature.
     */
    private StringBuffer print(Object str)
    {
        out.append(str);
        
        if(DEBUG)
            System.out.print(str);
        
        return out;
    }
    
    
    /**
     * Cleared whenever you retrieve it's contents.
     */
    private StringBuffer buffer = new StringBuffer(128);
    
    /**
     * Retrieves and clears the buffer.
     */
    public String getAndClearBuffer()
    {
        // this could be a lot of object creation, OTOH deleting the buffer is not particularily
        // efficient (and cannot resize it back to 128, only gets larger)
        final String string = buffer.toString();
        buffer = new StringBuffer(128);
        return string;
    }

    /**
     * Prints the contents of the buffer to the final signature, and clears the buffer.
     */
    private void flushBufferToSignature()
    {
        print(getAndClearBuffer());
    }
    
    /**
     * Definition (and MetaData) Nodes (e.g., top-level signature nodes) that use the buffer
     * are responsible for cleaning up and leaving it empty.
     */
    private void ASSERT_BUFFER_EMPTY(Node node)
    {
        ASSERT(buffer.length() == 0, "Sanity Failed: Buffer is not empty.", node);
    }
  
    /**
     * Purpose is to flag anything that compromises the safety/accuracy of a signature.
     */
    private static void ASSERT_SANITY(boolean expr, Node node)
    {
        ASSERT(expr, "Sanity Failed", node);
    }
    
    private static void UNTESTED_CODEPATH(Node node)
    {
        ASSERT(false, "Untested Codepath", node);
    }

    private static void UNREACHABLE_CODEPATH(Node node)
    {
        ASSERT(false, "Unreachable Codepath", node);
    }

    private static void UNIMPLEMENTED_CODEPATH(Node node)
    {
        ASSERT(false, "Unimplemented Codepath", node);
    }
  
    private static void ASSERT(boolean expr, String exn, Node node)
    {
        // assert expr : "Failed Sanity";
        if (!expr)
            throw new SignatureAssertionRuntimeException(exn, node);
    }
  
    
//                          _____            _             _
//            _____        | ____|_   ____ _| |_   _  __ _| |_ ___  _ __ ___         _____
//      _____|_____|_____  |  _| \ \ / / _` | | | | |/ _` | __/ _ \| '__/ __|  _____|_____|_____
//     |_____|_____|_____| | |___ \ V / (_| | | |_| | (_| | || (_) | |  \__ \ |_____|_____|_____|
//                         |_____| \_/ \__,_|_|\__,_|\__,_|\__\___/|_|  |___/
    
    public boolean checkFeature(Context cx, Node node)
    {
        if (DEBUG)
            System.err.println("VISITNG NODE: " + node);

        // RULES!
        // if none of the rules fail, process the node (default)
        boolean result = true;
        if (node instanceof DefinitionNode)
        {
            final AttributeInfo attInfo = attributeInfoCache.getAttributeInfo((DefinitionNode)node);
            if (node instanceof FunctionDefinitionNode)
            {
                result = !((attInfo.isPublic    && !signatureRules.KEEP_FUN_SCOPE_PUBLIC)    ||
                           (attInfo.isPrivate   && !signatureRules.KEEP_FUN_SCOPE_PRIVATE)   ||
                           (attInfo.isProtected && !signatureRules.KEEP_FUN_SCOPE_PROTECTED) ||
                           (attInfo.isUser      && !signatureRules.KEEP_FUN_SCOPE_USER)      ||
                           (attInfo.isInternal  && !signatureRules.KEEP_FUN_SCOPE_INTERNAL));
            }
            else if (node instanceof VariableDefinitionNode)
            {
                result = !((attInfo.isPublic    && !signatureRules.KEEP_VAR_SCOPE_PUBLIC)    ||
                           (attInfo.isPrivate   && !signatureRules.KEEP_VAR_SCOPE_PRIVATE)   ||
                           (attInfo.isProtected && !signatureRules.KEEP_VAR_SCOPE_PROTECTED) ||
                           (attInfo.isUser      && !signatureRules.KEEP_VAR_SCOPE_USER)      ||
                           (attInfo.isInternal  && !signatureRules.KEEP_VAR_SCOPE_INTERNAL));
            }
            else if (node instanceof ImportDirectiveNode)
            {
                result = signatureRules.KEEP_IMPORTS;
            }
            else if (node instanceof ClassDefinitionNode)
            {
                result = signatureRules.KEEP_CLASSES;
            }
            else if (node instanceof InterfaceDefinitionNode)
            {
                result = signatureRules.KEEP_INTERFACES;
            }
            else if (node instanceof UseDirectiveNode)
            {
                result = signatureRules.KEEP_USE_NAMESPACE;
            }
        }
        else if (node instanceof MetaDataNode)
        {
            // only print metadata if the definition it is attached to will get evaluated
            result = (signatureRules.KEEP_METADATA && checkFeature(cx, ((MetaDataNode)node).def));
        }
        
        return result;
    }
    
    public Value evaluate(Context unused_cx, ProgramNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        final Context cx = node.cx;

        // should never come up; this comes from the command line: asc -import <...>
        ASSERT_SANITY(node.imports.isEmpty(), node);

        // MAIN PACKAGE
        // we should always have at least one package
        ASSERT_SANITY(node.pkgdefs != null, node);
        ASSERT_SANITY(node.pkgdefs.size() > 0, node);
        {
            // TODO (why?) there can be multiple packages inside of a file for some reason
            // we're only interested in the main package, however
            final PackageDefinitionNode mainPackage = (PackageDefinitionNode) node.pkgdefs.first();
                
            // BODY
            if (mainPackage.statements != null)
            {
                indent++;
                mainPackage.statements.evaluate(cx, this);
                ASSERT_BUFFER_EMPTY(node);
                indent--;
            }
        }

        // this only seems to be in use during FlowAnalyzer and later
        ASSERT_SANITY(node.clsdefs == null, node);
        
        // TODO this doesn't seem to be used ANYWHERE, could be removed from Evaluator
        ASSERT_SANITY(node.fexprs  == null, node);
        
        // should never happen, it should have all been consumed by now
        ASSERT_BUFFER_EMPTY(node);
        
        return null;
    }

    
    
//    ______      __ _       _ _   _             _   _           _
//    |  _  \    / _(_)     (_) | (_)           | \ | |         | |
//    | | | |___| |_ _ _ __  _| |_ _  ___  _ __ |  \| | ___   __| | ___  ___
//    | | | / _ \  _| | '_ \| | __| |/ _ \| '_ \| . ` |/ _ \ / _` |/ _ \/ __|
//    | |/ /  __/ | | | | | | | |_| | (_) | | | | |\  | (_) | (_| |  __/\__ \
//    |___/ \___|_| |_|_| |_|_|\__|_|\___/|_| |_\_| \_/\___/ \__,_|\___||___/

    //TODO sort these
    public Value evaluate(Context unused_cx, ClassDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        final Context cx = node.cx;
        
        buffer.append(indent());
        
        // ATTRIBUTES
        if (node.attrs != null)
            node.attrs.evaluate(cx, this);
        
        // "No class name found for ClassDefinitionNode"
        ASSERT_SANITY(node.name      != null, node);
        ASSERT_SANITY(node.name.name != null, node);
        {
            //node.name.evaluate(cx, this);
            buffer.append("class ").append(NodeMagic.getUnqualifiedClassName(node));
        }
        
        if (node.baseclass != null)
        {
            buffer.append(" extends ");
            node.baseclass.evaluate(cx, this);
        }
        
        if (node.interfaces != null)
        {
            buffer.append(" implements ");
            // it's an unordered list, therefore it's sortable
            evaluateSorted(cx, (ListNode)node.interfaces);
            //node.interfaces.evaluate(cx, this);
        }
        
        buffer.append(NEWLINE)
              .append(indent()).append("{")
              .append(NEWLINE);
        
        flushBufferToSignature();

        // these only seem to be in use during FlowAnalyzer and later
        ASSERT_SANITY(node.fexprs        == null, node);
        ASSERT_SANITY(node.staticfexprs  == null, node);
        ASSERT_SANITY(node.instanceinits == null, node);

        if (node.statements != null)
        {
            indent++;
            node.statements.evaluate(cx, this);
            ASSERT_BUFFER_EMPTY(node);
            indent--;
        }
        
        buffer.append(indent()).append("}")
              .append(NEWLINE);
        
        flushBufferToSignature();
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }
    

    //TODO sort these
    public Value evaluate(Context cx, InterfaceDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        buffer.append(indent());
        
        // ATTRIBUTES
        if (node.attrs != null)
            node.attrs.evaluate(cx, this);
        
        // "No class name found for InterfaceDefinitionNode"
        ASSERT_SANITY(node.name      != null, node);
        ASSERT_SANITY(node.name.name != null, node);
        {
            //node.name.evaluate(cx, this);
            buffer.append("interface ").append(NodeMagic.getUnqualifiedClassName(node));
        }
        
        if (node.interfaces != null)
        {
            buffer.append(" extends ");
            // it's a list, it's sortable
            evaluateSorted(cx, (ListNode)node.interfaces);
            //node.interfaces.evaluate(cx, this);
        }

        // interfaces don't have a baseclass
        ASSERT_SANITY(node.baseclass == null, node);
        
        buffer.append(NEWLINE)
              .append(indent()).append("{")
              .append(NEWLINE);
        
        flushBufferToSignature();
        
        if (node.statements != null)
        {
            indent++;
            node.statements.evaluate(cx, this);
            ASSERT_BUFFER_EMPTY(node);
            indent--;
        }
        
        buffer.append(indent()).append("}")
              .append(NEWLINE);
        
        flushBufferToSignature();

        ASSERT_BUFFER_EMPTY(node);
        return null;
    }
    

    //TODO sort these
    public Value evaluate(Context unused_cx, FunctionDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        final Context cx = node.cx;

        // ATTRIBUTES
        // if (node.attrs != null)
        //     node.attrs.evaluate(cx, this);

        // peephole optimization
        //   * functions without attributes or access specifiers are always marked internal
        //
        //          var foo --> internal var foo
        // internal var foo --> internal var foo
        //   static var foo --> internal static var foo
        final TreeSet sortedAttributeSet = NodeMagic.getSortedAttributes(node.attrs);
        if(node.attrs == null || attributeInfoCache.getAttributeInfo(node).isInternal)
        {
            // it's a set, so it's okay if this is redundant
            sortedAttributeSet.add(NodeMagic.INTERNAL);
        }
        
        buffer.append(indent())
              .append(NodeMagic.setToString(sortedAttributeSet, " ")).append(" ")
              .append("function ");
        
        // GET or SET
        if (NodeMagic.functionIsGetter(node))
            buffer.append("get ");
        else if (NodeMagic.functionIsSetter(node))
            buffer.append("set ");

        // this should be a safe assumption, I think the only time this could be null is
        // when defining an anonymous function definition... within a function body
        ASSERT_SANITY(node.name            != null, node);
        ASSERT_SANITY(node.name.identifier != null, node);
        
        // though it's often a QualifiedIdentifierNode (which includes attributes and namespace)
        // I am only interested in the unqualified function name
        // another way: ((IdentifierNode)node.name.identifier).evaluate(cx, this);
        buffer.append(NodeMagic.getUnqualifiedFunctionName(node));
        
        ASSERT_SANITY(node.fexpr != null, node);
        
        // PARAMETERS
        buffer.append("(");
        if (NodeMagic.getFunctionParamCount(node) > 0)
        {
            for(final Iterator iter = node.fexpr.signature.parameter.items.iterator(); iter.hasNext(); )
            {
                final ParameterNode param = (ParameterNode)iter.next();
                
                ASSERT_SANITY(param.kind == VAR_TOKEN, node);

                // rest (...)
                if(param instanceof RestParameterNode)
                    buffer.append("...");
                
                // normal parameter
                else
                {
                    //TODO OPTIMIZATION:
                    // Is (x) the same as (x:*)? the difference should probably be
                    // reflectted in the signature for type checking and warning purposes.
                    
                    // if node has a type                       (x:foo.bar)
                    if(param.type != null)
                        param.type.evaluate(cx, this);
                    
                    // or there is no annotation                (x:*)
                    else if (!param.no_anno)
                        buffer.append(SymbolTable.NOTYPE);
                    
                    //else, no type declaration:                (x)
                        // print nothing
                    
                    // INITIALIZERS/DEFAULT VALUES
                    // TODO OPTIMIZATION:
                    // Is printing =... even necessary for the signature?
                    // For now assuming so -- though default values and return values
                    // do not affect dependent bytecode, I need to check if the type checking
                    // works without recompiling dependent files (catching what would be RTEs).
                    // If it does, then we can omit these, if not, then we need them.
                    if(param.init != null)
                    {
                        if (signatureRules.KEEP_FUN_PARAM_INITIALIZER)
                        {
                            // what kinds of initializers are possible -- numbers, strings, nulls,
                            // anything else, that could possibly print incorrectly? anonymous
                            // function definitions!? (if so, node.name will be null, eek)
                            buffer.append("=<");
                            param.init.evaluate(null, this);
                            buffer.append(">");
                        }
                        else
                        {
                            buffer.append("=...");
                        }
                    }
                    
                    if (iter.hasNext())
                        buffer.append(", ");
                }
            }
        }
        buffer.append(")");
        
        // RETURN TYPE
        
        // IGNORE ME: this is incomplete, but it may come in handy for testing
        // assertSanity(node.fexpr.signature.result == null ||
        //              node.fexpr.signature.result instanceof MemberExpressionNode ||
        //              node.fexpr.signature.result instanceof TypedIdentifierNode);
        
        // TODO OPTIMIZATION:
        // as long as type checking continues to work on dependent files, the return type
        // is technically not used in dependent files -- we would only be recompiling them
        // to do type checking. forget return values?
        
        //TODO patch NodeMagic.getFunctionTypeName with these cases?
        if (node.fexpr.signature.void_anno)
        {
            buffer.append(":void");
        }
        else if (node.fexpr.signature.no_anno)
        {
            // print nothing
        }
        else if(node.fexpr.signature.result != null) // the return type is a literal string
        {
            // following is insufficient because of MemberExpressions that have non-null .base
            // as in: function foo():bar.Baz
            //buffer.append(":" + NodeMagic.getFunctionTypeName(node));
            
            buffer.append(":");
            node.fexpr.signature.result.evaluate(cx, this);
            
            // don't reallty need this, but I just changed a whole lot of code...
            ASSERT_SANITY(buffer.charAt(buffer.length()-1) != ':', node);
        }
        else // else result type is *
        {
            buffer.append(":").append(SymbolTable.NOTYPE);
        }
        
        buffer.append(NEWLINE);
        flushBufferToSignature();

        //ignore node.body (we do need to evaluate the body looking for imports, though)
        //       node.fexpr.body
        //ignore .def, self-referrential
        ASSERT_SANITY(node.fexpr.def == node, node);
        
        //TODO Use statements and imports are block scoped, meaning they could show up in
        //     function bodies – that should only matter for the compilation unit though,
        //     not externally. for now I am going to not parse the bodies at all.
        
        // evaluate the body looking ONLY for imports using a
        // skeletal evluator that only secretes imports
        // if (node.fexpr.body != null)
        //     node.fexpr.body.evaluate(cx, methodBodyImportFinder);
        
        // this was handled implicitely rather than running through the evaluator
        // if (node.fexpr != null)
        //     node.fexpr.evaluate(cx, this);
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }
    
    
    //TODO sort these (do initializers need their own list?)
    //     DO NOT SORT those with initializers?
    //     are *static* initializers more special than instance initializers?
    public Value evaluate(Context cx, VariableDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        // ATTRIBUTES and USER NAMESPACE
        // if (node.attrs != null)
        //     node.attrs.evaluate(cx, this);

        // peephole optimization
        //   * variables without attributes or access specifiers are always marked internal
        //
        //          var foo --> internal var foo
        // internal var foo --> internal var foo
        //   static var foo --> internal static var foo
        final TreeSet sortedAttributeSet = NodeMagic.getSortedAttributes(node.attrs);
        if(node.attrs == null || attributeInfoCache.getAttributeInfo(node).isInternal)
        {
            // it's a set, so it's okay if this is redundant
            sortedAttributeSet.add(NodeMagic.INTERNAL);
        }
        
        final String kind =
            (node.kind == CONST_TOKEN) ? "const " :
            (node.kind == VAR_TOKEN)   ? "var "   : null;

        ASSERT_SANITY(kind != null, node); //, "Unknown VariableDefinitionNode.kind");

        // outputs a new variable declaration for each variable in a list
        // e.g. 'var a,b,c' => var a; var b; var c
        for(final Iterator iter = node.list.items.iterator(); iter.hasNext();)
        {
            final VariableBindingNode variableBinding = (VariableBindingNode)iter.next();

            // ATTRIBUTES and KIND
            buffer.append(indent())
                  .append(NodeMagic.setToString(sortedAttributeSet, " ")).append(" ")
                  .append(kind);
            
            // NAME
            buffer.append(variableBinding.variable.identifier.name);

            // TYPE
            // if there is an annotation...
            if(variableBinding.variable.no_anno == false)
            {
                buffer.append(":");
                
                if (variableBinding.variable.type != null)
                {
                    // :Object
                    variableBinding.variable.type.evaluate(cx, this);
                }
                else
                {
                    // :*
                    buffer.append(SymbolTable.NOTYPE);
                }
            }
            
            // TODO
            // I don't think variables with initializers need to emit their initialization;
            // if the initializer changes or not, we still recompile the file, and it doesn't
            // affect the API in the same way that a default argument affects the API.
            //
            // You can treat it as a a function body and omit it.
            //
            // (the reason I don't want to emit it is also because I'd need to write a very
            // complete Evaluator that will reproduce the exact statement following "=",
            // I cannot simply use SignatureEvaluator)
            
            // TODO
            // The question is, do I need to mark that it has an initializer at all?
            // For now, I will, with "=..." -- I might need to sort those with initializers separately.

            // INITIALIZER
            if (variableBinding.initializer != null)
            {
                //TODO do I need this at all?
                buffer.append("=...");
                // variableBinding.initializer.evaluate(cx, this);
            }
            
            buffer.append(NEWLINE);
        }
        
        flushBufferToSignature();

        // evaluated implicitely above
        //if (node.list != null)
        //    node.list.evaluate(cx, this);

        ASSERT_BUFFER_EMPTY(node);
        return null;
    }
    
    
    //  TODO sort these -- make sure they only get sorted for the current block scope
    //       since they can be defined outside of the package, in a package, in a class, etc.
    public Value evaluate(Context cx, ImportDirectiveNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        ASSERT_SANITY(node.attrs == null, node);
     
        // is it possible for node.name to be null?
        ASSERT_SANITY( node.name         != null,   node);
        ASSERT_SANITY( node.name.id.list != null,   node);
        ASSERT_SANITY(!node.name.id.list.isEmpty(), node);
        
        buffer.append(indent()).append("import ");
        buffer.append(NodeMagic.getDottedImportName(node));
        //if (node.name != null)
        //    node.name.evaluate(cx, this);
        buffer.append(NEWLINE);
        flushBufferToSignature();
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }

    
    //TODO sort these?
    public Value evaluate(Context cx, NamespaceDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        buffer.append(indent());
        
        if (node.attrs != null)
            node.attrs.evaluate(cx, this);

        buffer.append("namespace ");
        
        if (node.name != null)
            node.name.evaluate(cx, this);
        
        // namespace values DO affect dependent files/the signature
        // clement: The namespace value might appear in the dependent external definitions.
        if (node.value != null)
        {
            buffer.append("=");
            node.value.evaluate(cx, this);
        }
        
        buffer.append(NEWLINE);
        flushBufferToSignature();

        ASSERT_BUFFER_EMPTY(node);
        
        return null;
    }
    
    // e.g. "config namespace FOO"
    public Value evaluate(Context cx, ConfigNamespaceDefinitionNode node)
    {
        // they are for conditional compilation, unusable, and only part
        // of the syntax tree for error checking (prevent namespace shadowing)
        // they should not be part of the signature
        return null;
    }
    

    private PackageDefinitionNode currentPackage;
    public Value evaluate(Context cx, PackageDefinitionNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        ASSERT_SANITY(node.attrs == null, node);
        
        if (currentPackage == null)
        {
            currentPackage = node;
            buffer.append("package ")
                  .append(NodeMagic.getPackageName(node))
                  .append(NEWLINE)
                  .append("{")
                  .append(NEWLINE);
            flushBufferToSignature();
        }
        else
        {
            currentPackage = null;
            printLn("}");
        }
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }

    
    // TODO should use statements affect public sig? maybe not, depends how they are used
    //      certainly defining new members in a namespace, but redefining a namespsce var or fun?
    // 
    // notes on absurdity:
    // * "use include ..." seems to be valid in the parser
    // * "use namespace a, b" is valid in the grammar, but illegal when compiling...
    //   (I am going to assume that order matters and not emit this alphabetically, e.g..)
    // * "use namespace (AS3, mx_internal)" compiles correctly (UGH)
    // * the parser will accept "use namespace (true ? AS3 : AS3);" (UGHHHHH)
    //
    //TODO add use directives into a list and emit later? is that how they work? (are they scoped?)
    public Value evaluate(Context cx, UseDirectiveNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        ASSERT_SANITY(node.attrs == null, node);
        
        buffer.append(indent()).append("use namespace ");
        
        //TODO EvaluatorAdapter should get updated with this code (it's not there)
        //     there could be more than one namespace in expr
        if( node.expr != null )
        {
            // if it's a list, it's sortable
            if (node.expr instanceof ListNode)
                evaluateSorted(cx, (ListNode)node.expr);
            else
                node.expr.evaluate(cx,this);
        }

        buffer.append(NEWLINE);
        flushBufferToSignature();
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }
    
    
    //TODO sort these with their respective nodes -- classes, variables, and functions (any others?)
    public Value evaluate(Context cx, MetaDataNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        // this fills out node.id and node.values
        if (node.data != null)
            (new macromedia.asc.parser.MetaDataEvaluator()).evaluate(cx, node);
        
        ASSERT_SANITY(node.id != null, node);
        
        // MetaDataNode without a definition:
        //    Disabled because apparently "[Foo];" is valid
        //    but causes def to be null since ';' is an EmptyStatementNode.
        //    See ASC-2786.
        //ASSERT_SANITY(node.def != null, node);

        buffer.append(indent()).append("[").append(node.id)
              .append(NodeMagic.getSortedMetaDataParamString(node))
              .append("]").append(NEWLINE);
        
        flushBufferToSignature();
        
        ASSERT_BUFFER_EMPTY(node);
        return null;
    }

    
    /*
     * This can be a top-level Node in a class (or package?), but should only affect linkage,
     * not dependencies.
     * 
     * This comes up (as "Bar;") when you have code like:
     * class Foo
     * {
     *    import Bar;
     *    Bar;
     *    ...
     * }
     */
    public Value evaluate(Context cx, ExpressionStatementNode node)
    {
        ASSERT_BUFFER_EMPTY(node);
        
        // here's how to DEBUG it
//        boolean old = DEBUG;
//        DEBUG = true;
//        super.evaluate(cx, node);
//        DEBUG = old;
//        if (buffer.length() > 0)
//        {
//            buffer.insert(0, "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t");
//            System.out.println(getAndClearBuffer());
//        }
//        
//        assertBufferEmpty();
        return null;
    }
    
    /**
     * default xml namespace = new Namespace(...);
     */
    public Value evaluate(Context cx, DefaultXMLNamespaceNode node)
    {
        // I am pretty sure that the existence or value of this directive
        // cannot affect the signature of the current class (when used outside
        // of function scope) -- it should only affect the xmlns value of XML
        // objects within the block/class.
        return null;
    }
    
    public Value evaluate(Context cx, TryStatementNode node)
    {
        // these can show up as top-level statements in classes, and do not affect signature
        // ideally I'd like to check that the statement is either top level and return null,
        // or not top level an unreachable (in which case an assertion gets thrown).
        // currently no way to check what 'level' this definition is at... safe enough, though.
        //UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, CatchClauseNode node)
    {
        // see the comments above for TryStatementNode... same reasoning
        //UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    
    
    
    
    
//     _____ _   _                 _   _           _
//    |  _  | | | |               | \ | |         | |
//    | | | | |_| |__   ___ _ __  |  \| | ___   __| | ___  ___
//    | | | | __| '_ \ / _ \ '__| | . ` |/ _ \ / _` |/ _ \/ __|
//    \ \_/ / |_| | | |  __/ |    | |\  | (_) | (_| |  __/\__ \
//     \___/ \__|_| |_|\___|_|    \_| \_/\___/ \__,_|\___||___/

    public Value evaluate(Context cx, ArgumentListNode node)
    {
        for(Iterator iter = node.items.iterator(); iter.hasNext(); )
        {
            Node item = (Node)iter.next();
            item.evaluate(cx, this);
            
            // example: LiteralObjectNode.fieldlist: { foo:bar, two:2 } 
            if(iter.hasNext())
                buffer.append(", ");
        }
        return null;
    }
    
    
    public Value evaluate(Context cx, ListNode node)
    {
        for(Iterator iter = node.items.iterator(); iter.hasNext(); )
        {
            ((Node)iter.next()).evaluate(cx, this);
            
            // this can happen on "A" and "B" when, e.g., you have "implements A, B"
            if(iter.hasNext())
                buffer.append(", ");
        }
        return null;
    }
    
    
    public Value evaluateSorted(Context cx, ListNode node)
    {
        final StringBuffer lastBuffer = buffer;
        
        // temporarily swap out the in-use buffer
        final TreeSet sorted = new TreeSet();
        buffer = new StringBuffer(128);
        {
            // evaluate all elements of the list to strings, and sort on that
            for(Iterator iter = node.items.iterator(); iter.hasNext(); )
            {
                ((Node)iter.next()).evaluate(cx, this);
                sorted.add(getAndClearBuffer());
            }
        }
        buffer = lastBuffer;

        // now add the sorted elements into the original buffer
        for(Iterator iter = sorted.iterator(); iter.hasNext(); )
        {
            buffer.append(iter.next());
            if(iter.hasNext())
                buffer.append(", ");
        }
        
        return null;
    }
    
    
    public Value evaluate(Context cx, GetExpressionNode node)
    {
        if (node.expr != null)
        {
            if (node.expr instanceof ArgumentListNode)
            {
                //TODO commenting this because it probably works,
                //     but it is still technically untested:
                // UNTESTED_CODEPATH(node);
                buffer.append("[");
                node.expr.evaluate(cx, this);
                buffer.append("]");
            }
            else
                node.expr.evaluate(cx, this);
        }

        return null;
    }
    
    
    // e.g., mx_internal::bar.Baz
    public Value evaluate(Context cx, QualifiedIdentifierNode node)
    {
        if (node.qualifier != null)
        {
            node.qualifier.evaluate(cx, this);
            // NOTE: "::" is not correct syntax, it should just be a dot,
            //       but this is a signature, so it doesn't matter.
            //
            // Pete: I know this is just trying to generate a unique signature,
            //       so this is just academic, but wouldn't :: be correct if use
            //       namespace had not been declared on the file, and a dot if it had?           
            buffer.append("::");
        }
        
        // eval node to get it's value
        evaluate(cx, (IdentifierNode) node);
        
        return null;
    }
    
    
    // e.g., xmldata.@ns::["id"]
    // TODO This needs work to support all the types of this expression (incomplete):
    //        xmldata.@foo::["id"]
    //        xmldata.@*[1]
    //        xdata.@id
    public Value evaluate(Context cx, QualifiedExpressionNode node)
    {
        buffer.append("@");
        evaluate(cx, (QualifiedIdentifierNode)node);
        if (node.expr != null)
        {
            //TODO I don't know how complex the expr can be, e.g. (logical ? "id1" : "id2")
            //     but anything other than a simple literal is probably unsupported
            buffer.append("[");
            node.expr.evaluate(cx, this);
            buffer.append("]");
        }
        return null;
    }

    
    // e.g., interface foo extends bar.baz ("bar." is the base) 
    public Value evaluate(Context cx, MemberExpressionNode node)
    {
        if (node.base != null)
        {
            node.base.evaluate(cx, this);
            if ((node.selector instanceof GetExpressionNode) &&
                (!(((GetExpressionNode) node.selector).expr instanceof ArgumentListNode)))
            {
                buffer.append(".");
            }
        }

        if (node.selector != null)
            node.selector.evaluate(cx, this);

        return null;
    }
    
    /**
     * Basically the same as MemberExpressionNodes, but only for type expressions.
     */
    public Value evaluate(Context cx, TypeExpressionNode node)
    {
        return super.evaluate(cx, node);
    }
    
    
    /**
     * Attributes are printed in (alphabetical) order.
     */
    public Value evaluate(Context cx, AttributeListNode node)
    {
        final String attrs = NodeMagic.getSortedAttributeString(node, " ");
        if(attrs.length() > 0)
        {
            buffer.append(attrs).append(" ");
        }
        
        return null;
    }
    
    // Vector.<int>
    public Value evaluate(Context cx, ApplyTypeExprNode node)
    {
        // e.g. Vector 
        if (node.expr != null)
            node.expr.evaluate(cx, this);
        
        buffer.append(".<");

        // e.g. <int>
        if (node.typeArgs != null)
            node.typeArgs.evaluate(cx, this);
        
        buffer.append(">");
        
        return null;
    }
    
    
    
//     _     _ _                 _   _   _           _
//    | |   (_) |               | | | \ | |         | |
//    | |    _| |_ ___ _ __ __ _| | |  \| | ___   __| | ___  ___
//    | |   | | __/ _ \ '__/ _` | | | . ` |/ _ \ / _` |/ _ \/ __|
//    | |___| | ||  __/ | | (_| | | | |\  | (_) | (_| |  __/\__ \
//    \_____/_|\__\___|_|  \__,_|_| \_| \_/\___/ \__,_|\___||___/
    
//  TODO Different literal nodes can evaluate to the same textual representation,
//  for instance (x=2, y="2") or (x="true", y=true). If the value changes from one to
//  the other, the signature won't change. I could differentiate each literal's output
//  slightly, though it will look a little stupid. On the other other hand, it might not matter --
//  if a variable id typed to int and changes to String, it will diff; if it is typed to */Object,
//  then it may not matter that it changed type.
    
    public Value evaluate(Context cx, IdentifierNode node)
    {
        buffer.append(node.name);
        return null;
    }
    
    
    public Value evaluate(Context cx, LiteralArrayNode node)
    {
        buffer.append("[");
        {
            super.evaluate(cx, node);
        }
        buffer.append("]");
        
        return null;
    }

    
    public Value evaluate(Context cx, LiteralBooleanNode node)
    {
        buffer.append(node.value);
        return null;
    }

    
    // these are the key:value pairs in a LiteralObjectNode
    public Value evaluate(Context cx, LiteralFieldNode node)
    {
        if (node.name != null)
            node.name.evaluate(cx, this);

        if (node.value != null)
        {
            buffer.append(':');
            node.value.evaluate(cx, this);
        }
        
        return null;
    }

    
    public Value evaluate(Context cx, LiteralNullNode node)
    {
        buffer.append("null");
        return null;
    }
 
    
    public Value evaluate(Context cx, LiteralNumberNode node)
    {
        buffer.append(node.value);
        return null;
    }
    

    public Value evaluate(Context cx, LiteralObjectNode node)
    {
        buffer.append('{');
        {
            if (node.fieldlist != null)
                node.fieldlist.evaluate(cx, this);
        }
        buffer.append('}');
        return null;
    }

    
    public Value evaluate(Context cx, LiteralRegExpNode node)
    {
        buffer.append(node.value);
        return null;
    }

    
    public Value evaluate(Context cx, LiteralStringNode node)
    {
        // TODO I'd like to be able to print quotes someday... meaning checking for LSNs in places
        //      like InterfaceDefNodes and ClassDefNodes, and MemberExprNodes, and overriding
        //      the evaluating behavior
        
        // I don't do this anymore because LiteralStringNodes are used in places like
        // class Foo extends "FooPackage.Bar".MyClass
        // which looks wrong, AND it's used in places where the original syntax was "a string"
        
        // buffer.append('"');
        buffer.append(node.value);
        // buffer.append('"');
        
        return null;
    }

    
    
//   ____    _    _   _ ___ _______   __
//  / ___|  / \  | \ | |_ _|_   _\ \ / /
//  \___ \ / _ \ |  \| || |  | |  \ V /
//   ___) / ___ \| |\  || |  | |   | |
//  |____/_/   \_\_| \_|___| |_|   |_|
//
//     _ _  _  _  ___ ___   _   __  _ _   _   ___ _    ___    __ _  __  ___
//    | | || \| || o \ __| / \ / _|| U | / \ | o ) |  | __|  / _/ \|  \| __|
//    | U || \\ ||   / _| | o ( (_ |   || o || o \ |_ | _|  ( (( o ) o ) _|
//    |___||_|\_||_|\\___||_n_|\__||_n_||_n_||___/___||___|  \__\_/|__/|___|
//
// The following evaluators should be unreachable and not in-use (if I am not mistaken)
    
    public Value evaluate(Context cx, Node node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, VariableBindingNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, UntypedVariableBindingNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, TypedIdentifierNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, ParenExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ParenListExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, FunctionSignatureNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, FunctionCommonNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, PackageIdentifiersNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, PackageNameNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, ClassNameNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, FunctionNameNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ImportNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, ReturnStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, SuperExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, SuperStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, SwitchStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ThisExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ThrowStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, UnaryExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, WhileStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, WithStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, DeleteExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, DoStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, FinallyClauseNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, ForStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, IfStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, IncrementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, RestParameterNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, BreakStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, CallExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, CaseLabelNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ConditionalExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, ContinueStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    public Value evaluate(Context cx, BinaryClassDefNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, BinaryExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, BinaryFunctionDefinitionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, BinaryProgramNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // used in for..in loops
    public Value evaluate(Context cx, HasNextNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // used in for..in loops
    public Value evaluate(Context cx, LoadRegisterNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // used in for..in loops
    public Value evaluate(Context cx, StoreRegisterNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // used in for..in loops
    public Value evaluate(Context cx, RegisterNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // seems like these are unused in syntax trees, just used during parsing/generation of classes
    public Value evaluate(Context cx, InheritanceNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    // see DataBindingFirtPassEvaluator.java::evaluate(Context context, InvokeNode node)
    public Value evaluate(Context cx, InvokeNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    // evaluated implicitly FunctionDefinitionNode
    public Value evaluate(Context cx, ParameterListNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }

    // evaluated implicitly FunctionDefinitionNode
    public Value evaluate(Context cx, ParameterNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // evaluated implicitly FunctionDefinitionNode
    public Value evaluate(Context cx, RestExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // abusive old-school labels for break and continue statements
    public Value evaluate(Context cx, LabeledStatementNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    
    // TODO BoxNodes do not seem to be created ANYWHERE in ASC or MXMLC
    //      Remove from Evaluator?
    // not part of AS3 syntax
    public Value evaluate(Context cx, BoxNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // not part of AS3 syntax
    public Value evaluate(Context cx, CoerceNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // TODO ToObjectNodes do not seem to be created ANYWHERE in ASC or MXMLC
    //      Remove from Evaluator?
    // not part of AS3 syntax
    public Value evaluate(Context cx, ToObjectNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // unimplemented in AS3 syntax: "use pragmaDirective"
    public Value evaluate(Context cx, PragmaNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    // unimplemented in AS3 syntax: "use pragmaDirective"
    public Value evaluate(Context cx, PragmaExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    public Value evaluate(Context cx, SetExpressionNode node)
    {
        UNREACHABLE_CODEPATH(node);
        return null;
    }
    
    
    
//     ___  _ _  ___  __  ___   __   _    _  _  _ ___   _   _   _  ___  ___  ___  ___
//    |_ _|| U || __|/ _|| __| |  \ / \  | \| |/ \_ _| | \_/ | / \|_ _||_ _|| __|| o \
//     | | |   || _| \_ \| _|  | o | o ) | \\ ( o ) |  | \_/ || o || |  | | | _| |   /
//     |_| |_n_||___||__/|___| |__/ \_/  |_|\_|\_/|_|  |_| |_||_n_||_|  |_| |___||_|\\
//
// These are in-use and can be commented out for production and left to the superclass.

    public void setLocalizationManager(LocalizationManager l10n)
    {
        super.setLocalizationManager(l10n);
    }
    
    public Value evaluate(Context cx, DocCommentNode node)
    {
        return super.evaluate(cx, node);
    }
    
    public Value evaluate(Context cx, EmptyStatementNode node)
    {
        return super.evaluate(cx, node);
    }

    // [1, 2, , 4] -- where 3 is an EmptyElementNode
    public Value evaluate(Context cx, EmptyElementNode node)
    {
        return super.evaluate(cx, node);
    }

    public Value evaluate(Context cx, IncludeDirectiveNode node)
    {
        return super.evaluate(cx, node);
    }

    public Value evaluate(Context cx, StatementListNode node)
    {
        return super.evaluate(cx, node);
    }
    
    public Value evaluate(Context cx, LiteralXMLNode node)
    {
        return super.evaluate(cx, node);
    }
    
    // TODO I'd like to find a way to generate one of these...
    //      should I catch this node and assume that the signature is not valid?
    public Value evaluate(Context cx, ErrorNode node)
    {
        return super.evaluate(cx, node);
    }
}



//     _   _      _                   _____ _
//    | | | |    | |                 /  __ \ |
//    | |_| | ___| |_ __   ___ _ __  | /  \/ | __ _ ___ ___  ___  ___
//    |  _  |/ _ \ | '_ \ / _ \ '__| | |   | |/ _` / __/ __|/ _ \/ __|
//    | | | |  __/ | |_) |  __/ |    | \__/\ | (_| \__ \__ \  __/\__ \
//    \_| |_/\___|_| .__/ \___|_|     \____/_|\__,_|___/___/\___||___/
//                 | |
//                 |_|


//      _  ___  ___  ___ _  ___ _ _  ___  ___   _  _  _  ___ _     __   _   __  _ _  ___
//     / \|_ _||_ _|| o \ || o ) | ||_ _|| __| | || \| || __/ \   / _| / \ / _|| U || __|
//    | o || |  | | |   / || o \ U | | | | _|  | || \\ || _( o ) ( (_ | o ( (_ |   || _|
//    |_n_||_|  |_| |_|\\_||___/___| |_| |___| |_||_|\_||_| \_/   \__||_n_|\__||_n_||___|

class AttributeInfoCache
{
    private Map attributeInfoCache = new HashMap();
    
    /**
     * This caches computed AttributeInfo objects because:
     *   - It's not cheap to compute (look at NodeMagic.getUserNamespace for instance)
     *   - Evaluating MetaData requires looking at the attributes of MetaData.def
     *   - The use case of a particular definition getting looked up more than once is
     *     pretty high (due to MetaData.def)
     */
    public AttributeInfo getAttributeInfo(DefinitionNode node)
    {
        AttributeInfo info = (AttributeInfo)attributeInfoCache.get(node);
        
        if (info == null)
            attributeInfoCache.put(node, (info = new AttributeInfo(node)));
        
        return info;
    }
}



//      _  ___  ___  ___ _  ___ _ _  ___  ___   _  _  _  ___ _
//     / \|_ _||_ _|| o \ || o ) | ||_ _|| __| | || \| || __/ \
//    | o || |  | | |   / || o \ U | | | | _|  | || \\ || _( o )
//    |_n_||_|  |_| |_|\\_||___/___| |_| |___| |_||_|\_||_| \_/

class AttributeInfo
{
    public final boolean isInternal, isPrivate, isPublic, isProtected, isUser;
    
    /**
     * This is an expensive constructor, consider using an AttributeInfoCache
     * instead of calling this directly.
     */
    public AttributeInfo(DefinitionNode node)
    {
        final AttributeListNode attrs = node.attrs;
        
        // determine if we are internal
        if (attrs == null)
        {
            // implicit internal scope
            isPrivate = isPublic = isProtected = isUser = false;
            
            isInternal = true;
        }
        else
        {
            isPublic    = attrs.hasAttribute(NodeMagic.PUBLIC);
            isPrivate   = attrs.hasAttribute(NodeMagic.PRIVATE);
            isProtected = attrs.hasAttribute(NodeMagic.PROTECTED);
            isUser      = !NodeMagic.getUserNamespace(node).equals(QName.DEFAULT_NAMESPACE);
            
            isInternal  = (attrs.hasAttribute(NodeMagic.INTERNAL) ||
                    !(isPublic || isPrivate || isProtected || isUser));
        }
    }
}


/**
 * This is the only exception that SignatureEvaluator throws. It is unchecked for
 * efficiency and because the Evaluator interface is incompatible.
 */
class SignatureAssertionRuntimeException extends RuntimeException
{
    /** serialVersionUID */
    private static final long serialVersionUID = 345392517896916290L;
 
    public Node node;
    
    public SignatureAssertionRuntimeException(String exn, Node node)
    {
        super(exn);
        this.node = node;
    }
}
