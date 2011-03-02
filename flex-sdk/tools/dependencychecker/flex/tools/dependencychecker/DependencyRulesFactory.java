package flex.tools.dependencychecker;
public class DependencyRulesFactory {
	public DependencyRules createDependencyRules(String filename) {
		
		if (filename.toLowerCase().endsWith("framework.swc"))
		{	
			return new FrameworkSwcDependencyRules();
		}
		else if (filename.toLowerCase().endsWith("datavisualization.swc"))
		{
			return new FrameworkSwcDependencyRules();
		}
		else
		{
			throw new RuntimeException("Cannot find dependency rules for " + filename);
		}
	}
}
