////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import flex2.compiler.util.NameFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataBindingInfo
{
    private String className;
    /**
     * Root watchers are watchers that watch things hanging off the
     * document (as opposed to children).
     */
    private Map rootWatchers;
    private List bindingExpressions;
    private String watcherSetupUtilClassName;
    private Set imports;

    public DataBindingInfo(Set imports)
    {
        this.imports = imports;
        rootWatchers = new HashMap();
    }

    public List getBindingExpressions()
    {
        return bindingExpressions;
    }

    public String getClassName()
    {
        return className;
    }

    public Set getImports()
    {
        return imports;
    }

    public Map getRootWatchers()
    {
        return rootWatchers;
    }

    public String getWatcherSetupUtilClassName()
    {
        return watcherSetupUtilClassName;
    }

    public void setBindingExpressions(List bindingExpressions)
    {
        this.bindingExpressions = bindingExpressions;
    }

    public void setClassName(String className)
    {
        this.className = NameFormatter.toDot(className);
        watcherSetupUtilClassName = "_" + this.className.replace('.', '_') + "WatcherSetupUtil";
    }
}
