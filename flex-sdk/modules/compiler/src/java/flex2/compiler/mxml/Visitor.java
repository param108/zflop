////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml;

import java.util.List;

/**
 * @author Clement Wong
 */
public interface Visitor
{
	void parseApplication(Token app, List components);

	void parseComponent(Token comp, List components);

	void parseStyle(Token style, Token text);

	void parseScript(Token script, Token text);

	void parseMetaData(Token metadata, Token text);

	void parseModel(Token t, List objects);

	void parseXML(Token t, List objects);
    
    void parseXMLList(Token t, List objects);

	void parseArray(Token t, List array);

	void parseBinding(Token t);

	void parseAnonymousObject(Token t, List objects);

	void parseWebService(Token t, List children);

	void parseHTTPService(Token t, List children);

	void parseRemoteObject(Token t, List children);

	void parseOperation(Token t, List children);

	void parseRequest(Token t, List children);

	void parseMethod(Token t, List children);

	void parseArguments(Token t, List children);

	void parseString(Token s, Token data);

	void parseNumber(Token n, Token data);

    void parseInt(Token n, Token data);

    void parseUInt(Token n, Token data);

    void parseBoolean(Token b, Token data);

	void parseClass(Token b, Token data);

	void parseFunction(Token b, Token data);

	void parseInlineComponent(Token t, Token child);
}
