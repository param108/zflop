package flex.tools.dependencychecker;
import java.util.HashSet;
import java.util.Set;

import flex.tools.dependencychecker.nodes.DepNode;


public class FrameworkSwcDependencyRules implements DependencyRules {

	public boolean allowedDependency(String parent, String dependency,
			String type) {
		
		if (DepNode.isNamespaceDependency(type) || DepNode.isInheritsDependency(type))
			return true;
		
		// otherwise expression or signature dependency
		
		String parentCat = getCategory(parent);
		String childCat = getCategory(dependency);
		
		if (parentCat.equals("flash"))
		{
			// only happens for "FrameworkClasses" and classes 
			// generated for binding from RichTextEditor.
			return true;
		}
		
		if (parentCat.equals("skin"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("skin"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("manager"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("data"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("UITextField"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("component"))
				return true;
			
			if (childCat.equals("core"))
				return true;
			
			if (childCat.equals("UIComponent"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("UIComponent"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("core"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("component"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("component"))
				return true;
			
			if (childCat.equals("core"))
				return true;
			
			if (childCat.equals("UIComponent"))
				return true;
			
			return false;
		}
		
		if (parentCat.equals("core"))
		{
			if (childCat.equals("flash"))
				return true;
			
			if (childCat.equals("manager"))
				return true;
			
			if (childCat.equals("data"))
				return true;
			
			if (childCat.equals("component"))
				return true;
			
			if (childCat.equals("core"))
				return true;
			
			if (childCat.equals("UIComponent"))
				return true;
			
			return false;
		}
		
		return false;
	}

	
	public static boolean isInterface(String id)
	{
		String className = id.indexOf(":") == -1 ? id : id.substring(id.indexOf(":"));
		
		if (className.length() >= 3)
		{
			// first one is actually ":"
			char firstChar = className.charAt(1);
			char secondChar = className.charAt(2);
			
			return firstChar == 'I' && Character.isUpperCase(secondChar);
		}
		
		return false;
	}
	
	private static String getCategory(String name)
	{
		if (!name.startsWith("mx."))
		{
			return "flash";
		}
		else if (name.startsWith("mx.skins"))
		{
			return "skin";
		}
		else if (name.startsWith("mx.managers") || 
				name.equals("mx.styles:StyleManager") || 
				name.equals("mx.styles:StyleManagerImpl") ||
				name.equals("mx.effects:EffectManager") || 
				name.equals("mx.resources:ResourceManager") ||
				name.equals("mx.resources:ResourceManagerImpl") ||
				name.equals("mx.resources:ResourceBundle") ||
				name.equals("mx.modules:ModuleManager") || 
				name.equals("mx.binding:BindingManager"))
		{
			return "manager";
		}
		else if (isInterface(name) || name.startsWith("mx.utils") || 
				name.startsWith("mx.collections") || 
				dataObjects.contains(name))
		{
			return "data";
		}
		else if (name.equals("mx.core:UITextField"))
		{
			return "UITextField";
		}
		else if (name.equals("mx.core:UIComponent"))
		{
			return "UIComponent";
		}
		else if (name.startsWith("mx.core") && 
				!name.equals("mx.core:Applicaton") &&
				!name.equals("mx.core:Container") && 
				!name.equals("mx.core:LayoutContainer") && 
				!name.equals("mx.core:Repeater") && 
				!name.equals("mx.core:ScrollControlBase") && 
				!name.equals("mx.core:UITextField"))
		{
			return "core";
		}
		else
		{
			return "component";
		}
	}

	private static Set/*<String>*/ dataObjects = new HashSet/*<String>*/();
	
	static {
		dataObjects.add("mx.binding:BindabilityInfo");
		dataObjects.add("mx.binding:Binding");
		dataObjects.add("mx.binding:EvalBindingResponder");
		
		dataObjects.add("mx.containers:BoxDirection");
		
		dataObjects.add("mx.controls.listClasses:BaseListData");
		
		dataObjects.add("mx.core:ApplicationGlobals");
		dataObjects.add("mx.core:DragSource");
		dataObjects.add("mx.core:EdgeMetrics");
		dataObjects.add("mx.core:EmbeddedFont");
		dataObjects.add("mx.core:EmbeddedFontRegistry");
		dataObjects.add("mx.core:EventPriority");
		dataObjects.add("mx.core:FlexVersion");
		dataObjects.add("mx.core:mx_internal");
		dataObjects.add("mx.core:ResourceModuleRSLItem");
		dataObjects.add("mx.core:RSLItem");
		dataObjects.add("mx.core:SWFBridgeGroup");
		dataObjects.add("mx.core:Singleton");
		dataObjects.add("mx.core:UIComponentCachePolicy");
		dataObjects.add("mx.core:UIComponentDescriptor");
		dataObjects.add("mx.core:UIComponentGlobals");
		dataObjects.add("mx.core:UITextFormat");
		
		dataObjects.add("mx.effects.effectClasses:PropertyChanges");
		dataObjects.add("mx.effects:EffectTargetFilter");
		
		dataObjects.add("mx.events:BrowserChangeEvent");
		dataObjects.add("mx.events:ChildExistenceChangedEvent");
		dataObjects.add("mx.events:CollectionEvent");
		dataObjects.add("mx.events:CollectionEventKind");
		dataObjects.add("mx.events:DragEvent");
		dataObjects.add("mx.events:DynamicEvent");
		dataObjects.add("mx.events:EffectEvent");
		dataObjects.add("mx.events:FlexEvent");
		dataObjects.add("mx.events:FlexChangeEvent");
		dataObjects.add("mx.events:FlexMouseEvent");
		dataObjects.add("mx.events:InvalidateRequestData");
		dataObjects.add("mx.events:ModuleEvent");
		dataObjects.add("mx.events:MoveEvent");
		dataObjects.add("mx.events:PropertyChangeEvent");
		dataObjects.add("mx.events:PropertyChangeEventKind");
		dataObjects.add("mx.events:ResizeEvent");
		dataObjects.add("mx.events:ResourceEvent");
		dataObjects.add("mx.events:RSLEvent");
		dataObjects.add("mx.events:StateChangeEvent");
		dataObjects.add("mx.events:StyleEvent");
		dataObjects.add("mx.events:ToolTipEvent");
		dataObjects.add("mx.events:ValidationResultEvent");
		dataObjects.add("mx.events:EventListenerRequest");
		dataObjects.add("mx.events:FocusRequestDirection");
		dataObjects.add("mx.events:SWFBridgeEvent");
		dataObjects.add("mx.events:SWFBridgeRequest");
		dataObjects.add("mx.events:InterManagerRequest");
		dataObjects.add("mx.events:InterDragManagerEvent");
		dataObjects.add("mx.events:SandboxMouseEvent");
		
		dataObjects.add("mx.graphics:RectangularDropShadow");
		dataObjects.add("mx.graphics:RoundedRectangle");
		
		dataObjects.add("mx.messaging.config:LoaderConfig");
		
		dataObjects.add("mx.modules:ModuleManagerGlobals");
		
		dataObjects.add("mx.preloaders:DownloadProgressBar");
		dataObjects.add("mx.preloaders:Preloader");

		dataObjects.add("mx.resources:LocaleSorter");

		dataObjects.add("mx.states:State");
		dataObjects.add("mx.states:Transition");
		
		dataObjects.add("mx.styles:CSSStyleDeclaration");
		
		dataObjects.add("mx.validators:ValidationResult");
	}
}
