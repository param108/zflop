////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import flex2.compiler.mxml.rep.BindingExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class EvaluationWatcher extends Watcher
{
    private Map triggeringWatchers;
    private BindingExpression bindingExpression;
    private Watcher parentWatcher;

    public EvaluationWatcher(int id, BindingExpression bindingExpression)
    {
        super(id);
        this.bindingExpression = bindingExpression;
        triggeringWatchers = new HashMap();
    }

    public boolean shouldWriteSelf()
    {
        return getChildren().size() > 0;
    }

    public BindingExpression getBindingExpression()
    {
        return bindingExpression;
    }

    public Watcher getParentWatcher()
    {
        return parentWatcher;
    }

    public Collection getTriggeringWatchers()
    {
        return triggeringWatchers.values();
    }

    public void addTriggeringWatcher(Watcher watcher)
    {
        if (!triggeringWatchers.containsKey(new Integer(watcher.getId())))
        {
            triggeringWatchers.put(new Integer(watcher.getId()), watcher);
        }
    }

    public abstract String getEvaluationPart();

    public void setParentWatcher(Watcher parentWatcher)
    {
        this.parentWatcher = parentWatcher;
    }
}
