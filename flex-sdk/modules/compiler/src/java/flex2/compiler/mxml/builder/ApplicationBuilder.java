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

package flex2.compiler.mxml.builder;

import flex2.compiler.CompilationUnit;
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.lang.BindingHandler;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.lang.ValueNodeHandler;
import flex2.compiler.mxml.lang.TextParser;
import flex2.compiler.mxml.reflect.Property;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.*;
import flex2.compiler.util.QName;

import java.util.*;

/**
 * TODO rename this DocumentBuilder, as it processes the root node of an MXML document, not just an <Application/>.
 *
 * TODO the overriding of ComponentBuilder.ComponentChildNodeHandler callouts is starting to get complicated, due to
 * differences between processing the root and the children. Should probably bite the bullet and create a
 * DocumentChildNodeHandler in mxml.lang
 *
 * @author Clement Wong
 */
public class ApplicationBuilder extends ComponentBuilder
{
	/**
	 * Note: kind of a messy overlap here. There is a set of "special" root attributes (attributes that may appear on
	 * the root, that aren't properties/effects/styles/events). These are skipped by the normal compilation process,
	 * which uses isSpecialAttribute() to detect them. These all have meaning downstream, and are collected by
	 * parseRootAttributes(). They're held in the specialAttributes constant set.
	 * <p>
	 * Then there are also a handful of "ordinary" attributes that *also* have downstream meaning, when they appear on
	 * the root. They are *both* processed by parseRootAttributes(), and by the normal compilation process.
	 * These are listed below by the rootAttr* constants.
	 */
	private static final String rootAttrBackgroundColor = "backgroundColor";
	private static final String rootAttrHeight = "height";
	private static final String rootAttrWidth = "width";

	private static final String specialAttrFrameRate = "frameRate";
	private static final String specialAttrImplements = "implements";
	private static final String specialAttrLib = "lib";
	private static final String specialAttrPageTitle = "pageTitle";
	private static final String specialAttrPreloader = "preloader";
	private static final String specialAttrRsl = "rsl";
	private static final String specialAttrScriptRecursionLimit = "scriptRecursionLimit";
	private static final String specialAttrScriptTimeLimit = "scriptTimeLimit";
	private static final String specialAttrTheme = "theme";
	private static final String specialAttrUsePreloader = "usePreloader";

	private static Set specialAttributes = new HashSet(32);
	static
	{
		specialAttributes.add(specialAttrFrameRate);
		specialAttributes.add(specialAttrImplements);
		specialAttributes.add(specialAttrLib);
		specialAttributes.add(specialAttrPageTitle);
		specialAttributes.add(specialAttrPreloader);
		specialAttributes.add(specialAttrRsl);
		specialAttributes.add(specialAttrScriptRecursionLimit);
		specialAttributes.add(specialAttrScriptTimeLimit);
		specialAttributes.add(specialAttrTheme);
		specialAttributes.add(specialAttrUsePreloader);
	}

	public ApplicationBuilder(CompilationUnit unit,
							  TypeTable typeTable,
							  Configuration configuration,
							  MxmlDocument document)
	{
		super(unit, typeTable, configuration, document, null, false, null);

		//	NOTE: override already-initialized childNodeHandler
		this.childNodeHandler = new ApplicationChildNodeHandler(typeTable);

		this.rootAttributeParser = new RootAttributeParser(typeTable);
		this.nestedDeclarationNodeHandler = new NestedDeclarationNodeHandler();
		this.componentDeclarationBindingHandler = new ComponentDeclarationBindingHandler();
		this.primitiveDeclarationBindingHandler = new PrimitiveDeclarationBindingHandler();
	}

	protected RootAttributeParser rootAttributeParser;
	protected NestedDeclarationNodeHandler nestedDeclarationNodeHandler;
	protected ComponentDeclarationBindingHandler componentDeclarationBindingHandler;
	protected PrimitiveDeclarationBindingHandler primitiveDeclarationBindingHandler;

	private boolean generateLoader = true;

	public void analyze(Node node)
	{
		if (node.getAttribute("id") != null)
		{
			log(node, new IdNotAllowedOnRoot());
		}

		Type type = nodeTypeResolver.resolveType(node);

		constructComponent(type, node.beginLine);

		//	TODO eliminate horrible confusion by renaming one or the other "root" below,
		//	TODO for similar reasons, rename ApplicationNode (really DocumentNode) - see comment header

		//	NOTE: "document.root" means the root component, i.e. the component represented by the root node
		document.setRoot(component);

		processAttributes(node, type);
		processChildren(node, type);

		//	post-processing grab bag
		document.resolveTwoWayBindings();

		//	post-processing on *application component only* - that's what unit.isRoot() means
		if (unit.isRoot())
		{
			rootPostProcess(node);

			//	at the moment, binding destinations aren't set up until some arbitrary time after construction, so we have
			//  to do this import fixup late.
			//
			//	TODO add BindingExpression factory functions which set destination stuff up
			//  immediately, then shift this addImport() into MxmlDocument.addBindingExpression().
			//
			for (Iterator iter = document.getBindingExpressions().iterator(); iter.hasNext(); )
			{
				BindingExpression bexpr = (BindingExpression)iter.next();
				document.addImport(bexpr.getDestinationTypeName(), bexpr.getXmlLineNumber());
			}
		}
	}

	public void analyze(MetaDataNode node)
	{
		CDATANode cdata = (CDATANode)node.getChildAt(0);
		if (cdata != null && cdata.image != null)
		{
			//	metadata scripts are added to document info in InterfaceCompiler

            // If the document sets Frame metadata, then we must not overwrite it.
            // Is there a better way to do this?   This seems really hacky and brittle.
            if (node.getText().toString().indexOf( "[Frame" ) != -1)
            {
                assert unit.isRoot();
                generateLoader = false;
            }
		}
	}

	public void analyze(StyleNode node)
	{
		if (node.getStyleSheet() != null)
		{
			try
			{
				document.getStylesContainer().extractStyles(node.getStyleSheet(), true);
			}
			catch (Exception exception)
			{
				String message = exception.getLocalizedMessage();
				if (message == null)
				{
					message = exception.getClass().getName();
				}
				logError(node, message);
			}
		}
	}

	public void analyze(WebServiceNode node)
	{
		WebServiceBuilder builder = new WebServiceBuilder(unit, typeTable, configuration, document);
		node.analyze(builder);
	}

	public void analyze(HTTPServiceNode node)
	{
		HTTPServiceBuilder builder = new HTTPServiceBuilder(unit, typeTable, configuration, document);
		node.analyze(builder);
	}

	public void analyze(RemoteObjectNode node)
	{
		RemoteObjectBuilder builder = new RemoteObjectBuilder(unit, typeTable, configuration, document);
		node.analyze(builder);
	}

	/**
	 *
	 */
	public void analyze(BindingNode node)
	{
		String source = (String) node.getAttribute("source");
		if (source == null)
		{
			log(node, new MissingAttribute("source"));
			return;
		}

		String destination = (String) node.getAttribute("destination");
		if (destination == null)
		{
			log(node, new MissingAttribute("destination"));
			return;
		}

		//	Note: allow source="{expr}" or source="expr"
		Object value = textParser.parseValue(source, typeTable.stringType, 0, node.beginLine, "source");
		BindingExpression bindingExpression = value instanceof BindingExpression ? (BindingExpression)value :
			new BindingExpression((String)value, node.beginLine, document);

		bindingExpression.setDestinationProperty(destination);
		bindingExpression.setDestinationLValue(destination);

		component.setProperty(destination, bindingExpression, bindingExpression.getXmlLineNumber());
	}

	/**
	 *
	 */
	protected boolean isLegalLanguageNode(Node node)
	{
		Class nodeClass = node.getClass();
		return super.isLegalLanguageNode(node) ||
				nodeClass == MetaDataNode.class ||
				nodeClass == StyleNode.class ||
				nodeClass == RemoteObjectNode.class ||
				nodeClass == HTTPServiceNode.class ||
				nodeClass == WebServiceNode.class ||
				nodeClass == BindingNode.class;
	}

	/**
	 * Override ComponentBuilder's isProhibitedProperty.
	 */
	protected boolean isAllowedProperty(Property property)
	{
		return true;
	}

	/**
	 * override ComponentBuilder.ComponentAttributeHandler for root-node special handling
	 */
	protected class ApplicationAttributeHandler extends ComponentBuilder.ComponentAttributeHandler
	{
		/**
		 * even if our supertype is dynamic, we (an MXML document) never define a dynamic class.
		 */
		protected void dynamicProperty(String name)
		{
			unknownAttributeError(name, line);
		}
	}

	/**
	 * override ComponentBuilder.ChildNodeHandler for root-node special handling
	 */
	protected class ApplicationChildNodeHandler extends ComponentBuilder.ComponentChildNodeHandler
	{
		ApplicationChildNodeHandler(TypeTable typeTable)
		{
			super(typeTable);
		}

		/**
		 * even if our supertype is dynamic, we (an MXML document) never define a dynamic class. If the dynamic
		 * property handler has been called, it means that no statically defined entity by this name could be found on
		 * our backing type, so here we just route it to the handler it would've gone to had our backing class been static.
		 */
		protected void dynamicProperty(String name)
		{
			nestedDeclaration();
		}

		/**
		 * Default properties are suppressed on the root - they step on the syntactic territory used by top-level
		 * declarations. So here we're routing to the handler that would have been called had our backing class had no DP.
		 * <p>
		 * But note that we come through here on the root of an inline component as well. It would be consistent to
		 * treat them identically, but since a) nested declarations are used 0% of the time within inline components,
		 * and b) the misunderstanding would be silent, we're going to choose usability over consistency here.
		 */
		protected void defaultPropertyElement(boolean locError)
		{
			if (ApplicationBuilder.this.document.getIsInlineComponent())
			{
				//	In an inline component, process default property elements as usual. Note that this
				//	suppresses nested declarations inside inline components, as intended (see header comment).
				super.defaultPropertyElement(locError);
			}
			else
			{
				//	NOTE: we can *not* simply report an error here. If we did, MXML components based on classes with
				//	default properties would no longer be able to declare things FC-style at the top level.
				nestedDeclaration();
			}
		}

		/**
		 * in our case, nested declarations are top-level declarations. However, note that here we're subclassing the
		 * sub-handler that runs *after* the visual-child and repeater special cases have been checked.
		 * Note that we'll only be called for "value nodes" - nodes that represent values that can be translated into AS
		 * values everywhere. (See NodeTypeResolver.isValueNode() and ChildNodeHandler.invoke()) for details). Other top-level
		 * tags are considered "language tags" and result in a call to langaugeNode() rather than nestedDeclaration() in the handler.
		 */
		protected void processNestedDeclaration()
		{
			nestedDeclarationNodeHandler.invoke(child);
		}
	}

	/**
	 *
	 */
	protected class NestedDeclarationNodeHandler extends ValueNodeHandler
	{
		protected void componentNode(Node node)
		{
			ComponentBuilder builder = new ComponentBuilder(unit, typeTable, configuration, document, component, true,
					componentDeclarationBindingHandler);
			node.analyze(builder);
		}

		protected void arrayNode(ArrayNode node)
		{
			ArrayBuilder builder = new ArrayBuilder(unit, typeTable, configuration, document);
			node.analyze(builder);
		}

		protected void primitiveNode(PrimitiveNode node)
		{
			PrimitiveBuilder builder = new PrimitiveBuilder(unit, typeTable, configuration, document, true, primitiveDeclarationBindingHandler);
			node.analyze(builder);
		}

		protected void xmlNode(XMLNode node)
		{
			XMLBuilder builder = new XMLBuilder(unit, typeTable, configuration, document);
			node.analyze(builder);
			registerModel(node, builder.xml, true);
		}
        
        protected void xmlListNode(XMLListNode node)
        {
            XMLListBuilder builder = new XMLListBuilder(unit, typeTable, configuration, document);
            node.analyze(builder);
            registerModel(node, builder.xmlList, true);
        }

		protected void modelNode(ModelNode node)
		{
			ModelBuilder builder = new ModelBuilder(unit, typeTable, configuration, document);
			node.analyze(builder);
		}

		protected void inlineComponentNode(InlineComponentNode node)
		{
			InlineComponentBuilder builder = new InlineComponentBuilder(unit, typeTable, configuration, document, true);
			node.analyze(builder);
		}

		protected void unknown(Node node)
		{
			assert false : "Unexpected node class in processNestedDeclaration: " + node.getClass();
		}
	}

	/**
	 *
	 */
	protected static class ComponentDeclarationBindingHandler implements BindingHandler
	{
		public BindingExpression invoke(BindingExpression bindingExpression, Model dest)
		{
			if (dest.getParent() != null)
			{
				bindingExpression.setDestination(dest.getParent());
				bindingExpression.setDestinationProperty(dest.getId());
				bindingExpression.setDestinationLValue(dest.getId());
				dest.getParent().setProperty(dest.getId(), bindingExpression, bindingExpression.getXmlLineNumber());
			}
			else
			{
				//	TODO does this ever happen?
				bindingExpression.setDestination(dest);
				bindingExpression.setDestinationLValue("");
			}

			return bindingExpression;
		}
	}

	/**
	 *
	 */
	protected static class PrimitiveDeclarationBindingHandler implements BindingHandler
	{
		public BindingExpression invoke(BindingExpression bindingExpression, Model dest)
		{
			bindingExpression.setDestination(dest);
			((Primitive)dest).setValue(null);
			return bindingExpression;
		}
	}


	/**
	 * Override ComponentAnalyzer.isSpecialAttribute()... Root node doesn't allow "id", but it allows for
	 * the attributes specified in isRootAttribute()...
	 */
	protected boolean isSpecialAttribute(String namespaceURI, String localPart)
	{
		return namespaceURI.length() == 0 && specialAttributes.contains(localPart);
	}

	/**
	 * Override ComponentBuilder.processSpecialAttributes()...
	 */
	protected void processSpecialAttributes(Node node)
	{
        if (unit.getSource().isRoot())
        {
		    parseRootAttributes(node);
        }
	}

    private static String buildSwfMetadata( Map varmap )
    {
        if ((varmap == null) || (varmap.size() == 0))
            return null;
        StringBuffer buf = new StringBuffer( 50 );
        buf.append( "[SWF( " );
        boolean more = false;
        for (Iterator it = varmap.keySet().iterator(); it.hasNext(); )
        {
            String var = (String) it.next();
            Object val = varmap.get( var );

            if (more)
                buf.append( ", " );
            else
                more = true;

            buf.append( var );
            buf.append( "='" );
            buf.append( val );
            buf.append( "'" );
        }
        buf.append( ")]" );

        return buf.toString();
    }

	// C: Most of these root attributes are linker properties. They should be saved to the CompilationUnit
	//    object or to a per-compile Context object...

	private void parseRootAttributes(Node node)
	{
        // NOTE: only put variables relevant to SWF production into the SWF metadata!
        // MXML-specific bootstrap info should be saved into the unit's context
        // and incorporated into the generated IFlexBootstrap derivative.

        Map swfvarmap = new TreeMap();
		String frameRate = (String) node.getAttribute(specialAttrFrameRate);
		if (frameRate != null)
		{
			Object value = rootAttributeParser.parseUInt(frameRate, node.getLineNumber(specialAttrFrameRate), specialAttrFrameRate);
			if (value != null)
			{
	            swfvarmap.put( specialAttrFrameRate, value.toString() );
			}
		}

		String scriptRecursionLimit = (String) node.getAttribute(specialAttrScriptRecursionLimit);
		if (scriptRecursionLimit != null)
		{
			Object value = rootAttributeParser.parseUInt(scriptRecursionLimit, node.getLineNumber(specialAttrScriptRecursionLimit), specialAttrScriptRecursionLimit);
			if (value != null)
			{
	            swfvarmap.put( specialAttrScriptRecursionLimit, value.toString() );
			}
		}

		String scriptTimeLimit = (String) node.getAttribute(specialAttrScriptTimeLimit);
		if (scriptTimeLimit != null)
		{
			Object value = rootAttributeParser.parseUInt(scriptTimeLimit, node.getLineNumber(specialAttrScriptTimeLimit), specialAttrScriptTimeLimit);
			if (value != null)
			{
	            swfvarmap.put( specialAttrScriptTimeLimit, value.toString() );
			}
		}

		String bgcolor = (String) node.getAttribute(rootAttrBackgroundColor);
		if (bgcolor != null)
		{
            Object value = rootAttributeParser.parseColor(bgcolor, node.getLineNumber(rootAttrBackgroundColor), rootAttrBackgroundColor);
            if (value != null)
            {
                swfvarmap.put( rootAttrBackgroundColor, value.toString() );
            }
		}

		String title = (String) node.getAttribute(specialAttrPageTitle);
		if (title != null)
		{
			swfvarmap.put( specialAttrPageTitle, title );
		}

		// Only do the "percent" logic for Application nodes, not modules. There is no
		// html wrapper for a module and the logic keeps modules from sizing
		// to the ModuleLoader component, SDK-9527.
        Type nodeType = nodeTypeResolver.resolveType(node);
		boolean isApplication = StandardDefs.isApplication(nodeType);
		
		String width = (String) node.getAttribute(rootAttrWidth);
        if (width != null && isApplication)
        {
			Object value = rootAttributeParser.parseNumberOrPercentage(width, node.getLineNumber(rootAttrWidth), rootAttrWidth);
			if (value != null)
			{
				if (rootAttributeParser.wasPercentage())
				{
					if (width.toString().endsWith("%"))
                    {
                        swfvarmap.put( "widthPercent", width.toString());
                    }
                    else
                    {
                        swfvarmap.put( "widthPercent", width.toString() + '%');
                    }

					//	HACK for 174078: width="n%" at the root of an MXML app is a specification of the ratio of
					//	player to browser width, not app to player width. So we pass it through to the SWF, but strip
					//	it from the MXML DOM, preventing it from showing up in the property settings for the root UIC.
					node.removeAttribute(new QName("", rootAttrWidth));
				}
				else
				{
					if (value instanceof Double)
					{
						value = new Integer(((Double) value).intValue());
					}
					swfvarmap.put( rootAttrWidth, value );
				}
			}
        }

        String height = (String) node.getAttribute(rootAttrHeight);
        if (height != null && isApplication)
        {
			Object value = rootAttributeParser.parseNumberOrPercentage(height, node.getLineNumber(rootAttrHeight), rootAttrHeight);
			if (value != null)
			{
				if (rootAttributeParser.wasPercentage())
				{
					if (height.toString().endsWith("%"))
                    {
                        swfvarmap.put( "heightPercent", height.toString());
                    }
                    else
                    {
                        swfvarmap.put( "heightPercent", height.toString() + '%');
                    }

					//	HACK for 174078: as above for width
					node.removeAttribute(new QName("", rootAttrHeight));
				}
				else
				{
					if (value instanceof Double)
					{
						value = new Integer(((Double) value).intValue());
					}
					swfvarmap.put( rootAttrHeight, value );
				}
			}
        }

		String usePreloader = (String) node.getAttribute(specialAttrUsePreloader);
		if (usePreloader != null)
		{
			Object value = rootAttributeParser.parseBoolean(usePreloader, node.getLineNumber(specialAttrUsePreloader), specialAttrUsePreloader);
			if (value != null)
			{
				document.setUsePreloader(((Boolean)value).booleanValue());
			}
		}

		String preloader = (String) node.getAttribute(specialAttrPreloader);
		if (preloader != null)
		{
			String preloaderClassName = TextParser.parseClassName(preloader);
			if (preloaderClassName != null)
			{
				document.setPreloader(preloader);
			}
			else
			{
				log(node, new InvalidPreLoaderClassName(preloader));
			}
		}

        if (swfvarmap.size() > 0)
        {
            String metadata = buildSwfMetadata( swfvarmap );
            Script script = new Script( metadata );
            document.addMetadata( script );
        }

		String theme = (String) node.getAttribute(specialAttrTheme);
		if (theme != null)
		{
            log(new ThemeAttributeError());
		}

		String rsl = (String) node.getAttribute(specialAttrRsl);
		if (rsl != null)
		{
            log(new RslAttributeError());
		}

		String lib = (String) node.getAttribute(specialAttrLib);
		if (lib != null)
		{
            log(new LibAttributeError());                        
		}
	}

	/**
	 *
	 */
	protected class RootAttributeParser extends TextValueParser
	{
		protected RootAttributeParser(TypeTable typeTable)
		{
			super(typeTable);
		}

		public Object parseUInt(String text, int line, String name)
		{
			return parseValue(text, typeTable.uintType, 0, line, name);
		}

		public Object parseColor(String text, int line, String name)
		{
			return parseValue(text, typeTable.uintType, FlagConvertColorNames, line, name);
		}

		public Object parseNumberOrPercentage(String text, int line, String name)
		{
			return parseValue(text, typeTable.numberType, FlagAllowPercentages, line, name);
		}

		public Object parseBoolean(String text, int line, String name)
		{
			return parseValue(text, typeTable.booleanType, 0, line, name);
		}

		//	TextParseHandler impl

		public Object embed(String text, Type type)
		{
			log(line, new EmbedNotAllowed());
			return null;
		}

		public Object bindingExpression(String converted)
		{
			log(line, new BindingNotAllowed());
			return null;
		}
	}

	/**
	 *
	 */
	private void rootPostProcess(Node node)
	{
		if (generateLoader)
   		{
   			generateLoaderInfo(node);
   		}
		else
		{
			document.addMetadata( new Script( "[Frame(extraClass=\"FlexInit\")]\n" ) );
		}
	}

	/**
	 *
	 */
	private void generateLoaderInfo(Node node)
	{
		String baseLoaderClass = document.getSuperClass().getLoaderClass();
		if (baseLoaderClass == null)
			return;

		unit.auxGenerateInfo = new HashMap();

		String generateInitClass = "_" + document.getClassName() + "_FlexInit";
        generateInitClass = generateInitClass.replaceAll( "[^A-Za-z0-9]", "_" );

		document.addMetadata( new Script( "[Frame(extraClass=\"" + generateInitClass + "\")]\n" ) );

		// fixme - the lingo of the classes specified are in package:name syntax
		// in order to be able to find them in unit.topLevelDefs.
		baseLoaderClass = baseLoaderClass.replace( ':', '.' );

		String generateLoaderClass = "_" + document.getClassName() + "_" + baseLoaderClass;
		generateLoaderClass = generateLoaderClass.replaceAll( "[^A-Za-z0-9]", "_" );

		document.addMetadata( new Script( "[Frame(factoryClass=\"" + generateLoaderClass + "\")]\n" ) );

		Map rootAttributeMap = new HashMap();
		//	Type type = typeTable.getType(node.getNamespace(), node.getLocalPart());

		for (Iterator it = node.getAttributeNames(); it != null && it.hasNext();)
		{
			QName qname = (QName) it.next();
			//	String namespace = qname.getNamespace();
			String localPart = qname.getLocalPart();

			/*
			if ((type.getProperty( localPart ) != null)
				|| (type.hasEffect( localPart ))
				|| (type.getStyle( localPart ) != null)
				|| (type.getEvent( localPart ) != null))
				continue;
			*/

			String value = (String) node.getAttribute( qname );
			value = value.replaceAll( "\"", "\\\"" );
			rootAttributeMap.put( localPart, node.getAttribute( qname ) );
		}

        String windowClass = document.getClassName();
        if ((document.getPackageName() != null) && (document.getPackageName().length() != 0))
        {
            windowClass = document.getPackageName() + "." + document.getClassName();
        }

        unit.auxGenerateInfo.put( "baseLoaderClass", baseLoaderClass );
		unit.auxGenerateInfo.put( "generateLoaderClass", generateLoaderClass );
		unit.auxGenerateInfo.put( "windowClass", windowClass );
		unit.auxGenerateInfo.put( "preloaderClass", document.getPreloader() );
		unit.auxGenerateInfo.put( specialAttrUsePreloader, new Boolean( document.getUsePreloader() ) );
		unit.auxGenerateInfo.put( "rootAttributes", rootAttributeMap );
	}

    public static class DefaultPropertyError extends CompilerError
    {
    }

    public static class IdNotAllowedOnRoot extends CompilerError
    {
    }

    public static class MissingAttribute extends CompilerError
    {
        public String attribute;

        public MissingAttribute(String attribute)
        {
            this.attribute = attribute;
        }
    }

    public static class ThemeAttributeError extends CompilerError
    {
    }

    public static class RslAttributeError extends CompilerError
    {
    }

    public static class LibAttributeError extends CompilerError
    {
    }

    public static class EmbedNotAllowed extends CompilerError
    {
    }

	public static class InvalidPreLoaderClassName extends CompilerError
	{
		public String className;
		public InvalidPreLoaderClassName(String className) { this.className = className; }
	}
}
