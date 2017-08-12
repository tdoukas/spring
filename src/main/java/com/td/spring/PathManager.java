package com.td.spring;
/*****************************************************************************
 * PathManager.java
 *
 * Erzeugung und Verwaltung von starren Kantenzuegen (Oberklasse).
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: PathManager.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

abstract class PathManager implements ControlProvider {

	protected Random random = new Random();
	protected Graph graph;

	private double[] dist = new double[3]; // fuer compDistance()

	/* Fuer das Management der Pfade */

	protected boolean pathsChanged = false;
	protected boolean pathsNeedMark = true;
	protected Vector<Path> paths = new Vector<Path>();
	// Schattenvariable, verhindert konkurrierende Modifikation der Pfadliste
	private Vector<Path> thePaths = new Vector<Path>();

	/* Fuer das Management der Fokus-Regionen */

	// Anzahl der noch abrufbaren Knoten
	private int verticesAvailable = 0;

	// Zeit (Anzahl der Iterationen), fuer die die aktuelle Auswahl von Fokus-
	// Knoten noch gueltig ist
	private int verticesAvailableFor = 0;

	// Groesse der Fokusregion
	private int focusRegion = 1;

	// Alle Knoten des Graphen, nach Fokus-Relevanz sortiert
	private Vector<Vertex> focussedVertices;

	/*************************************************************************
	 * Von Unterklasse zu implementieren:
	 *************************************************************************/

	/* Name des Pfadmanagers */

	abstract public String getName();

	/* Fuehre einen Management-Schritt aus, wird regelmaessig aufgerufen */

	abstract protected void doManage();

	/*************************************************************************
	 ** Pfade hinzufuegen und loeschen
	 *************************************************************************/

	/* Fuege path hinzu */

	protected void addPath(Path path) {
		synchronized (this) {
			paths.add(path);
			pathsNeedMark = true;
			pathsChanged = true;
		}
	}

	/* Gebe Pfad an Stelle nr */

	protected Path getPath(int nr) {
		synchronized (this) {
			return paths.elementAt(nr);
		}
	}

	/* Entferne Pfad von Stelle nr */

	protected void removePath(int nr) {
		synchronized (this) {
			paths.remove(nr);
			pathsNeedMark = true;
			pathsChanged = true;
		}
	}

	/* Entferne path */

	protected void removePath(Path path) {
		synchronized (this) {
			paths.remove(path);
			pathsNeedMark = true;
			pathsChanged = true;
		}
	}

	/* Entferne zuletzt hinzugefuegten Pfad */

	protected void removeLastPath() {
		synchronized (this) {
			int num = numPaths();
			if (num > 0)
				removePath(num - 1);
		}
	}

	/* Entferne alle Pfade */

	protected void clearPaths() {
		synchronized (this) {
			paths.clear();
			pathsNeedMark = true;
			pathsChanged = true;
		}
	}

	/* Gib Anzahl der Pfade */

	protected int numPaths() {
		synchronized (this) {
			return paths.size();
		}
	}

	/* Loesche aktuelle Pfade und fuege alle Pfade aus paths hinzu */

	protected void setPaths(Vector<Path> paths) {
		synchronized (this) {
			clearPaths();
			for (Path path : paths)
				addPath(new Path(path));
			pathsNeedMark = true;
			pathsChanged = true;
		}
	}

	/*************************************************************************
	 ** Oeffentliche get/set-Methoden
	 *************************************************************************/

	public void setGraph(Graph graph) {
		this.graph = graph;
		clearPaths();
		verticesAvailable = 0;
	}

	public String toString() {
		return getName();
	}

	/* Hat sich die Pfadliste geaendert? */

	public boolean getPathsChanged() {
		synchronized (this) {
			boolean changed = pathsChanged;
			pathsChanged = false;
			return changed;
		}
	}

	/*
	 * Gebe Pfadliste, verwende dazu Schattenvariable; so wird verhindert, dass eine
	 * Liste zurueckgegeben wird, die von einem anderen Thread gerade modifiziert
	 * wird.
	 */

	public Vector<Path> getPaths() {
		return thePaths;
	}

	/*************************************************************************
	 ** Pfade managen
	 *************************************************************************/

	/*
	 * Haupt-Einsprungspunkt innerhalb der Iteration: Wird von Model bei jeder
	 * Iteration aufgerufen
	 */

	public void manage() {

		synchronized (this) {

			if (graph == null)
				return;

			// Gueltigkeitsdauer fuer Problemknoten herunterzaehlen

			if (verticesAvailableFor-- < 0)
				setupFocussedVertices();

			// Management der konkreten Instanz aufrufen

			doManage();

			thePaths = new Vector<Path>(paths);

			// Zum Schluss: Pfade rot markieren, wenn noetig

			maybeMarkPaths();
		}
	}

	/* Markiere Pfade wenn noetig */

	private void maybeMarkPaths() {
		if (pathsNeedMark) {
			graph.unmark(1);
			for (Path path : paths)
				path.maybeMark(1);
			pathsNeedMark = false;
		}
	}

	/*
	 * Wird von Unterklasse implementiert: Pfadmanager in Ausgangszustand versetzen.
	 */

	public void reset() {
	}

	/*************************************************************************
	 ** Pfad erzeugen. Die Methoden werden von Unterklassen benutzt.
	 *************************************************************************/

	/*
	 * Erzeuge einen Pfad der maximalen Laenge maxLength, ausgehend von v in zwei
	 * Richtungen, so dass das Skalarprodukt der Richtungen moeglichst nah an ideal
	 * ist.
	 */

	protected Path buildPath2(Vertex v, int maxLength, double ideal) {

		Vector<Edge> edges = graph.edgesFor(v);

		int n = edges.size();

		// Nur, wenn Knotengrad >= 2

		if (n < 2)
			return null;

		// Lokale Klasse, speichert zwei Kanten und einen "Wert" des Paares

		class EdgePair {
			Edge e1, e2;
			double value;

			EdgePair(Edge e1, Edge e2, double value) {
				this.e1 = e1;
				this.e2 = e2;
				this.value = value;
			}
		}

		Vector<EdgePair> edgePairs = new Vector<EdgePair>();

		// Stelle alle moeglichen Kantenpaare ausgehend von v zusammen
		// Gewichte mit der Differenz ihres normalisierten Skalarprodukt vom
		// gewuenschten Wert (ideal)

		for (int i = 0; i < n - 1; i++)
			for (int j = i + 1; j < n; j++) {

				Edge ei = edges.elementAt(i);
				Edge ej = edges.elementAt(j);
				Vertex u = ei.leadsTo(v);
				Vertex w = ej.leadsTo(v);

				double p = Util.normalizedInnerProduct(v, u, v, w);
				double diff = Math.abs(p - ideal);

				edgePairs.add(new EdgePair(ei, ej, diff));
			}

		// Sortiere die Liste: beste Paare nach vorne

		try {
			Collections.sort(edgePairs, new Comparator<EdgePair>() {
				public int compare(EdgePair ep1, EdgePair ep2) {
					return (int) (ep1.value - ep2.value);
				}
			});
		} catch (Exception e) {
		}

		// Nimm das erste Paar, das keine Markierung traegt

		EdgePair edgePair = null;
		while (edgePairs.size() > 0) {
			EdgePair ep = edgePairs.remove(0);
			if (ep.e1.mark == 0 && ep.e2.mark == 0) {
				edgePair = ep;
				break;
			}
		}

		if (edgePair == null)
			return null;

		Edge e1 = edgePair.e1;
		Edge e2 = edgePair.e2;

		// Erweitere Pfade in die Richtungen e1 und e2

		int halfLength = Math.max(1, maxLength / 2);
		Path p1 = buildPathAlong(v, e1, halfLength);
		Path p2 = buildPathAlong(v, e2, halfLength);

		if (p1 == null || p2 == null)
			return null;

		// Fuege beide Pfade zusammen

		p1.reverse();

		if (p1.vEnd != p2.vStart)
			System.out.println("bugger");
		if (p1.mayAppend(p2)) {
			p1.append(p2);
			p1.setInitialDistance();
			return p1;
		}

		return null;
	}

	/*
	 * Erzeuge Pfad ausgehend von v in Richtung e, mit maximaler laenge maxLength
	 */

	protected Path buildPathAlong(Vertex v, Edge e, int maxLength) {

		Path path = new Path(v);
		while (maxLength-- > 0) {
			v = e.leadsTo(v);
			if (path.contains(v))
				break;
			path.add(e, v);
			e = bestFollowingEdge(v, e);
			if (e == null || e.mark > 0)
				break;
		}

		if (path.length() >= 1) {
			path.setInitialDistance();
			return path;
		}

		return null;
	}

	/*
	 * Suche fuer einen Knoten v die Kante, deren Richtung am besten mit der von e0
	 * uebereinstimmt
	 */

	private Edge bestFollowingEdge(Vertex v, Edge e0) {

		// Richtung bestimmen

		Vertex v0 = e0.leadsTo(v);
		Util.compDistance(v, v0, dist);

		double ex = dist[0], ey = dist[1], e2 = dist[2];
		double er = Math.sqrt(e2);
		ex /= er;
		ey /= er;

		// Sammle alle in Frage kommenden Kanten, gewichte mit Skalarprodukt

		Vector<Edge> edges = new Vector<Edge>(graph.edgesFor(v));
		for (Edge e : edges) {

			Vertex u = e.leadsTo(v);
			Util.compDistance(u, v, dist);

			double dx = dist[0], dy = dist[1], d2 = dist[2];
			double dr = Math.sqrt(d2);
			dx /= dr;
			dy /= dr;

			double iProd = ex * dx + ey * dy;
			e.weight = iProd;
		}

		// Sortiere: beste Kante nach vorne

		Collections.sort(edges, new Comparator<Edge>() {
			public int compare(Edge e1, Edge e2) {
				if (e1.weight < e2.weight)
					return 1;
				if (e1.weight > e2.weight)
					return -1;
				return 0;
			}
		});

		// Nimm beste Kante

		return edges.elementAt(0);
	}

	/*************************************************************************
	 ** Knotenauswahl ("Problemknoten")
	 *************************************************************************/

	/* Gebe Problemknoten zurueck */

	protected Vertex getFocussedVertex() {

		// Berechne Fokusknoten wenn noetig

		if (focussedVertices == null || verticesAvailable == 0 || verticesAvailableFor <= 0)
			setupFocussedVertices();

		verticesAvailable--;

		// Suche zufaellig einen Knoten aus der Fokusregion aus

		return focussedVertices.elementAt(random.nextInt(Math.min(focusRegion, focussedVertices.size())));
	}

	/* Aktualisiere Liste der Problemknoten */

	private void setupFocussedVertices() {

		double[] dist = new double[3];
		Vector<Vertex> vertices = new Vector<Vertex>(graph.v);

		// Iteriere ueber alle (!) Knoten v und ueber alle von v ausgehenden
		// Kanten. Bestimme die Summe aller normierten Richtungsvektoren.
		// Gewicht der Kante entspricht der Laenge der Resultierenden.

		for (Vertex v : vertices) {
			v.weight = 0;
			v.highlight = false;
			double lx = 0, ly = 0;
			for (Edge e : graph.edgesFor(v)) {
				Vertex u = e.leadsTo(v);
				Util.compDistance(u, v, dist);
				double dx = dist[0], dy = dist[1], d2 = dist[2];
				double d = Math.sqrt(d2);
				dx /= d;
				dy /= d;
				lx += dx;
				ly += dy;
			}
			v.weight = Math.sqrt(lx * lx + ly * ly);
		}

		// Sortiere: Knoten mit moeglichst asymmetrischer Kantenverteilung
		// nach vorne.

		Collections.sort(vertices, new Comparator<Vertex>() {
			public int compare(Vertex u, Vertex v) {
				if (u.weight < v.weight)
					return 1;
				if (u.weight > v.weight)
					return -1;
				return 0;
			}
		});

		// Fokusregion: max. 20 Knoten

		focusRegion = Math.min(20, vertices.size());

		// Setze highlight

		for (int i = 0; i < focusRegion; i++)
			vertices.elementAt(i).highlight = true;

		// Speichere Liste der fokussierten Knoten

		focussedVertices = vertices;
		verticesAvailable = focusRegion;
		verticesAvailableFor = 100;
	}
}
