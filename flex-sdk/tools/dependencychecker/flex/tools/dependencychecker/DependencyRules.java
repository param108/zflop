package flex.tools.dependencychecker;

public interface DependencyRules {
	
	/**
	 * Checks to see if this Dependency is an allowed dependency.
	 * @param parent the class bringing in the dependency
	 * @param dependency the classes that is depended upon
	 * @param type the type of dependency (s, e)
	 * @return true if dependency is allowed, false otherwise
	 */
	public boolean allowedDependency(String parent, String dependency, String type);
}
