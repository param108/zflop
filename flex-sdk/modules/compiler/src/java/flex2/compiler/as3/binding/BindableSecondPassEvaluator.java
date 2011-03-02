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

package flex2.compiler.as3.binding;

import flex2.compiler.CompilationUnit;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.as3.genext.GenerativeClassInfo;
import flex2.compiler.as3.genext.GenerativeExtension;
import flex2.compiler.as3.genext.GenerativeSecondPassEvaluator;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import macromedia.asc.parser.ClassDefinitionNode;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.VariableDefinitionNode;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author Paul Reilly
 */
public class BindableSecondPassEvaluator extends GenerativeSecondPassEvaluator
{
	private static final String CODEGEN_TEMPLATE_PATH = "flex2/compiler/as3/binding/";
	private static final String STATIC_EVENT_DISPATCHER = "staticEventDispatcher";
	private BindableInfo bindableInfo;
	private boolean inClass = false;

	public BindableSecondPassEvaluator(CompilationUnit unit, Map classMap,
									   TypeAnalyzer typeAnalyzer, String generatedOutputDirectory)
	{
		super(unit, classMap, typeAnalyzer, generatedOutputDirectory);
	}

	/**
	 *
	 */
	public Value evaluate(Context context, ClassDefinitionNode node)
	{
		if (!evaluatedClasses.contains(node))
		{
			inClass = true;

			String className = NodeMagic.getClassName(node);

			bindableInfo = (BindableInfo) classMap.get(className);

			if (bindableInfo != null)
			{
				ClassInfo classInfo = bindableInfo.getClassInfo();
				if (!classInfo.implementsInterface(StandardDefs.PACKAGE_FLASH_EVENTS,
												   GenerativeExtension.IEVENT_DISPATCHER))
				{
					bindableInfo.setNeedsToImplementIEventDispatcher(true);

					MultiName multiName = new MultiName(StandardDefs.PACKAGE_FLASH_EVENTS,
														GenerativeExtension.IEVENT_DISPATCHER);
					InterfaceInfo interfaceInfo = typeAnalyzer.analyzeInterface(context, multiName, classInfo);

                    // interfaceInfo will be null if IEventDispatcher was not resolved.
                    // This most likely means that playerglobal.swc was not in the
                    // external-library-path and other errors will be reported, so punt.
					if ((interfaceInfo == null) || checkForExistingMethods(context, node, classInfo, interfaceInfo))
					{
						return null;
					}

					classInfo.addInterfaceMultiName(StandardDefs.PACKAGE_FLASH_EVENTS,
													GenerativeExtension.IEVENT_DISPATCHER);
				}

				if (bindableInfo.getRequiresStaticEventDispatcher() &&
					(!classInfo.definesVariable(STATIC_EVENT_DISPATCHER) &&
					 !classInfo.definesGetter(STATIC_EVENT_DISPATCHER, true)))
				{
					bindableInfo.setNeedsStaticEventDispatcher(true);
				}

				postProcessClassInfo(context, bindableInfo);

				prepClassDef(node);

				if (node.statements != null)
				{
					node.statements.evaluate(context, this);

					modifySyntaxTree(context, node, bindableInfo);
				}

				bindableInfo = null;
			}

			inClass = false;

			// Make sure we don't process this class again.
			evaluatedClasses.add(node);
		}

		return null;
	}

	/**
	 * prepare class def node for augmentation. Currently, all we need to do is strip class-leve [Bindable] md.
	 */
	private void prepClassDef(ClassDefinitionNode node)
	{
		if (node.metaData != null && node.metaData.items != null)
		{
			for (Iterator iter = node.metaData.items.iterator(); iter.hasNext(); )
			{
				MetaDataNode md = (MetaDataNode)iter.next();
				if (StandardDefs.MD_BINDABLE.equals(md.id) && md.count() == 0)
				{
					iter.remove();
				}
			}
		}
	}

	/**
	 * Hide *setters* which have had bindable versions generated. Getters are not wrapped.
	 *
	 * In BindableFirstPassEvaluator we visited the interior of a function definition, in order to generate errors on
	 * [Bindable] metadata we found there. Here we avoid FunctionDefinitionNodes because the VariableDefinitionNodes
	 * within them might otherwise be spuriously renamed.
	 */
	public Value evaluate(Context context, FunctionDefinitionNode node)
	{
		if (inClass)
		{
			QName qname = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getFunctionName(node));
			GenerativeClassInfo.AccessorInfo accessorInfo = bindableInfo.getAccessor(qname);
			if (accessorInfo != null)
			{
				if (!NodeMagic.functionIsGetter(node))
				{
					hideFunction(node, accessorInfo);
					registerRenamedAccessor(accessorInfo);
				}
				else if (!bindableInfo.getClassInfo().definesSetter(qname.getLocalPart(), false))
				{
					context.localizedError2(node.pos(), new MissingNonInheritedSetter(qname.getLocalPart()));
				}
			}
		}

		return null;
	}

	/**
	 * visits all variable definitions that occur inside class definitions, outside function definitions, and mangles
	 * their names if they've been marked for [Bindable] codegen.
	 */
	public Value evaluate(Context context, VariableDefinitionNode node)
	{
		if (inClass)
		{
			QName qname = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getVariableName(node));
			GenerativeClassInfo.AccessorInfo accessorInfo = bindableInfo.getAccessor(qname);
			if (accessorInfo != null)
			{
				hideVariable(node, accessorInfo);
				registerRenamedAccessor(accessorInfo);
			}
		}

		return null;
	}

	/**
	 *
	 */
	protected String getTemplateName()
	{
		return "BindableProperty";
	}

	protected String getTemplatePath()
	{
		return CODEGEN_TEMPLATE_PATH;
	}

	/**
	 *
	 */
	protected Map getTemplateVars()
	{
		Map vars = new HashMap();
		vars.put("bindableInfo", bindableInfo);

		return vars;
	}

	/**
	 *
	 */
	protected String getGeneratedSuffix()
	{
		return "-binding-generated.as";
	}

	public static class MissingNonInheritedSetter extends CompilerMessage.CompilerError
	{
		public String getter;

		public MissingNonInheritedSetter(String getter)
		{
			this.getter = getter;
		}
	}
}
