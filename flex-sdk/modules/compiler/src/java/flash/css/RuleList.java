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

package flash.css;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RuleList
{
    private List rules = new ArrayList();

    public void add(Rule rule)
    {
        rules.add(rule);
    }

    public void add(int index, Rule rule)
    {
        rules.add(index, rule);
    }

    public Rule get(int index)
    {
        return (Rule) rules.get(index);
    }

    public int getLength()
    {
        return rules.size();
    }

    public Rule item(int index)
    {
        return (Rule) rules.get(index);
    }

    public Iterator iterator()
    {
        return rules.iterator();
    }

    public void remove(int index)
    {
        rules.remove(index);
    }
}
