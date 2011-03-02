////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3.binding;

import flex2.compiler.util.NameFormatter;

import java.util.*;

public abstract class Watcher
{
    public static final String CURRENT_INDEX = "currentIndex";
    public static final String CURRENT_ITEM = "currentItem";
    public static final String DATA_PROVIDER = "dataProvider";
    public static final String REPEATER_ITEM = "repeaterItem";

    protected Map childWatchers;
    private int id;
    private Watcher parent;
    private boolean isPartOfAnonObjectGraph;
    private Set changeEvents;
    private boolean shouldWriteChildren;
    private boolean operation;
    private String className;

    protected static final ChangeEvent NO_CHANGE_EVENT = new ChangeEvent("__NoChangeEvent__", true);

    public Watcher(int id)
    {
        this.id = id;
        childWatchers = new HashMap();
        isPartOfAnonObjectGraph = false;
        changeEvents = new HashSet();
        shouldWriteChildren = true;
    }

    public Set getChangeEvents()
    {
        return changeEvents;
    }

    public String getClassName()
    {
        return className;
    }

    public int getId()
    {
        return id;
    }

    public Watcher getParent()
    {
        return parent;
    }

    public boolean getShouldWriteChildren()
    {
        return shouldWriteChildren;
    }

    public boolean isPartOfAnonObjectGraph()
    {
        return isPartOfAnonObjectGraph;
    }

    public void setPartOfAnonObjectGraph(boolean isPartOfAnonObjectGraph)
    {
        this.isPartOfAnonObjectGraph = isPartOfAnonObjectGraph;
    }

    public void setShouldWriteChildren(boolean shouldWriteChildren)
    {
        this.shouldWriteChildren = shouldWriteChildren;
    }

    public boolean shouldWriteSelf()
    {
        return true;
    }

    public void addChild(Watcher child)
    {
        if (child instanceof PropertyWatcher)
        {
            childWatchers.put(((PropertyWatcher)child).getProperty(), child);
        }
        else
        {
            childWatchers.put(new Integer(child.id), child);
        }
        child.parent = this;
    }

    public PropertyWatcher getChild(String property)
    {
        if (childWatchers.containsKey(property))
        {
            return (PropertyWatcher)childWatchers.get(property);
        }
        else
        {
            return null;
        }
    }

    public Collection getChildren()
    {
        return childWatchers.values();
    }

    public void addChangeEvent(String name)
    {
        addChangeEvent(name, true);
    }

    public void addChangeEvent(String name, boolean validate)
    {
        changeEvents.add(new ChangeEvent(name, validate));
    }

    public void addNoChangeEvent()
    {
        changeEvents.add(NO_CHANGE_EVENT);
    }

    public boolean isOperation()
    {
        return operation;
    }

    public void setClassName(String className)
    {
        this.className = NameFormatter.toDot(className);
    }

    public void setOperation(boolean operation)
    {
        this.operation = operation;
    }
}
