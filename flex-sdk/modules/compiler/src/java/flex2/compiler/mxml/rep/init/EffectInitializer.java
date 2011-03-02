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

package flex2.compiler.mxml.rep.init;

import flex2.compiler.mxml.gen.TextGen;
import flex2.compiler.mxml.reflect.Effect;
import flex2.compiler.mxml.reflect.Type;

/**
 * Note: need member for type because of classref/instance modality. See processing in Model and Builder
 */
public class EffectInitializer extends NamedInitializer
{
	protected final Effect effect;
	protected final Type type;

	public EffectInitializer(Effect effect, Object value, Type type, int line)
	{
		super(value, line);
		this.effect = effect;
		this.type = type;
	}

	public String getName()
	{
		return effect.getName();
	}

	public Type getLValueType()
	{
		return type;
	}

	public String getEventName()
	{
		return effect.getEvent();
	}

	public String getAssignExpr(String lvalueBase)
	{
		return lvalueBase + ".setStyle(" + TextGen.quoteWord(getName()) + ", " + getValueExpr() + ")";
	}

}
