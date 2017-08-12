package com.td.spring;

/*****************************************************************************
 * Edge.java
 *
 * Eine Kante des Graphen.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: Edge.java 228 2015-12-11 13:34:55Z
 * dude $
 *****************************************************************************/

class Edge {

	// Eine Kante enthaelt zwei Knoten
	Vertex v1, v2;

	// Markierung (Farbe)
	int mark = 0;

	// Wird von Algorithmen unterschiedlich genutzt, typischerweise wenn
	// Kanten nach Gewicht sortiert werden sollen
	double weight;

	// Kantengruppe (Partitionierung nach visueller Zusammengehoerigkeit)
	int group = 0;

	/* Konstruktor */

	Edge(Vertex v1, Vertex v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	/* Ist v Bestandteil dieser Kante? */

	boolean containsVertex(Vertex v) {
		return v == v1 || v == v2;
	}

	/* Gebe den anderen Knoten der Kante (nicht v) */

	Vertex leadsTo(Vertex v) {
		if (v == v1)
			return v2;
		if (v == v2)
			return v1;
		return null;
	}

	/* Setze Markierung. Z.B. werden Kanten vom PathManager markiert. */

	void mark(int mark) {
		this.mark = mark;
	}

	/* Gebe die Laenge der Kante, in Graph-Einheiten */

	double distance() {
		double dx = v1.x - v2.x;
		double dy = v1.y - v2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/* Ist diese Kante mit e benachbart? */

	boolean connectsTo(Edge e) {
		return v1 == e.v1 || v1 == e.v2 || v2 == e.v1 || v2 == e.v2;
	}

	/* Hat diese Kante eine aehnliche Richtung wie e? */

	boolean sameDirectionAbs(Edge e, double maxAngle) {
		double angle = Math.acos(Math.min(1, Math.max(0, Math.abs(Util.normalizedInnerProduct(v1, v2, e.v1, e.v2)))));
		angle = angle * 180 / Math.PI;
		return angle <= maxAngle;
	}

	/* Fuer Debug-Output */

	public String toString() {
		return "[" + v1 + "-" + v2 + "]";
	}
}
