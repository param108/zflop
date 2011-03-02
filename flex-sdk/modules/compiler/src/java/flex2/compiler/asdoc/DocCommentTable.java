////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.asdoc;

import java.util.List;
import java.util.Map;

public interface DocCommentTable
{
    /**
     * Leave anything unneeded as null. (Ex. For retrieving a class level DocComment
     * you may leave name as null. Top level definitionns (such as functions)
     * can be retrieved with both null, empty, or "null" className. Empty package
     * is equivalent to packageName == null or packageName == "".
     * 
     * @param name
     * @param className
     * @param packageName
     * @param type Will not work with DocComment.METADATA
     * @return a specific DocComment given parameters, null if not found or error occured.
     */
    public DocComment getComment(String name, String className, String packageName, int type);

    /**
     * @return Map of all packages where key = package name, value = DocComment.
     */
    public Map getPackages();
    
    /**
     * Useful to retrieve all the class names from a package (since they must be unique
     * within a package).
     * 
     * @param packageName
     * @return Map of all classes and interfaces in a specific package where
     * key = class or interface name, value = DocComment.
     *
     */
    public Map getClassesAndInterfaces(String packageName);
    
    /**
     * @param packageName
     * @return Map of all classes in a specific package where 
     * key = class name, value = DocComment.
     */
    public Map getClasses(String packageName);
    
    /**
     * @param packageName
     * @return Map of all interfaces in a specific package where 
     * key = interface name, value = DocComment.
     */
    public Map getInterfaces(String packageName);
    
    /**
     * Does not include get and set methods (those must be retrieved separately)
     * 
     * @param className
     * @param packageName
     * @return Map of all functions in a specific class or interface where 
     * key = function name, value = DocComment.
     */
    public Map getFunctions(String className, String packageName);
    
    /**
     * @param className
     * @param packageName
     * @return Map of all fields in a specific class or interface where 
     * key = field name, value = DocComment.
     */
    public Map getFields(String className, String packageName);
    
    /**
     * @param className
     * @param packageName
     * @return Map of all get methods in a specific class or interface where 
     * key = get function name, value = DocComment.
     */
    public Map getGetMethods(String className, String packageName);
    
    /**
     * @param className
     * @param packageName
     * @return Map of all set methods in a specific class or interface where 
     * key = set function name, value = DocComment.
     */
    public Map getSetMethods(String className, String packageName);
    
    /**
     * @return all the DocComments stored in this DocCommentTable
     */
    public List getAllComments();
    
    /**
     * @param className
     * @param packageName
     * @return all the DocComments associated with the specified class and package
     */
    public List getAllClassComments(String className, String packageName);
}