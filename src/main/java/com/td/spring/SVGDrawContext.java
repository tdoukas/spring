package com.td.spring;
/*****************************************************************************
 * SVGDrawContext.java
 *
 * Kontext fuer eine Zeichnung als SVG-Export.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: SVGDrawContext.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.awt.Color;
import java.io.PrintStream;

class SVGDrawContext extends DrawContext {

	private PrintStream out; // Hierhin werden die Daten geschrieben

	public SVGDrawContext(PrintStream out, int width, int height) {
		super(width, height);
		this.out = out;
	}

	/*************************************************************************
	 ** Implementierung der Zeichenprimitiven, siehe DrawContext.
	 *************************************************************************/

	public void begin() {

		// Schreibe einleitenden svg-Tag

		out.println(
				String.format("<svg xmlns='http://www.w3.org/2000/svg' " + "width='%d' height='%d'>", width, height));

		// Schreibe stylesheet

		out.println("<style type='text/css'>");
		for (Style style : styles) {
			out.println("." + style.getName() + " { fill: " + colorString(style.getVertexColor()) + "; stroke: "
					+ colorString(style.getLineColor()) + "; stroke-width: " + style.getLineWidth() + "; }");
			out.println("." + style.getHaloName() + " { stroke: " + colorString(bgColor) + "; stroke-width: "
					+ (style.getLineWidth() + 3) + "; }");
		}
		out.println(".highlight { fill: " + colorString(highlightColor) + "; }");
		out.println(".background { fill: " + colorString(bgColor) + "; }");
		out.println("</style>");

		out.println(String.format("<rect class='background' x='0' y='0' width='%d' height='%d'/>", width, height));
	}

	public void end() {
		out.println("</svg>");
	}

	public void drawEdge(int x1, int y1, int x2, int y2) {
		if (drawHalo)
			drawLine(x1, y1, x2, y2, style.getHaloName());
		drawLine(x1, y1, x2, y2);
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		drawLine(x1, y1, x2, y2, style.getName());
	}

	public void drawVertex(int x, int y) {
		if (drawHighlight)
			drawVertex(x, y, style.getVertexSize() + 5, "highlight");
		drawVertex(x, y, style.getVertexSize(), style.getName());
	}

	/*
	 * Dies sind die eigentlichen SVG-Primitive, also diejenigen, die auch eine
	 * Ausgabe im Ausgabestrom out erzeugen.
	 */

	public void drawString(String text, int x, int y) {
		out.println(String.format("<text class='%s' x='%d' y='%d'>%s</text>", style.getName(), x, y, text));
	}

	private String colorString(Color color) {
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	private void drawVertex(int x, int y, int r, String cls) {
		if (roundVertex)
			out.println(String.format("<circle class='%s' cx='%d' cy='%d' r='%d' />", cls, x, y, r));
		else
			out.println(String.format("<rect class='%s' x='%d' y='%d' " + "width='%d' height='%d' />", cls, x - r,
					y - r, 2 * r, 2 * r));
	}

	private void drawLine(int x1, int y1, int x2, int y2, String cls) {
		out.println(String.format("<line class='%s' x1='%d' y1='%d' x2='%d' y2='%d' />", cls, x1, y1, x2, y2));
	}
}
