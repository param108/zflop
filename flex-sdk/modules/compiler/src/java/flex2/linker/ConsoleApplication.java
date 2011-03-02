////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.linker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import flex2.compiler.CompilationUnit;
import flex2.compiler.DependencyGraph;
import flex2.compiler.Source;
import flex2.compiler.util.Algorithms;
import flex2.compiler.util.QName;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.Vertex;
import flex2.compiler.util.Visitor;

/**
 * @author Clement Wong
 */
public class ConsoleApplication
{
	public ConsoleApplication(Configuration configuration)
	{		
		abcList = new ArrayList();
		enableDebugger = configuration.generateDebugTags();
		exportedUnits = new LinkedList();
	}
	
	private List abcList;
	private byte[] main;
	public final boolean enableDebugger;
	private List exportedUnits;
	
	public List getABCs()
	{
		return abcList;
	}
	
	public void generate(List units) throws LinkerException // List<CompilationUnit>
	{		
		// create a dependency graph based on source file dependencies...
        final DependencyGraph dependencies = extractCompilationUnitInfo(units);
        exportDependencies(dependencies);

        if (ThreadLocalToolkit.errorCount() > 0)
        {
  			throw new LinkerException.LinkingFailed();
        }
	}

    private DependencyGraph extractCompilationUnitInfo(List units)
    {
        final DependencyGraph dependencies = new DependencyGraph(); // DependencyGraph<CompilationUnit>
		final Map qnames = new HashMap(); // QName, VirtualFile.getName()

        for (int i = 0, length = units.size(); i < length; i++)
        {
            CompilationUnit u = (CompilationUnit) units.get(i);
            Source s = u.getSource();
            String path = s.getName();

            dependencies.put(path, u);
			if (!dependencies.containsVertex(s.getName()))
			{
				dependencies.addVertex(new Vertex(path));
			}
				
			// register QName --> VirtualFile.getName()
			for (Iterator j = u.topLevelDefinitions.iterator(); j.hasNext();)
			{
				qnames.put(j.next(), s.getName());
			}
        }

		// setup inheritance-based dependencies...
		for (int i = 0, size = units.size(); i < size; i++)
		{
            CompilationUnit u = (CompilationUnit) units.get(i);
            Source s = u.getSource();
            String head = s.getName();

			for (Iterator k = u.inheritance.iterator(); k.hasNext();)
			{
				Object obj = k.next();
				if (obj instanceof QName)
				{
					QName qname = (QName) obj;
					String tail = (String) qnames.get(qname);

					if (tail != null && !head.equals(tail) && !dependencies.dependencyExists(head, tail))
					{
						dependencies.addDependency(head, tail);
					}
				}
			}
		}

        return dependencies;
    }

	private void exportDependencies(final DependencyGraph dependencies)
	{
		// export compilation units
		Algorithms.topologicalSort(dependencies, new Visitor() // Visitor<Vertex<String>>
		{
			public void visit(Object v) // Vertex<String> v
			{
				String fileName = (String) ((Vertex) v).getWeight(); // fileName = v.getWeight();
				CompilationUnit u = (CompilationUnit) dependencies.get(fileName);
				if (!u.getSource().isInternal())
				{
					if (u.isRoot())
					{
						main = u.getByteCodes();
					}
					else
					{
						abcList.add(u.getByteCodes());
					}
					exportedUnits.add(u);
				}
			}
		});
		
		abcList.add(main);
	}
	
    public List getExportedUnits()
    {
        return exportedUnits;
    }
}
