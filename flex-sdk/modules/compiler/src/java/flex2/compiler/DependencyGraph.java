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

import flex2.compiler.util.Edge;
import flex2.compiler.util.Graph;
import flex2.compiler.util.Vertex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Clement Wong
 */
public class DependencyGraph extends Graph // DependencyGraph<T>, Graph<String, Object>
{
	public DependencyGraph()
	{
		map = new HashMap(300); // HashMap<String, T>
		vertices = new HashMap(300); // HashMap<String, Vertex<String>>
	}

	private Map map; // Map<String, T>
	private Map vertices; // Map<String, Vertex<String>>

	// put(), get(), remove() are methods for 'map'

	public void put(String key, Object value) // T value
	{
		map.put(key, value);
	}

	public Object get(String key) // T
	{
		return map.get(key);
	}

	public void remove(String key)
	{
		map.remove(key);
	}

	public Set keySet()
	{
		return map.keySet();
	}

	public int size()
	{
		return map.size();
	}

	public boolean containsKey(String key)
	{
		return map.containsKey(key);
	}

	public boolean containsVertex(String key)
	{
		return vertices.containsKey(key);
	}

	public void clear()
	{
		super.clear();
		map.clear();
		vertices.clear();
	}

	// methods for graph manipulations

	public void addVertex(Vertex v) // Vertex<String>
	{
		super.addVertex(v);
		vertices.put(v.getWeight(), v);
	}

	public Vertex getVertex(Object weight)
	{
		return (Vertex) vertices.get(weight);
	}
	
	public void removeVertex(String weight)
	{
		Vertex v = (Vertex) vertices.remove(weight);
		if (v != null)
		{
			super.removeVertex(v);
		}
	}

	public void addDependency(String name, String dep)
	{
		Vertex tail = null, head = null; // Vertex<String>

		// if ((head = vertices.get(name)) == null)
		if ((head = (Vertex) vertices.get(name)) == null)
		{
			head = new Vertex(name); // Vertex<String>
			addVertex(head);
		}

		// if ((tail = vertices.get(dep)) == null)
		if ((tail = (Vertex) vertices.get(dep)) == null)
		{
			tail = new Vertex(dep); // Vertex<String>
			addVertex(tail);
		}

		addEdge(new Edge(tail, head, null)); // Edge<Object>
	}

	public boolean dependencyExists(String name, String dep)
	{
		Vertex tail = null, head = null; // Vertex<String>

		// if ((head = vertices.get(name)) == null)
		if ((head = (Vertex) vertices.get(name)) == null)
		{
			return false;
		}

		// if ((tail = vertices.get(dep)) == null)
		if ((tail = (Vertex) vertices.get(dep)) == null)
		{
			return false;
		}

		Set predecessors = head.getPredecessors();

		if (predecessors != null)
		{
			return predecessors.contains(tail);
		}
		else
		{
			return false;
		}
	}
}

