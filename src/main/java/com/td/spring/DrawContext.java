package com.td.spring;
/*****************************************************************************
 * DrawContext.java
 *
 * Kontext fuer eine Zeichnung. Stellt eine Abstraktion der Zeichenfunktionen
 * zur Verfuegung, so dass aus demselben Programmablauf sowohl die Zeichnung
 * auf dem Bildschirm, als auch Zeichnungen fuer Speicherformate erstellt
 * werden.
 *
 * Enthaelt ebenfalls die Definition der Style-Klasse, siehe weiter unten.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: DrawContext.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.Vector;

abstract class DrawContext {

	/* Zustand */

	protected int width, height;
	protected Color bgColor, highlightColor;
	protected boolean drawHalo = false;
	protected boolean drawHighlight = false;
	protected boolean roundVertex = false;
	protected Vector<Style> styles = new Vector<Style>();
	protected Style style;

	/* Konstruktor: initialisiere Kontext */

	DrawContext(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/* Gebe Breite und Hoehe der Zeichenflaeche */

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/* Setze Attribute des Zustands */

	public void setBackgroundColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	public void setHighlightColor(Color highlightColor) {
		this.highlightColor = highlightColor;
	}

	public void setDrawHalo(boolean drawHalo) {
		this.drawHalo = drawHalo;
	}

	public void setRoundVertex(boolean roundVertex) {
		this.roundVertex = roundVertex;
	}

	public void setHighlight(boolean drawHighlight) {
		this.drawHighlight = drawHighlight;
	}

	public void defineStyle(Style style) {
		styles.add(style);
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	/* Zeichenprimitive, von Unterklasse zu implementieren */

	public abstract void begin();

	public abstract void end();

	public abstract void drawEdge(int x1, int y1, int x2, int y2);

	public abstract void drawLine(int x1, int y1, int x2, int y2);

	public abstract void drawVertex(int x, int y);

	public abstract void drawString(String text, int x, int y);
}

/*****************************************************************************
 ** Style-Klasse: abstrahiert Farben, Punktgroessen und Linienbreiten
 *****************************************************************************/

class Style {

	/* Attribute eines Style */

	private String name;
	private Color vertexColor;
	private int vertexSize;
	private Color lineColor;
	private int lineWidth;
	private Stroke stroke;
	private Stroke haloStroke;

	/* Konstruiere Style */

	Style(String name, Color vertexColor, int vertexSize, Color lineColor, int lineWidth) {
		this.name = name;
		this.vertexColor = vertexColor;
		this.vertexSize = vertexSize;
		this.lineColor = lineColor;
		this.lineWidth = lineWidth;
	}

	/* Gebe Attribute des Style zurueck */

	public String getName() {
		return name;
	}

	public String getHaloName() {
		return name + "_halo";
	}

	public Color getVertexColor() {
		return vertexColor;
	}

	public int getVertexSize() {
		return vertexSize;
	}

	public Color getLineColor() {
		return lineColor;
	}

	public int getLineWidth() {
		return lineWidth;
	}

	public Stroke getStroke() {
		if (stroke == null)
			stroke = new BasicStroke(lineWidth);
		return stroke;
	}

	public Stroke getHaloStroke() {
		if (haloStroke == null)
			haloStroke = new BasicStroke(lineWidth + 3);
		return haloStroke;
	}
}
