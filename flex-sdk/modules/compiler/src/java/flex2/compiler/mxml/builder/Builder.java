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
import flex2.compiler.util.CompilerMessage.CompilerWarning;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.dom.*;
import flex2.compiler.mxml.lang.*;
import flex2.compiler.mxml.reflect.*;
import flex2.compiler.mxml.rep.*;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.QName;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
abstract class Builder extends AnalyzerAdapter
{
	protected TypeTable typeTable;
	protected MxmlDocument document;

	protected NodeTypeResolver nodeTypeResolver;
	protected TextValueParser textParser;
	protected RValueNodeHandler rvalueNodeHandler;

	Builder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document)
	{
		super(unit, configuration);

		this.typeTable = typeTable;
		this.document = document;

		this.nodeTypeResolver = new NodeTypeResolver(typeTable);
		this.textParser = new TextValueParser(typeTable);
		this.rvalueNodeHandler = new RValueNodeHandler();
	}

	/**
	 * Builder-generic text value parser. Uses Builder members for e.g. document access, error reporting.
	 * <p>Also, importantly, implements some universal side-effects for certain parsed values, including:
	 * <li>- parsed binding expressions are turned into BindingExpression objects, and added to the builder's MxmlDocument
	 * <li>- parsed @Embed expressions are turned into AtEmbed objects, and added to the builder's document
	 * <li>- parsed values for Class-typed properties (i.e., class names) are added as imports to the builder's document
	 * <p>Subclasses may provide custom entry points and handlers.
	 */
	protected class TextValueParser extends TextParser
	{
		protected String lvalueName;
		protected int line;
		protected String desc;
		protected boolean wasPercentage;

		TextValueParser(TypeTable typeTable)
		{
			super(typeTable);
		}

		/**
		 *
		 */
		public Object parseValue(String text, Type type, int flags, int line, String desc)
		{
			return parseValue(text, type, typeTable.objectType, flags, line, desc);
		}

		/**
		 *
		 */
		public Object parseValue(String text, Type type, Type arrayElementType, int flags, int line, String desc)
		{
			this.line = line;
			this.desc = desc;
			this.wasPercentage = false;
			return super.parse(text, type, arrayElementType, flags);
		}

		/**
		 * prevent subclasses from inadvertantly calling super utility routine
		 */
		protected Object parse(String text, Type type, Type arrayElementType, int flags)
		{
			assert false : "internal parse() called";
			return null;
		}

		/**
		 *
		 */
		public boolean wasPercentage()
		{
			return wasPercentage;
		}

        //	TextParser impl

		/**
		 *
		 */
		public String contextRoot(String text)
		{
			String contextRoot = configuration.getContextRoot();
			if (contextRoot == null)
			{
				error(ErrUndefinedContextRoot, text, null, null);
				return null;
			}
			else
			{
				return text.replaceAll("@ContextRoot\\(\\)", contextRoot);
			}
		}

		/**
		 *
		 */
		public Object embed(String text, Type type)
		{
            boolean strType = type.isAssignableTo(typeTable.stringType);
			AtEmbed atEmbed = AtEmbed.create(typeTable.getPerCompileData(), unit.getSource(), line, text, strType);
			if (atEmbed != null)
			{
				document.addAtEmbed(atEmbed);

				if (StandardDefs.isIFactory(type))
				{
					return factoryFromClass(atEmbed.getPropName(), line);
				}
				else if (StandardDefs.isIDeferredInstance(type))
				{
					return instanceFromClass(atEmbed.getPropName(), line, false);
				}


				return atEmbed;
			}
			else
			{
				return null;
			}
		}

		/**
		 * Handles an @Resource() directive.
		 * @param text The @Resource() directive that was parsed as the attribute value,
		 * such as "@Resource(bundle='MyResources', key='OPEN')"
		 * @param type Specifies the type (e.g., a String or an int) for the MXML attribute
		 */
        public Object resource(String text, Type type)
        {
            AtResource atResource = AtResource.create(typeTable, unit.getSource(), line, text, type);
			if (atResource != null)
			{
                document.addAtResource(atResource);
				return atResource;
			}
			else
			{
				return null;
			}
        }

		/**
		 *
		 */
		public Object bindingExpression(String converted)
		{
			return new BindingExpression(converted, line, document);
		}

		/**
		 * set was-percentage flag and return percentage as Integer. Subclasses might do prop-name swapping, etc.
		 */
		public Object percentage(String pct)
		{
			this.wasPercentage = true;
			return Double.valueOf(pct.substring(0, pct.indexOf('%')));
		}

		/**
		 *
		 */
		public Object array(Collection entries, Type arrayElementType)
		{
			Array array = new Array(document, arrayElementType, line);
			array.addEntries(entries, line);
			return array;
		}

		/**
		 *
		 */
		public Object functionText(String text)
		{
			return text;
		}

		/**
		 *
		 */
		public Object className(String name, Type lvalueType)
		{
			document.addImport(name, line);
			if (StandardDefs.isIFactory(lvalueType))
			{
				return factoryFromClass(name, line);
			}
			else if (StandardDefs.isIDeferredInstance(lvalueType))
			{
				return instanceFromClass(name, line, true);
			}
			else
			{
				assert lvalueType.equals(typeTable.classType);
				return name;
			}
		}

		/**
		 *
		 */
		public void error(int err, String text, Type type, Type arrayElementType)
		{
			switch(err)
			{
				case ErrTypeNotContextRootable:
					log(line, new TypeNotContextRootable(desc, NameFormatter.toDot(type.getName())));
					break;

				case ErrUndefinedContextRoot:
					log(line, new UndefinedContextRoot());
					break;

				case ErrTypeNotEmbeddable:
					log(line, new TypeNotEmbeddable(desc, NameFormatter.toDot(type.getName())));
					break;

				case ErrInvalidTextForType:
					log(line, new InvalidTextForType(desc,
                                                     NameFormatter.toDot(type.getName()),
                                                     (type.equals(typeTable.arrayType) ? "[" + NameFormatter.toDot(arrayElementType.getName()) + "]" : ""),
                                                     text));
					break;

				case ErrInvalidPercentage:
					log(line, new InvalidPercentage(desc, text));
					break;

				case ErrTypeNotSerializable:
					log(line, new TypeNotSerializable(desc, NameFormatter.toDot(type.getName())));
					break;

				case ErrPercentagesNotAllowed:
					log(line, new PercentagesNotAllowed(desc));
					break;

				case ErrUnrecognizedAtFunction:
					log(line, new UnrecognizedAtFunction(desc));
					break;

				default:
					assert false : "unhandled text parse error, code = " + err;
			}
		}
	}

	/**
	 * distinguish between different kinds of MXML text - affects parse flags
	 */
	public static class TextOrigin
	{
		public static int FROM_ATTRIBUTE = 0;
		public static int FROM_CHILD_TEXT = 1;
		public static int FROM_CHILD_CDATA = 2;

		public static int fromChild(boolean cdata) { return cdata ? FROM_CHILD_CDATA : FROM_CHILD_TEXT; }
	}

	/**
	 *
	 */
	protected boolean processPropertyText(Property property, String text, int origin, int line, Model model)
	{
		String name = property.getName();

		ensureSingleInitializer(model, name, line);

		if (!checkPropertyUsage(property, text, line))
		{
			return false;
		}

		int flags =
				((origin == TextOrigin.FROM_CHILD_CDATA) ? TextParser.FlagInCDATA : 0) |
				(getIsColor(property) ? TextParser.FlagConvertColorNames : 0) |
				((origin != TextOrigin.FROM_ATTRIBUTE && property.collapseWhiteSpace()) ? TextParser.FlagCollapseWhiteSpace : 0) |
				(getPercentProxy(model.getType(), property, line) != null ? TextParser.FlagAllowPercentages : 0);

		Object value = textParser.parseValue(text, property.getType(), getArrayElementType(property), flags, line, name);

		if (value != null)
		{
			postProcessBindingExpression(value, model, name);

			if (textParser.wasPercentage())
			{
				property = getPercentProxy(model.getType(), property, line);
			}

			model.setProperty(property, value, line);

			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 *
	 */
	protected boolean processDynamicPropertyText(String name, String text, int origin, int line, Model model)
	{
		ensureSingleInitializer(model, name, line);

		int flags = (origin == TextOrigin.FROM_CHILD_CDATA) ? TextParser.FlagInCDATA : 0;

		Object value = textParser.parseValue(text, typeTable.objectType, typeTable.objectType, flags, line, name);

		if (value != null)
		{
			postProcessBindingExpression(value, model, name);

			model.setDynamicProperty(typeTable.objectType, name, value, line);

			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * TODO move this to TypeTable.PropertyHelper?
	 */
	protected boolean getIsColor(Property property)
	{
		Inspectable inspectable = property.getInspectable();
		if (inspectable != null)
		{
		 	String type = inspectable.getFormat();
			if (type != null)
			{
				return type.equals(StandardDefs.MDPARAM_INSPECTABLE_FORMAT_COLOR);
			}
		}
		return false;
	}

	/**
	 * TODO move this to TypeTable.PropertyHelper?
	 */
	protected Property getPercentProxy(Type type, Property property, int line)
	{
		String percentProxyName = property.getPercentProxy();
		if (percentProxyName != null)
		{
			Property percentProxy = type.getProperty(percentProxyName);
			if (percentProxy != null)
			{
				return percentProxy;
			}
			else
			{
				log(line, new PercentProxyWarning(percentProxyName,
                                                  property.getName(),
                                                  NameFormatter.toDot(type.getName())));
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 *
	 */
	private void postProcessBindingExpression(Object value, Model model, String name)
	{
		if (value instanceof BindingExpression)
		{
			BindingExpression bindingExpression = (BindingExpression)value;
			bindingExpression.setDestination(model);
			bindingExpression.setDestinationLValue(name);
			bindingExpression.setDestinationProperty(name);
		}
	}

	/**
	 *
	 */
	private void ensureSingleInitializer(Model model, String name, int line)
	{
		if (model.hasProperty(name))
		{
			//	presence of default property can make error nonobvious, so put out some extra text in that case
			Type type = model.getType();
			Property dp = type.getDefaultProperty();
			if (dp != null && dp.getName().equals(name))
			{
                log(line, new MultiplePropertyInitializerWithDefaultError(name, NameFormatter.toDot(type.getName())));
			}
            else
            {
                log(line, new MultiplePropertyInitializerError(name));
            }
		}
	}

	/**
	 *
	 */
	protected void processEventText(Event event, String text, int line, Model model)
	{
		//	TODO check for multiple initializers of event.

		if (text.length() > 0)
		{
			//	register Event type as import
			Type eventType = event.getType();
            if (eventType == null)
            {
                log(line, new EventTypeUnavailable(event.getTypeName()));
                return;
            }
			document.addImport(NameFormatter.toDot(eventType.getName()), line);

			// preilly: Don't check for binding expressions,
			// because they are not supported inside event
			// values.  Using curly braces are allowed,
			// because event values are just ActionScript
			// snippets and curly braces are part of the language.
			model.setEvent(event, text, line);
		}
		else
		{
			log(line, new EventHandlerEmpty());
		}
	}

	/**
	 *
	 */
	public boolean processStyleText(Style style, String text, int origin, int line, Model model)
	{
		String name = style.getName();
		Type type = style.getType();

		if (model.hasStyle(name))
		{
			log(line, new MultipleStyleInitializerError(name));
		}

		int flags =
				((origin == TextOrigin.FROM_CHILD_CDATA) ? TextParser.FlagInCDATA : 0) |
				(getIsColor(style) ? TextParser.FlagConvertColorNames : 0);

		Object value = textParser.parseValue(text, type, flags, line, name);
		if (value != null)
		{
			if (value instanceof BindingExpression)
			{
				BindingExpression bindingExpression = (BindingExpression)value;
				bindingExpression.setDestination(model);
				bindingExpression.setDestinationLValue(name);
				bindingExpression.setDestinationStyle(name);
			}

			model.setStyle(name, value, line);
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * TODO move this to TypeTable.StyleHelper?
	 */
	protected boolean getIsColor(Style style)
	{
		String format = style.getFormat();
		return format != null && format.equals(StandardDefs.MDPARAM_STYLE_FORMAT_COLOR);
	}

	/**
	 *
	 */
	protected boolean processEffectText(Effect effect, String text, int origin, int line, Model model)
	{
		String name = effect.getName();

		if (model.hasEffect(name))
		{
			log(line, new MultipleEffectInitializerError(name));
		}

		int flags = (origin == TextOrigin.FROM_CHILD_CDATA) ? TextParser.FlagInCDATA : 0;
		Object value = textParser.parseValue(text, typeTable.stringType, flags, line, name);

		if (value != null)
		{
			if (value instanceof BindingExpression)
			{
				BindingExpression bindingExpression = (BindingExpression)value;
				bindingExpression.setDestination(model);
				bindingExpression.setDestinationStyle(name);
				bindingExpression.setDestinationLValue(name);
			}
			else
			{
				if (FrameworkDefs.isBuiltinEffectName(text))
				{
					// for 1.5 compatibility
					document.addTypeRef(FrameworkDefs.builtinEffectsPackage + "." + text, line);
				}
			}

			model.setEffect(name, value, typeTable.stringType, line);
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 *
	 */
	protected boolean processPropertyNodes(Node parent, Property property, Model model)
	{
		return processPropertyNodes(parent.getChildren(), property, model, parent.beginLine);
	}


	/**
	 * Note: nodes must not be empty
	 */
    protected boolean processPropertyNodes(Collection nodes, Property property, Model model, int line)
	{
		CDATANode cdata = getTextContent(nodes, true);
		if (cdata != null)
		{
			return processPropertyText(property, cdata.image, TextOrigin.fromChild(cdata.inCDATA), cdata.beginLine, model);
		}
		else
		{
			String name = property.getName();

			//	check for multiple inits to this property
			ensureSingleInitializer(model, name, line);

			//	check other usage constraints
			//	TODO replace ""-passing approach with something that results in a better errmsg for enum violation
			if (!checkPropertyUsage(property, "", ((Node)nodes.iterator().next()).beginLine))
			{
				return false;
			}

			//	lvalue type - initializers to IDeferredInstance-typed properties are values to be returned by the generated factory.
			Type lvalueType = property.getType();
			if (StandardDefs.isIDeferredInstance(lvalueType))
			{
				lvalueType = getInstanceType(property);
			}

			//	array element storage type
			Type arrayElementStoreType = getArrayElementType(property);

			//	array element parse type - initializers to Array<IDeferredInstance>-typed properties are values to be returned by the generated factory.
			Type arrayElementParseType = StandardDefs.isIDeferredInstance(arrayElementStoreType) ?
					getInstanceType(property) :
					arrayElementStoreType;

			//	process
			Object rvalue = processRValueNodes(nodes, lvalueType, name, model, arrayElementParseType, arrayElementStoreType);
			if (rvalue != null)
			{
				model.setProperty(property, rvalue, line);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	private Type getArrayElementType(Property property)
	{
		Type arrayElementStoreType;
		String arrayElementStoreTypeName = property.getArrayElementType();

		if (arrayElementStoreTypeName == null)
		{
			arrayElementStoreType = typeTable.objectType;
		}
		else
		{
			arrayElementStoreType = typeTable.getType(arrayElementStoreTypeName);

			if (arrayElementStoreType == null)
			{
				log(new NullArrayElementStoreType(StandardDefs.MD_ARRAYELEMENTTYPE, arrayElementStoreTypeName));

				arrayElementStoreType = typeTable.objectType;
			}
		}

		return arrayElementStoreType;
	}

	private Type getInstanceType(Property property)
	{
		Type instanceType;
		String instanceTypeName = property.getInstanceType();

		if (instanceTypeName == null)
		{
			instanceType = typeTable.objectType;
		}
		else
		{
			instanceType = typeTable.getType(instanceTypeName);

			if (instanceType == null)
			{
				log(new NullInstanceType(StandardDefs.MD_INSTANCETYPE, instanceTypeName));

				instanceType = typeTable.objectType;
			}
		}

		return instanceType;
	}

	/**
	 * Note: nodes must not be empty
	 */
    protected boolean processDynamicPropertyNodes(Node parent, String name, Model model)
	{
		Collection nodes = parent.getChildren();

		CDATANode cdata = getTextContent(nodes, true);
		if (cdata != null)
		{
			return processDynamicPropertyText(name, cdata.image, TextOrigin.fromChild(cdata.inCDATA), cdata.beginLine, model);
		}
        else
		{
			if (model.hasProperty(name))
			{
				log(parent, new MultiplePropertyInitializerError(name));
			}

			Object rvalue = processRValueNodes(nodes, typeTable.objectType, name, model);
			if (rvalue != null)
			{
				model.setDynamicProperty(typeTable.objectType, name, rvalue, parent.beginLine);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	/**
	 * Note: nodes must not be empty
	 */
    protected boolean processStyleNodes(Node parent, Style style, Model model)
	{
		Collection nodes = parent.getChildren();

		CDATANode cdata = getTextContent(nodes, true);
		if (cdata != null)
		{
			return processStyleText(style, cdata.image, TextOrigin.fromChild(cdata.inCDATA), cdata.beginLine, model);
		}
		else
		{
			String name = style.getName();
			if (model.hasStyle(name))
			{
				log(parent, new MultipleStyleInitializerError(name));
			}

			//	TODO replace ""-passing approach with something that results in a better errmsg for enum violation
			if (!checkStyleUsage(style, "", ((Node)nodes.iterator().next()).beginLine))
			{
				return false;
			}

			//	lvalue type - initializers to IDeferredInstance-typed styles are values to be returned by the generated factory.
			Type lvalueType = style.getType();
			if (StandardDefs.isIDeferredInstance(lvalueType))
			{
				lvalueType = typeTable.objectType;
			}

			Object rvalue = processRValueNodes(nodes, lvalueType, name, model);
			if (rvalue != null)
			{
				model.setStyle(name, rvalue, parent.beginLine);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	/**
	 * Note: nodes must not be empty
	 */
    protected boolean processEffectNodes(Node parent, Effect effect, Model model)
	{
		Collection nodes = parent.getChildren();

		CDATANode cdata = getTextContent(nodes, true);
		if (cdata != null)
		{
			return processEffectText(effect, cdata.image, TextOrigin.fromChild(cdata.inCDATA), cdata.beginLine, model);
		}
		else
		{
			String name = effect.getName();

			if (model.hasEffect(name))
			{
				log(parent, new MultipleEffectInitializerError(name));
			}

			Type effectBaseType = typeTable.getType(StandardDefs.CLASS_EFFECT);

			Object rvalue = processRValueNodes(nodes, effectBaseType, name, model);
			if (rvalue != null)
			{
				model.setEffect(name, rvalue, effectBaseType, parent.beginLine);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	/**
	 * override with typed-array arguments omitted
	 */
	protected Object processRValueNodes(Collection nodes, Type type, String name, Model model)
	{
		return processRValueNodes(nodes, type, name, model, typeTable.objectType, typeTable.objectType);
	}

	/**
	 * Note: if type is Array, then arrayElementStoreType is the type of element actually stored to the array, and
	 * arrayElementParseType is the type which elements specified in MXML need to be compatible with. They are equal
	 * unless arrayElementStoreType is (assignable to) a factory interface, in which case arrayElementParseType is the
	 * instance type which the factory is required to produce (Object unless specified by [InstanceType]).
	 *
	 * The two-type scheme is a bit clumsy, but both types are needed here currently because the 'storage' element type
	 * must be passed into ArrayBuilder, while the 'result' element type is used to verify type compatibility.
	 *
	 * TODO refactor backwards from codegen in such a way that the storage type is directly available from reflection
	 * objects at codegen time. This is impossible currently because ArrayBuilder and the Array VO obscure higher-level
	 * reflection info (Property, Style, etc.)
	 */
	protected Object processRValueNodes(Collection nodes, Type type, String name, Model model,
										Type arrayElementParseType, Type arrayElementStoreType)
	{
		switch (checkTypeCompatibility(nodes, type, arrayElementParseType, name))
		{
			case TypeCompatibility.Ok:
				//	nodes represents an rvalue that is directly assignable to (lvalue) type
				return rvalueNodeHandler.process((Node)nodes.iterator().next(), name, model, arrayElementStoreType);

			case TypeCompatibility.OkCoerceToArray:
				//	nodes is a sequence of rvalues that can be coerced to an array that's assignable to (lvalue) type.
				ArrayBuilder arrayBuilder =
					new ArrayBuilder(unit, typeTable, configuration, document, model, name, false, arrayElementStoreType);
				arrayBuilder.createSyntheticArrayModel(((Node)nodes.iterator().next()).beginLine);
				arrayBuilder.processChildren(nodes);
				return arrayBuilder.array;

			default:
				return null;
		}
	}

	/**
	 *
	 */
	protected class RValueNodeHandler extends ValueNodeHandler
	{
		protected String lvalueName;
		protected Model model;
		protected Type arrayElementType;
		protected Object result;

		protected Object process(Node node, String lvalueName, Model model, Type arrayElementType)
		{
			this.lvalueName = lvalueName;
			this.model = model;
			this.arrayElementType = arrayElementType;
			invoke(node);
			return result;
		}

		protected void componentNode(Node node)
		{
			ComponentBuilder builder = new ComponentBuilder(unit, typeTable, configuration, document, model, false, null);
			node.analyze(builder);
			builder.component.setParentIndex(lvalueName);
			result = builder.component;
		}

		protected void arrayNode(ArrayNode node)
		{
			ArrayBuilder builder = new ArrayBuilder(unit,
					typeTable, configuration, document, model, lvalueName, false, arrayElementType);
			node.analyze(builder);
			result = builder.array;
		}

		protected void primitiveNode(PrimitiveNode node)
		{
			PrimitiveBuilder builder = new PrimitiveBuilder(unit, typeTable, configuration, document, false, null);
			node.analyze(builder);
			result = builder.value;
		}

		protected void xmlNode(XMLNode node)
		{
			XMLBuilder builder = new XMLBuilder(unit, typeTable, configuration, document, model);
			node.analyze(builder);
			builder.xml.setParentIndex(lvalueName);
			result = builder.xml;
		}

        protected void xmlListNode(XMLListNode node)
        {
            XMLListBuilder builder = new XMLListBuilder(unit, typeTable, configuration, document, model);
            node.analyze(builder);
            builder.xmlList.setParentIndex(lvalueName);
            result = builder.xmlList;
        }

		protected void modelNode(ModelNode node)
		{
			ModelBuilder builder = new ModelBuilder(unit, typeTable, configuration, document);
			node.analyze(builder);
			result = builder.graph;
		}

		protected void inlineComponentNode(InlineComponentNode node)
		{
            InlineComponentBuilder builder = new InlineComponentBuilder(unit, typeTable, configuration, document, false);
            node.analyze(builder);
            result = builder.getRValue();
		}

		protected void unknown(Node node)
		{
			assert false : "Unexpected node class in processRValueNode: " + node.getClass();
			result = null;
		}
	}

	/**
	 * Note: callers can opt out of class checking, due to use-cases involving late-gen classes e.g. @Embed
	 */
	protected final Model instanceFromClass(String className, int line, boolean checkClass)
	{
		if (checkClass)
		{
			Type classType = typeTable.getType(NameFormatter.toColon(className));
			if (classType == null)
			{
				log(line, new ClassNotAvailable(className));
			}
		}
		return new Primitive(document, typeTable.classType, className, line);
	}

	/**
	 * Note: expects dot (not colon) delimited className. See note in NameFormaterr for notes on
	 * string-formatted classname migration.
	 */
	protected final Model factoryFromClass(String className, int line)
	{
		Type classFactoryType = typeTable.getType(StandardDefs.CLASS_CLASSFACTORY);
		if (classFactoryType == null)
		{
			log(line, new TypeNotAvailable(StandardDefs.CLASS_CLASSFACTORY));
			return new Model(document, typeTable.objectType, line);	//	fail semi-gracefully
		}

		Model model = new Model(document, classFactoryType, line);

		//	generator
		model.setProperty(StandardDefs.PROP_CLASSFACTORY_GENERATOR,
				new Primitive(document, typeTable.classType, className, line));

		//	introspect the classdef for property sites that match things we can auto-set
		//	NOTE: if className is unqualified, this code will not reach it.
		//	TODO either add support to introspect unqualified classNames via imports, or document its absence
		Type classType = typeTable.getType(NameFormatter.toColon(className));
		if (classType != null)
		{
			//	prop object will carry one or more properties to set on the newInstance()
			Model propObject = null;

			//	outerDocument
			Property outerDocumentProperty = classType.getProperty(DocumentInfo.OUTER_DOCUMENT_PROP);
			if (outerDocumentProperty != null)
			{
				//	check type agreement between outerDocument and our document type
				String qualName = document.getQName().toString();
				Type selfType = typeTable.getType(qualName);
				assert selfType != null : "skeleton type for class '" + NameFormatter.toDot(qualName) + "' not available";

				if (selfType.isAssignableTo(outerDocumentProperty.getType()))
				{
					propObject = new Model(document, typeTable.objectType, line);

					//	HACK: using classType here simply to bypass codegen formatting machinery.
					//	TODO: add Reference rvalue type - will need for <PropertyRef/> etc.
					propObject.setProperty(outerDocumentProperty, new Primitive(document, typeTable.classType, "this", line), line);
				}
			}

			//	if we picked anything up, attach properties initializer
			if (propObject != null)
			{
				model.setProperty(StandardDefs.PROP_CLASSFACTORY_PROPERTIES, propObject);
			}
		}

		return model;
	}

	/**
	 *
	 */
	protected int checkTypeCompatibility(Collection nodes, Type lvalueType, Type lvalueArrayElemType, String lvalueDesc)
	{
		switch (nodes.size())
		{
			case 0:
				assert false;	//	empty collection is illegal argument
				return TypeCompatibility.ErrRTypeNotAssignableToLType;

			case 1:
				return checkTypeCompatibility((Node)nodes.iterator().next(), lvalueType, lvalueArrayElemType, lvalueDesc, true);

			default:
				int compat = TypeCompatibility.Ok;
				for (Iterator iter = nodes.iterator(); iter.hasNext(); )
				{
					compat = checkTypeCompatibility((Node)iter.next(), lvalueType, lvalueArrayElemType, lvalueDesc, false);
					if (compat != TypeCompatibility.Ok && compat != TypeCompatibility.OkCoerceToArray)
					{
						break;
					}
				}
				return compat;
		}
	}

	/**
	 *
	 */
	protected int checkTypeCompatibility(Node node,
										 Type lvalueType,
										 Type lvalueArrayElemType,
										 String lvalueDesc,
										 boolean rvalueIsSingleton)
	{
		Type rtype = nodeTypeResolver.resolveType(node);
		assert rtype != null;	//	all types must be resolved by now

		int compat = TypeCompatibility.check(lvalueType, lvalueArrayElemType, rtype, rvalueIsSingleton);
		switch (compat)
		{
			case TypeCompatibility.Ok:
			case TypeCompatibility.OkCoerceToArray:
				return compat;

			case TypeCompatibility.ErrRTypeNotAssignableToLType:
				log(node.beginLine, new TypeNotAssignableToLType(lvalueDesc,
                                                                 NameFormatter.toDot(rtype.getName()),
                                                                 NameFormatter.toDot(lvalueType.getName())));
				return compat;

			case TypeCompatibility.ErrLTypeNotMultiple:
				log(node.beginLine, new TypeNotMultiple(lvalueDesc, NameFormatter.toDot(lvalueType.getName())));
				return compat;

			case TypeCompatibility.ErrSingleRValueNotArrayOrArrayElem:
				log(node.beginLine, new SingleRValueNotArrayOrArrayElem(lvalueDesc,
                                                                        NameFormatter.toDot(rtype.getName()),
                                                                        NameFormatter.toDot(lvalueType.getName()),
                                                                        NameFormatter.toDot(lvalueArrayElemType.getName())));
				return compat;

			case TypeCompatibility.ErrMultiRValueNotArrayElem:
				log(node.beginLine, new MultiRValueNotArrayElem(lvalueDesc,
                                                                NameFormatter.toDot(rtype.getName()),
                                                                NameFormatter.toDot(lvalueArrayElemType.getName())));
				return compat;

			default:
				assert false;
				return compat;
		}
	}

	/**
	 *
	 */
	protected boolean checkPropertyUsage(Property property, String text, int line)
	{
		if (!isAllowedProperty(property))
		{
			log(line, new InitializerNotAllowed(property.getName()));
			return false;
		}

		checkDeprecation(property, line);

		Inspectable inspectable = property.getInspectable();
		if (inspectable != null)
		{
			checkImageType(inspectable.getFormat(), text, line);

			if (!TextParser.isBindingExpression(text))
			{
				if (!checkEnumeration(inspectable.getEnumeration(), text, line))
				{
					return false;
				}
			}
		}

		if (property.readOnly())
		{
			log(line, new PropertyReadOnly(property.getName()));
			return false;
		}

		//	TODO make sure this never happens
		if (property.getType() == null)
		{
			log(line, new PropertyUnreachable(property.getName()));
			return false;
		}

		return true;
	}

	/**
	 * Subclasses implement this to prohibit properties for special reasons. This only gets called if the property is
	 * valid - i.e., if it exists and is visible on the type of the backing class. But this usage check is called before
	 * others, so e.g. deprecation, enumeration warnings, etc. will be short-circuited if this returns false.
	 */
	protected boolean isAllowedProperty(Property property)
	{
		return true;
	}

	/**
	 *
	 */
	protected boolean checkStyleUsage(Style style, String text, int line)
	{
		checkImageType(style.getFormat(), text, line);

		if (!checkEnumeration(style.getEnumeration(), text, line))
		{
			return false;
		}

		//	TODO make sure this never happens
		if (style.getType() == null)
		{
			log(line, new StyleUnreachable(style.getName()));
			return false;
		}

		return true;
	}

	/**
	 * Return true if the check is okay.
	 * Note: must happen on parsed value, e.g. to avoid failing {bindings}, which passed in 1.5
	 */
	protected boolean checkEnumeration(String[] enums, String value, int line)
	{
		if (enums != null)
		{
			for (int j = 0, count = enums.length; j < count; j++)
			{
				if (enums[j].equals(value))
				{
					return true;
				}
			}
			StringBuffer buffer = new StringBuffer();
			for (int j = 0, count = enums.length; j < count; j++)
			{
				buffer.append(enums[j]);
				if (j < count - 1)
				{
					buffer.append(", ");
				}
			}

			log(line, new InvalidEnumerationValue(value, buffer.toString()));

			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Return true if the check is okay.
	 * Note: must happen on parsed value. collapseWhiteSpace, etc. will affect the test
	 */
	protected boolean checkImageType(String format, String value, int line)
	{
		if ("File".equals(format))
		{
			if ( value.endsWith(".svg") )
			{
				log(line, new RuntimeSVGNotSupported());
				return false;
			}
		}

		return true;
	}
    
    /**
     * Logs the appropriate Deprecation warning based on available information in the metadata;
     * will not log anything if since, message, and replacement are null. 
     * 
     * Returns true if a warning was logged.
     */
    //*** IF YOU MODIFY THIS, update macromedia.asc.embedding.LintEvaluator::logDeprecationWarning() ***
    private boolean checkLogDeprecationWarning(int line,
                                               String name,
                                               String since,
                                               String message,
                                               String replacement)
    {
        assert ((name != null) &&
                (name.length() > 0));
        
        final boolean hasSince       = (since       != null) && (since.length()       > 0);
        final boolean hasMessage     = (message     != null) && (message.length()     > 0);
        final boolean hasReplacement = (replacement != null) && (replacement.length() > 0);
        
        if (hasMessage)
        {
            // [Deprecated("foo")]
            // [Deprecated(message="foo")]
            log(line, new DeprecatedMessage(message));
        }
        else if (hasReplacement)
        {
            if (hasSince)
            {
                // [Deprecated(since="1983", replacement="foo")]
                log(line, new DeprecatedSince(name, since, replacement));
            }
            else
            {
                // [Deprecated(replacement="foo")]
                log(line, new DeprecatedUseReplacement(name, replacement));                   
            }
        }
        else if (hasSince)
        {
            // [Deprecated(since="1983")]
            log(line, new DeprecatedSinceNoReplacement(name, since));
        }
        else if ((message != null) || (replacement != null) || (since != null))
        {
            // deprecation was intended by providing at least one non-null string,
            // though no message was provided, e.g.:
            // [Deprecated(replacement="")] or [Style(deprecatedReplacement="")]
            log(line, new Deprecated(name));
        }
        else
        {
            // probably not [Deprecated]
            return false;
        }
        
        return true;
    }

	/**
	 * Return true if the check is okay.
	 */
	protected boolean checkDeprecation(Property property, int line)
	{
		final flex2.compiler.mxml.reflect.Deprecated deprecated = property.getDeprecated();
		if ((deprecated != null) && configuration.showDeprecationWarnings())
		{
            // since there was definitely a [Deprecated], try logging a deprecation warning;
            // if no arguments were given and nothing is logged (call returns false),
            // log the default deprecation warning.
			if (!checkLogDeprecationWarning(line,
                                            property.getName(),
                                            deprecated.getSince(),
                                            deprecated.getMessage(),
                                            deprecated.getReplacement()))
            {
                log(line, new Deprecated(property.getName()));
            }
			return false;
		}
		return true;
	}

	/**
	 * 
	 */
	protected void checkEventDeprecation(Event event, int line)
	{
		if (configuration.showDeprecationWarnings())
        {
            checkLogDeprecationWarning(line,
                                  event.getName(),
                                  event.getDeprecatedSince(),
                                  event.getDeprecatedMessage(),
                                  event.getDeprecatedReplacement());
        }
	}
	
	/**
	 * 
	 */
	protected void checkEffectDeprecation(Effect effect, int line)
	{
        if (configuration.showDeprecationWarnings())
        {
            checkLogDeprecationWarning(line,
                                  effect.getName(),
                                  effect.getDeprecatedSince(),
                                  effect.getDeprecatedMessage(),
                                  effect.getDeprecatedReplacement());
        }
	}
	
	/**
	 * 
	 */
	protected void checkStyleDeprecation(Style style, int line)
	{
        if (configuration.showDeprecationWarnings())
        {
            checkLogDeprecationWarning(line,
                                  style.getName(),
                                  style.getDeprecatedSince(),
                                  style.getDeprecatedMessage(),
                                  style.getDeprecatedReplacement());
        }
	}

	/**
	 *
	 */
	protected boolean checkNonEmpty(Node node, Type type)
	{
		if (node.getChildren().isEmpty())
		{
			if (!allowEmptyDefault(type))
			{
				log(node.beginLine, new EmptyChildInitializer(NameFormatter.toDot(type.getName())));
			}

			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 *
	 */
	protected boolean allowEmptyDefault(Type type)
	{
		return typeTable.stringType.isAssignableTo(type);
	}

	/**
	 *
	 */
	protected boolean hasAttributeInitializers(Node node)
	{
		for (Iterator iter = node.getAttributeNames(); iter.hasNext(); )
		{
			QName qname = (QName)iter.next();
			if (!isSpecialAttribute(qname.getNamespace(), qname.getLocalPart()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Subclasses should override this method to define what they consider special attributes.
	 */
	protected boolean isSpecialAttribute(String namespaceURI, String localPart)
	{
		return false;
	}

	/**
	 *
	 */
	protected void registerModel(Node node, Model model, boolean topLevel)
	{
		registerModel((String)node.getAttribute("id"), model, topLevel);
	}

	/**
	 * register an rvalue (currently aka Model) to our MxmlDocument, as a declaration.
	 */
	protected void registerModel(String id, Model model, boolean topLevel)
	{
		if (id != null)
		{
			model.setId(id, false);
			document.addDeclaration(model, topLevel);
		}
		else if (topLevel)
		{
			document.addDeclaration(model, true);
		}
	}

	/**
	 *
	 * errors, warnings from here to EOF
	 *
	 */

	public static class BindingNotAllowed extends CompilerError
    {
    }

    public static class TypeNotContextRootable extends CompilerError
    {
        public String desc;
        public String type;

        public TypeNotContextRootable(String desc, String type)
        {
            this.desc = desc;
            this.type = type;
        }
    }

    public static class UndefinedContextRoot extends CompilerError
    {
        public UndefinedContextRoot()
        {
        }
    }

    public static class TypeNotEmbeddable extends CompilerError
    {
        public String desc;
        public String type;

        public TypeNotEmbeddable(String desc, String type)
        {
            this.desc = desc;
            this.type = type;
        }
    }

    public static class InvalidTextForType extends CompilerError
    {
        public String desc;
        public String type;
        public String array;
        public String text;

        public InvalidTextForType(String desc, String type, String array, String text)
        {
            this.desc = desc;
            this.type = type;
            this.array = array;
            this.text = text;
        }
    }

    public static class InvalidPercentage extends CompilerError
    {
        public String desc;
        public String text;

        public InvalidPercentage(String desc, String text)
        {
            this.desc = desc;
            this.text = text;
        }
    }

    public static class TypeNotSerializable extends CompilerError
    {
        public String desc;
        public String type;

        public TypeNotSerializable(String desc, String type)
        {
            this.desc = desc;
            this.type = type;
        }
    }

    public static class PercentagesNotAllowed extends CompilerError
    {
        public String desc;

        public PercentagesNotAllowed(String desc)
        {
            this.desc = desc;
        }
    }

    public static class UnrecognizedAtFunction extends CompilerError
    {
        public String desc;

        public UnrecognizedAtFunction(String desc)
        {
            this.desc = desc;
        }
    }

    public static class PercentProxyWarning extends CompilerWarning
    {
        public String proxyName;
        public String property;
        public String type;

        public PercentProxyWarning(String proxyName, String property, String type)
        {
            this.proxyName = proxyName;
            this.property = property;
            this.type = type;
        }
    }

    public static class MultiplePropertyInitializerError extends CompilerError
    {
        public String name;

        public MultiplePropertyInitializerError(String name)
        {
            this.name = name;
        }
    }

    public static class MultiplePropertyInitializerWithDefaultError extends CompilerError
    {
        public String name;
        public String type;

        public MultiplePropertyInitializerWithDefaultError(String name, String type)
        {
            this.name = name;
            this.type = type;
        }
    }

    public static class EventTypeUnavailable extends CompilerError
    {
        public String type;

        public EventTypeUnavailable(String type)
        {
            this.type = type;
        }
    }

    public static class EventHandlerEmpty extends CompilerWarning
    {
    }

    public static class MultipleStyleInitializerError extends CompilerError
    {
        public String name;

        public MultipleStyleInitializerError(String name)
        {
            this.name = name;
        }
    }

	public static class MultipleEffectInitializerError extends CompilerError
	{
		public String name;

		public MultipleEffectInitializerError(String name)
		{
			this.name = name;
		}
	}

    public static class NullArrayElementStoreType extends CompilerWarning
    {
        public String arrayElementType;
        public String arrayElementStoreTypeName;

        public NullArrayElementStoreType(String arrayElementType, String arrayElementStoreTypeName)
        {
            this.arrayElementType = arrayElementType;
            this.arrayElementStoreTypeName = arrayElementStoreTypeName;
        }
    }

    public static class NullInstanceType extends CompilerWarning
    {
        public String instanceType;
        public String instanceTypeName;

        public NullInstanceType(String instanceType, String instanceTypeName)
        {
            this.instanceType = instanceType;
            this.instanceTypeName = instanceTypeName;
        }
    }

    public static class ClassNotAvailable extends CompilerError
    {
        public String className;

        public ClassNotAvailable(String className)
        {
            this.className = className;
        }
    }

    public static class TypeNotAvailable extends CompilerError
    {
        public String type;

        public TypeNotAvailable(String type)
        {
            this.type = type;
        }
    }

    public static class TypeNotAssignableToLType extends CompilerError
    {
        public String lvalue;
        public String type;
        public String targetType;

        public TypeNotAssignableToLType(String lvalue, String type, String targetType)
        {
            this.lvalue = lvalue;
            this.type = type;
            this.targetType = targetType;
        }
    }

    public static class TypeNotMultiple extends CompilerError
    {
        public String lvalue;
        public String targetType;

        public TypeNotMultiple(String lvalue, String targetType)
        {
            this.lvalue = lvalue;
            this.targetType = targetType;
        }
    }

    public static class SingleRValueNotArrayOrArrayElem extends CompilerError
    {
        public String lvalue;
        public String type;
        public String targetType;
        public String targetElememtType;

        public SingleRValueNotArrayOrArrayElem(String lvalue, String type, String targetType, String targetElememtType)
        {
            this.lvalue = lvalue;
            this.type = type;
            this.targetType = targetType;
            this.targetElememtType = targetElememtType;
        }
    }

    public static class MultiRValueNotArrayElem extends CompilerError
    {
        public String lvalue;
        public String type;
        public String targetElememtType;

        public MultiRValueNotArrayElem(String lvalue, String type, String targetElememtType)
        {
            this.lvalue = lvalue;
            this.type = type;
            this.targetElememtType = targetElememtType;
        }
    }

    public static class InitializerNotAllowed extends CompilerError
    {
        public String name;

        public InitializerNotAllowed(String name)
        {
            this.name = name;
        }
    }

    public static class PropertyReadOnly extends CompilerError
    {
        public String name;

        public PropertyReadOnly(String name)
        {
            this.name = name;
        }
    }

    public static class PropertyUnreachable extends CompilerError
    {
        public String name;

        public PropertyUnreachable(String name)
        {
            this.name = name;
        }
    }

    public static class StyleUnreachable extends CompilerError
    {
        public String name;

        public StyleUnreachable(String name)
        {
            this.name = name;
        }
    }

    public static class InvalidEnumerationValue extends CompilerError
    {
        public String value;
        public String values;

        public InvalidEnumerationValue(String value, String values)
        {
            this.value = value;
            this.values = values;
        }
    }

    public static class RuntimeSVGNotSupported extends CompilerWarning
    {
    }

    public static class Deprecated extends CompilerWarning
    {
        public String name;

        public Deprecated(String name)
        {
            this.name = name;
        }
    }
    
    public static class DeprecatedMessage extends CompilerWarning
    {
        public String deprecationMessage;

        public DeprecatedMessage(String deprecationMessage)
        {
            this.deprecationMessage = deprecationMessage;
        }
    }

    public static class DeprecatedUseReplacement extends CompilerWarning
    {
        public String name;
        public String replacement;

        public DeprecatedUseReplacement(String name, String replacement)
        {
            this.name = name;
            this.replacement = replacement;
        }
    }

    public static class DeprecatedSince extends CompilerWarning
    {
        public String name;
        public String replacement;
        public String since;

        public DeprecatedSince(String name, String since, String replacement)
        {
            this.name = name;
            this.since = since;
            this.replacement = replacement;
        }
    }
    
    public static class DeprecatedSinceNoReplacement extends CompilerWarning
    {
        public String name;
        public String since;

        public DeprecatedSinceNoReplacement(String name, String since)
        {
            this.name = name;
            this.since = since;
        }
    }
    
    public static class EmptyChildInitializer extends CompilerError
    {
        public String type;

        public EmptyChildInitializer(String type)
        {
            this.type = type;
        }
    }
}
