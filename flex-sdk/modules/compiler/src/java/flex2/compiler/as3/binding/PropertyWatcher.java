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

import flex2.compiler.mxml.rep.BindingExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PropertyWatcher extends Watcher
{
    private String property;
	private boolean suppressed;
    private boolean staticProperty;
	private Map bindingExpressions;

    public PropertyWatcher(int id, String property)
    {
        super(id);
        this.property = property;
        bindingExpressions = new HashMap();
		suppressed = false;
	}

    public void addBindingExpression(BindingExpression be)
    {
        bindingExpressions.put(new Integer(be.getId()), be);
    }

	public boolean shouldWriteSelf()
	{
		return !suppressed;
	}

	public Collection getBindingExpressions()
    {
        return bindingExpressions.values();
    }

    public String getPathToProperty()
    {
        String result;

        Watcher parent = getParent();
        if (parent instanceof PropertyWatcher)
        {
            PropertyWatcher parentPropertyWatcher = (PropertyWatcher) parent;

            result = parentPropertyWatcher.getPathToProperty() + "." + property;
        }
        else
        {
            result = property;
        }

        return result;
    }

    public String getProperty()
    {
        return property;
    }

    public boolean getStaticProperty()
    {
        return staticProperty;
    }

    public void setStaticProperty(boolean staticProperty)
    {
        this.staticProperty = staticProperty;
    }

	public void suppress()
	{
		suppressed = true;
	}
}
