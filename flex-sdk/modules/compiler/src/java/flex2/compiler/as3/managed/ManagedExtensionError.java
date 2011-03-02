////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.managed;

import java.util.Iterator;

import macromedia.asc.parser.MetaDataNode;
import flash.localization.LocalizationManager;
import flex2.compiler.CompilationUnit;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.as3.Extension;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.ThreadLocalToolkit;

public class ManagedExtensionError implements Extension {

	public void parse1(CompilationUnit unit, TypeTable typeTable) {
		macromedia.asc.util.Context cx = (macromedia.asc.util.Context) unit.getContext().getAttribute("cx");
		for (Iterator iter = unit.metadata.iterator(); iter.hasNext(); )
		{
			MetaDataNode metaDataNode = (MetaDataNode)iter.next();
			if (StandardDefs.MD_MANAGED.equals(metaDataNode.id))
			{
				LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
				cx.localizedError2(metaDataNode.pos(), new ManagedOnMXMLComponentError());
			}
		}
	}
	
    public void parse2(CompilationUnit unit, TypeTable typeTable)
    {
    }

	public void analyze1(CompilationUnit unit, TypeTable typeTable) {
	}

	public void analyze2(CompilationUnit unit, TypeTable typeTable) {
	}

	public void analyze3(CompilationUnit unit, TypeTable typeTable) {
	}

	public void analyze4(CompilationUnit unit, TypeTable typeTable) {
	}

	public void generate(CompilationUnit unit, TypeTable typeTable) {
	}

	/**
	 * Compiler Error Messages
	 */
	public static class ManagedOnMXMLComponentError extends CompilerMessage.CompilerError {}

}
