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
import flex2.compiler.mxml.lang.*;
import flex2.compiler.mxml.reflect.*;
import flex2.compiler.mxml.rep.*;
import flex2.compiler.util.QName;
import flex2.compiler.util.NameFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Clement Wong
 */
public class ComponentBuilder extends Builder
{
	ComponentBuilder(CompilationUnit unit, TypeTable typeTable, Configuration configuration, MxmlDocument document,
					 Model parent, boolean topLevelChild, BindingHandler bindingHandler)
	{
		super(unit, typeTable, configuration, document);

		this.parent = parent;
		this.topLevelChild = topLevelChild;
		this.bindingHandler = bindingHandler;

		this.attributeHandler = new ComponentAttributeHandler();
		this.childNodeHandler = new ComponentChildNodeHandler(typeTable);

	}

	protected Model parent;
	protected boolean topLevelChild;
	protected BindingHandler bindingHandler;

	protected ComponentAttributeHandler attributeHandler;
	protected ComponentChildNodeHandler childNodeHandler;

	Model component;

	/**
	 *
	 */
	public void analyze(Node node)
	{
		assert component == null : "ComponentBuilder.analyze(Node) called twice";

		Type type = nodeTypeResolver.resolveType(node);

		constructComponent(type, node.beginLine);

		processAttributes(node, type);
		processChildren(node, type);

		//	NOTE: must do this after processing, due to/until removal of swapout of this.component in processTextInitializer
		registerModel(node, component, topLevelChild);
	}

	public void analyze(ScriptNode node)
	{
		//	scripts are added to document info in InterfaceCompiler
	}

	/**
	 *
	 */
	protected void constructComponent(Type type, int line)
	{
		component = StandardDefs.isIUIComponent(type) ?
				new MovieClip(document, type, parent, line) :
				new Model(document, type, parent, line);

		if (type.equals(typeTable.objectType))
		{
			component.setInspectable(true);
		}
	}

	/**
	 *
	 */
	protected void processAttributes(final Node node, Type type)
	{
		processSpecialAttributes(node);

		for (Iterator iter = node.getAttributeNames(); iter.hasNext(); )
		{
			attributeHandler.invoke(node, type, (QName)iter.next());
		}
	}

	/**
	 *
	 */
	protected class ComponentAttributeHandler extends AttributeHandler
	{
		protected boolean isSpecial(String namespace, String localPart)
		{
			return isSpecialAttribute(namespace, localPart);
		}

		protected void special(String namespace, String localPart)
		{
			//	already done in processSpecialAttributes(), currently
		}

		protected void event(Event event)
		{
			checkEventDeprecation(event, line);
			processEventText(event, text, line, component);
		}

		protected void property(Property property)
		{
			processPropertyText(property, text, Builder.TextOrigin.FROM_ATTRIBUTE, line, component);
		}

		protected void effect(Effect effect)
		{
			checkEffectDeprecation(effect, line);
			processEffectText(effect, text, Builder.TextOrigin.FROM_ATTRIBUTE, line, component);
		}

		protected void style(Style style)
		{
			checkStyleDeprecation(style, line);
			processStyleText(style, text, Builder.TextOrigin.FROM_ATTRIBUTE, line, component);
		}

		protected void dynamicProperty(String name)
		{
			processDynamicPropertyText(name, text, Builder.TextOrigin.FROM_ATTRIBUTE, line, component);
		}

		protected void unknownNamespace(String namespace)
		{
			log(line, new UnknownNamespace(namespace, text));
		}

		protected void unknown(String name)
		{
			unknownAttributeError(name, line);
		}
	}

	/**
	 *
	 */
	protected void unknownAttributeError(String name, int line)
	{
		log(line, new UnknownAttribute(name, NameFormatter.toDot(component.getType().getName())));
	}

	/**
	 *
	 */
	protected void processChildren(Node node, Type type)
	{
		childNodeHandler.scanChildNodes(node, type);

		if (!childNodeHandler.getDefaultPropertyNodes().isEmpty())
		{
			processPropertyNodes(childNodeHandler.getDefaultPropertyNodes(), type.getDefaultProperty(), component, node.beginLine);
		}
	}

	/**
	 *
	 */
	protected class ComponentChildNodeHandler extends ChildNodeHandler
	{
		protected Collection defaultPropertyNodes;

		public ComponentChildNodeHandler(TypeTable typeTable)
		{
			super(typeTable);
		}

		public Collection getDefaultPropertyNodes()
		{
			return defaultPropertyNodes != null ? defaultPropertyNodes : Collections.EMPTY_LIST;
		}

		protected void addDefaultPropertyNode(Node node)
		{
			(defaultPropertyNodes != null ? defaultPropertyNodes : (defaultPropertyNodes = new ArrayList(1))).add(node);
		}

		//	ChildNodeHandler impl

		protected void event(Event event)
		{
			CDATANode cdata = getTextContent(child.getChildren(), false);
			if (cdata != null)
			{
				processEventText(event, cdata.image, cdata.beginLine, component);
			}
		}

		protected void property(Property property)
		{
			Type type = property.getType();
			if (checkNonEmpty(child, type))
			{
				processPropertyNodes(child, property, component);
			}
			else if (allowEmptyDefault(type))
			{
				processPropertyText(property, "", Builder.TextOrigin.FROM_CHILD_CDATA, child.beginLine, component);
			}
		}

		protected void effect(Effect effect)
		{
			if (checkNonEmpty(child, typeTable.classType))
			{
				processEffectNodes(child, effect, component);
			}
		}

		protected void style(Style style)
		{
			Type type = style.getType();
			if (checkNonEmpty(child, type))
			{
				processStyleNodes(child, style, component);
			}
			else if (allowEmptyDefault(type))
			{
				processStyleText(style, "", Builder.TextOrigin.FROM_CHILD_CDATA, child.beginLine, component);
			}
		}

		protected void dynamicProperty(String name)
		{
			Type type = typeTable.objectType;
			if (checkNonEmpty(child, type))
			{
				processDynamicPropertyNodes(child, name, component);
			}
			else if (allowEmptyDefault(type))
			{
				processDynamicPropertyText(name, "", Builder.TextOrigin.FROM_CHILD_CDATA, child.beginLine, component);
			}
		}

        protected void defaultPropertyElement(boolean locError)
		{
			if (locError)
			{
				log(child, new NonContiguous());
			}

			addDefaultPropertyNode(child);
		}

		/**
		 * Note that here is where we implement the visual-child-of-visual-container special case, as well as the
		 * (urp) RadioButtonGroup special case.
		 */
		protected void nestedDeclaration()
		{
			Type childType = nodeTypeResolver.resolveType(child);
			assert childType != null : "nested declaration node type == null, node = " + child.image;

			if (StandardDefs.isContainer(parentType) && StandardDefs.isIUIComponent(childType))
			{
                processVisualChild(child);
            }
			else if (StandardDefs.isContainer(parentType) && childType.isAssignableTo(FrameworkDefs.classRadioButtonGroup))
			{
				ComponentBuilder builder = new ComponentBuilder(unit, typeTable, configuration, document, component, true, null);
				child.analyze(builder);
			}
			else
			{
				processNestedDeclaration();
			}
		}

		/**
		 * Note: actual nested declarations are only allowed at the root level - see override in ApplicationChildNodeHandler.
		 * Note: put out a slightly more verbose error if we're within an IContainer, since they may have meant to
		 * specify a visual child.
		 */
		protected void processNestedDeclaration()
		{
			if (StandardDefs.isContainer(parentType))
			{
				log(child, new NestedFlexDeclaration(NameFormatter.toDot(StandardDefs.INTERFACE_IUICOMPONENT)));
			}
			else
			{
				log(child, new NestedDeclaration());
			}
		}

		protected void textContent()
		{
			if (parent.getChildCount() > 1)
			{
				log(child, new MixedContent());
			}
			else if (hasAttributeInitializers(parent))
			{
				log(child, new InitializersNotAllowed());
			}
			else
			{
				processTextInitializer((CDATANode)child);
			}
		}

		protected void languageNode()
		{
			if (isLegalLanguageNode(child))
			{
				child.analyze(ComponentBuilder.this);
			}
			else
			{
				log(child, new IllegalLanguageNode(child.image));
			}
		}
	}

	/**
	 * TODO can we make these top-level only?
	 */
	protected boolean isLegalLanguageNode(Node node)
	{
		Class nodeClass = node.getClass();
		return nodeClass == ScriptNode.class;
	}

	/**
	 *	process visual child of visual container
	 */
	protected void processVisualChild(Node node)
	{
		ComponentBuilder builder = new ComponentBuilder(unit, typeTable, configuration, document, component, false, null);
		node.analyze(builder);
		((MovieClip)component).addChild((MovieClip)builder.component);
	}

	/**
	 *
	 */
	protected void processTextInitializer(CDATANode cdata)
	{
		if (!component.isEmpty())
		{
			log(cdata.beginLine, new MixedInitializers());
		}
		else
		{
			String text = cdata.image;
			Type type = component.getType();
			int line = cdata.beginLine;

			int flags = cdata.inCDATA ? TextParser.FlagInCDATA : 0;
			Object value = textParser.parseValue(text, type, typeTable.objectType, flags, line, NameFormatter.toDot(type.getName()));

			if (value != null)
			{
				if (value instanceof BindingExpression)
				{
					if (bindingHandler != null)
					{
						bindingHandler.invoke((BindingExpression)value, component);
					}
					else
					{
						log(line, new BindingNotAllowed());
					}
				}
				else
				{
					//	Note: here we're in an atypical situation. We've encountered a text initializer for something we
					// 	thought was a (non-Primitive) component. This may happen e.g. when primitives are exposed in MXML
					// 	namespaces other than Parser.MXML_NAMESPACE. (NOTE also that all global definitions are implictly
					// 	available in the namespace "*", due to the optimistic approach used NameMappings for package-style
					//  namespaces. We may want to suppress this at some point, but it would require changing NM's approach.)
					//  When this happens, the parser will package (e.g.) <String>foo</String> in an ordinary Node rather
					// 	than the corresponding type-specific node, e.g. StringNode, etc. That will bring us here.
					//	TODO finish generalizing the processing of class-backed MXML tags. This will eliminate this
					//	special case, the various type-specific primitive node classes, and so on.
					//	Until then, the MO here is: swap our prepped component member variable for the appropriate
					// 	replacement containing the parsed value, carrying over the already-registered id.

					Model preppedComponent = component;

					if (value instanceof Model)
					{
						//	textParser has returned a Model
						component = (Model)value;
					}
					else
					{
						//	textParser has returned a POJO
						component = new Primitive(document, preppedComponent.getType(), value, line);
					}

					component.setId(preppedComponent.getId(), preppedComponent.getIdIsAutogenerated());
				}
			}
		}
	}

	/**
	 * Currently, our only prohibition is on setting UIComponent.states[] on anything but a root UIComponent.
	 * (ApplicationBuilder re-subclasses to allow the root case.)
	 */
	protected boolean isAllowedProperty(Property property)
	{
       	if (property.getName().equals(StandardDefs.PROP_UICOMPONENT_STATES) &&
			property.getType().equals(typeTable.arrayType) &&
			component.getType().isAssignableTo(StandardDefs.CLASS_UICOMPONENT))
		{
			return false;
		}

		return true;
	}

	/**
	 * Subclasses should override this method to define what they consider special attributes.
	 */
	protected boolean isSpecialAttribute(String namespaceURI, String localPart)
	{
		return namespaceURI.length() == 0 && "id".equals(localPart);
	}

	/**
	 * process special attributes, like "id"... Subclasses, e.g. ApplicationBuilder, can override
	 * processSpecialAttributes() to process "usePreloader", "preloader", etc...
	 */
	protected void processSpecialAttributes(Node node)
	{
	}

    public static class UnknownNamespace extends CompilerError
    {
        public String namespace;
        public String text;

        public UnknownNamespace(String namespace, String text)
        {
            this.namespace = namespace;
            this.text = text;
        }
    }

    public static class UnknownAttribute extends CompilerError
    {
        public String name;
        public String type;

        public UnknownAttribute(String name, String type)
        {
            this.name = name;
            this.type = type;
        }
    }

    public static class NonContiguous extends CompilerError
    {
    }

    public static class NestedFlexDeclaration extends CompilerError
    {
        public String interfaceName;

        public NestedFlexDeclaration(String interfaceName)
        {
            this.interfaceName = interfaceName;
        }
    }

    public static class NestedDeclaration extends CompilerError
    {
    }

    public static class MixedContent extends CompilerError
    {
    }

    public static class InitializersNotAllowed extends CompilerError
    {
    }

    public static class IllegalLanguageNode extends CompilerError
    {
        public String image;

        public IllegalLanguageNode(String image)
        {
            this.image = image;
        }
    }

    public static class MixedInitializers extends CompilerError
    {
    }
}
