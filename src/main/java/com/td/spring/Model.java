package com.td.spring;
/*****************************************************************************
 * Model.java
 *
 * Koordiniert die Berechnungen. 
 * Benutzt dazu Graph, Embedder, REModel und PathManager.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Model.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Vector;

class Model implements ControlProvider {

	private Graph graph;
	private Embedder embedder;
	private REModel reModel;
	private PathManager pathManager;

	// Externe Kraefte und Drehmomente herausrechnen?
	private boolean fixTranslation = false;
	private boolean fixRotation = false;

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private RealControl friCtrl = new LinearControl("Friction", "Simulate dynamic system", this, 1.0, 0, 1.0);

	public Control[] getControls() {
		return new Control[] { friCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Berechnungsschritt, vgl. Abb. 2 bzw. Abb. 22.
	 *************************************************************************/

	/* Wird in jeder Iteration von Application aufgerufen. */

	public void step() {

		clearForces();

		embedder.prepareStep(graph);

		embedder.computeForces(graph);
		storeForces();

		reModel.computeForces(graph);

		embedder.limitForces(graph);

		addForces();

		if (fixRotation)
			fixRotation();
		if (fixTranslation)
			fixTranslation();

		computeDisplacement();

		if (pathManager != null) {
			synchronized (pathManager) {
				pathManager.manage();
				if (pathManager.getPathsChanged())
					notifyPathsChanged();
			}
		}
	}

	/* Loesche alle Kraefte dx,dy und dx2, dy2 (siehe auch Vertex.java) */

	private void clearForces() {
		for (Vertex v : graph.v)
			v.dx = v.dy = v.dx2 = v.dy2 = 0;
	}

	/* Speichere die Kraft Delta q, so dass sie gezeichnet werden kann */

	private void storeForces() {
		for (Vertex v : graph.v) {
			v.fx = v.dx;
			v.fy = v.dy;
		}
	}

	/* Addiere Delta q und Delta q', also dx,dy und dx2,dy2 */

	private void addForces() {
		for (Vertex v : graph.v) {
			v.dx += v.dx2;
			v.dy += v.dy2;
		}
	}

	/*
	 * Berechne Anwendung der Kraefte auf die Knotenpositionen, beruecksichtige hier
	 * auch die Reibung (friCtrl)
	 */

	private void computeDisplacement() {

		double friction = friCtrl.getValue();
		for (Vertex v : graph.v) {

			v.vx += v.dx;
			v.vy += v.dy;

			v.x += v.vx;
			v.y += v.vy;

			v.vx *= (1 - friction);
			v.vy *= (1 - friction);
		}
	}

	/*************************************************************************
	 ** Getter und Setter
	 *************************************************************************/

	public void setGraph(Graph graph) {
		this.graph = graph;
		if (embedder != null)
			embedder.restart();
		if (pathManager != null) {
			pathManager.setGraph(graph);
			pathManager.reset();
		}
	}

	public Graph getGraph() {
		return graph;
	}

	public void setEmbedder(Embedder embedder) {
		embedder.restart();
		this.embedder = embedder;
	}

	public Embedder getEmbedder() {
		return embedder;
	}

	public void setREModel(REModel reModel) {
		this.reModel = reModel;
		notifyPathsChanged();
	}

	public REModel getREModel() {
		return reModel;
	}

	public void setPathManager(PathManager pathManager) {
		pathManager.setGraph(graph);
		this.pathManager = pathManager;
		pathManager.reset();
	}

	public PathManager getPathManager() {
		return pathManager;
	}

	public boolean getFixTranslation() {
		return fixTranslation;
	}

	public void setFixTranslation(boolean fixTranslation) {
		this.fixTranslation = fixTranslation;
	}

	public boolean getFixRotation() {
		return fixRotation;
	}

	public void setFixRotation(boolean fixRotation) {
		this.fixRotation = fixRotation;
	}

	/*************************************************************************
	 ** Aktionen, extern ausgeloest
	 *************************************************************************/

	/* Setze alle Knoten auf zufaellige Ausgangspositionen zurueck */

	void resetVertices() {
		if (graph != null && embedder != null) {
			graph.resetVertices();
			restartEmbedder();
			if (pathManager != null)
				pathManager.reset();
		}
	}

	/* Generiere den Graph neu */

	void generateGraph() {
		if (graph != null) {
			graph.generate();
			graph.resetVertices();
			restartEmbedder();
			if (pathManager != null)
				pathManager.reset();
		}
	}

	/* Embedder anlaufen lassen */

	void restartEmbedder() {
		if (embedder != null)
			embedder.restart();
	}

	/*************************************************************************
	 ** Notifier
	 *************************************************************************/

	/* Wird von Application aufgerufen */

	void notifyPathsChanged() {
		if (reModel != null)
			reModel.pathsChanged(getAllPaths());
	}

	/*
	 * Liefere alle Pfade zurueck, sowohl die vom Benutzer ausgewaehlten als auch
	 * die vom Pfadmanager erzeugten. Wird nur in notifyPathsChanged() benutzt
	 */

	private Vector<Path> getAllPaths() {
		Vector<Path> paths = new Vector<Path>(Application.getInstance().getSelectedPaths());
		if (pathManager != null)
			paths.addAll(pathManager.getPaths());
		return paths;
	}

	/*************************************************************************
	 ** Schwerpunktstranslation und Gesamtdrehmoment herausrechnen
	 *************************************************************************/

	/* Eliminiere Schwerpunktstranslation */

	private void fixTranslation() {

		double dx = 0;
		double dy = 0;

		for (Vertex v : graph.v) {
			dx += v.dx;
			dy += v.dy;
		}

		int n = graph.v.size();
		dx /= n;
		dy /= n;

		for (Vertex v : graph.v) {
			v.dx -= dx;
			v.dy -= dy;
		}
	}

	/* Eliminiere Gesamtdrehmoment */

	private void fixRotation() {

		int n = graph.v.size();

		if (n == 0)
			return;

		double bx = 0;
		double by = 0;

		for (Vertex v : graph.v) {
			bx += v.x;
			by += v.y;
		}

		bx /= n; // (bx,by) = barycenter
		by /= n;

		double torq = 0;
		double sumR = 0;

		for (Vertex v : graph.v) {

			double rx = v.x - bx;
			double ry = v.y - by;
			sumR += Math.sqrt(rx * rx + ry * ry);
			torq += rx * v.dy - ry * v.dx;
		}

		torq /= sumR;

		for (Vertex v : graph.v) {

			double rx = v.x - bx;
			double ry = v.y - by;

			double r = Math.sqrt(rx * rx + ry * ry);

			v.dx += ry * torq / r;
			v.dy -= rx * torq / r;
		}
	}

	/*************************************************************************
	 ** Zum Debuggen
	 *************************************************************************/

	/* Berechne externe Kraft */

	private double getOverallTranslation() {

		double sx = 0, sy = 0;
		for (Vertex v : graph.v) {
			sx += v.dx;
			sy += v.dy;
		}

		return Math.sqrt(sx * sx + sy * sy);
	}

	/* Berechne externes Drehmoment */

	private double getOverallTorque() {

		int n = graph.v.size();
		double bx = 0;
		double by = 0;

		for (Vertex v : graph.v) {
			bx += v.x;
			by += v.y;
		}

		bx /= n; // (bx,by) = barycenter
		by /= n;

		double torq = 0;

		for (Vertex v : graph.v) {

			double rx = v.x - bx;
			double ry = v.y - by;
			torq += rx * v.dy - ry * v.dx;
		}

		return torq;
	}

	/* Debug-Ausgabe der beiden Werte */

	private void reportTT(String pref) {
		System.out.println(pref + ":" + getOverallTranslation() + ", " + getOverallTorque());
	}
}
