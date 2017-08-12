package com.td.spring;

/*****************************************************************************
 * EadesEmbedder.java
 *
 * Implementierung des Sping-Embedders von Eades (1984).
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: EadesEmbedder.java 228 2015-12-11
 * 13:34:55Z dude $
 *****************************************************************************/

class EadesEmbedder extends Embedder {

	/* Name des Embedders */

	String getName() {
		return "Eades (84)";
	}

	/* Konstanten C1-C4, siehe Paper von Eades */

	private double c1, c2, c3, c4;

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private RealControl c1Ctrl = new LinearControl("C1", "Attractive force", this, 2.0, 0, 5);
	private RealControl c2Ctrl = new LogarithmicControl("C2", "Ideal spring length", this, 1.0, 0.1, 10);
	private RealControl c3Ctrl = new LinearControl("C3", "Repulsive force", this, 0.1, 0, 2);
	private RealControl c4Ctrl = new LinearControl("C4", "Step size", this, 0.1, 0, 1);
	private IntControl iterCtrl = new IntControl("N", "Number of iterations", this, 100, 10, 1000);

	public Control[] getControls() {
		return new Control[] { c1Ctrl, c2Ctrl, c3Ctrl, c4Ctrl, iterCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Abstossende und anziehende Kraft
	 *************************************************************************/

	private double fr(double d) {
		return c3 / d * d;
	}

	private double fa(double d) {
		return c1 * Math.log(d / c2);
	}

	/*************************************************************************
	 ** Berechnungsschritt
	 *************************************************************************/

	public void computeForces(Graph g) {

		double[] dist = new double[3]; // fuer compDistance()

		// Setze die Parameter c1-c4 aus den Controls

		c1 = c1Ctrl.getValue();
		c2 = c2Ctrl.getValue();
		c3 = c3Ctrl.getValue();
		c4 = c4Ctrl.getValue();

		// Berechne Kraft auf jeden Knoten v

		for (Edge e : g.e) {
			Util.compDistance(e.v1, e.v2, dist);
			double dx = dist[0], dy = dist[1], d2 = dist[2];
			double d = Math.sqrt(d2);
			double f = fa(d) / d;
			e.v1.dx -= f * dx;
			e.v1.dy -= f * dy;
			e.v2.dx += f * dx;
			e.v2.dy += f * dy;
		}

		for (Vertex v : g.v) {
			for (Vertex u : g.v)
				if (u != v) {
					Util.compDistance(u, v, dist);
					double dx = dist[0], dy = dist[1], d2 = dist[2];
					double d = Math.sqrt(d2);
					double f = -fr(d) / d;
					v.dx += f * dx / d;
					v.dy += f * dy / d;
				}
		}

		// Multipliziere dx, dy mit Schrittweite c4

		for (Vertex v : g.v) {
			v.dx *= c4;
			v.dy *= c4;
		}

		// Wenn maximum der Iterationen erreicht ist, beende.

		if (iterCtrl.getValue() <= Application.getInstance().getIteration())
			finished();
	}
}
