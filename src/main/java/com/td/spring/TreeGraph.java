package com.td.spring;

/*****************************************************************************
 * TreeGraph.java
 *
 * Graph eines Baumes mit variabler Tiefe und Anzahl von Kindknoten.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: TreeGraph.java 228 2015-12-11
 * 13:34:55Z dude $
 *****************************************************************************/

class TreeGraph extends Graph {

	String getName() {
		return "Tree";
	}

	/*************************************************************************
	 ** Controls
	 *************************************************************************/

	private IntControl depthCtrl = new IntControl("depth", "Depth of tree", this, 3, 1, 5);
	private IntControl numChildsCtrl = new IntControl("numChilds", "Number of childs for each node", this, 4, 1, 6);

	public Control[] getControls() {
		return new Control[] { depthCtrl, numChildsCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Graph erzeugen
	 *************************************************************************/

	void generate() {

		int depth = depthCtrl.getValue();
		int numChilds = numChildsCtrl.getValue();

		v.clear();
		e.clear();

		Vertex root = new Vertex();
		add(root);
		generate(root, depth, numChilds);
	}

	/* Rekursiv weitere Teilbaeume erzeugen */

	private void generate(Vertex v, int depth, int numChilds) {
		if (depth <= 0)
			return;
		for (int i = 0; i < numChilds; i++) {
			Vertex u = new Vertex();
			add(u);
			add(new Edge(u, v));
			generate(u, depth - 1, numChilds);
		}
	}
}
