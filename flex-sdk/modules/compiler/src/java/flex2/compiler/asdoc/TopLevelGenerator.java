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
import java.util.Iterator;

public class TopLevelGenerator implements DocCommentGenerator
{
    StringBuffer xml;
    
    public TopLevelGenerator()
    {
        xml = new StringBuffer();
    }
    
    /**
     * Future implementation of this will just save it to a file in generate.
     */
    public String toString()
    {
        return xml.toString();
    }
    
    /**
     * Iterates through all classes and creates toplevel.xml
     */
    public void generate(DocCommentTable table)
    {
        xml.append("<asdoc>\n");
        Iterator packageIterator = table.getPackages().keySet().iterator();
        while (packageIterator.hasNext())
        {
            String currentPackage = (String)packageIterator.next();
            Iterator classIterator = table.getClassesAndInterfaces(currentPackage).keySet().iterator();
            while (classIterator.hasNext())
            {
                String currentClass = (String)classIterator.next();
                Iterator commentsIterator = table.getAllClassComments(currentClass, currentPackage).iterator();
                while (commentsIterator.hasNext())
                {
                    emitDocComment((DocComment)commentsIterator.next());
                }
            }
        }
        xml.append("\n</asdoc>\n");
    }
    
    /**
     * helper method for printing a tag in toplevel.xml
     * @param tagName
     * @param value
     */
    private void appendTag(String tagName, String value)
    {
        xml.append("\n<");
        xml.append(tagName);
        xml.append("><![CDATA[");
        xml.append(value);
        xml.append("]]></");
        xml.append(tagName);
        xml.append(">");
    }
    
    /**
     * append all tags in xml format (except inheritDoc)
     * @param tags
     */
    private void emitTags(Map tags)
    {
        Iterator tagIterator = tags.keySet().iterator();
        while (tagIterator.hasNext())
        {
            String tagName = ((String)tagIterator.next()).intern();
            Object o = tags.get(tagName);
            if (o == null)
                continue;
            if (o instanceof Boolean)
            {
                boolean b = ((Boolean)o).booleanValue();
                if (b && !(tagName == "inheritDoc"))
                {
                    appendTag(tagName, "");
                }
                else 
                    continue;
            }
            else if (o instanceof List)
            {
                List l = (List)o;
                for (int i = 0; i < l.size(); i++)
                {
                    String value = (String)l.get(i);
                    appendTag(tagName, value);
                }
            }
            else if (o instanceof Map)   //custom Tags (implied tagName.equals("custom")
            {
                Map m = (Map)o;
                Iterator customTagIter = m.keySet().iterator();
                while (customTagIter.hasNext())
                {
                    tagName = (String)customTagIter.next();
                    String value = (String)m.get(tagName);
                    appendTag(tagName, value);
                }
            }
            else
            {
                String value = (String)o;
                appendTag(tagName, value);
            }
        }
    }

    /**
     * appends metadata associated with a definition.
     * @param metadata
     */
    private void emitMetadata(List metadata)
    {
        for (int i = 0; i < metadata.size(); i++)
        {
            DocComment meta = (DocComment)metadata.get(i);
            String metadataType = meta.getMetadataType().intern();
            xml.append("\n<metadata>\n");
            xml.append("\t<");
            xml.append(metadataType);
            xml.append(" owner='");
            xml.append(meta.getOwner());
            xml.append("' ");
            String name = meta.getName().intern();
            if (!(name == "IGNORE"))
                xml.append("name='").append(name).append("' ");
            String type_meta = meta.getType_meta();
            if (type_meta != null)
            {
                xml.append("type='").append(type_meta).append("' ");
            }
            String event_meta = meta.getEvent_meta();
            if (event_meta != null)
            {
                xml.append("event='").append(event_meta).append("' ");
            }
            String kind_meta = meta.getKind_meta();
            if (kind_meta != null)
            {
                xml.append("kind='").append(kind_meta).append("' ");
            }
            String arrayType_meta = meta.getArrayType_meta();
            if (arrayType_meta != null)
            {
                xml.append("arrayType='").append(arrayType_meta).append("' ");
            }
            String format_meta = meta.getFormat_meta();
            if (format_meta != null)
            {
                xml.append("format='").append(format_meta).append("' ");
            }
            String enumeration_meta = meta.getEnumeration_meta();
            if (enumeration_meta != null)
            {
                xml.append("enumeration='").append(enumeration_meta).append("' ");
            }
            String inherit_meta = meta.getInherit_meta();
            if (inherit_meta != null)
            {
                xml.append("inherit='").append(inherit_meta).append("' ");
            }
            xml.append(">");
            
            //These types of metadata can have comments associated with them
            if (metadataType == "Event" || metadataType == "Style" || metadataType == "Effect")
            {
                String desc = meta.getDescription();
                if (desc != null)
                    appendTag("description", meta.getDescription());
                emitTags(meta.getAllTags());
            }
            xml.append("\n\t</");
            xml.append(metadataType);
            xml.append(">\n</metadata>");
        }
        
    }
    
    /**
     * Appends a package.
     * @param comment
     */
    private void emitPackage(DocComment comment)
    {
        xml.append("\n<packageRec name='");
        xml.append(comment.getFullname());
        xml.append("' fullname='");
        xml.append(comment.getFullname());
        xml.append("'>");
        
        String desc = comment.getDescription();
        if (desc != null)
            appendTag("description", comment.getDescription());
        emitTags(comment.getAllTags());
        
        xml.append("\n</packageRec>");
    }
    
    /**
     * Appends a class or interface
     * @param comment
     */
    private void emitClass(DocComment comment)
    {
        String tagName = (comment.getType() == DocComment.CLASS) ? "classRec" : "interfaceRec"; 
        xml.append("\n<");
        xml.append(tagName);
        xml.append(" name='");
        xml.append(comment.getName());
        xml.append("' fullname='");
        xml.append(comment.getFullname());
        String sourcefile = comment.getSourceFile();
        if (sourcefile != null)
        {
            xml.append("' sourcefile='");
            xml.append(sourcefile);
        }
        xml.append("' namespace='");
        xml.append(comment.getNamespace());
        xml.append("' access='");
        xml.append(comment.getAccess());
        xml.append("' ");
        if (comment.getType() == DocComment.INTERFACE)
        {
            String[] baseClasses = comment.getBaseclasses();
            if (baseClasses != null)
            {
                xml.append("baseClasses='");
                for (int i = 0; i < baseClasses.length; i++)
                {
                    String baseclass = baseClasses[i];
                    if (baseclass != null)
                    {
                        if (i != 0)
                            xml.append(";");
                        xml.append(baseclass);
                    }
                }
                xml.append("' ");
            }
        }
        else
        {
            xml.append("baseclass='");
            xml.append(comment.getBaseClass());
            xml.append("' ");
            String[] interfaces = comment.getInterfaces();
            if (interfaces != null)
            {
                xml.append("interfaces='");
                for (int i = 0; i < interfaces.length; i++)
                {
                    String inter = interfaces[i];
                    if (inter != null)
                    {
                        if (i != 0)
                            xml.append(";");
                        xml.append(inter);
                    }
                }
                xml.append("' ");
            }
        }
        xml.append("isFinal='");
        xml.append(comment.isFinal());
        xml.append("' ");
        xml.append("isDynamic='");
        xml.append(comment.isDynamic());
        xml.append("' ");
        xml.append(">");
        
        String desc = comment.getDescription();
        if (desc != null)
            appendTag("description", comment.getDescription());
        emitTags(comment.getAllTags());
        
        if (comment.getMetadata() != null)
            emitMetadata(comment.getMetadata());
        xml.append("\n</");
        xml.append(tagName);
        xml.append(">");
    }
    
    /**
     * Appends a function
     * @param comment
     */
    private void emitFunction(DocComment comment)
    {
        xml.append("\n<method name='");
        xml.append(comment.getName());
        xml.append("' fullname='");
        xml.append(comment.getFullname());
        xml.append("' ");
        xml.append("isStatic='");
        xml.append(comment.isStatic());
        xml.append("' ");
        xml.append("isFinal='");
        xml.append(comment.isFinal());
        xml.append("' ");
        xml.append("isOverride='");
        xml.append(comment.isOverride());
        xml.append("' ");
        
        String[] param_names = comment.getParamNames();
        if (param_names != null)
        {
            xml.append(" param_names='");
            for (int i = 0; i < param_names.length; i++)
            {
                String pname = param_names[i];
                if (pname != null)
                {
                    if (i != 0)
                        xml.append(";");
                    xml.append(pname);
                }
            }
            xml.append("'");
            
            String[] param_types = comment.getParamTypes();
            xml.append(" param_types='");
            for (int i = 0; i < param_types.length; i++)
            {
                String ptype = param_types[i];
                if (ptype != null)
                {
                    if (i != 0)
                        xml.append(";");
                    xml.append(ptype);
                }
            }
            xml.append("'");
            
            String[] param_defaults = comment.getParamDefaults();
            xml.append(" param_defaults='");
            for (int i = 0; i < param_defaults.length; i++)
            {
                String pdefa = param_defaults[i];
                if (pdefa != null)
                {
                    if (i != 0)
                        xml.append(";");
                    xml.append(pdefa);
                }
            }
            xml.append("'");
        }
        
        xml.append(" result_type='");
        xml.append(comment.getResultType());
        xml.append("'>");
        
        String desc = comment.getDescription();
        if (desc != null)
            appendTag("description", comment.getDescription());
        emitTags(comment.getAllTags());
        
        if (comment.getMetadata() != null)
            emitMetadata(comment.getMetadata());
        xml.append("\n</method>");
    }
    
    /**
     * Appends a field.
     * @param comment
     */
    private void emitField(DocComment comment)
    {
        xml.append("\n<field name='");
        xml.append(comment.getName());
        xml.append("' fullname='");
        xml.append(comment.getFullname());
        xml.append("' type='");
        String type = comment.getVartype();
        if (type != null)
            xml.append(comment.getVartype());
        xml.append("' isStatic='");
        xml.append(comment.isStatic());
        xml.append("' isConst='");
        xml.append(comment.isConst());
        xml.append("' ");
        String defaultValue = comment.getDefaultValue();
        if (defaultValue != null)
        {
            xml.append("defaultValue='");
            xml.append(defaultValue);
            xml.append("' ");
        }
        xml.append(">");
        
        String desc = comment.getDescription();
        if (desc != null)
            appendTag("description", comment.getDescription());
        emitTags(comment.getAllTags());
        
        if (comment.getMetadata() != null)
            emitMetadata(comment.getMetadata());
        xml.append("\n</field>");
    }
    
    /**
     * Appends a specific comment to the StringBuffer.
     * @param comment
     */
    private void emitDocComment(DocComment comment)
    {
        if (!comment.isExcluded())
        {
            int type = comment.getType();
            if (type == DocComment.PACKAGE)
                emitPackage(comment);
            else if (type == DocComment.CLASS || type == DocComment.INTERFACE)
                emitClass(comment);
            else if (type >= DocComment.FUNCTION && type <= DocComment.FUNCTION_SET)
                emitFunction(comment);
            else if (type == DocComment.FIELD)
                emitField(comment);
        }
    }

}
