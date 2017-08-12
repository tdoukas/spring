package com.td.spring;

/*****************************************************************************
 * Embedder.java
 *
 * Superklasse fuer alle Embedder.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: Embedder.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

abstract class Embedder implements ControlProvider {

	/*************************************************************************
	 ** Von Unterklasse zu implementieren:
	 *************************************************************************/

	/* Bereite Berechnung vor (berechne z.B. Konstanten) */

	public void prepareStep(Graph graph) {
	}

	/* Berechne Kraefte auf Graph */

	abstract public void computeForces(Graph graph);

	/*************************************************************************
	 ** Kraefte begrenzen
	 *************************************************************************/

	/* Begrenze Kraefte (dx,dy) in graph auf festen Wert */

	public void limitForces(Graph graph) {
		limitForces(graph, 1.0);
	}

	/* Begrenze Kraefte (dx,dy) in Graph auf angegebenen Wert limit */

	protected void limitForces(Graph g, double limit) {
		for (Vertex v : g.v) {

			double d = Math.sqrt(v.dx * v.dx + v.dy * v.dy);
			if (d > 0.0001) {

				double r = Math.min(limit, d);
				v.dx = v.dx * r / d;
				v.dy = v.dy * r / d;
			}
		}
	}

	/*************************************************************************
	 ** Getter, Setter
	 *************************************************************************/

	abstract String getName();

	public String toString() {
		return getName();
	}

	/*************************************************************************
	 ** Aktionen und Notifier
	 *************************************************************************/

	/*
	 * Wird vom Hauptprogramm (Application) aufgerufen, wenn der Embedder neu
	 * gestartet werden soll.
	 */

	void restart() {
	}

	/* Wird vom Embedder aufgerufen, wenn die Berechnung beendet ist. */

	void finished() {
		Application.getInstance().notifyEmbedderFinished();
	}
}
