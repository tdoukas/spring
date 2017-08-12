package com.td.spring;

/*****************************************************************************
 * StraightREModel.java
 *
 * Kraeftemodell fuer starre Kantenzuege
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: StraightREModel.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

class StraightREModel extends REModel {

	/* Name des Modells */

	public String getName() {
		return "Straight";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private final String DONOTHING = "Do nothing";
	private final String ELIM_ALL = "Eliminate all";
	private final String ELIM_BORDER = "Eliminate at borders";
	private final String COLLECT = "Collect and apply";

	private BoolControl railCtrl = new BoolControl("Rail mode", "Allow movement along connecting line", this, false);
	private OptionControl<String> efCtrl = new OptionControl<String>("Embedder forces", "How to handle embedder forces",
			this, new String[] { ELIM_ALL, ELIM_BORDER, COLLECT, DONOTHING }, ELIM_ALL);

	public Control[] getControls() {
		return new Control[] { railCtrl, efCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Kraefte berechnen, vgl. Unterabschnitte 4.3 und 4.4.
	 *************************************************************************/

	/*
	 * Berechne Kraefte auf einen Pfad, siehe auch REModel Oberklasse. Deligiert die
	 * Arbeit je nach Wert des railCtrl - Controls an die folgenden beiden Methoden.
	 */

	public void computeForces(Path path) {
		if (railCtrl.getValue())
			computeForcesRail(path);
		else
			computeForcesFix(path);
	}

	/* Berechne Kraefe auf einen Pfad, nach 4.3: (normales) gerades Modell */

	private void computeForcesFix(Path path) {

		int len = path.length();
		if (len < 2)
			return;

		double[] dist = new double[3];
		Util.compDistance(path.vEnd, path.vStart, dist);

		double dx = dist[0], dy = dist[1];
		dx /= len;
		dy /= len;

		Vertex v = path.vStart;
		double x = v.x;
		double y = v.y;

		if (efCtrl.getValue() == ELIM_BORDER || efCtrl.getValue() == ELIM_ALL) {
			path.vStart.dx = 0;
			path.vStart.dy = 0;
			path.vEnd.dx = 0;
			path.vEnd.dy = 0;
		}

		boolean elimInner = efCtrl.getValue() == ELIM_ALL || efCtrl.getValue() == COLLECT;

		double sx = 0, sy = 0;

		for (Edge e : path.edges) {

			x += dx;
			y += dy;

			v = e.leadsTo(v);
			if (v == path.vEnd)
				break;

			double ddx = x - v.x;
			double ddy = y - v.y;

			sx += v.dx;
			sy += v.dy;

			if (elimInner) {
				v.dx = 0;
				v.dy = 0;
			}

			v.dx2 = ddx;
			v.dy2 = ddy;
		}

		if (efCtrl.getValue() == COLLECT) {
			sx /= 2;
			sy /= 2;
			path.vStart.dx += sx;
			path.vStart.dy += sy;
			path.vEnd.dx += sx;
			path.vEnd.dy += sy;
		}
	}

	/* Berechne Kraefte auf einen Pfad: Schienenvariante aus 4.4 */

	private void computeForcesRail(Path path) {

		int len = path.length();
		if (len < 2)
			return;

		double[] dist = new double[3];
		Util.compDistance(path.vEnd, path.vStart, dist);

		double dx = dist[0], dy = dist[1], d2 = dist[2];
		Vertex v = path.vStart;
		double xStart = v.x, yStart = v.y;

		if (efCtrl.getValue() == ELIM_BORDER || efCtrl.getValue() == ELIM_ALL) {
			path.vStart.dx = 0;
			path.vStart.dy = 0;
			path.vEnd.dx = 0;
			path.vEnd.dy = 0;
		}

		boolean collect = efCtrl.getValue() == COLLECT;
		boolean elimInner = efCtrl.getValue() == ELIM_ALL || collect;

		double sx = 0, sy = 0;

		for (Edge e : path.edges) {

			v = e.leadsTo(v);
			if (v == path.vEnd)
				break;

			double l = (((v.x - xStart) * dx + (v.y - yStart) * dy)) / d2;

			double x = xStart + l * dx;
			double y = yStart + l * dy;

			double ddx = x - v.x;
			double ddy = y - v.y;

			// tangentialer Anteil der Embedder-Kraft Delta q
			double t = (v.dx * dx + v.dy * dy) / d2;
			double tx = t * dx;
			double ty = t * dy;

			// senkrechter ("perpendicular") Anteil
			double px = v.dx - tx;
			double py = v.dy - ty;

			sx += px;
			sy += py;

			if (elimInner) {
				v.dx = tx;
				v.dy = ty;
			}

			v.dx2 = ddx;
			v.dy2 = ddy;
		}

		if (efCtrl.getValue() == COLLECT) {
			sx /= 2;
			sy /= 2;
			path.vStart.dx += sx;
			path.vStart.dy += sy;
			path.vEnd.dx += sx;
			path.vEnd.dy += sy;
		}
	}
}
