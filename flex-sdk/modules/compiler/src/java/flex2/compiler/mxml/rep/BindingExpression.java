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

package flex2.compiler.mxml.rep;

import flex2.compiler.as3.binding.Watcher;
import flex2.compiler.mxml.reflect.Property;
import flex2.compiler.mxml.reflect.Style;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.IntegerPool;
import flex2.compiler.SymbolTable;

import java.util.*;

/**
 * A BindingExpression is used to store binding expressions (surprise!) when we come
 * across them while parsing MXML.  As we go, we fill in the destination of each
 * BindingExpression, and when we're done parsing we compile the source expression
 * in order to figure out how to attach ActionScript watchers and binding objects.
 *
 * @author gdaniels
 * @author mchotin
 * @author preilly
 */
public class BindingExpression
{
    /** The source expression for this binding */
    private String sourceExpression;
    /** The destination Model of this binding */
    private Model destination;
    /** The destination property within the Model (numeric for Arrays) */
    private String destinationProperty;
    /** The destination style */
    private String destinationStyle;
    /** If destinationProperty is an array index, this is true.  Controlled by
     * calling setDestinationProperty(int).
     */
    private boolean arrayAccess = false;
    /**
     * The lvalue is the expression that can be used for the left side of the destination expression.
     * For destinationLiteral = false, destinationLValue == destinationProperty,
     * for destinationLiteral = true, destinationLValue is XML AS code while destinationProperty is dotted expression
     */
    private String destinationLValue;
    /**
     * The id of the binding expression, used for variable name. 
     */
    private int id;
    /** Is this an XML attribute? */
    private boolean isDestinationXMLAttribute;
    /** Is this an XML node value? */
    private boolean isDestinationXMLNode;
    /** Is this XMLnode an E4X assignment? */
    private boolean isDestinationE4X;
    /** Is the destination a Model? */
    private boolean isDestinationObjectProxy;
    /** The line number where this binding expression was set up*/
    public int xmlLineNumber;

    private Watcher uiComponentWatcher;
    private boolean multipleUIComponentWatchers;

    private MxmlDocument mxmlDocument;

    private BindingExpression twoWayCounterpart;

	// namespace-aware e4x expressions need namespaces
	private Map namespaces;

	public BindingExpression(String bindingExpression, int xmlLineNumber, MxmlDocument mxmlDocument)
    {
        this.sourceExpression = bindingExpression;
		this.xmlLineNumber = xmlLineNumber;

		assert mxmlDocument != null;
		setMxmlDocument(mxmlDocument);

		multipleUIComponentWatchers = false;
    }

	public void setMxmlDocument(MxmlDocument mxmlDocument)
	{
		this.mxmlDocument = mxmlDocument;
		mxmlDocument.addBindingExpression(this);
	}

    public boolean isDestinationXMLAttribute()
    {
        return isDestinationXMLAttribute;
    }

    public boolean isDestinationXMLNode()
    {
        return isDestinationXMLNode;
    }

    public boolean isDestinationE4X()
    {
        return isDestinationE4X;
    }

    public boolean isDestinationObjectProxy()
    {
        return isDestinationObjectProxy;
    }

    /**
     * Sometimes the destination is not a member of the document, so
     * we have to climb the parent tree to find a parent that is.  Here is an example:
     *
     *   <mx:AreaChart>
     *    <mx:horizontalAxis>
     *      <mx:CategoryAxis dataProvider="{expenses}"/>
     *    </mx:horizontalAxis>
     *   </mx:AreaChart>
     *
     * For the above example the destination stack would be
     * ["_AreaChart1", "horizontalAxis"].
     */
    private Stack generateDestinationStack()
    {
        Stack destinationStack = new Stack();
        Model model = destination;

        while (model != null)
        {
            destinationStack.push(model);

            if ((model.getId() == null) || model.getIsAnonymous())
            {
                model = model.getParent();
            }
            else
            {
                break;
            }
        }

        return destinationStack;
    }

    public String getDestinationPath(boolean doXML)
    {
        StringBuffer buffer = new StringBuffer();

        if (!(doXML && (isDestinationXMLAttribute || isDestinationXMLNode)))
        {
            buffer.append( getDestinationPathRoot(false) );
        }

        if ((destination != null) &&
            (destinationProperty != null || destinationStyle != null) &&
            !isArrayAccess() &&
            !(doXML && (isDestinationXMLAttribute || isDestinationXMLNode)))
        {
            buffer.append(".");
        }

        if (isArrayAccess())
        {
            buffer.append("[");
        }

        if ((doXML || (!(isDestinationXMLAttribute || isDestinationXMLNode))) && (destinationLValue != null))
        {
            buffer.append(destinationLValue);
        }
        else if (destinationProperty != null)
        {
            buffer.append(destinationProperty);
        }
        else if (destinationStyle != null)
        {
            buffer.append(destinationStyle);
        }

        if (doXML && isDestinationXMLNode && !isDestinationE4X)
        {
            buffer.append(".nodeValue");
        }
        else if (isArrayAccess())
        {
            buffer.append("]");
        }

        return buffer.toString();
    }

    public String getDestinationPathRoot(boolean doRepeatable)
    {
        if (destination == null)
        {
            return "";
        }

        StringBuffer destinationRoot = new StringBuffer();

        Stack destinationStack = generateDestinationStack();

        //ensure that the highest-level model is declared
        Model model = (Model) destinationStack.peek();

        if (!((model instanceof XML) ||
              (model instanceof AnonymousObjectGraph) ||
              model.equals(mxmlDocument.getRoot())) &&
            (model.getId() != null))
        {
            // This object needs to have an id at runtime, so instruct
            // SWCBuilder to emit one.
            mxmlDocument.ensureDeclaration(model);
        }

        boolean writeRepeaterIndices = doRepeatable;

        while (!destinationStack.isEmpty())
        {
            model = (Model) destinationStack.pop();

            if (model.equals(mxmlDocument.getRoot()))
            {
                destinationRoot.append("this");
            }
            else
            {
                String parentIndex = model.getParentIndex();

                if ((parentIndex != null) && (destinationRoot.length() > 0))
                {
                    destinationRoot.append("[");
                    destinationRoot.append(parentIndex);
                    destinationRoot.append("]");
                }
                else
                {
                    String id = model.getId();

                    if (id != null)
                    {
                        if (!model.getIsAnonymous())
                        {
                            mxmlDocument.ensureDeclaration(model);
                        }
                        destinationRoot.append(id);
                    }
                }
            }

            if (writeRepeaterIndices && isRepeatable())
            {
                for (int i = 0; i < model.getRepeaterLevel(); ++i)
                {
                    destinationRoot.append("[instanceIndices[");
                    destinationRoot.append(i);
                    destinationRoot.append("]]");
                }
                writeRepeaterIndices = false;
            }

            if (!destinationStack.isEmpty())
            {
                Model child = (Model) destinationStack.peek();
                
                if (child.getParentIndex() == null)
                {
                    destinationRoot.append(".");
                }
            }
        }

        return destinationRoot.toString();
    }

    public String getDestinationTypeName()
    {
        String result = SymbolTable.NOTYPE;

        if ((destination != null) &&
            !(destination instanceof AnonymousObjectGraph) &&
            !(destination instanceof XML))
        {
            if (destinationProperty != null)
            {
                Type type = destination.getType();
                if (!type.getName().equals(StandardDefs.CLASS_OBJECTPROXY))
                {
                    Property property = type.getProperty(destinationProperty);
                    if (property != null)
                    {
                        result = property.getType().getName();
                        result = result.replace(':', '.');
                    }
                }
            }
            else if (destinationStyle != null)
            {
                Type type = destination.getType();
                Style style = type.getStyle(destinationStyle);
                if (style != null)
                {
                    result = style.getType().getName();
                    result = result.replace(':', '.');
                }
            }
            else
            {
                result = destination.getType().getName();
            }
        }

        return result;
    }

    public int getId()
    {
        return id;
    }

    public String getRepeatableSourceExpression()
    {
        String repeatableSourceExpression = sourceExpression;
        List repeaterParents = destination.getRepeaterParents();
        Iterator iterator = repeaterParents.iterator();

        while ( iterator.hasNext() )
        {
            Model repeater = (Model) iterator.next();
            int repeaterLevel = repeater.getRepeaterLevel();
            StringBuffer buffer = new StringBuffer();
            int i;

            for (i = 0; i < repeaterLevel; i++)
            {
                buffer.append("[instanceIndices[");
                buffer.append(i);
                buffer.append("]]");
            }

            buffer.append(".mx_internal::getItemAt(repeaterIndices[");
            buffer.append(i);
            buffer.append("])");

            repeatableSourceExpression = repeatableSourceExpression.replaceAll(repeater.getId() + "\\.currentItem",
                                                                               repeater.getId() + buffer.toString());

            repeatableSourceExpression = repeatableSourceExpression.replaceAll(repeater.getId() + "\\.currentIndex",
                                                                               "repeaterIndices[" + i + "]");
        }

        return repeatableSourceExpression;
    }

    public String getSourceExpression()
    {
        return sourceExpression;
    }

    public void setDestinationProperty(String destinationProperty)
    {
        this.destinationProperty = destinationProperty;
    }

    public void setDestinationProperty(int destinationProperty)
    {
        this.destinationProperty = Integer.toString(destinationProperty);
        arrayAccess = true;
    }

    public void setDestinationStyle(String destinationStyle)
    {
        this.destinationStyle = destinationStyle;
    }

    public String getDestinationStyle()
    {
        return destinationStyle;
    }

    public boolean isStyle()
    {
        return destinationStyle != null;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setDestinationXMLAttribute(boolean isDestinationXMLAttribute)
    {
        this.isDestinationXMLAttribute = isDestinationXMLAttribute;
    }

    public void setDestinationXMLNode(boolean isDestinationXMLNode)
    {
        this.isDestinationXMLNode = isDestinationXMLNode;
    }

    public void setDestinationE4X(boolean isDestinationE4X)
    {
        this.isDestinationE4X = isDestinationE4X;
    }

    public void setDestinationObjectProxy(boolean isDestinationObjectProxy)
    {
        this.isDestinationObjectProxy = isDestinationObjectProxy;
    }

    public String getDestinationProperty()
    {
        return destinationProperty;
    }

    public boolean isArrayAccess()
    {
        return arrayAccess;
    }

    public String getDestinationLValue()
    {
        return destinationLValue;
    }

    public void setDestinationLValue(String lvalue)
    {
        destinationLValue = lvalue;
    }

    public Model getDestination()
    {
        return destination;
    }

    public void setDestination(Model destination)
    {
        this.destination = destination;
        if (this.xmlLineNumber == 0)
        {
	        // C: The destination xml line number may not be as accurate as the binding expression's original number...
	        this.xmlLineNumber = destination.getXmlLineNumber();
        }
    }

    public boolean isRepeatable()
    {
        return ((destination != null) && (destination.getRepeaterLevel() > 0));
    }

    public int getRepeaterLevel(String var)
    {
        if (var.indexOf("[repeaterIndices") > -1)
        {
            var = var.substring(0, var.indexOf("["));
        }
        List repeaters = destination.getRepeaterParents();
        int repeaterLevel = repeaters.size() - 1;
        for (; repeaterLevel >= 0; --repeaterLevel)
        {
            Model r = (Model) repeaters.get(repeaterLevel);
            if (var.equals(r.getId()))
            {
                break;
            }
        }
        return repeaterLevel;
    }

    public BindingExpression getTwoWayCounterpart()
    {
        return twoWayCounterpart;
    }

    public void setTwoWayCounterpart(BindingExpression twoWayCounterpart)
    {
        this.twoWayCounterpart = twoWayCounterpart;
    }

	/**
	 *
	 */
	public int getXmlLineNumber()
	{
		return xmlLineNumber;
	}

	public void addNamespace(String nsUri, int i)
	{
		if (namespaces == null)
		{
			namespaces = new HashMap();
		}
		namespaces.put(nsUri, IntegerPool.getNumber(i));
	}

	public String getNamespaceDeclarations()
	{
		if (namespaces != null)
		{
			StringBuffer b = new StringBuffer();
			for (Iterator i = namespaces.keySet().iterator(); i.hasNext();)
			{
				String uri = (String) i.next();
				int k = ((Integer) namespaces.get(uri)).intValue();
				b.append("var ns").append(k).append(":Namespace = new Namespace(\"").append(uri).append("\");");
			}
			return b.toString();
		}
		else
		{
			return "";
		}
	}
}
