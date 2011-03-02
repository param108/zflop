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

package flex2.compiler.as3.managed;

import flex2.compiler.CompilationUnit;
import flex2.compiler.as3.binding.ClassInfo;
import flex2.compiler.as3.binding.InterfaceInfo;
import flex2.compiler.as3.binding.TypeAnalyzer;
import flex2.compiler.as3.genext.GenerativeClassInfo;
import flex2.compiler.as3.genext.GenerativeExtension;
import flex2.compiler.as3.genext.GenerativeSecondPassEvaluator;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.QName;
import macromedia.asc.parser.ClassDefinitionNode;
import macromedia.asc.parser.FunctionDefinitionNode;
import macromedia.asc.parser.VariableDefinitionNode;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Reilly
 */
public class ManagedSecondPassEvaluator extends GenerativeSecondPassEvaluator
{
	private static final String CODEGEN_TEMPLATE_PATH = "flex2/compiler/as3/managed/";
    private static final String IMANAGED = "IManaged";

    private ManagedClassInfo currentInfo;
    private boolean inClass = false;

	public ManagedSecondPassEvaluator(CompilationUnit unit, Map classMap,
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

			currentInfo = (ManagedClassInfo) classMap.get(className);

			if (currentInfo != null)
			{
				ClassInfo classInfo = currentInfo.getClassInfo();

				if (!classInfo.implementsInterface(StandardDefs.PACKAGE_FLASH_EVENTS,
												   GenerativeExtension.IEVENT_DISPATCHER))
				{
					currentInfo.setNeedsToImplementIEventDispatcher(true);

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

				if (!classInfo.implementsInterface(StandardDefs.PACKAGE_MX_DATA, IMANAGED))
				{
					currentInfo.setNeedsToImplementIManaged(true);

                    // Don't be tempted to check for mx.core.IUID here, because
                    // analyzeInterface() sets up the inheritance for downstream
                    // consumers and if we only add IUID to the inheritance, then
                    // the check for IManaged in the enclosing if statement will fail.
					MultiName multiName = new MultiName(StandardDefs.PACKAGE_MX_DATA, IMANAGED);
					InterfaceInfo interfaceInfo = typeAnalyzer.analyzeInterface(context, multiName, classInfo);

					// interfaceInfo will be null if IManaged was not resolved.
					// This most likely means that fds.swc was not in the
					// library-path and other errors will be reported, so punt.
					if ((interfaceInfo == null) || checkForExistingMethods(context, node, classInfo, interfaceInfo))
					{
						return null;
					}

					classInfo.addInterfaceMultiName(StandardDefs.PACKAGE_MX_DATA, IMANAGED);
				}

				postProcessClassInfo(context, currentInfo);

				if (node.statements != null)
				{
					node.statements.evaluate(context, this);

					modifySyntaxTree(context, node, currentInfo);
				}

				currentInfo = null;
			}

			inClass = false;

			// Make sure we don't process this class again.
			evaluatedClasses.add(node);
		}

		return null;
	}

    /**
     *
     */
    public Value evaluate(Context context, FunctionDefinitionNode node)
    {
		if (inClass)
		{
			QName qname = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getFunctionName(node));
			GenerativeClassInfo.AccessorInfo accessorInfo = currentInfo.getAccessor(qname);
			if (accessorInfo != null)
			{
				hideFunction(node, accessorInfo);
				registerRenamedAccessor(accessorInfo);
			}
		}

		return null;
    }

    /**
     * visits all variable definitions that occur inside class definitions (and outside function definitions) and mangles
     * their names
     */
    public Value evaluate(Context context, VariableDefinitionNode node)
    {
        if (inClass)
        {
			QName qname = new QName(NodeMagic.getUserNamespace(node), NodeMagic.getVariableName(node));
			GenerativeClassInfo.AccessorInfo info = currentInfo.getAccessor(qname);
			if (info != null)
			{
				hideVariable(node, info);
				registerRenamedAccessor(info);
			}
        }

        return null;
    }

	/**
	 *
	 */
	protected String getTemplateName()
	{
		return "ManagedProperty";
	}

	/**
	 *
	 */
	protected Map getTemplateVars()
	{
		Map vars = new HashMap();
		vars.put("managedInfo", currentInfo);

		return vars;
	}

	/**
	 *
	 */
	protected String getTemplatePath()
	{
		return CODEGEN_TEMPLATE_PATH;
	}

	/**
	 *
	 */
	protected String getGeneratedSuffix()
	{
		return "-managed-generated.as";
	}

}
