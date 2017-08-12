package com.td.spring;
/*****************************************************************************
 * MLGraph.java
 *
 * Zugriff auf die Graphen in graphml.zip.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: MLGraph.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

class MLGraph extends ZipGraphLibrary {

	/* Name des Graph-Generators */

	public String getName() {
		return "GraphML";
	}

	/* Name des Zipfiles */

	String getZipFileName() {
		return "graphml.zip";
	}

	/*************************************************************************
	 ** Generiere Graph
	 *************************************************************************/

	/* Generiere Graph aus InputStream, siehe ZipGraphLibrary */

	void generate(InputStream in) throws IOException {
		try {
			generate(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Generiere Graph aus einem XML-Dokument */

	private void generate(Document doc) {

		NodeList elements = doc.getElementsByTagName("edge");
		for (int i = 0; i < elements.getLength(); i++) {
			NamedNodeMap attributes = elements.item(i).getAttributes();
			int a = Integer.parseInt(attributes.getNamedItem("source").getNodeValue());
			int b = Integer.parseInt(attributes.getNamedItem("target").getNodeValue());
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
