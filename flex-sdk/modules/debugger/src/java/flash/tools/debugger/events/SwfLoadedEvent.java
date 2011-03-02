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

package flash.tools.debugger.events;

/**
 * This event is fired when the player has completed the loading of 
 * the specified SWF.
 */
public class SwfLoadedEvent extends DebugEvent
{
	/** unique identifier for the SWF */
	public long id;				

	/** index of swf in Session.getSwfs() array */
	public int index;

	/** full path name for  SWF */
	public String path;

	/** size of the loaded SWF in bytes */
	public long swfSize;

	/** URL of the loaded SWF */
	public String url;

	/** port number related to the URL */
	public long port;

	/** name of host in which the SWF was loaded */
	public String host;

	public SwfLoadedEvent(long sId, int sIndex, String sPath, String sUrl, String sHost, long sPort, long sSwfSize)
	{
		id = sId;
		index = sIndex;
		swfSize = sSwfSize;
		port = sPort;
		path = sPath;
		url = sUrl;
		host = sHost;
	}
}
