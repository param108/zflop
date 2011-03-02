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

package flex2.compiler.mxml.reflect;

/**
 * @author Clement Wong
 */
public interface Deprecated
{
    // [Deprecated(...)] statments
	String DEPRECATED   = "Deprecated";
	String REPLACEMENT  = "replacement";
	String MESSAGE		= "message";
	String SINCE		= "since";

    // the following are used in other statements, e.g. [Style(..., deprecatedMessage=...)]
    //TODO want a "deprecated"-only property for Styles 
    String DEPRECATED_REPLACEMENT = "deprecatedReplacement";
	String DEPRECATED_MESSAGE     = "deprecatedMessage";
    String DEPRECATED_SINCE       = "deprecatedSince";

	String getReplacement();

	String getMessage();
	
	String getSince();
}
