////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.analyzer;

import flash.css.StyleParser;
import flash.css.StyleSheet;
import flash.css.StyleParser.StyleSheetInvalidCharset;
import flash.fonts.FontManager;
import flash.util.FileUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.mxml.*;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;

import java.io.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 1. verify syntax tree, e.g. checking language tag attributes
 * 2. register includes and dependencies
 *
 * @author Clement Wong
 */
public class SyntaxAnalyzer extends AnalyzerAdapter
{
	public SyntaxAnalyzer(CompilationUnit unit, Configuration configuration)
	{
		super(unit, configuration);
	}

	/**
	 * At parse-time, we want to register dependent packages/classes...
	 */
	public void analyze(Node node)
	{
		/**
		 * NOTE: since this analyzer runs at parse time, the information that would allow us to distinguish <mx:Component/>
		 * from <mx:childPropertyAssignment/> is not yet (guaranteed to be) available. As a result, both types of nodes will
		 * pass through this method, so we can't yet raise errors when a tag name fails to resolve to an implementing class.
		 */
		super.analyze(node);
	}

	public void analyze(CDATANode node)
	{
		// do nothing
	}

	public void analyze(StyleNode node)
	{
		checkForExtraAttributes(StyleNode.attributes, node);

		String source = (String) node.getAttribute("source");
		CDATANode cdata = (CDATANode) node.getChildAt(0);

		if (source != null && cdata != null)
		{
			log(node, node.getLineNumber("source"), new IgnoreEmbeddedStylesheet());
		}

		if (source != null)
		{
            if (TextParser.getBindingExpressionFromString(source) != null)
            {
                log(node, node.getLineNumber("source"), new CompileTimeAttributeBindingExpressionUnsupported());
                return;
            }

			// C: Look at the problem this way, AS3 can have [Embed], MXML can have @embed, CSS can have @embed.
			// AS3 and MXML can "import" each others types. Does it make sense for AS3 or MXML to "import" CSS?
			// Currently, external CSS stylesheets are pulled in and codegen within MXML-generated classes. Can
			// CSS be generated in a separate class/factory and make MXML "import" it?
			//
			// Can CSS embedded within <mx:Style> be generated within the MXML-generated class as an inner class?

			VirtualFile file = unit.getSource().resolve(source);

			if (file == null)
			{
                VirtualFile[] sourcePath = configuration.getSourcePath();

                if (sourcePath != null)
                {
                    for (int i = 0; (i < sourcePath.length) && (file == null); i++)
                    {
                        file = sourcePath[i].resolve(source);
                    }
                }
			}

			if (file == null)
			{
				log(node, node.getLineNumber("source"), new StylesheetNotFound(source));
			}
			else
			{
				unit.getSource().addFileInclude(file);
				cdata = parseExternalFile(node, file);
				if (cdata != null)
				{
					//	parseStyle(node, unit.getSource().getName(), cdata);
					parseStyle(node, file.getName(), file.getLastModified(), cdata);
				}
			}
		}
		else if (cdata != null)
		{
			parseStyle(node, unit.getSource().getName(), unit.getSource().getLastModified(), cdata.beginLine);
		}
	}

	public void analyze(ScriptNode node)
	{
		checkForExtraAttributes(ScriptNode.attributes, node);
		script(node);
	}

	public void analyze(MetaDataNode node)
	{
		checkForExtraAttributes(MetaDataNode.attributes, node);
	}

	public void analyze(ModelNode node)
	{
		checkForExtraAttributes(ModelNode.attributes, node);

		String source = (String) node.getAttribute("source");
		int count = node.getChildCount();

		if (source != null && count > 0)
		{
			log(node, node.getLineNumber("source"), new EmptyTagIfSourceSpecified());
		}

		if (source != null)
		{
            if (TextParser.getBindingExpressionFromString(source) != null)
            {
                log(node, node.getLineNumber("source"), new CompileTimeAttributeBindingExpressionUnsupported());
                return;
            }

			// parse external XML file...
			VirtualFile f = unit.getSource().resolve(source);
			if (f == null)
			{
				log(node, node.getLineNumber("source"), new ModelNotFound(source));
			}
			else
			{
				unit.getSource().addFileInclude(f);
				Node root = parseExternalXML(node, f);

				// C: 2.0 behavior: don't remove the root tag for <mx:Model>. it should be similar to
				//    <mx:XML> w.r.t. syntactical processing.
				if (root != null)
				{
				    node.setSourceFile(new Node[] {root});
				}

				/* C: 1.x behavior...
				int size = (root == null) ? 0 : root.getChildCount();
				if (size > 0)
				{
					if (size == 1 && root.getChildAt(0) instanceof CDATANode)
					{
						log(node, node.getLineNumber("source"), new ScalarContentOnlyUnsupportedInExternalModel());
					}
					else
					{
						// C: Keep the document structure intact. Add the source-based nodes to ModelNode separately
						// from the children...
						Node[] nodes = new Node[size];
						for (int j = 0; j < size; j++)
						{
							nodes[j] = (Node) root.getChildAt(j);
						}
						node.setSourceFile(nodes);
					}
				}
				*/
			}
		}
	}

	public void analyze(XMLNode node)
	{
		checkForExtraAttributes(XMLNode.attributes, node);

		String source = (String) node.getAttribute("source");
		// C: count = 0 or 1 CDATA or multiple child tags
		int count = node.getChildCount();

		if (source != null && count > 0)
		{
			log(node, node.getLineNumber("source"), new IgnoreInlineXML());
		}

		if (source != null)
		{
            if (TextParser.getBindingExpressionFromString(source) != null)
            {
                log(node, node.getLineNumber("source"), new CompileTimeAttributeBindingExpressionUnsupported());
                return;
            }

			// parse external XML file...
			VirtualFile f = unit.getSource().resolve(source);
			if (f == null)
			{
				log(node, node.getLineNumber("source"), new XMLNotFound(source));
			}
			else
			{
				unit.getSource().addFileInclude(f);
				Node root = parseExternalXML(node, f);

                if (root != null)
                {
                    node.setSourceFile(new Node[] {root});
                }
			}
		}
	}

    public void analyze(XMLListNode node)
    {
        checkForExtraAttributes(XMLListNode.attributes, node);
    }

	public void analyze(ArrayNode node)
	{
		checkForExtraAttributes(ArrayNode.attributes, node);
		super.analyze(node);
	}

	public void analyze(BindingNode node)
	{
		checkForExtraAttributes(BindingNode.attributes, node);

		String source = (String) node.getAttribute("source");
		if (source == null || source.trim().length() == 0)
		{
			log(node, new BindingMustHaveSource());
		}

		String destination = (String) node.getAttribute("destination");
		if (destination == null || destination.trim().length() == 0)
		{
			log(node, new BindingMustHaveDestination());
		}
	}

	public void analyze(StringNode node)
	{
		checkForExtraAttributes(StringNode.attributes, node);
		primitive(node);
	}

	public void analyze(NumberNode node)
	{
		checkForExtraAttributes(NumberNode.attributes, node);
		primitive(node);
	}

    public void analyze(IntNode node)
    {
        checkForExtraAttributes(IntNode.attributes, node);
        primitive(node);
    }

    public void analyze(UIntNode node)
    {
        checkForExtraAttributes(UIntNode.attributes, node);
        primitive(node);
    }

    public void analyze(BooleanNode node)
	{
		checkForExtraAttributes(BooleanNode.attributes, node);
		primitive(node);
	}

	public void analyze(RequestNode node)
	{
		checkForExtraAttributes(RequestNode.attributes, node);
		super.analyze(node);
	}

	public void analyze(ArgumentsNode node)
	{
		checkForExtraAttributes(ArgumentsNode.attributes, node);
		super.analyze(node);
	}

	public void analyze(InlineComponentNode node)
	{
		checkForExtraAttributes(InlineComponentNode.attributes, node);

		if (node.getChildCount() == 0)
		{
			log(node, new InlineComponentMustHaveOneChild());
		}

		super.analyze(node);
	}

	// private void checkForExtraAttributes(Set<QName> validAttributes, Iterator<QName> attributes)
	private void checkForExtraAttributes(Set validAttributes, Node node)
	{
		for (Iterator attributes = node.getAttributeNames(); attributes != null && attributes.hasNext();)
		{
			// QName qname = attributes.next();
			QName qname = (QName) attributes.next();
			if (!validAttributes.contains(qname))
			{
				log(node, node.getLineNumber(qname), new UnknownAttribute(qname, node.image));
			}
		}
	}

	private void script(ScriptNode node)
	{
		String source = (String) node.getAttribute("source");
		CDATANode cdata = (CDATANode) node.getChildAt(0);

		if (source != null && cdata != null)
		{
			log(node, node.getLineNumber("source"), new IgnoreInlineScript());
		}

		// C: Again, all source="..." must be registered to unit.includes.

		if (source != null)
		{
            if (TextParser.getBindingExpressionFromString(source) != null)
            {
                log(node, node.getLineNumber("source"), new CompileTimeAttributeBindingExpressionUnsupported());
                return;
            }

			VirtualFile f = unit.getSource().resolve(source);
			if (f == null)
			{
				log(node, node.getLineNumber("source"), new ScriptNotFound(source));
			}
			else
			{
				unit.getSource().addFileInclude(f);
				CDATANode n = parseExternalFile(node, f);

				// C: We want to keep the document structure intact and parse the external file up-front. Store
				// the source="..." content in ScriptNode.

				if (n != null)
				{
					cdata = n;
					node.setSourceFile(n);
				}
			}
		}
	}

	private void primitive(PrimitiveNode node)
	{
		String source = (String) node.getAttribute("source");
		CDATANode cdata = (CDATANode) node.getChildAt(0);

		if (source != null && cdata != null)
		{
			log(node, node.getLineNumber("source"), new IgnoreEmbeddedString());
		}

		if (source != null)
		{
            if (TextParser.getBindingExpressionFromString(source) != null)
            {
                log(node, node.getLineNumber("source"), new CompileTimeAttributeBindingExpressionUnsupported());
                return;
            }

			// parse external plain text...
			VirtualFile f = unit.getSource().resolve(source);
			if (f == null)
			{
				log(node, node.getLineNumber("source"), new PrimitiveFileNotFound(source));
			}
			else
			{
				unit.getSource().addFileInclude(f);
				CDATANode n = parseExternalFile(node, f);

				// C: We want to keep the document structure intact and parse the external file up-front. Store
				// the source="..." content in PrimitiveNode.

				if (n != null)
				{
					cdata = n;
					node.setSourceFile(n);
				}
			}
		}
	}

	private Node parseExternalXML(Node node, VirtualFile f)
	{
		BufferedInputStream in = null;
		Node anonymousObject = null;
		try
		{
			in = new BufferedInputStream(f.getInputStream());
			Scanner s = new Scanner(in);
			Parser p = new Parser(s);
			Visitor v = new SyntaxTreeBuilder();
			p.setVisitor(v);
			anonymousObject = (Node) p.parseAnonymousObject();
		}
		catch (ScannerError se)
		{
			log(node, new XMLParseProblem1(f.getName(), se.getLineNumber(), se.getReason()));
        }
        catch (ParseException ex)
		{
			log(node, new XMLParseProblem2(f.getName()));
			Token token = ex.currentToken.next;
			logError(node, token.beginLine, ex.getMessage());
		}
		catch (IOException ex)
		{
			log(node, new XMLParseProblem3(f.getName(), ex.getMessage()));
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException ex)
				{
				}
			}
		}
		return anonymousObject;
	}

	private CDATANode parseExternalFile(Node node, VirtualFile f)
	{
		BufferedReader reader = null;
		CDATANode cdata = null;
		try
		{
            BufferedInputStream bufferedInputStream = new BufferedInputStream(f.getInputStream());
            String charsetName = null;
            
            // special handling to get the charset for CSS files.
            if (f.getName().toLowerCase().endsWith(".css")) 
            {
                try
                {
                    charsetName = StyleParser.readCSSCharset(bufferedInputStream);
                }
                catch (StyleSheetInvalidCharset e)
                {
                    // add filename to exception and log warning.
                    log(node, new StyleSheetInvalidCharset(f.getName(), e.charsetName));
                    return null;
                }
            }
            String bomCharsetName = FileUtils.consumeBOM(bufferedInputStream, null, true);
            if (charsetName == null) {
                charsetName = bomCharsetName;
            }
			reader = new BufferedReader(new InputStreamReader(bufferedInputStream, 
                                                              charsetName));
			StringWriter buffer = new StringWriter();
			PrintWriter out = new PrintWriter(buffer);
			String str = null;
			while ((str = reader.readLine()) != null)
			{
				out.println(str);
			}
			out.flush();
			cdata = new CDATANode();
			cdata.image = buffer.toString().trim();
		}
		catch (FileNotFoundException ex)
		{
			// f is not null. don't think this will happen.
			log(node, new ExternalFileNotFound(f.getName()));
		}
		catch (IOException ex)
		{
			log(node, new ParseFileProblem(f.getName(), ex.getMessage()));
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException ex)
				{
				}
			}
		}
		return cdata;
	}

    private void parseStyle(StyleNode node, String stylePath, long lastModified, CDATANode cdata)
	{
		FontManager fontManager = configuration.getFontsConfiguration().getTopLevelManager();

		StyleSheet styleSheet = new StyleSheet();
		styleSheet.checkDeprecation(configuration.showDeprecationWarnings());
		styleSheet.parse(stylePath, new StringReader(cdata.image), ThreadLocalToolkit.getLogger(), fontManager);

		if (styleSheet.errorsExist())
		{
			// Error
			log(node, new StyleSheetParseError(stylePath));
		}

		node.setStyleSheet(styleSheet);
	}

	private void parseStyle(StyleNode node, String enclosingDocumentPath, long lastModified, int startLine)
	{
		FontManager fontManager = configuration.getFontsConfiguration().getTopLevelManager();

		CDATANode cdata = (CDATANode) node.getChildAt(0);
		StyleSheet styleSheet = new StyleSheet();
		styleSheet.checkDeprecation(configuration.showDeprecationWarnings());
		styleSheet.parse(enclosingDocumentPath, startLine, new StringReader(cdata.image), ThreadLocalToolkit.getLogger(), fontManager);
		if (styleSheet.errorsExist())
		{
			// Error
			log(node, new StyleSheetParseError(enclosingDocumentPath));
		}

		node.setStyleSheet(styleSheet);
	}

	// error messages

	public static class IgnoreEmbeddedStylesheet extends CompilerMessage.CompilerWarning
	{
		public IgnoreEmbeddedStylesheet()
		{
			super();
		}
	}

	public static class CompileTimeAttributeBindingExpressionUnsupported extends CompilerMessage.CompilerError
	{
		public CompileTimeAttributeBindingExpressionUnsupported()
		{
			super();
		}
	}

	public static class StylesheetNotFound extends CompilerMessage.CompilerError
	{
		public StylesheetNotFound(String source)
		{
			super();
			this.source = source;
		}

		public final String source;
	}

	public static class EmptyTagIfSourceSpecified extends CompilerMessage.CompilerWarning
	{
		public EmptyTagIfSourceSpecified()
		{
			super();
		}
	}

	public static class ModelNotFound extends CompilerMessage.CompilerError
	{
		public ModelNotFound(String source)
		{
			super();
			this.source = source;
		}

		public final String source;
	}

	public static class ScalarContentOnlyUnsupportedInExternalModel extends CompilerMessage.CompilerError
	{
		public ScalarContentOnlyUnsupportedInExternalModel()
		{
			super();
		}
	}

	public static class IgnoreInlineScript extends CompilerMessage.CompilerWarning
	{
		public IgnoreInlineScript()
		{
			super();
		}
	}

	public static class IgnoreInlineXML extends CompilerMessage.CompilerWarning
	{
		public IgnoreInlineXML()
		{
			super();
		}
	}

	public static class XMLNotFound extends CompilerMessage.CompilerError
	{
		public XMLNotFound(String source)
		{
			super();
			this.source = source;
		}

		public final String source;
	}

	public static class BindingMustHaveSource extends CompilerMessage.CompilerError
	{
		public BindingMustHaveSource()
		{
			super();
		}
	}

	public static class BindingMustHaveDestination extends CompilerMessage.CompilerError
	{
		public BindingMustHaveDestination()
		{
			super();
		}
	}

	public static class UnknownAttribute extends CompilerMessage.CompilerError
	{
		public UnknownAttribute(QName qname, String tag)
		{
			super();
			this.qname = qname;
			this.tag = tag;
		}

		public final QName qname;
		public final String tag;
	}

	public static class ScriptNotFound extends CompilerMessage.CompilerError
	{
		public ScriptNotFound(String source)
		{
			super();
			this.source = source;
		}

		public final String source;
	}

	public static class IgnoreEmbeddedString extends CompilerMessage.CompilerWarning
	{
		public IgnoreEmbeddedString()
		{
			super();
		}
	}

	public static class PrimitiveFileNotFound extends CompilerMessage.CompilerError
	{
		public PrimitiveFileNotFound(String source)
		{
			super();
			this.source = source;
		}

		public final String source;
	}

	public static class XMLParseProblem1 extends CompilerMessage.CompilerError
	{
		public XMLParseProblem1(String name, int line, String reason)
		{
			super();
			this.name = name;
			this.line = line;
			this.reason = reason;
		}

		public final String name;
		public final int line;
		public final String reason;
	}

	public static class XMLParseProblem2 extends CompilerMessage.CompilerError
	{
		public XMLParseProblem2(String name)
		{
			super();
			this.name = name;
		}

		public final String name;
	}

	public static class XMLParseProblem3 extends CompilerMessage.CompilerError
	{
		public XMLParseProblem3(String name, String message)
		{
			super();
			this.name = name;
			this.message = message;
		}

		public final String name;
		public final String message;
	}

	public static class ExternalFileNotFound extends CompilerMessage.CompilerError
	{
		public ExternalFileNotFound(String name)
		{
			super();
			this.name = name;
		}

		public final String name;
	}

	public static class ParseFileProblem extends CompilerMessage.CompilerError
	{
		public ParseFileProblem(String name, String message)
		{
			super();
			this.name = name;
			this.message = message;
		}

		public final String name;
		public final String message;
	}

	public static class StyleSheetParseError extends CompilerMessage.CompilerError
	{
		public StyleSheetParseError(String stylePath)
		{
			super();
			this.stylePath = stylePath;
		}

		public final String stylePath;
	}

    public static class InlineComponentMustHaveOneChild extends CompilerMessage.CompilerError
	{
		public InlineComponentMustHaveOneChild()
		{
			super();
		}
	}
}

