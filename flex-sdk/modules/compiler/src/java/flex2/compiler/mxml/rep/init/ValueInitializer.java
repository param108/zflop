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

package flex2.compiler.mxml.rep.init;

import flash.util.StringUtils;
import flex2.compiler.mxml.gen.CodeFragmentList;
import flex2.compiler.mxml.gen.DescriptorGenerator;
import flex2.compiler.mxml.gen.TextGen;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.*;
import flex2.compiler.util.IteratorList;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.CompilerMessage;
import org.apache.commons.collections.iterators.SingletonIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * implements the generation of ordinary rvalues. Subclasses handle codegen variations for different lvalues.
 *
 * TODO this logic is complicated a bit by the fact that some legacy builders upstream are still using POJOs
 * as their initializer rvalues. Once those have all been ported to use (at least) Primitives, all the
 * "if (value instanceof Model)" scaffolding can be removed. At that point it would also make sense to move
 * from a subclasses-of-Model approach (another remaining bit of legacy) to an explicit-discriminant approach.
 */
public abstract class ValueInitializer implements Initializer
{
	protected final Object value;
	protected final int line;

	ValueInitializer(Object value, int line)
	{
		this.value = value;
		this.line = line;
	}

	public Object getValue()
	{
		return value;
	}

	//	Initializer impl

	public int getLineRef()
	{
		return line;
	}

	public boolean isBinding()
	{
		return value instanceof BindingExpression || (value instanceof Primitive && ((Primitive)value).hasBindings());
	}

	/**
	 *
	 */
	public boolean hasDefinition()
	{
		if (value instanceof Model)
		{
			Model model = (Model)value;
			return model.isDeclared() || !modelHasInlineRValue() || isInstanceGeneratorOverDefinition();
		}
		else
		{
			assert isBinding() || !StandardDefs.isInstanceGenerator(getLValueType())
					: "instance generator lvalue has non-Model, non-BindingExpression rvalue (" + value.getClass() + ")";
			return false;
		}
	}

	/**
	 * note the exception for simple classdef-based deferrals
	 */
	protected boolean isInstanceGeneratorOverDefinition()
	{
		Type ltype = getLValueType();
		return StandardDefs.isIFactory(ltype) || (StandardDefs.isIDeferredInstance(ltype) && !rvalueIsClassRef());
	}

	/**
	 * TODO replace with actual ClassRef subclass of Model or Primitive
	 */
	protected boolean rvalueIsClassRef()
	{
		return value instanceof Primitive && ((Primitive)value).getType().equals(getTypeTable().classType);
	}

	/**
	 *
	 */
	public String getValueExpr()
	{
		Type lvalueType = getLValueType();

		if (StandardDefs.isIDeferredInstance(lvalueType))
		{
			if (rvalueIsClassRef())
			{
				return "new " + NameFormatter.toDot(StandardDefs.CLASS_DEFERREDINSTANCEFROMCLASS) + "(" + getInlineRValue() + ")";
			}
			else
			{
				return "new " + NameFormatter.toDot(StandardDefs.CLASS_DEFERREDINSTANCEFROMFUNCTION) + "(" + getDefinitionName() + ")";
			}
		}
		else
		{
			return hasDefinition() ? getDefinitionName() + "()" : getInlineRValue();
		}
	}

	/**
	 *
	 */
	protected boolean modelHasInlineRValue()
	{
		assert value instanceof Model;
		Model model = (Model)value;
		return model instanceof XML ||
                model instanceof XMLList ||
				model instanceof Primitive ||
				model instanceof Array ||
				model.getType().equals(getTypeTable().objectType);
	}

	/**
	 *
	 */
	protected String getInlineRValue()
	{
		if (value instanceof Model)
		{
			if (value instanceof Primitive)
			{
				Primitive primitive = (Primitive)value;
				return formatExpr(primitive.getType(), primitive.getValue());
			}
			else if (value instanceof Array)
			{
				return asArrayLiteral((Array)value);
			}
			else if (value instanceof XML)
			{
				XML xml = (XML)value;
				return asXmlLiteral(xml);
			}
            else if (value instanceof XMLList)
            {
                return asXMLList((XMLList)value);
            }
			else if (((Model)value).getType().equals(getTypeTable().objectType))
			{
				return asObjectLiteral((Model)value);
			}
			else
			{
				assert false : "can't generate inline expr for values of type " + value.getClass();
				return null;
			}
		}
		else
		{
			return formatExpr(getLValueType(), value);
		}
	}

	/**
	 * Note: the definition function will create and return our rvalue. If the rvalue is declared (i.e. carries an id),
	 * then it will also initialize the declared variable by side effect. Hence the "_init" vs "_create" suffixes.
	 */
	protected String getDefinitionName()
	{
		assert hasDefinition() : "no definition in getDefinitionName()";
		assert value instanceof Model : "non-Model value has definition in getDefinitionName()";

		return ((Model)value).getDefinitionName() + (((Model)value).isDeclared() ? "_i" : "_c");
	}

	/**
	 *
	 */
	protected CodeFragmentList getDefinitionBody()
	{
		assert hasDefinition() : "no definition in getDefinitionBody()";
		assert value instanceof Model : "non-Model value has definition in getDefinitionBody()";

		final String varName = "temp";

		Model self = (Model)value;
		Type selfType = self.getType();
		String selfTypeName = NameFormatter.toDot(selfType.getName());

		boolean isDeclared = self.isDeclared();
		String id = isDeclared ? self.getId() : varName;

		int line = getLineRef();

		CodeFragmentList list = new CodeFragmentList();

		//	function header
		//
		list.add("private function ", getDefinitionName(), "() : ", selfTypeName, line);
		list.add("{", line);

		//	value creation
		//
		if (modelHasInlineRValue())
		{
			list.add("\tvar ", varName, " : ", selfTypeName, " = " + getInlineRValue() + ";", line);

			if (isDeclared)
			{
				if (self.getRepeaterLevel() == 0)
				{
					list.add("\t", id, " = ", varName, ";", line);
				}
				else
				{
					ThreadLocalToolkit.log(new DeclaredAndProceduralWithinRepeater(), self.getDocument().getSourcePath(), line);
				}
			}
		}
		else
		{
			//	TODO confirm the availability of a 0-arg ctor!! but do it upstream from here, like when Model is built
			list.add("\tvar ", varName, " : ", selfTypeName, " = new " + selfTypeName + "();", line);

			if (isDeclared)
			{
				if (self.getRepeaterLevel() == 0)
				{
					list.add("\t", id, " = ", varName, ";", line);
				}
				else
				{
					ThreadLocalToolkit.log(new DeclaredAndProceduralWithinRepeater(), self.getDocument().getSourcePath(), line);
				}
			}

			addAssignExprs(list, self.getPropertyInitializerIterator(self.getType().hasDynamic()), varName);
		}

		//	set styles
		addAssignExprs(list, self.getStyleInitializerIterator(), varName);

		//	set effects
		addAssignExprs(list, self.getEffectInitializerIterator(), varName);

		//	add event handlers
		addAssignExprs(list, self.getEventInitializerIterator(), varName);

		//	register effect names
		String effectEventNames = self.getEffectNames();
		if (effectEventNames.length() > 0)
		{
			list.add("\t", varName, ".registerEffects([ ", effectEventNames, " ]);", line);
		}

		//	post-init actions for values that are being assigned to properties (via id attribution)
		if (isDeclared && StandardDefs.isIUIComponentWithIdProperty(selfType))
		{
			//	set id on IUIComponents that carry an id prop
			list.add("\t", varName, ".id = \"", id, "\";", line);
		}

		//	evaluate all property bindings for this object - i.e. initialize properties of the object whose values
		//	are binding expressions. E.g. if we've just created <mx:Foo id="bar" x="100" y="{baz.z}"/>, then
		//	we need to evaluate (baz.z) and assign it to bar.y. This explicit evaluation pass is necessary because
		// 	baz may already have been initialized, although the fact that we do it even when that's not the case is
		// 	suboptimal.
		if (self.hasBindings())
		{
			list.add("\t", NameFormatter.toDot(StandardDefs.CLASS_BINDINGMANAGER),
					".executeBindings(this, ", TextGen.quoteWord(id), ", " + id + ");", line);
		}

		//	UIComponent-specific init steps
		if (StandardDefs.isIUIComponent(selfType))
		{
			assert self instanceof MovieClip : "isIUIComponent(selfType) but !(self instanceof MovieClip)";

			//	MXML implementations of IUIComponent initialize set their document property to themselves at
			//	construction time. Others need it set to the enclosing document (us).

			list.add("\tif (!", varName, ".document) ", varName, ".document = this;", line);

			//	add visual children
			if (!StandardDefs.isRepeater(selfType))
			{
				//	non-repeater - replicate DI child-creation sequence procedurally:
				addAssignExprs(list, ((MovieClip)self).getChildInitializerIterator(), varName);
			}
			else
			{
				//	repeater-specific init sequence: don't add children directly, instead use existing DI setup
				//	initializing repeater's childDescriptors property, for now

				list.add("\tvar cd:Array = ", varName, ".childDescriptors = [", line);

				for (Iterator childIter = ((MovieClip)self).children().iterator(); childIter.hasNext(); )
				{
					VisualChildInitializer init = (VisualChildInitializer)childIter.next();
					DescriptorGenerator.addDescriptorInitializerFragments(list, (MovieClip)init.getValue(), "\t\t");

					if (childIter.hasNext())
					{
						list.add(",", 0);
					}
				}

				list.add("\t];", line);

				list.add("\tfor (var i:int = 0; i < cd.length; i++) cd[i].document = this;", line);

				//	CAUTION: lots of carnal knowledge here:
				//	1. initializeRepeater's first argument is an IRepeaterContainer. If the MXML has a repeater within a
				// 	non-root visual component, that component is guaranteed to have been declared, so we can use its id
				// 	for this argument.
				// 	Otherwise, the parent is either non-visual (e.g. AddChild) or the root document. In either case, we
				// 	use "this".
				//	2. second param to initializeRepeater is a recursion flag. Setting to true but not really sure how this
				// 	affects phased instantiation.
				//
				String docRef;

				Model parent = self.getParent();
				if (parent != null && parent instanceof MovieClip && parent != self.getDocument().getRoot())
				{
					assert self.getParent().isDeclared() : "Visual non-root parent of repeater not declared";
					docRef = parent.getId();
				}
				else
				{
					docRef = "this";
				}

				list.add("\t", varName, ".initializeRepeater(", docRef, ", true);", line);
			}
		}

		//	call IMXMLObject.initialized() on implementors
		if (self.getType().isAssignableTo(StandardDefs.INTERFACE_IMXMLOBJECT))
		{
			String idParam = (isDeclared ? TextGen.quoteWord(id) : "null");
			list.add("\t", varName, ".initialized(this, ", idParam, ")", line);
		}

		//	return created value
		list.add("\treturn ", varName, ";", line);
		list.add("}", line);

		return list;
	}

	/**
	 * return an iterator over our definition if we have one, and all the definitions of our children
	 */
	public Iterator getDefinitionsIterator()
	{
		IteratorList iterList = null;

		if (hasDefinition())
		{
			//	Note: isDescribed() guard omits our own definition if we're in a descriptor tree
			// 	TODO remove this once DI is done directly
			if (!(value instanceof Model) || !((Model)value).isDescribed())
			{
				(iterList = new IteratorList()).add(new SingletonIterator(getDefinitionBody()));
			}
		}

		if (value instanceof Model)
		{
			(iterList != null ? iterList : (iterList = new IteratorList())).add(((Model)value).getSubDefinitionsIterator());
		}

		return iterList != null ? iterList.toIterator() : Collections.EMPTY_LIST.iterator();
	}

	/**
	 *
	 */
	private TypeTable getTypeTable()
	{
		return getLValueType().getTypeTable();
	}

	/**
	 *
	 */
	private static void addAssignExprs(CodeFragmentList list, Iterator initIter, String name)
	{
		while (initIter.hasNext())
		{
			Initializer init = (Initializer)initIter.next();
			list.add("\t", init.getAssignExpr(name), ";", init.getLineRef());
		}
	}

	/**
	 * TODO once all POJO rvalues have been eliminated, this can go away completely
	 */
	protected String formatExpr(Type targetType, Object value)
	{
		assert targetType != null;
		assert value != null;

		TypeTable typeTable = getTypeTable();

		if (value instanceof BindingExpression)
		{
			if (targetType.equals(typeTable.booleanType) ||
				targetType.equals(typeTable.numberType) ||
				targetType.equals(typeTable.intType) ||
				targetType.equals(typeTable.uintType))
			{
				return StandardDefs.UNDEFINED;
			}
			else
			{
				return StandardDefs.NULL;
			}
		}

		if (value instanceof AtEmbed)
		{
			return ((AtEmbed) value).getPropName();
		}

		if (value instanceof AtResource)
		{
			return ((AtResource)value).getValueExpression();
		}

		if (targetType.equals(typeTable.stringType))
		{
			return StringUtils.formatString((String)value);
		}
		else if (targetType.equals(typeTable.booleanType) ||
				targetType.equals(typeTable.numberType) ||
				targetType.equals(typeTable.intType) ||
				targetType.equals(typeTable.uintType))
		{
			return value.toString();
		}
		else if (targetType.equals(typeTable.objectType) || targetType.equals(typeTable.noType))
		{
			if (value instanceof String)
			{
				return StringUtils.formatString((String)value);
			}
			else if (value instanceof Number || value instanceof Boolean)
			{
				return value.toString();
			}
			else
			{
				assert false : "formatExpr: unsupported rvalue type '" + value.getClass() + "' for lvalue type 'Object'";
			}
		}
		else if (targetType.equals(typeTable.classType))
		{
			return value.toString();
		}
		else if (targetType.equals(typeTable.functionType))
		{
			return value.toString();
		}
		else if (targetType.equals(typeTable.regExpType))
		{
			return value.toString();
		}
		else if (targetType.equals(typeTable.xmlType))
		{
			return asXmlLiteral((XML)value);
		}
        else if (targetType.equals(typeTable.xmlListType))
        {
            return asXMLList((XMLList)value);
        }
		else if (StandardDefs.isInstanceGenerator(targetType))
		{
			assert false : "formatExpr: instance generator lvalue with non-Model lvalue";
		}
		else
		{
			assert false : "formatExpr: unsupported lvalue type: " + targetType.getName();
		}

		return null;
	}

	/**
	 *
	 */
	private static String asArrayLiteral(Array array)
	{
		List elements = new ArrayList();

		for (Iterator iter = array.getElementInitializerIterator(); iter.hasNext(); )
		{
			elements.add(((Initializer)iter.next()).getValueExpr());
		}

		return "[" + TextGen.toCommaList(elements.iterator()) + "]";
	}

	/**
	 *
	 */
	private static String asObjectLiteral(Model model)
	{
		List pairs = new ArrayList();

		for (Iterator iter = model.getPropertyInitializerIterator(); iter.hasNext(); )
		{
			NamedInitializer init = (NamedInitializer)iter.next();
			pairs.add(init.getName() + ": " + init.getValueExpr());
		}

		return "{" + TextGen.toCommaList(pairs.iterator()) + "}";
	}

	/**
	 *
	 */
	private static StringBuffer fixupXMLString(String orig)
	{
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < orig.length(); i++)
		{
			if (orig.charAt(i) == '\r')
			{
				continue;
			}
			else if (orig.charAt(i) == '\n')
			{
				result.append("\\n");
				continue;
			}
			if (orig.charAt(i) == '\"')
			{
				result.append('\\');
			}
			result.append(orig.charAt(i));
		}

		return result;
	}

	/**
	 *
	 */
	private static String asXmlLiteral(XML component)
	{
		String xml = component.getLiteralXML();
		if (component.getIsE4X())
		{
			return xml;
		}
		else
		{
			StringBuffer buf = new StringBuffer(NameFormatter.toDot(StandardDefs.CLASS_XMLUTIL) + ".createXMLDocument(\"");
			buf.append(fixupXMLString(xml).toString());
			buf.append("\").firstChild");
			return buf.toString();
		}
	}

    private static String asXMLList(XMLList component)
    {
        StringBuffer buf = new StringBuffer("<>");
        buf.append(component.getLiteralXML());
        buf.append("</>");
        return buf.toString();
    }

	/**
	 * NOTE this class should NOT contain Errors or Warnings. This exception is a late-stage patch to replace
	 * an intolerably obscure error from generated AS (due to an outstanding bug) with a more specific error,
	 * to improve usability while the bug still exists. To be removed when 173905 is fixed.
	 */
	public static class DeclaredAndProceduralWithinRepeater extends CompilerMessage.CompilerError {}
}
