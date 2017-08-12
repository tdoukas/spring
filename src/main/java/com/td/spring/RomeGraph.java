package com.td.spring;
/*****************************************************************************
 * RomeGraph.java
 *
 * Zugriff auf die Rome Graph Bibliothek. Die Arbeit des Zugriffs auf das
 * ZipFile wird von ZipGraphLibrary erledigt. Hier muss im wesentlichen nur
 * generate (InputStream) implementiert werden.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: RomeGraph.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class RomeGraph extends ZipGraphLibrary {

	/* Name des Zip-Files */

	String getZipFileName() {
		return "rome-lib.zip";
	}

	/* Name des Graph-Generators */

	public String getName() {
		return "Rome Graph Library";
	}

	/*************************************************************************
	 ** Generiere Graph
	 *************************************************************************/

	void generate(InputStream inStream) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

		// Ignoriere einleitende Kommentarzeilen

		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.equals("#"))
				break;
		}

		// Parse restliche Zeilen

		for (String line = in.readLine(); line != null; line = in.readLine()) {

			String[] tokens = line.split(" ");
			int a = Integer.parseInt(tokens[2]);
			int b = Integer.parseInt(tokens[3]);
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
		}
	}
}
