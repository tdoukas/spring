package com.td.spring;
/*****************************************************************************
 * ConvexREModel.java
 *
 * Kraeftemodell fuer starre Kantenzuege, hier: das konvexe Modell (4.5).
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: ConvexREModel.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Iterator;

class ConvexREModel extends REModel {

	/* Name des Modells */

	public String getName() {
		return "Convex";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	RealControl fCtrl = new LogarithmicControl("Force", "Force along path", this, 1E5, 1E-2, 1E9);

	public Control[] getControls() {
		return new Control[] { fCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Kraefte berechnen, vgl. Unterabschnitt 4.5.
	 *************************************************************************/

	/* Berechne Kraefte auf einen Pfad, siehe auch REModel Oberklasse. */

	public void computeForces(Path path) {

		final double k = 1;

		double[] dist = new double[3];
		double force = fCtrl.getValue();
		Vertex u = path.vStart;
		Iterator<Vertex> i = path.vertices.iterator();
		Vertex q = i.next();

		while (i.hasNext()) {

			Vertex v = i.next();

			double r1x = u.x - q.x;
			double r1y = u.y - q.y;
			double r1 = Math.sqrt(r1x * r1x + r1y * r1y);
			double r2x = v.x - q.x;
			double r2y = v.y - q.y;
			double r2 = Math.sqrt(r2x * r2x + r2y * r2y);

			if (r1 > 0 && r2 > 0) {

				double cos = (r1x * r2x + r1y * r2y) / (r1 * r2);

				/*
				 * cos kann durch FP-Arithmetik ausserhalb von [-1, 1] liegen, daher:
				 */

				cos = Math.min(1.0, Math.max(-1.0, cos));

				double angle = Math.PI - Math.acos(cos);
				double f = force * angle / k;

				double det = r1x * r2y - r1y * r2x;
				if (det > 0)
					f *= -1;

				double e1x = r1y / r1;
				double e1y = -r1x / r1;
				double e2x = r2y / r2;
				double e2y = -r2x / r2;

				double f1 = f / r1;
				double f2 = f / r2;

				double f1x = f1 * e1x;
				double f1y = f1 * e1y;
				double f2x = f2 * e2x;
				double f2y = f2 * e2y;

				u.dx -= f1x;
				u.dy -= f1y;

				q.dx += f1x;
				q.dy += f1y;
				q.dx -= f2x;
				q.dy -= f2y;

				v.dx += f2x;
				v.dy += f2y;
			}

			u = q;
			q = v;
		}
	}
}
