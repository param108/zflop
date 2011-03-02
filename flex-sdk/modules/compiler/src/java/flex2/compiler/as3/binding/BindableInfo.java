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

import flex2.compiler.SymbolTable;
import flex2.compiler.as3.genext.GenerativeClassInfo;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.mxml.lang.StandardDefs;
import macromedia.asc.parser.DefinitionNode;
import macromedia.asc.util.Context;

public class BindableInfo extends GenerativeClassInfo
{
    private boolean needsToImplementIEventDispatcher;
    private boolean needsStaticEventDispatcher;
    private boolean requiresStaticEventDispatcher;

    public BindableInfo(Context context, SymbolTable symbolTable)
    {
        super(context, symbolTable);
    }

    public boolean needsAdditionalInterfaces()
    {
        return needsToImplementIEventDispatcher;
    }

    // Bean like methods for Velocity Template
    public boolean getNeedsToImplementIEventDispatcher()
    {
        return needsToImplementIEventDispatcher;
    }

    public boolean getNeedsStaticEventDispatcher()
    {
        return needsStaticEventDispatcher;
    }

    public boolean getRequiresStaticEventDispatcher()
    {
        return requiresStaticEventDispatcher;
    }

    public void removeOriginalMetaData(DefinitionNode definitionNode)
    {
        NodeMagic.removeMetaData(definitionNode, StandardDefs.MD_BINDABLE);
    }

    public void setNeedsToImplementIEventDispatcher(boolean needsToImplementIEventDispatcher)
    {
        this.needsToImplementIEventDispatcher = needsToImplementIEventDispatcher;
    }

    public void setNeedsStaticEventDispatcher(boolean needsStaticEventDispatcher)
    {
        this.needsStaticEventDispatcher = needsStaticEventDispatcher;
    }

    public void setRequiresStaticEventDispatcher(boolean requiresStaticEventDispatcher)
    {
        this.requiresStaticEventDispatcher = requiresStaticEventDispatcher;
    }
}
