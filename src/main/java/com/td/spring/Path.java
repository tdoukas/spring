package com.td.spring;
/*****************************************************************************
 * Path.java
 *
 * Ein gerichteter Pfad (Kantenzug) zwischen zwei Knoten.
 *
 *         +------+-----+-----+-----+-----+-------- edges
 * vStart  |      |     |     |     |     |   vEnd 
 *    o--------o-----o-----o-----o-----o-------o
 *             |     |     |     |     |       |
 *             +-----+-----+-----+-----+-------+--- vertices
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Path.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

class Path {

	// Start- und Endknoten
	Vertex vStart, vEnd;

	// Enthaelt alle Knoten des Pfades, ausser dem Startknoten
	Vector<Vertex> vertices = new Vector<Vertex>();

	// Enthaelt alle Kanten des Pfades
	Vector<Edge> edges = new Vector<Edge>();

	int ttl; // Fuer PfadManager "V1" (RandomPathManager)
	double initialDistance; // Fuer konvexes Modell
	double weight; // Wird verschieden genutzt, zum Sortieren

	/* Konstruktor: benoetigt wenigstens Startknoten */

	Path(Vertex vStart) {
		this.vStart = vStart;
	}

	/* Konstruktur mit Startknoten und erster Kante */

	Path(Vertex vStart, Edge edge) {
		this.vStart = vStart;
		vEnd = edge.leadsTo(vStart);
		add(edge, vEnd);
	}

	/* Konstruktor: erzeuge Kopie von path */

	Path(Path path) {
		vStart = path.vStart;
		vEnd = path.vEnd;
		vertices.addAll(path.vertices);
		edges.addAll(path.edges);
		initialDistance = path.initialDistance;
	}

	/* Kehre Reihenfolge aller Knoten und Kanten um */

	void reverse() {
		Collections.reverse(vertices);
		Collections.reverse(edges);
		Vertex v = vStart;
		vStart = vertices.remove(0);
		vertices.add(v);
		vEnd = v;
	}

	/* Fuege Kante e und Zielknoten u hinzu */

	void add(Edge e, Vertex u) {
		if (u == vStart)
			throw new RuntimeException("Attempt to add vstart=" + u + " to vertices.");
		edges.add(e);
		vertices.add(u);
		this.vEnd = u;
	}

	/* Haenge p an diesen Pfad an */

	void append(Path p) {
		if (vEnd != null && p.vStart != vEnd)
			throw new RuntimeException("Can't append this=" + this + " and p=" + p + " because this.vEnd != p.vStart");
		edges.addAll(p.edges);
		vertices.addAll(p.vertices);
		this.vEnd = p.vEnd;
	}

	/* Enthaelt dieser Pfad den Knoten v? */

	boolean contains(Vertex v) {
		return v == vStart || vertices.contains(v);
	}

	/*
	 * Pruefe, ob Pfad p an diesen Pfad angehaengt werden kann; vermeide zirkulaere
	 * Pfade
	 */

	boolean mayAppend(Path p) {
		if (vEnd != null && p.vStart != vEnd)
			return false;
		for (Vertex v : p.vertices) {
			if (contains(v) || vStart == v)
				return false;
		}
		return true;
	}

	/* Markiere den Pfad (Knoten und Kanten) mit (Farbe) m. */

	void mark(int m) {
		for (Edge e : edges)
			e.mark(m);
		for (Vertex v : vertices)
			v.mark(m);
		vStart.mark(m);
	}

	/*
	 * Markiere den Pfad mit m, aber nur an den Stellen, die noch keine Markierung
	 * besitzen.
	 */

	void maybeMark(int m) {
		for (Edge e : edges)
			if (e.mark == 0)
				e.mark(m);
		for (Vertex v : vertices)
			if (v.mark == 0)
				v.mark(m);
		if (vStart.mark == 0)
			vStart.mark(m);
	}

	/* Gebe Laenge des Pfades (Anzahl der Kanten) */

	int length() {
		return edges.size();
	}

	/* Berechne die Summe aller Kantenlaengen (in Grapheinheiten) */

	double distance() {

		double distance = 0;
		for (Edge e : edges) {
			distance += e.distance();
		}

		return distance;
	}

	/* Setze die intiale Distanz, fuer konkaven Pfadmanager. */

	void setInitialDistance() {
		initialDistance = distance();
	}

	/* Berechne ein Mass fuer die "Geradheit" des Pfades. */

	double getStraightness() {

		double straightness = 0;
		Vertex v = vStart;
		Iterator<Edge> i = edges.iterator();
		if (!i.hasNext())
			return -1;

		Edge e = i.next();
		Vertex u = e.leadsTo(v);

		while (i.hasNext()) {
			e = i.next();

			Vertex w = e.leadsTo(u);
			straightness += Util.normalizedInnerProduct(v, u, u, w);
			v = u;
			u = w;
		}

		return straightness;
	}

	/* Fuer's Debugging. */

	public String toString() {
		if (vStart == null)
			return "Empty path";

		StringBuffer sb = new StringBuffer();
		sb.append("<");
		sb.append(vStart);
		sb.append("|");
		for (Edge e : edges) {
			sb.append(e);
		}
		sb.append("|");
		for (Vertex v : vertices) {
			sb.append(v);
		}
		sb.append(">");
		return sb.toString();
	}

	/* Pruefe Pfad auf Wohldefiniertheit (unvollstaendig) */

	public void check() {
		if (vertices.contains(vStart))
			throw new RuntimeException("while checking " + this + ": vertices contains vStart=" + vStart);
	}
}
