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

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.rep.VariableDeclaration;
import flex2.compiler.SymbolTable;

import java.util.*;

/**
 * framework-specific AS support classes, packages, import sets, etc.
 */
public class FrameworkDefs
{
	//	classes
	public static final String classRadioButtonGroup = 			"mx.controls:RadioButtonGroup";
	public static final String classDownloadProgressBar = 		"mx.preloaders:DownloadProgressBar";

	//	packages
	public static final String builtinEffectsPackage = 			"mx.effects";

	/**
	 *
	 */
	public static final Set builtInEffectNames;
	static
	{
		builtInEffectNames = new HashSet();
		builtInEffectNames.add("Dissolve");
		builtInEffectNames.add("Fade");
		builtInEffectNames.add("WipeLeft");
		builtInEffectNames.add("WipeRight");
		builtInEffectNames.add("WipeUp");
		builtInEffectNames.add("WipeDown");
		builtInEffectNames.add("Zoom");
		builtInEffectNames.add("Resize");
		builtInEffectNames.add("Move");
		builtInEffectNames.add("Pause");
		builtInEffectNames.add("Rotate");
		builtInEffectNames.add("Iris");
		builtInEffectNames.add("Blur");
		builtInEffectNames.add("Glow");
	}

	/**
	 *
	 */
	public static final Set requiredTopLevelDescriptorProperties;
	static
	{
		requiredTopLevelDescriptorProperties = new HashSet();
		requiredTopLevelDescriptorProperties.add("height");
		requiredTopLevelDescriptorProperties.add("width");
		requiredTopLevelDescriptorProperties.add("creationPolicy");
	}

	/**
	 *
	 */
	public static boolean isBuiltinEffectName(String name)
	{
		return builtInEffectNames.contains(name);
	}

	/**
	 * (generated) binding management variable sets
	 */
	public static final List bindingManagementVars = new ArrayList();
	static
	{
		bindingManagementVars.add(new VariableDeclaration("mx_internal", "_bindings", SymbolTable.ARRAY, "[]"));
		bindingManagementVars.add(new VariableDeclaration("mx_internal", "_watchers", SymbolTable.ARRAY, "[]"));
		bindingManagementVars.add(new VariableDeclaration("mx_internal", "_bindingsByDestination", SymbolTable.OBJECT, "{}"));
		bindingManagementVars.add(new VariableDeclaration("mx_internal", "_bindingsBeginWithWord", SymbolTable.OBJECT, "{}"));
	}
}
