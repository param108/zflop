////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler;

import flash.localization.LocalizationManager;

/**
 * The compiler may use logger interfaces defined in other libraries. In that case, implement the interface
 * in this class. flash.css.Logger is an example.
 * 
 * @author Clement Wong
 */
public interface Logger extends flash.css.Logger
{
    int errorCount();

    int warningCount();

    void logInfo(String info);

    void logDebug(String debug);

    void logWarning(String warning);

    void logError(String error);

    void logInfo(String path, String info);

    void logDebug(String path, String debug);

    void logWarning(String path, String warning);

	void logWarning(String path, String warning, int errorCode);

    void logError(String path, String error);

	void logError(String path, String error, int errorCode);

    void logInfo(String path, int line, String info);

    void logDebug(String path, int line, String debug);

    void logWarning(String path, int line, String warning);

	void logWarning(String path, int line, String warning, int errorCode);

    void logError(String path, int line, String error);

	void logError(String path, int line, String error, int errorCode);

    void logInfo(String path, int line, int col, String info);

    void logDebug(String path, int line, int col, String debug);

    void logWarning(String path, int line, int col, String warning);

    void logError(String path, int line, int col, String error);

    void logWarning(String path, int line, int col, String warning, String source);

	void logWarning(String path, int line, int col, String warning, String source, int errorCode);

    void logError(String path, int line, int col, String error, String source);

	void logError(String path, int line, int col, String error, String source, int errorCode);

    void log(ILocalizableMessage m);

	void log(ILocalizableMessage m, String source);

    void needsCompilation(String path, String reason);

	void includedFileUpdated(String path);

	void includedFileAffected(String path);

    void setLocalizationManager( LocalizationManager mgr );
}
