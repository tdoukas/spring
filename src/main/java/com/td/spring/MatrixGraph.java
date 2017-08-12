package com.td.spring;
/*****************************************************************************
 * MatrixGraph.java
 *
 * Erstellt einen Graph aus einer Adjazenzmatrix, die aus einem File geladen
 * wird. Erkennt nur eine begrenzte Untermenge des MatrixMarket-Formates,
 * eigentlich gerade genug, um die Graphen im Matrix/-Ordner zu parsen.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: MatrixGraph.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

class MatrixGraph extends Graph {

	/* Name des Generators */

	String getName() {
		return "Matrix";
	}

	/* Name des Verzeichnisses */

	final String dataDirName = "matrixData";

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	FileSelectorControl fileCtrl = new FileSelectorControl("File", "Filename to load", this, Util.findDir(dataDirName),
			null);

	public Control[] getControls() {
		return new Control[] { fileCtrl };
	}

	public void valueChanged(Control ctrl) {
		if (ctrl == fileCtrl) {
			Application.getInstance().uiGenerate();
		}
	}

	/*************************************************************************
	 ** Graph erzeugen
	 *************************************************************************/

	void generate() {
		clear();

		String filename = fileCtrl.getValue();
		if (filename == null)
			return;

		try {
			BufferedReader r = new BufferedReader(new FileReader(new File(Util.findDir(dataDirName), filename)));

			r.readLine();
			r.readLine();

			for (String line = r.readLine(); line != null; line = r.readLine()) {
				try {
					String[] tokens = line.split(" ");
					int a = Integer.parseInt(tokens[0]);
					int b = Integer.parseInt(tokens[1]);
					if (a == b)
						continue;
					Vertex va = findVertex(a);
					if (va == null) {
						va = new Vertex(a);
						add(va);
					}
					Vertex vb = findVertex(b);
					if (vb == null) {
						vb = new Vertex(b);
						add(vb);
					}
					add(new Edge(va, vb));
				} catch (Exception e) {
					System.out.println("While parsing " + filename + ": ");
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
