package com.td.spring;
/*****************************************************************************
 * GraphicsDrawContext.java
 *
 * Kontext fuer eine Zeichnung auf java.awt.Graphics.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: GraphicsDrawContext.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

class GraphicsDrawContext extends DrawContext {

	private Graphics2D g; // Das Java-Graphics-Objekt, auf dem gezeichnet wird

	/*
	 * Konstruktor: uebergebe neben Java-Graphics-Objekt noch Abmessungen der
	 * Zeichenebene sowie antialias-Einstellung
	 */

	public GraphicsDrawContext(Graphics2D g, int width, int height, boolean antialias) {
		super(width, height);
		this.g = g;

		if (antialias)
			g.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
	}

	/*************************************************************************
	 ** Implementierung der Zeichenprimitiven, siehe DrawContext.
	 *************************************************************************/

	public void begin() {
		g.setColor(bgColor);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.black);
	}

	public void end() {
	}

	public void drawEdge(int x1, int y1, int x2, int y2) {
		if (drawHalo) {
			g.setColor(bgColor);
			g.setStroke(style.getHaloStroke());
			g.drawLine(x1, y1, x2, y2);
		}
		drawLine(x1, y1, x2, y2);
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		g.setColor(style.getLineColor());
		g.setStroke(style.getStroke());
		g.drawLine(x1, y1, x2, y2);
	}

	public void drawVertex(int x, int y) {

		int s = style.getVertexSize();
		if (drawHighlight)
			drawVertex(x, y, s + 2, highlightColor);
		drawVertex(x, y, s, style.getVertexColor());
	}

	public void drawString(String text, int x, int y) {
		g.setColor(style.getVertexColor());
		g.drawString(text, x, y);
	}

	private void drawVertex(int x, int y, int s, Color color) {
		g.setColor(color);

		int d = s * 2;
		if (roundVertex)
			g.fillOval(x - s, y - s, d, d);
		else
			g.fillRect(x - s, y - s, d, d);
	}
}
