package com.td.spring;
/*****************************************************************************
 * REModel.java
 *
 * Kraeftemodell fuer starre Kantenzuege: Oberklasse
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: REModel.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Vector;

abstract class REModel implements ControlProvider {

	private Vector<Path> paths; // Speichert Liste der starren Pfade

	public abstract String getName();

	public String toString() {
		return getName();
	}

	/*************************************************************************
	 ** Path / Vertex management
	 *************************************************************************/

	/* Notifier (Callback), wird von Model aufgerufen */

	public void pathsChanged(Vector<Path> paths) {
		this.paths = paths;
	}

	/*************************************************************************
	 ** Berechne Kraefte
	 *************************************************************************/

	/* Berechne die Kraefte dieses Modells auf den Graphen g */

	public void computeForces(Graph g) {
		for (Path path : paths)
			computeForces(path);
	}

	/*
	 * Berechne die Kraefte dieses Modells auf einen Kantenzug path (von der
	 * Unterklasse zu implementieren)
	 */

	public abstract void computeForces(Path path);
}
