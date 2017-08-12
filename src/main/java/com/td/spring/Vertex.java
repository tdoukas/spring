package com.td.spring;

/*****************************************************************************
 * Vertex.java
 *
 * Ein Knoten eines Graphen
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: Vertex.java 246 2016-01-09 13:27:32Z
 * dude $
 *****************************************************************************/

class Vertex {

	double x, y; // Position
	double dx, dy; // "Kraft" (Delta q, vgl. 4.2.)
	double dx2, dy2; // Delta q', vgl. 4.2.
	double vx, vy; // Geschwindigkeit, vgl. Abb. 22
	double fx, fy; // Kopie der Kraefte dx,dy (um sie zu zeichnen)
	int id = -1; // ID des Knotens
	int mark = 0; // Markierung (Farbe)
	double weight; // Wird von Algorithmen unterschiedlich verwendet
	int dist; // Fuer Dijkstras Algorithmus, siehe Graph.java
	Vertex pred; // Ebenso
	boolean highlight = false; // Teil der Fokusregion?
	int nr; // Laufende Nummer (eindeutig)

	/* Konstruktoren */

	Vertex() {
	}

	Vertex(int id) {
		this.id = id;
	}

	Vertex(double x, double y) {
		this.x = x;
		this.y = y;
	}

	Vertex(int id, double x, double y) {
		this.id = id;
		this.x = x;
		this.y = y;
	}

	/* Setze Markierung (Farbe) */

	void mark(int mark) {
		this.mark = mark;
	}

	/* Entferne Markierung */

	void unmark(int mark) {
		if (this.mark == mark)
			this.mark = 0;
	}

	/* Fuer Debug-Output */

	public String toString() {
		return "(" + id + ")";
	}
}
