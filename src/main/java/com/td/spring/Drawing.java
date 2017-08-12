package com.td.spring;
/*****************************************************************************
 * Drawing.java
 *
 * Das Zeichenfenster der Anwendung.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Drawing.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;

class Drawing extends JPanel implements MouseListener {

	/*************************************************************************
	 ** Variablen und Konstanten
	 *************************************************************************/

	/* Externe Komponenten */

	private Graph graph;

	/* Bildgeometrie, die Werte werden von setupGeometryMapping() berechnet */

	private int screenCenterX, screenCenterY; // Bildschirm
	private double centerX, centerY; // Graph
	private double scaleX, scaleY; // Verhaeltnis Bildschirm / Graph
	private double minX, minY, maxX, maxY; // Bounding Box des Graphen

	/* Merkmale der Zeichnung */

	private boolean antialias = true;
	private boolean drawVertices = true;
	private boolean highlight = false;
	private boolean showMark = true;
	private boolean roundVertices = false;
	private boolean drawGrid = true;
	private boolean keepAspect = true;
	private boolean drawGridText = true;
	private boolean drawEdgeGroups = false;
	private boolean drawHalo = true;
	private boolean drawForces = false;

	private final double visibleAmount = 0.95; // Leerer Rand in der Zeichnung
	private final double maxForceLength = 0.2;
	private Color bgColor = new Color(250, 250, 210);
	private Color highlightColor = new Color(255, 255, 0);

	/*
	 * Styles, siehe auch DrawContext. Styles fassen Farbe, Punktgroesse,
	 * Linienbreite unter einem Namen zusammen.
	 */

	private Style regularStyle = new Style("regular", new Color(0, 0, 0), 3, new Color(50, 50, 50), 1);
	private Style systemStyle = new Style("system", new Color(255, 0, 0), 4, new Color(255, 0, 0), 3);
	private Style selectedStyle = new Style("selected", new Color(0, 255, 0), 6, new Color(0, 255, 0), 5);
	private Style userStyle = new Style("user", new Color(0, 180, 0), 5, new Color(0, 180, 0), 4);
	private Style gridStyle = new Style("grid", new Color(250, 220, 180), 1, new Color(250, 220, 180), 1);
	private Style[] indexStyles = new Style[] { regularStyle, systemStyle, selectedStyle, userStyle };

	/* Fuer die Pfadauswahl */

	private Path selectedPath;
	private Vector<Path> selectedPaths = new Vector<Path>();
	private JPanel buttonPanel = new JPanel();

	/* Misst die zum Zeichnen benoetigte Zeit */

	private StopWatch watch = new StopWatch();

	/*************************************************************************
	 ** Konstruktor und ueberschriebene Methoden der Superklasse
	 *************************************************************************/

	/* Erzeuge Drawing-Objekt */

	public Drawing() {
		addMouseListener(this);
		setLayout(new BorderLayout());
		buttonPanel.setOpaque(false);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		add(buttonPanel, BorderLayout.NORTH);
		updateButtonPanel();
	}

	/* Ueberschreibe JPanel: bevorzugte Groesse */

	public Dimension getPreferredSize() {
		return new Dimension(800, 800);
	}

	/* Ueberschreibe JPanel: Zeichne diese Swing-Komponente */

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Application.getInstance().paintDrawingSynchronous(g);
	}

	public void doPaintComponent(Graphics g) {
		if (graph != null) {
			watch.start();
			draw((Graphics2D) g, getWidth(), getHeight());
			watch.stop();
			doNotifyScale();
		}
	}

	/*
	 * Teile der Application den aktuellen Scale-Wert mit (wird nur von obigem
	 * paintComponent aufgerufen)
	 */

	private void doNotifyScale() {
		double dx = maxX - minX;
		double dy = maxY - minY;
		Application.getInstance().notifyScale(Math.sqrt(dx * dx + dy * dy));
	}

	/*************************************************************************
	 ** Verschiedene Getter und Setter
	 *************************************************************************/

	public int getDrawTime() {
		return watch.meanValue();
	}

	public Vector<Path> getSelectedPaths() {
		return new Vector<Path>(selectedPaths);
	}

	public boolean getAntialias() {
		return antialias;
	}

	public void setAntialias(boolean antialias) {
		this.antialias = antialias;
	}

	public boolean getHighlight() {
		return highlight;
	}

	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

	public boolean getDrawForces() {
		return drawForces;
	}

	public void setDrawForces(boolean drawForces) {
		this.drawForces = drawForces;
	}

	public boolean getKeepAspect() {
		return keepAspect;
	}

	public void setKeepAspect(boolean keepAspect) {
		this.keepAspect = keepAspect;
	}

	public boolean getDrawGrid() {
		return drawGrid;
	}

	public void setDrawGrid(boolean drawGrid) {
		this.drawGrid = drawGrid;
	}

	public boolean getDrawGridText() {
		return drawGridText;
	}

	public void setDrawGridText(boolean drawGridText) {
		this.drawGridText = drawGridText;
	}

	public void setDrawEdgeGroups(boolean drawEdgeGroups) {
		this.drawEdgeGroups = drawEdgeGroups;
	}

	public boolean getDrawEdgeGroups() {
		return drawEdgeGroups;
	}

	public boolean getDrawHalo() {
		return drawHalo;
	}

	public void setDrawHalo(boolean drawHalo) {
		this.drawHalo = drawHalo;
	}

	public boolean getShowMark() {
		return showMark;
	}

	public void setShowMark(boolean showMark) {
		this.showMark = showMark;
	}

	public Vertex getSelectedVertex() {
		if (selectedPath != null && selectedPath.length() == 0 && selectedPath.vStart != null)
			return selectedPath.vStart;
		return null;
	}

	void setGraph(Graph graph) {
		clearSelectedPaths();
		this.graph = graph;
	}

	/*************************************************************************
	 ** Die Zeichnung selbst: gezeichnet wird auf einem DrawContext
	 *************************************************************************/

	/*
	 * Fertige Zeichnung an auf gegebenem DrawContext, unabhaengig vom eigentlichen
	 * Ausgabemedium (java.awt.Graphics, oder auch SVG-Export)
	 */

	synchronized private void draw(DrawContext c) {

		// Bereite Styles vor

		for (int i = 0; i < indexStyles.length; i++)
			c.defineStyle(indexStyles[i]);
		c.defineStyle(gridStyle);

		// Setze Attribute

		c.setBackgroundColor(bgColor);
		c.setHighlightColor(highlightColor);
		c.setDrawHalo(drawHalo);
		c.setHighlight(false);
		c.setRoundVertex(roundVertices);

		// Start

		c.begin();

		// Bestimme Geometrie

		setupGeometryMapping(c.getWidth(), c.getHeight(), graph.getBounds());

		// Zeichne Gitterlinien

		if (drawGrid)
			drawGrid(c);

		// Zeichne Kanten

		for (Edge e : graph.e) {

			double x1 = e.v1.x, y1 = e.v1.y, x2 = e.v2.x, y2 = e.v2.y;
			int mx1 = mapX(x1), my1 = mapY(y1), mx2 = mapX(x2), my2 = mapY(y2);
			int mark = showMark ? e.mark : 0;
			c.setStyle(indexStyles[mark]);
			c.drawEdge(mx1, my1, mx2, my2);
		}

		// Zeichne Kraefte

		if (drawForces) {
			double max2 = 0;
			for (Vertex v : graph.v) {
				double f2 = v.fx * v.fx + v.fy * v.fy;
				max2 = Math.max(max2, f2);
			}
			double f = Math.sqrt(max2);
			double sx = (maxX - minX) * maxForceLength;
			double sy = (maxY - minY) * maxForceLength;
			c.setStyle(systemStyle);
			for (Vertex v : graph.v) {
				double d = Math.sqrt(v.fx * v.fx + v.fy * v.fy);
				double ex = v.fx / d;
				double ey = v.fy / d;
				double fx = ex * sx * d / f;
				double fy = ey * sy * d / f;
				c.drawLine(mapX(v.x), mapY(v.y), mapX(v.x + fx), mapY(v.y + fy));
			}
		}

		// Zeichne Knoten

		if (drawVertices)
			for (Vertex v : graph.v) {
				int x = mapX(v.x), y = mapY(v.y);
				int mark = showMark ? v.mark : 0;
				c.setStyle(indexStyles[mark]);
				c.setHighlight(highlight && v.highlight);
				c.drawVertex(x, y);
			}

		// Beschrifte Kanten

		if (drawEdgeGroups) {
			c.setStyle(regularStyle);
			for (Edge e : graph.e) {

				double x1 = e.v1.x, y1 = e.v1.y, x2 = e.v2.x, y2 = e.v2.y;
				int mx1 = mapX(x1), my1 = mapY(y1), mx2 = mapX(x2), my2 = mapY(y2);
				int mark = showMark ? e.mark : 0;
				c.drawString(e.group + "", (mx1 + mx2) / 2, (my1 + my2) / 2);
			}
		}

		// Zeichnung fertig

		c.end();
	}

	/* Zeichne die Gitterlinien des Hintergrundes */

	private void drawGrid(DrawContext c) {

		c.setStyle(gridStyle);

		int mag = Math.min((int) Math.floor(Math.log10(maxX - minX)), (int) Math.floor(Math.log10(maxY - minY)));
		double step = Math.pow(10, mag) / 2;
		int width = c.getWidth(), height = c.getHeight();

		// Vertikale Linien

		double xStart = ((int) (invMapSX(0) / step)) * step;
		double xEnd = invMapSX(width);
		int n = 0;
		for (double mx = xStart; mx < xEnd; mx += step) {
			int x = mapX(mx);
			c.drawLine(x, 0, x, height);
			if (mx == 0) {
				c.drawLine(x - 1, 0, x - 1, height);
			}
			if (drawGridText)
				if (mx % (step * 2) == 0)
					c.drawString(String.format("%.2e", mx), x + 1, 20);
		}

		// Horizontale Linien

		double yStart = ((int) (invMapSY(0) / step)) * step;
		double yEnd = invMapSY(height);
		for (double my = yStart; my < yEnd; my += step) {
			int y = mapY(my);
			c.drawLine(0, y, width, y);
			if (my == 0) {
				c.drawLine(0, y + 1, width, y + 1);
			}
			if (drawGridText)
				if (my % (step * 2) == 0)
					c.drawString(String.format("%.2e", my), 2, y - 1);
		}
	}

	/* Zeichnung auf externer Bitmap (-> PNG export) */

	public void draw(BufferedImage image) {
		boolean rv = roundVertices;
		roundVertices = true;
		draw(image.createGraphics(), image.getWidth(), image.getHeight());
		roundVertices = rv;
	}

	/* Zeichnung in Stream (-> SVG export) */

	public void draw(OutputStream out, int width, int height) {
		boolean rv = roundVertices;
		roundVertices = true;
		draw(new SVGDrawContext(new PrintStream(out), width, height));
		roundVertices = rv;
	}

	/* Zeichnung auf Graphics2D-Objekt */

	private void draw(Graphics2D g, int width, int height) {
		draw(new GraphicsDrawContext(g, width, height, antialias));
	}

	/*************************************************************************
	 ** Projektion Graph-Koordinaten <-> Bildschirm-Koordinaten
	 *************************************************************************/

	/*
	 * Bestimme Geometrie der Zeichnung (centerX/Y, scaleX/Y) fuer die Abbildung von
	 * Graph-Koordinaten auf Bildschirmkoordinaten. bounds ist ein Array mit den
	 * Werten (minX, minY, maxX, maxY), d.h. die Bounding-Box des Graphen (siehe
	 * Graph.getBounds())
	 */

	private void setupGeometryMapping(int screenWidth, int screenHeight, double[] bounds) {

		minX = bounds[0];
		minY = bounds[1];
		maxX = bounds[2];
		maxY = bounds[3];

		int halfScreenWidth = screenWidth / 2;
		int halfScreenHeight = screenHeight / 2;
		screenCenterX = halfScreenWidth;
		screenCenterY = halfScreenHeight;

		double halfWidth = (maxX - minX) / 2;
		double halfHeight = (maxY - minY) / 2;
		centerX = minX + halfWidth;
		centerY = minY + halfHeight;

		scaleX = halfScreenWidth / halfWidth;
		scaleY = halfScreenHeight / halfHeight;

		if (keepAspect)
			scaleX = scaleY = Math.min(scaleX, scaleY);

		scaleX *= visibleAmount;
		scaleY *= visibleAmount;

		if (drawForces) {
			double s = 1 - maxForceLength;
			scaleX *= s;
			scaleY *= s;
		}
	}

	/* Abbildung Graph -> Zeichnung */

	private int mapX(double x) {
		return screenCenterX + (int) ((x - centerX) * scaleX);
	}

	private int mapY(double y) {
		return screenCenterY + (int) ((y - centerY) * scaleY);
	}

	/* Abbildung Zeichnung -> Graph */

	private double invMapSX(int sx) {
		return (sx - screenCenterX) / scaleX + centerX;
	}

	private double invMapSY(int sy) {
		return (sy - screenCenterY) / scaleY + centerY;
	}

	/* Besimme Knoten an Bildschirmposition (sx,sy) */

	private Vertex vertexAt(int sx, int sy) {

		double x = invMapSX(sx);
		double y = invMapSY(sy);
		Vertex nearestVertex = null;
		double nearestD2 = 0;
		for (Vertex v : graph.v) {

			double dx = v.x - x;
			double dy = v.y - y;
			double d2 = dx * dx + dy * dy;
			if (nearestVertex == null || d2 < nearestD2) {
				nearestVertex = v;
				nearestD2 = d2;
			}
		}
		return nearestVertex;
	}

	/*************************************************************************
	 ** Pfadauswahl
	 *************************************************************************/

	/*
	 * Aktualisiere den Inhalt des Button-Panels, d.h. kontextabhaengig die drei
	 * Buttons "+", "C" und "AC".
	 */

	private void updateButtonPanel() {
		buttonPanel.removeAll();
		if (selectedPath != null) {
			JButton clearButton = new JButton("C");
			buttonPanel.add(clearButton);
			clearButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					clearSelectedPath();
				}
			});
		}
		if (selectedPaths.size() > 0) {
			JButton clearAllButton = new JButton("AC");
			buttonPanel.add(clearAllButton);
			clearAllButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					clearSelectedPaths();
				}
			});
		}
		if (selectedPath != null && selectedPath.vEnd != null) {
			JButton storeButton = new JButton("+");
			buttonPanel.add(storeButton);
			storeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectedPath.mark(3);
					selectedPath.setInitialDistance();
					selectedPaths.add(selectedPath);
					selectedPath = null;
					graph.unmark(2);
					updateButtonPanel();
					repaint();
					Application.getInstance().notifyPathsChanged();
				}
			});
		}
		revalidate();
	}

	/* Entferne den gerade in Konstruktion befindlichen Pfad */

	private void clearSelectedPath() {
		selectedPath = null;
		graph.unmark(2);
		updateButtonPanel();
		repaint();
	}

	/* Entferne alle vom Benutzer selektierten Pfade */

	private void clearSelectedPaths() {
		selectedPath = null;
		selectedPaths.clear();
		if (graph != null) {
			graph.unmark(2);
			graph.unmark(3);
		}
		updateButtonPanel();
		repaint();
		Application.getInstance().notifyPathsChanged();
	}

	/*************************************************************************
	 ** MouseListener-Implementation zur Pfadauswahl
	 *************************************************************************/

	/*
	 * Maus im Zeichenfenster geklickt: Bestimme gewaehlten Knoten und fuehre
	 * Selektierung aus
	 */

	public void mouseClicked(MouseEvent e) {
		graph.unmark(2);

		Vertex v = vertexAt(e.getX(), e.getY());
		if (v != null) {
			if (selectedPath == null)
				selectedPath = new Path(v);
			else if (v != selectedPath.vStart && !selectedPath.contains(v)) {
				Path path = graph.findShortestPath(selectedPath.vEnd == null ? selectedPath.vStart : selectedPath.vEnd,
						v);
				if (selectedPath.mayAppend(path))
					selectedPath.append(path);
			}
		}
		if (selectedPath != null) {
			selectedPath.mark(2);
			selectedPath.check();
		}
		updateButtonPanel();
		repaint();
	}

	public void mousePressed(MouseEvent event) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}
