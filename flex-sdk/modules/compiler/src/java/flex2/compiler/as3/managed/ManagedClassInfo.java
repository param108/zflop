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

import flex2.compiler.SymbolTable;
import flex2.compiler.as3.genext.GenerativeClassInfo;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.QName;
import macromedia.asc.parser.DefinitionNode;
import macromedia.asc.util.Context;
import macromedia.asc.util.IntegerPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ManagedClassInfo extends GenerativeClassInfo
{
    /**
     * Modes are: hierarchical (default), association (no transitive change events), or
     * manual (no codegen).
     */
    public static final int MODE_INVALID = -1, MODE_HIER = 0, MODE_ASSOC = 1, MODE_MANUAL = 2;

    /**
     * Explicit property-level mode settings are stored here. Note that during processing
     * by (@link ManagedFirstPassEvaluator ManagedFirstPassEvaluator) this map will have
     * entries for all property-level [Bindable] metadata encountered, but the final result
     * of processing is:
     * <li>- accessorMap contains only non-manual-mode properties
     * <li>- propertyModes contains only non-default, non-manual-mode properties
     */
    private final Map propertyModes;

    private boolean needsToImplementIEventDispatcher;
    private boolean needsToImplementIManaged;
    private Set transientProperties;

    public ManagedClassInfo(Context context, SymbolTable symbolTable, String className)
    {
        super(context, symbolTable);
        setClassName(className);
        propertyModes = new HashMap();
    }

    /**
     * per-property modes
     */
    public void setPropertyMode(QName propertyQName, int mode)
    {
        propertyModes.put(propertyQName, IntegerPool.getNumber(mode));
    }

    public int getPropertyMode(QName propertyQName)
    {
        Integer propertyMode = (Integer)propertyModes.get(propertyQName);
        return propertyMode != null ? propertyMode.intValue() : MODE_HIER;
    }

    public boolean hasExplicitMode(QName propertyQName)
    {
        return propertyModes.get(propertyQName) != null;
    }

    public boolean isAssociative(AccessorInfo accessorInfo)
    {
        return getPropertyMode(new QName(accessorInfo.getUserNamespace(), accessorInfo.getPropertyName())) == MODE_ASSOC;
    }

    /**
     * transient properties
     */
    public void setTransientProperties(Set properties)
    {
        transientProperties = properties;
    }

    public boolean isTransientProperty(String propertyName)
    {
        if (transientProperties != null)
        {
            return transientProperties.contains(propertyName);
        }
        return false;
    }

    /**
     * interfaces
     */
    public boolean needsAdditionalInterfaces()
    {
        return needsToImplementIEventDispatcher || needsToImplementIManaged;
    }

    // Bean like methods for Velocity Template
    public boolean getNeedsToImplementIEventDispatcher()
    {
        return needsToImplementIEventDispatcher;
    }

    public boolean getNeedsToImplementIManaged()
    {
        return needsToImplementIManaged;
    }

    public void removeOriginalMetaData(DefinitionNode definitionNode)
    {
        NodeMagic.removeMetaData(definitionNode, StandardDefs.MD_MANAGED);
    }

    public void setNeedsToImplementIEventDispatcher(boolean needsToImplementIEventDispatcher)
    {
        this.needsToImplementIEventDispatcher = needsToImplementIEventDispatcher;
    }

    public void setNeedsToImplementIManaged(boolean needsToImplementIManaged)
    {
        this.needsToImplementIManaged = needsToImplementIManaged;
    }
}
