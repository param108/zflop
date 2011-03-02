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
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Clement Wong
 */
public final class Algorithms
{
	public static boolean isCyclic(Graph g)
	{
		ConnectednessCounter counter = new ConnectednessCounter();
		topologicalSort(g, counter);
		return counter.count != g.getVertices().size();
	}

	public static Set detectCycles(Graph g) // Set<Vertex>
	{
		ConnectednessCounter counter = new ConnectednessCounter(g.getVertices());
		topologicalSort(g, counter);
		return counter.remained;
	}

	public static void topologicalSort(Graph g, Visitor visitor)
	{
		int[] inDegree = new int[g.getVertices().size()];
		Vertex[] vertices = new Vertex[inDegree.length];

		for (Iterator i = g.getVertices().iterator(); i.hasNext();) // Iterator<Vertex>
		{
			// Vertex v = i.next();
			Vertex v = (Vertex) i.next();
			vertices[v.id] = v;
			inDegree[v.id] = v.inDegrees();
		}

		LinkedList queue = new LinkedList(); // LinkedList<Vertex>
		for (int i = 0, length = vertices.length; i < length; i++)
		{
			// in case of seeing multiple degree-zero candidates, we could
			// use the vertices different weights...
			if (inDegree[i] == 0)
			{
				queue.add(vertices[i]);
			}
		}

		while (!queue.isEmpty())
		{
			// Vertex v = queue.removeFirst();
			Vertex v = (Vertex) queue.removeFirst();
			if (visitor != null)
			{
				visitor.visit(v);
			}
			if (v.getSuccessors() != null)
			{
				for (Iterator i = v.getSuccessors().iterator(); i.hasNext();) // Iterator<Vertex>
				{
					// Vertex head = i.next();
					Vertex head = (Vertex) i.next();
					inDegree[head.id] -= 1;
					if (inDegree[head.id] == 0)
					{
						queue.add(head);
					}
				}
			}
		}
	}

	private static class ConnectednessCounter implements Visitor // Visitor<Vertex>
	{
		private ConnectednessCounter()
		{
			count = 0;
		}

		private ConnectednessCounter(Set vertices) // Set<Vertex>
		{
			this.remained = new HashSet(vertices); // new HashSet<Vertex>(vertices);
		}

		private int count;
		private Set remained; // Set<Vertex> remained;

		public void visit(Object v) // public void visit(Vertex v)
		{
			count++;
			remained.remove(v);
		}
	}
}
