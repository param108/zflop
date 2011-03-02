////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler;

import flex2.compiler.io.VirtualFile;
import flash.swf.tags.DefineTag;
import flash.swf.tags.DefineFont;

import java.util.*;

/**
 * @author Clement Wong
 */
public final class Assets
{
	private Map assets; // Map<String, AssetInfo>

	public void add(String className, AssetInfo assetInfo)
	{
		if (assets == null)
		{
			assets = new HashMap(4);
		}

		assets.put(className, assetInfo);
	}

	// FIXME - this is cheating, not sure what the best thing to do here is.
	// Used by CompilerSwcContext.
	public void add(String className, DefineTag tag)
	{
		if (assets == null)
		{
			assets = new HashMap(4);
		}

		assets.put(className, new AssetInfo(tag));
	}

	public void addAll(Assets ass)
	{
		if (ass.assets == null)
		{
			return;
		}

		if (assets == null)
		{
			assets = new HashMap(4);
		}

		assets.putAll(ass.assets);
	}

	public int count()
	{
		return assets == null ? 0 : assets.size();
	}

	public boolean contains(String className)
	{
		return assets == null ? false : assets.containsKey(className);
	}

	public AssetInfo get(String className)
	{
		return assets == null ? null : (AssetInfo) assets.get(className);
	}

	/**
	 * This is used by the webtier compiler.
	 */
	public Iterator iterator() // Iterator<Map.Entry<String, AssetInfo>>
	{
		return assets == null ? EMPTY_ITERATOR : assets.entrySet().iterator();
	}

	public boolean isUpdated()
	{
		boolean result = false;

		if (assets != null)
		{
			for (Iterator i = assets.values().iterator(); i.hasNext();)
			{
				AssetInfo assetInfo = (AssetInfo) i.next();
				VirtualFile path = assetInfo.getPath();

				// If the path is null, it's probably a system font
				// that doesn't get resolved by us, so just assume it
				// hasn't changed.
				if ((path != null) && (assetInfo.getCreationTime() != path.getLastModified()))
				{
					result = true;
				}
			}
		}

		return result;
	}

	public List getFonts()
	{
		LinkedList fonts = new LinkedList();

		if (assets != null)
		{
			for (Iterator it = assets.values().iterator(); it.hasNext();)
			{
				AssetInfo assetInfo = (AssetInfo) it.next();
				DefineTag defineTag = assetInfo.getDefineTag();

				if (defineTag instanceof DefineFont)
				{
					fonts.add(defineTag);
				}
			}
		}

		return fonts;
	}

	public boolean exists(String name)
	{
		return assets != null && assets.containsValue(name);
	}
	
	public int size()
	{
		return assets == null ? 0 : assets.size();
	}
	
	private static final Iterator EMPTY_ITERATOR = new Iterator()
	{
		public boolean hasNext()
		{
			return false;
		}

		public Object next()
		{
			return null;
		}

		public void remove()
		{
		}
	};
}
