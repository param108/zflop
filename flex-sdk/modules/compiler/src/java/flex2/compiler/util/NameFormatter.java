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

package flex2.compiler.util;

import flex2.compiler.Source;
import flex2.compiler.swc.SwcScript;

/**
 * AS3 definition name conversion utilities
 *
 * TODO need to migrate away from having both colon- and dot-delimited classnames encoded in Strings. It's
 * impossible to tell except from (sometimes nonlocal) context whether 'String className' means dot-format or
 * colon-format, and since source code requires one and the reflection system needs the other, this is an ongoing
 * source of bugs, confusion etc. Eventually, all colon-delimited classnames should be stored as QNames -
 * qname.toString() is (now) a reasonably cheap way to recover the colon-delimited format. The goal is to get the
 * code to the point that 'String className' can be assumed to be a classname in dot format. The sign that we've
 * succeeded will be the successful removal of NameFormatter.toColon(String, String).
 *
 * TODO remove inline ':' -> '.' type code from the codebase, call these methods instead.
 * TODO once the abstraction is clear, "toColon"/"toDot" should be renamed to reflect the semantics of the names rather
 * than the actual characters involved: "toInternal", "toExternal" or whatever. But for now I can never remember which
 * is which.
 */
public class NameFormatter
{
	/**
	 * convert pair of strings to single string delimited by '.'
	 */
	public static String toDot(String ns, String n)
	{
		return ns.length() > 0 ? ns + '.' + n : n;
	}

	/**
	 * convert pair of strings to single string delimited by ':'
	 */
	public static String toColon(String ns, String n)
	{
		return ns.length() > 0 ? (ns + ':' + n).intern() : n;
	}

	/**
	 * convert p.q:C to p.q.C
	 * NOTE: idempotent
	 */
	public static String toDot(String n)
	{
		assert n.indexOf('/') == -1;
		return toDot(n, ':');
	}

	/**
	 *
	 */
	public static String toDot(QName qname)
	{
		return toDot(qname.getNamespace(), qname.getLocalPart());
	}

	/**
	 * convert <strong>all instances</strong> of <code>delimiter</code> with '.'.
	 */
	public static String toDot(String n, char delimiter)
	{
		return n.replace(delimiter, '.');
	}

	/**
	 * convert p.q.C to p.q:C
	 * NOTE: idempotent
	 */
	public static String toColon(String n)
	{
		int i = toDot(n).lastIndexOf('.');
		return i > 0 ? (n.substring(0, i) + ':' + n.substring(i + 1)).intern() : n;
	}

	/**
	 *
	 */
	public static String toDotStar(String pkg)
	{
		return pkg + ".*";
	}

	/**
	 * retrieve package name from qualified name (dot or slash format ok)
	 */
	public static String retrievePackageName(String n)
	{
		int i = toDot(n).lastIndexOf('.');
		return i == -1 ? "" : n.substring(0, i);
	}

    /**
     * just removes $internal from package name.   
     */
	public static String normalizePackageName(String n)
	{
        return n.endsWith("$internal") ? n.substring(0, n.length() - "$internal".length()) : n;
    }

	/**
	 * retrieve class name from qualified name (dot or slash format ok)
	 */
	public static String retrieveClassName(String n)
	{
		int i = toDot(n).lastIndexOf('.');
		return i == -1 ? n : n.substring(i + 1);
	}

	/**
	 * convert name (dot or slash format ok) to MultiName
	 */
	public static MultiName toMultiName(String n)
	{
		int i = toDot(n).lastIndexOf('.');
		return i >= 0 ?
				new MultiName(new String[]{n.substring(0, i)}, n.substring(i + 1)) :
				new MultiName(new String[]{""}, n);
	}

	/**
	 * QName to Multiname
	 */
	public static MultiName toMultiName(QName qname)
	{
		return new MultiName(qname.getNamespace(), qname.getLocalPart());
	}

	/**
	 * convert name (dot or slash format ok) to QName
	 */
	public static QName toQName(String n)
	{
		int i = toDot(n).lastIndexOf('.');
		return i >= 0 ?
				new QName(n.substring(0, i), n.substring(i + 1)) :
				new QName("", n);
	}

    /**
     * Derive class name from file path
     */
    private static String classNameFromSource(Source source)
    {
    	if (source.isSourcePathOwner() || source.isSourceListOwner())
    	{
    		return source.getShortName();
    	}
    	else if (source.isSwcScriptOwner() || source.getCompilationUnit() != null)
    	{
    		return source.getCompilationUnit().topLevelDefinitions.first().getLocalPart();
    	}
    	else
    	{
    		assert false;
    		return null;
    	}
    }

    public static String nameFromSource(Source source)
    {
    	if (source.isSwcScriptOwner())
    	{
    		SwcScript script = (SwcScript) source.getOwner();
    		return script.getName();
    	}
    	else if (source.getCompilationUnit() != null)
    	{
        	return source.getCompilationUnit().topLevelDefinitions.first().toString().replace(':', '/').replace('.', '/');
    	}
    	else
    	{
    		assert false;
    		return null;
    	}
    }

	public static QName qNameFromSource(Source src)
	{
		String rel = src.getRelativePath();
		String name = (rel == null || rel.length() == 0 ? "" : (rel.replace('/', '.').replace('\\', '.') + ":")) + classNameFromSource(src);
		return new QName(name);
	}
}
