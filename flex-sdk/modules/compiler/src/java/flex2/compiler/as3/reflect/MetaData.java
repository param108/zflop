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

package flex2.compiler.as3.reflect;

import macromedia.asc.parser.MetaDataEvaluator.KeyValuePair;
import macromedia.asc.parser.MetaDataEvaluator.KeylessValue;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Clement Wong
 */
public final class MetaData implements flex2.compiler.abc.MetaData
{
	public MetaData(MetaDataNode node)
	{
		this.id = node.id;
		this.values = node.values;
	}

	private String id;
	private Value[] values;

	public String getID()
	{
		return id;
	}

	public String getKey(int index)
	{
		if (index < 0 || index >= count())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		else if (values[index] instanceof KeylessValue)
		{
			return null;
		}
		else if (values[index] instanceof KeyValuePair)
		{
			return ((KeyValuePair) values[index]).key;
		}
		else
		{
			return null;
		}
	}

	public String getValue(String key)
	{
		for (int i = 0, length = count(); i < length; i++)
		{
			if (values[i] instanceof KeyValuePair)
			{
				if (((KeyValuePair) values[i]).key.equals(key))
				{
					return ((KeyValuePair) values[i]).obj;
				}
			}
		}
		return null;
	}

	public String getValue(int index)
	{
		if (index < 0 || index >= count())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		else if (values[index] instanceof KeylessValue)
		{
			return ((KeylessValue) values[index]).obj;
		}
		else if (values[index] instanceof KeyValuePair)
		{
			return ((KeyValuePair) values[index]).obj;
		}
		else
		{
			return null;
		}
	}

	public Map getValueMap()
	{
		Map result = new HashMap();

		for (int i = 0, length = count(); i < length; i++)
		{
			if (values[i] instanceof KeyValuePair)
			{
				KeyValuePair keyValuePair = (KeyValuePair) values[i];

				result.put(keyValuePair.key, keyValuePair.obj);
			}
		}

		return result;
	}

	public int count()
	{
		return values != null ? values.length : 0;
	}
}
