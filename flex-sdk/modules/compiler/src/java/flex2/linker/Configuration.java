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

package flex2.linker;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * @author Clement Wong
 */
public interface Configuration
{
    // C: If you add a method here, please add it to flex2.tools.oem.internal.LinkerConfiguration as well.
    
    int backgroundColor();

    /**
     * Generate SWFs for debugging.
     */
    boolean generateDebugTags();

    boolean optimize();

    boolean keepDebugOpcodes();

    boolean useNetwork();

    boolean lazyInit();

    boolean scriptLimitsSet();

    int getScriptTimeLimit();

    int getScriptRecursionLimit();

    int getFrameRate();

    String getMetadata();

    /**
     * The password to include in debuggable swfs.
     */
    String debugPassword();

    /**
     * SWF width
     */
    String width();

    int defaultWidth();

    /**
     * SWF height
     */
    String height();

    int defaultHeight();
    
    /**
     * SWF height percentage
     */
    String heightPercent();

    /**
     * SWF width percentage
     */
    String widthPercent();

    /**
     * browser page title
     */
    String pageTitle();

    /**
     * @param mainDefinition the name of the app class to instantiate
     */
    void setMainDefinition( String mainDefinition );
    String getMainDefinition();

    /**
     * @return the name of the root SWF class
     */
    String getRootClassName();

    /**
     * @param rootClassName the name of the root SWF class
     */
    void setRootClassName( String rootClassName );

    /**
     * @return list of frame classes
     */
    List getFrameList();

    /**
     * @return set of configured external symbols
     */
    Set getExterns();

    /**
     * @return set of symbols to always include
     */
    Set getIncludes();

    /**
     * @return set of symbols that were not resolved (includes any referenced externs)
     */
    Set getUnresolved();

    /**
     * @return name of compile report file, null if none
     */
    String getLinkReportFileName();
    boolean generateLinkReport();

    /**
     * @return name of resource bundle list file, null if none
     */
    String getRBListFileName();
    boolean generateRBList();

    /**
     * @return set of resource bundles for resource bundle list
     */
    SortedSet getResourceBundles();
    
    /**
     * @return the as3 metadata to keep
     */
    String[] getMetadataToKeep();
    
    /**
     * @return true if the digest should be computed, false otherwise.
     */
     boolean getComputeDigest();
     
     String getCompatibilityVersionString();

     int getCompatibilityVersion();

    /**
     * @return The major version of the player targeted by this application.
     *         The returned value will be greater to or equal to 9.  
     */
     int getTargetPlayerMajorVersion();

    /**
     * @return The minor version of the player targeted by this application.
     *         The returned value will be greater to or equal to 0.  
     */
     int getTargetPlayerMinorVersion();
    
    /**
     * @return The revision of the player targeted by this application.
     *         The returned value will be greater to or equal to 0.  
     */
     int getTargetPlayerRevision();     
}
