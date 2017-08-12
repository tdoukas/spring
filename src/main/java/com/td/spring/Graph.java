package com.td.spring;
/*****************************************************************************
 * Graph.java
 *
 * Graph: enthaelt Knoten (Vertex) und Kanten (Edge).
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Graph.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

abstract class Graph implements ControlProvider {

	Vector<Vertex> v = new Vector<Vertex>(); // Knoten des Graphen
	Vector<Edge> e = new Vector<Edge>(); // Kanten

	// Mapping Vertex.id -> Vertex
	private Hashtable<Integer, Vertex> vertexHt = new Hashtable<Integer, Vertex>();

	// Fuer edgesFor(Vertex)
	private Hashtable<Vertex, Vector<Edge>> edgeHt = new Hashtable<Vertex, Vector<Edge>>();

	private int lastVertexNr = -1; // Knoten bekommen automatisch eine Nummer

	private Random random = new Random();

	public String toString() {
		return getName();
	}

	/*************************************************************************
	 * Von Unterklasse zu implementieren:
	 *************************************************************************/

	/* Gebe Namen des Graph-Generators */

	abstract String getName();

	/* Erzeuge den Graph */

	abstract void generate();

	/*************************************************************************
	 ** Methoden zur Konstruktion des Graphen
	 *************************************************************************/

	/* Loesche alle Knoten und Kanten */

	protected void clear() {
		v.clear();
		e.clear();
		vertexHt.clear();
		edgeHt.clear();
		lastVertexNr = -1;
	}

	/* Fuege Knoten v hinzu */

	protected void add(Vertex v) {
		v.nr = ++lastVertexNr;
		if (v.id == -1)
			v.id = v.nr;
		this.v.add(v);
		vertexHt.put(v.id, v);
	}

	/* Fuege Kante e hinzu */

	protected void add(Edge e) {
		this.e.add(e);
	}

	/* Fuege Kante e hinzu, falls noch nicht Teil des Graphen */

	protected void addNew(Edge e) {
		if (!hasEdge(e.v1, e.v2))
			add(e);
	}

	/* Finde Knoten anhand seiner id */

	protected Vertex findVertex(int id) {
		return vertexHt.get(id);
	}

	/* Gibt es zwischen Knoten u, v schon eine Kante? */

	protected boolean hasEdge(Vertex u, Vertex v) {
		for (Edge e : this.e) {
			if ((e.v1 == u && e.v2 == v) || (e.v1 == v && e.v2 == u))
				return true;
		}
		return false;
	}

	/* Finde alle Kanten, die v enthalten. */

	protected Vector<Edge> edgesFor(Vertex v) {
		Vector<Edge> vec = edgeHt.get(v);
		if (vec != null)
			return vec;
		vec = new Vector<Edge>();
		edgeHt.put(v, vec);
		for (Edge e : this.e)
			if (e.containsVertex(v))
				vec.add(e);
		return vec;
	}

	/*************************************************************************
	 ** Verschiedene Zugriffsmethoden
	 *************************************************************************/

	/* Gebe einen zufaelligen Knoten zurueck. */

	public Vertex getRandomVertex() {
		return v.elementAt(random.nextInt(v.size()));
	}

	/* Bringe alle Knoten an zufaellige Ausgangspositionen in [0,1]x[0,1]. */

	public void resetVertices() {
		for (Vertex v : this.v) {
			v.x = Math.random();
			v.y = Math.random();
			v.dx = v.dy = v.vx = v.vy = 0;
		}
	}

	/* Gebe minimale und maximale Koordinaten des Graphen. */

	public double[] getBounds() {

		double xmin = 0, xmax = 0, ymin = 0, ymax = 0;
		boolean firstIter = true;
		for (Vertex v : this.v) {
			if (firstIter) {
				xmin = v.x;
				xmax = v.x;
				ymin = v.y;
				ymax = v.y;
				firstIter = false;
			} else {
				xmin = Math.min(v.x, xmin);
				xmax = Math.max(v.x, xmax);
				ymin = Math.min(v.y, ymin);
				ymax = Math.max(v.y, ymax);
			}
		}
		return new double[] { xmin, ymin, xmax, ymax };
	}

	/* Loesche Markierungen des gewaehlten Typs von allen Knoten und Kanten. */

	public void unmark(int mark) {
		for (Vertex v : this.v)
			if (v.mark == mark)
				v.mark = 0;
		for (Edge e : this.e)
			if (e.mark == mark)
				e.mark = 0;
	}

	/*************************************************************************
	 ** Finde den kuerzesten Weg zwischen vStart und vEnd (Dijkstras Algorithmus)
	 *************************************************************************/

	public Path findShortestPath(Vertex vStart, Vertex vEnd) {

		Vector<Vertex> vertices = new Vector<Vertex>(this.v);
		boolean foundVStart = false, foundVEnd = false;

		for (Vertex v : vertices) {
			v.dist = vertices.size();
			v.pred = null;
			if (v == vStart)
				foundVStart = true;
			if (v == vEnd)
				foundVEnd = true;
		}

		if (vStart == vEnd)
			throw new RuntimeException("findShortestPath: start=end");
		if (!foundVStart)
			throw new RuntimeException("findShortestPath: Graph doesn't " + "contain vStart");
		if (!foundVEnd)
			throw new RuntimeException("findShortestPath: Graph doesn't " + "contain vEnd");

		vStart.dist = 0;
		boolean done = false;

		while (!done) {
			Collections.sort(vertices, new Comparator<Vertex>() {
				public int compare(Vertex v1, Vertex v2) {
					return v1.dist - v2.dist;
				}
			});

			Vertex closest = vertices.remove(0);
			int newDist = closest.dist + 1;

			for (Edge e : edgesFor(closest)) {
				Vertex v = e.leadsTo(closest);
				if (v.dist > newDist) {
					v.dist = newDist;
					v.pred = closest;
					if (v == vEnd) {
						done = true;
						break;
					}
				}
			}
		}

		vertices.clear();

		for (Vertex v = vEnd; v != vStart; v = v.pred)
			vertices.add(0, v);
		vertices.add(0, vStart);

		Vertex u = vertices.remove(0);
		Path p = new Path(u);
		while (vertices.size() > 0) {
			Vertex v = vertices.remove(0);
			Edge e = findEdge(u, v);
			p.add(e, v);
			u = v;
		}
		return p;
	}

	private boolean isNeighbour(Vertex va, Vertex vb, int dist) {
		if (va == vb)
			return true;
		if (dist == 0)
			return false;

		for (Edge e : edgesFor(va)) {
			Vertex v = e.leadsTo(va);
			if (isNeighbour(v, vb, dist - 1))
				return true;
		}
		return false;
	}

	private Edge findEdge(Vertex v, Vertex u) {
		for (Edge e : edgesFor(v))
			if (e.leadsTo(v) == u)
				return e;
		return null;
	}

	/*************************************************************************
	 ** Visuelle Komplexitaet des Graphen
	 *************************************************************************/

	public int visualComplexity() {
		return visualComplexity(5.0);
	}

	/* Fuehre Paritionierung durch gemaess Unterabschnitt 6.3, Abb. 15 */

	public int visualComplexity(double angle) {

		Vector<Edge> edges = new Vector<Edge>(e);
		for (Edge e : edges)
			e.group = 0;

		int group = 0;

		Vertex v1 = null, v2 = null;
		Edge e = null;

		whileLoop: while (edges.size() > 0) {

			if (v1 == null) {
				e = edges.remove(0);
				e.group = ++group;
				v1 = e.v1;
				v2 = e.v2;
			}

			for (Edge e2 : edges) {

				Vertex v3 = null;
				if (e2.v1 == v2)
					v3 = e2.v2;
				else if (e2.v2 == v2)
					v3 = e2.v1;

				if (v3 == null)
					continue;

				double ang = Math.acos(Math.min(1, Math.max(0, Util.normalizedInnerProduct(v1, v2, v2, v3))));
				if (ang * 180 / Math.PI >= angle)
					continue;

				edges.remove(e2);
				e2.group = group;
				v1 = v2;
				v2 = v3;
				continue whileLoop;
			}

			if (e == null)
				v1 = v2 = null;
			else {
				v1 = e.v2;
				v2 = e.v1;
				e = null;
			}
		}

		return group;
	}

	/*************************************************************************
	 ** Ueberlappende Kanten, vgl. Unterabschnitt 6.2.
	 *************************************************************************/

	public int numOverlappingEdges() {
		return numOverlappingEdges(2, 0.1);
	}

	/* Bestimme Anzahl der ueberlappenden Kanten */

	public int numOverlappingEdges(double deg, double dim) {

		int num = 0;
		int n = e.size();
		for (int i = 0; i < n - 1; i++)
			for (int j = i; j < n; j++)
				if (i != j)
					if (edgesOverlap(e.elementAt(i), e.elementAt(j), deg, dim))
						num++;
		return num;
	}

	/* Ueberlappen die Kanten e1 und e2? */

	private boolean edgesOverlap(Edge e1, Edge e2, double deg, double dim) {

		double angle = Math
				.acos(Math.min(1, Math.max(0, Math.abs(Util.normalizedInnerProduct(e1.v1, e1.v2, e2.v1, e2.v2)))));

		return (angle < Math.PI * deg / 180) && (vertexNearEdge(e1.v1, e2, dim) || vertexNearEdge(e1.v2, e2, dim)
				|| vertexNearEdge(e2.v1, e1, dim) || vertexNearEdge(e2.v2, e1, dim));
	}

	/* Ist v in der Naehe von e? (vgl. Abb. 13) */

	private boolean vertexNearEdge(Vertex v, Edge e, double dim) {

		if (v == e.v1 || v == e.v2)
			return false;

		double rx = e.v2.x - e.v1.x;
		double ry = e.v2.y - e.v1.y;
		double r2 = rx * rx + ry * ry;
		double r = Math.sqrt(r2);

		double v1x = v.x - e.v1.x;
		double v1y = v.y - e.v1.y;

		double l = (v1x * rx + v1y * ry) / r2;

		double dx = v.x - (e.v1.x + l * rx);
		double dy = v.y - (e.v1.y + l * ry);
		double d = Math.sqrt(dx * dx + dy * dy);

		return l > -dim && l < (1.0 + dim) && d < r * dim;
	}
}
