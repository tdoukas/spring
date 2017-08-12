package com.td.spring;

/*****************************************************************************
 * QuadMeshGraph.java
 *
 * Ein Gitternetz mit variabler Breite und Hoehe. Kann sowohl in X- als auch in
 * Y-Richtung geschlossen werden; beim Schliessen in einer Richtung entsteht
 * eine Zylinderflaeche, werden beide Richtungen geschlossen entsteht ein Ring.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: QuadMeshGraph.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

class QuadMeshGraph extends Graph {

	/* Name des Graph-Generators */

	String getName() {
		return "QuadMesh";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	IntControl wCtrl = new IntControl("w", "Width of mesh", this, 10, 2, 100);
	IntControl hCtrl = new IntControl("h", "Height of mesh", this, 10, 2, 100);
	BoolControl closexCtrl = new BoolControl("Closed(X)", "Close surface in X-direction", this, false);
	BoolControl closeyCtrl = new BoolControl("Closed(Y)", "Close surface in Y-direction", this, false);

	public Control[] getControls() {
		return new Control[] { wCtrl, hCtrl, closexCtrl, closeyCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Graph erzeugen
	 *************************************************************************/

	void generate() {
		v.clear();
		e.clear();

		int w = wCtrl.getValue();
		int h = hCtrl.getValue();
		int n = w * h;

		for (int i = 0; i < n; i++)
			add(new Vertex(i, Math.random(), Math.random()));

		for (int x = 0; x < w - 1; x++)
			for (int y = 0; y < h - 1; y++) {
				addNew(new Edge(v.elementAt(y * w + x), v.elementAt(y * w + x + 1)));
				addNew(new Edge(v.elementAt(y * w + x + 1), v.elementAt((y + 1) * w + x + 1)));
				addNew(new Edge(v.elementAt((y + 1) * w + x + 1), v.elementAt((y + 1) * w + x)));
				addNew(new Edge(v.elementAt((y + 1) * w + x), v.elementAt(y * w + x)));
			}

		if (closexCtrl.getValue())
			for (int y = 0; y < h; y++)
				addNew(new Edge(v.elementAt(y * w), v.elementAt((y + 1) * w - 1)));

		if (closeyCtrl.getValue())
			for (int x = 0; x < w; x++)
				addNew(new Edge(v.elementAt(x), v.elementAt(x + (h - 1) * w)));
	}
}
