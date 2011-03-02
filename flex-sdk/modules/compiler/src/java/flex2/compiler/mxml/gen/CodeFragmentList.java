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

package flex2.compiler.mxml.gen;

import flex2.compiler.mxml.rep.Script;

import java.util.ArrayList;

public class CodeFragmentList extends ArrayList
{
	public CodeFragmentList()
	{
	}

	public CodeFragmentList(String fragment, int line)
	{
		add(fragment, line);
	}

	public final boolean add(String fragment, int line)
	{
		return super.add(new Script(fragment, line));
	}

	// add these new add() methods in order to set exact-size StringBuffer

	public final boolean add(String s1, String s2, int line)
	{
		StringBuffer fragment = new StringBuffer(s1.length() + s2.length());
		fragment.append(s1);
		fragment.append(s2);
		return add(fragment.toString(), line);
	}

	public final boolean add(String s1, String s2, String s3, int line)
	{
		StringBuffer fragment = new StringBuffer(s1.length() + s2.length() + s3.length());
		fragment.append(s1);
		fragment.append(s2);
		fragment.append(s3);
		return add(fragment.toString(), line);
	}

	public final boolean add(String s1, String s2, String s3, String s4, int line)
	{
		StringBuffer fragment = new StringBuffer(s1.length() + s2.length() + s3.length() + s4.length());
		fragment.append(s1);
		fragment.append(s2);
		fragment.append(s3);
		fragment.append(s4);
		return add(fragment.toString(), line);
	}

	public final boolean add(String s1, String s2, String s3, String s4, String s5, int line)
	{
		StringBuffer fragment = new StringBuffer(s1.length() + s2.length() + s3.length() + s4.length() + s5.length());
		fragment.append(s1);
		fragment.append(s2);
		fragment.append(s3);
		fragment.append(s4);
		fragment.append(s5);
		return add(fragment.toString(), line);
	}

	public void add(int index, String fragment, int line)
	{
		super.add(index, new Script(fragment, line));
	}

	/**
	 * prohibit unmapped lines - override super.add(Object)
	 */
	public boolean add(Object obj)
	{
		assert false : "CodeFragmentList.add(...) without line ref";
		return false;
	}

	/**
	 * prohibit unmapped lines - override super.add(index, Object)
	 */
	public void add(int index, Object obj)
	{
		assert false : "CodeFragmentList.add(index, ...) without line ref";
	}
}
