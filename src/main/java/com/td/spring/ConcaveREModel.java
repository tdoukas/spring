package com.td.spring;

/*****************************************************************************
 * ConcaveREModel.java
 *
 * Kraeftemodell fuer starre Kantenzuege, hier: das konkave Modell (4.6).
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: ConcaveREModel.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

class ConcaveREModel extends REModel {

	/* Name des Modells */

	public String getName() {
		return "Concave";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	RealControl fCtrl = new LogarithmicControl("Force", "Force along path", this, 1E4, 1E-6, 1E8);

	public Control[] getControls() {
		return new Control[] { fCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Kraefte berechnen
	 *************************************************************************/

	/* Berechne Kraefte auf einen Pfad, siehe auch REModel Oberklasse. */

	public void computeForces(Path path) {

		double[] dist = new double[3];
		double force = fCtrl.getValue();

		Vertex u = path.vStart;
		Vertex v = path.vEnd;
		Util.compDistance(u, v, dist);

		double dx = dist[0], dy = dist[1], d2 = dist[2];
		double d = Math.sqrt(d2);
		double idealD = path.initialDistance;
		double diff = idealD - d;
		double f = force * diff * diff;

		if (diff < 0)
			f *= -1;

		double ddx = f * dx / d;
		double ddy = f * dy / d;

		u.dx += ddx;
		u.dy += ddy;
		v.dx -= ddx;
		v.dy -= ddy;
	}
}
