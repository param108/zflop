////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.swc;

/**
 * The features enabled for a Swc
 *
 * @author Brian Deitte
 */
public class Versions
{
	private String libVersion;
	private String flexVersion;
    private String flexBuild;

	public String getLibVersion()
	{
		return libVersion;
	}

	public void setLibVersion(String libVersion)
	{
		this.libVersion = libVersion;
	}

	public String getFlexVersion()
	{
		return flexVersion;
	}

	public void setFlexVersion(String flexVersion)
	{
		this.flexVersion = flexVersion;
	}

	public String getFlexBuild()
	{
		return flexBuild;
	}

	public void setFlexBuild(String flexBuild)
	{
		this.flexBuild = flexBuild;
	}
}
