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

package flex2.compiler.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Clement Wong
 */
public class Graph // Graph <V,E>
{
	public Graph()
	{
		vertices = new HashSet(300); // new HashSet<Vertex<V>>();
		edges = new HashSet(300); // new HashSet<Edge<E>>();
	}

	private int counter;
	private Vertex root; // Vertex<V> root;
	private Set vertices; // Set<Vertex<V>> vertices;
	private Set edges; // Set<Edge<E>> edges;


	public Vertex getRoot()
	{
		return root;
	}

	public Set getVertices() // Set<Vertex<V>>
	{
		return vertices;
	}

	public Set getEdges() // Set<Edge<E>>
	{
		return edges;
	}

	public void clear()
	{
		counter = 0;
		root = null;
		vertices.clear();
		edges.clear();
	}

	public void addVertex(Vertex v) // Vertex<V>
	{
		if (vertices.size() == 0)
		{
			root = v;
		}
		v.id = counter++;
		vertices.add(v);
	}
	
	public void removeVertex(Vertex v)
	{
		vertices.remove(v);
		if (v == root)
		{
			Iterator i = vertices.iterator();
			root = i.hasNext() ? (Vertex) i.next() : null;
		}

		Set s = v.getEmanatingEdges();
		if (s != null)
		{
			for (Iterator i = s.iterator(); i.hasNext(); )
			{
				Edge e = (Edge) i.next();
				Vertex h = e.getHead();
				h.removeIncidentEdge(e);
				h.removePredecessor(v);
				
				edges.remove(e);
			}
		}
		
		s = v.getIncidentEdges();
		if (s != null)
		{
			for (Iterator i = s.iterator(); i.hasNext(); )
			{
				Edge e = (Edge) i.next();
				Vertex t = e.getTail();
				t.removeEmanatingEdge(e);
				t.removeSuccessor(v);
				
				edges.remove(e);
			}
		}
		
		normalize();
	}

	public void addEdge(Edge e) // Edge<E>
	{
		edges.add(e);
	}

	public void normalize()
	{
		counter = 0;
		for (Iterator i = vertices.iterator(); i.hasNext();) // Iterator<Vertex<V>>
		{
			((Vertex) i.next()).id = counter++;
		}
	}
}
