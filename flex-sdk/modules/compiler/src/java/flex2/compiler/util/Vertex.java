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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Clement Wong
 */
public final class Vertex // Vertex<W>
{
	private static final int INITIAL_CAPACITY = 5;

	public Vertex(Object weight) // W weight
	{
		this.weight = weight;
	}

	private Object weight; // W weight;

	int id;
	private Set incidentEdges; // Set<Edge> - pointing to this vertex
	private Set emanatingEdges; // Set<Edge> - pointing out of this vertex
	private Set predecessors; // Set<Vertex<W>> - tails of the incident edges
	private List successors; // List<Vertex<W>> - heads of the emanating edges

	public Object getWeight() // W getWeight()
	{
		return weight;
	}

	public void addIncidentEdge(Edge e)
	{
		if (incidentEdges == null)
		{
			incidentEdges = new HashSet(INITIAL_CAPACITY); // HashSet<Edge>
		}
		incidentEdges.add(e);
	}
	
	public void removeIncidentEdge(Edge e)
	{
		if (incidentEdges != null)
		{
			incidentEdges.remove(e);
		}
	}

	public Set getIncidentEdges() // Set<Edge>
	{
		return incidentEdges;
	}

	public void addEmanatingEdge(Edge e)
	{
		if (emanatingEdges == null)
		{
			emanatingEdges = new HashSet(INITIAL_CAPACITY); // HashSet<Edge>
		}
		emanatingEdges.add(e);
	}

	public void removeEmanatingEdge(Edge e)
	{
		if (emanatingEdges != null)
		{
			emanatingEdges.remove(e);
		}
	}

	public Set getEmanatingEdges()
	{
		return emanatingEdges;
	}

	public void addPredecessor(Vertex v)
	{
		if (predecessors == null)
		{
			predecessors = new HashSet(INITIAL_CAPACITY); // HashSet<Vertex<W>>
		}
		predecessors.add(v);
	}
	
	public void removePredecessor(Vertex v)
	{
		if (predecessors != null)
		{
			predecessors.remove(v);
		}
	}

	public Set getPredecessors() // Set<Vertex<W>>
	{
		return predecessors;
	}

	public void addSuccessor(Vertex v)
	{
		if (successors == null)
		{
			successors = new ArrayList(INITIAL_CAPACITY);
		}
		successors.add(v);
	}

	public void removeSuccessor(Vertex v)
	{
		if (successors != null)
		{
			successors.remove(v);
		}
	}

	public List getSuccessors() // List<Vertex<W>>
	{
		return successors;
	}

	public int inDegrees()
	{
		return incidentEdges == null ? 0 : incidentEdges.size();
	}

	public int outDegrees()
	{
		return emanatingEdges == null ? 0 : emanatingEdges.size();
	}

	public boolean equals(Object object)
	{
		if (object instanceof Vertex)
		{
			return (weight == null) ? super.equals(object) : weight.equals(((Vertex) object).weight);
		}
		else
		{
			return false;
		}
	}

	public int hashCode()
	{
		return (weight != null) ? weight.hashCode() : super.hashCode();
	}
}
