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

/**
 * @author Clement Wong
 */
public final class Edge // Edge<W>
{
	public Edge(Vertex tail, Vertex head, Object weight) // W weight
	{
		this.head = head;
		this.tail = tail;
		this.weight = weight;

		tail.addEmanatingEdge(this);
		tail.addSuccessor(head);
		head.addIncidentEdge(this);
		head.addPredecessor(tail);
	}

	private Vertex head, tail;
	private Object weight; // W weight;

	public Vertex getHead()
	{
		return head;
	}

	public Vertex getTail()
	{
		return tail;
	}

	public Object getWeight() // W getWeight()
	{
		return weight;
	}

	public boolean equals(Object object)
	{
		if (object instanceof Edge)
		{
			Edge e = (Edge) object;
			return e.head == head && e.tail == tail && e.weight == weight;
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
