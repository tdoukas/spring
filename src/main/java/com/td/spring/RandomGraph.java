package com.td.spring;

/*****************************************************************************
 * RandomGraph.java
 *
 * Ein Graph mit einer vorgegebenen Anzahl von Knoten (N) und zufaelligen Kanten
 * (mit Wahrscheinlichkeit p fuer Kante zwischen v_i und v_j, i<j).
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: RandomGraph.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

class RandomGraph extends Graph {

	/* Name des Graph-Generators */

	String getName() {
		return "Gilbert (Random)";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private IntControl nCtrl = new IntControl("N", "Number of vertices", this, 10, 2, 100);
	private LinearControl pCtrl = new LinearControl("p", "Probability for edge", this, 1.0, 0, 1.0);

	public Control[] getControls() {
		return new Control[] { nCtrl, pCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Graph erzeugen
	 *************************************************************************/

	void generate() {
		v.clear();
		e.clear();

		int n = nCtrl.getValue();
		double p = pCtrl.getValue();

		for (int i = 0; i < n; i++)
			add(new Vertex(Math.random(), Math.random()));

		for (int i = 0; i < n - 1; i++)
			for (int j = i + 1; j < n; j++)
				if (Math.random() < p)
					add(new Edge(v.elementAt(i), v.elementAt(j)));
	}
}
