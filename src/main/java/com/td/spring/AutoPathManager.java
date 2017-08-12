package com.td.spring;
/*****************************************************************************
 * AutoPathManager.java
 *
 * Erzeugung und Verwaltung von starren Kantenzuegen.
 * Dies ist der PathManager "V3", siehe Abschnitt 6 des Textes.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: AutoPathManager.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Hashtable;
import java.util.Vector;

class AutoPathManager extends PathManager {

	private int counter = 0; // fuer Konvergenzphase
	private Vector<PathSet> pathSets = // Ein Stack fuer die ausprobierten
			new Vector<PathSet>(); // Mengen von Kantenzuegen
	private int currentValue; // aktuelle Qualitaet der Zeichnung
	private int tried; // Aktion (0-2), die gerade versucht wurde
	private int[] fail = new int[3]; // Zaehler fuer missglueckte Versuche
	private int maxFail = 3; // Maximale Anzahl von Fehlversuchen
	private boolean done = false; // Flag wenn fertig

	/* Name des Pfadmanagers */

	public String getName() {
		return "V3";
	}

	/*************************************************************************
	 ** Klasse PathSet: Speichert eine Konfiguration von Kantenzuegen und
	 ** Knotenpositionen
	 *************************************************************************/

	class PathSet {
		Vector<Path> paths;
		Hashtable<Vertex, double[]> vertexPositions = new Hashtable<Vertex, double[]>();
		int value;

		PathSet(Vector<Path> paths, int value) {
			this.paths = new Vector<Path>();
			for (Path path : paths)
				this.paths.add(new Path(path));
			this.value = value;
		}
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private RealControl delayCtrl = // Laenge der Konvergenzphase
			new LogarithmicControl("Delay", "Delay", this, 500, 1, 100000);

	private BoolControl runCtrl = new BoolControl("Run", "Run", this, false);

	private StatusControl statusCtrl = new StatusControl();

	private ButtonControl resetCtrl = new ButtonControl("Reset") {
		public void clicked() {
			uiReset();
		}
	};

	private RealControl degCtrl = new LinearControl("Degrees", "Direction within which edges are considered parallel",
			this, 2.0, 1.0, 20.0, "%2.1f");

	private RealControl dimCtrl = new LinearControl("Box-Dim", "Dimension of bounding box around edge", this, 0.1, 0.02,
			0.5, "%1.2f");

	public Control[] getControls() {
		return new Control[] { statusCtrl, runCtrl, delayCtrl, resetCtrl, degCtrl, dimCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Manager
	 *************************************************************************/

	/*
	 * Haupt-Einsprungspunkt (von Model). Der Manager wird nur jeden n-ten Schritt
	 * aktiv (n = Wert von delayCtrl)
	 */

	public void doManage() {
		synchronized (this) {
			if (counter++ >= 0 && (counter % ((int) delayCtrl.getValue())) == 0) {

				// regelmaessige Vorgaenge (Qualitaet messen)
				maintenance();

				// Pfadauswahl und -Modifikation nur wenn aktiv
				if (runCtrl.getValue())
					manageOneStep();
			}
		}
	}

	/* Versetze den Pfadmanager in seinen Ausgangszustand */

	public void reset() {
		counter = -100;
		pathSets.clear();
		clearPaths();
		fail[0] = fail[1] = fail[2] = 0;
		done = false;
		runCtrl.setValue(false);
	}

	/*************************************************************************
	 ** Ein Schritt
	 *************************************************************************/

	/* Regelmaessig Qualitaet messen, auch wenn nicht aktiv */

	private void maintenance() {
		currentValue = compValue();
	}

	/* Ein Schritt des Manager-Ablaufes */

	private void manageOneStep() {

		if (done) {
			runCtrl.setValue(false);
			return;
		}

		if (pathSets.size() > 0) {

			// Qualitaet der vorherigen Zeichnung

			int prevValue = pathSets.elementAt(pathSets.size() - 1).value;

			// Aktuelle Zeichnung schlechter als vorherige?

			if (currentValue < prevValue) {

				// stelle vorherige Situation wieder her

				setPathSet(pathSets.elementAt(pathSets.size() - 1));

				// Wenn die Verschlechterung eingetreten ist, obwohl kein
				// neuer Versuch unternommen wurde (tried=-1), entferne die
				// wiederhergestellte Situation vom Stapel, d.h. gehe einen
				// Schritt zurueck (das kann passieren, wenn die Konvergenz-
				// phase beim vorherigen Versuch noch nicht abgeschlossen war
				// und eine nachtraegliche Verschlechterung eintritt).

				if (tried == -1)
					pathSets.remove(pathSets.size() - 1);

				// Sonst: Zaehle den Fehlversuch.

				else
					fail[tried]++;

				// Versuch beendet.

				tried = -1;
				return;
			}

			// Zeichnung nicht schlechter: wenn eine Aktion versucht wurde,
			// schreibe einen Versuch gut...

			else if (tried != -1) {
				if (fail[tried] > 0)
					fail[tried]--;

				// ... und setze die Fehlversuche fuer alle darueberliegenden
				// Aktionen wieder auf 0, so dass sie erneut probiert werden.

				for (int i = 0; i < tried; i++)
					fail[i] = 0;
			}
		}

		// Wenn wir bis hierhin gekommen sind und eine Aktion versucht wurde,
		// resultierte sie nicht in einer Verschlechterung -> speichere den
		// neuen Zustand.

		if (tried != -1) {
			tried = -1;
			pathSets.add(getPathSet());
		}

		// Suche eine Aktion aus, die ihre maximale Anzahl von Versuchen noch
		// nicht erreicht hat. Suche in aufsteigender Reihenfolge 0,1,2.

		if (fail[0] < maxFail && tryAddNewPath(2)) {
			tried = 0;
			return;
		} else if (fail[1] < maxFail && tryAugmentPath()) {
			tried = 1;
			return;
		} else if (fail[2] < maxFail && tryConnectPaths()) {
			tried = 2;
			return;
		}

		// Wenn keine Aktion mehr in Frage kommt -> fertig.

		else
			done = true;
	}

	/*
	 * Speichere aktuellen Zustand (Pfade, Vertex-Positionen) in einem PathSet
	 * (getPathSet) bzw. stelle Zustand aus einem PathSet wieder her (setPathSet).
	 */

	private PathSet getPathSet() {
		PathSet ps = new PathSet(this.paths, currentValue);
		for (Vertex v : graph.v)
			ps.vertexPositions.put(v, new double[] { v.x, v.y });
		return ps;
	}

	private void setPathSet(PathSet pathSet) {
		setPaths(pathSet.paths);
		for (Vertex v : graph.v) {
			double[] pos = pathSet.vertexPositions.get(v);
			if (pos != null) {
				v.x = pos[0];
				v.y = pos[1];
			}
		}
	}

	/*************************************************************************
	 ** Aktuellen Wert der Zeichnung bestimmen
	 *************************************************************************/

	private int compValue() {
		int numOverlap = graph.numOverlappingEdges(degCtrl.getValue(), dimCtrl.getValue());
		int visComp = graph.visualComplexity();
		int value = -numOverlap * 3 - visComp;

		String status = "[" + pathSets.size() + "] quality=" + value + " overlap=" + numOverlap + " #=" + visComp;
		if (done)
			status += " done";
		statusCtrl.setText(status);
		return value;
	}

	/*************************************************************************
	 ** Generiere / Modifiziere Kantenzuege Wert in Klammern (0-2) ist der Wert der
	 * tried-Variable (s.o.)
	 *************************************************************************/

	/* (0) Fuege neuen Kantenzug hinzu */

	private boolean tryAddNewPath(int size) {

		Vector<Vertex> vertices = new Vector<Vertex>(graph.v);

		while (vertices.size() > 0) {
			Vertex v = vertices.remove(random.nextInt(vertices.size()));
			Path p = buildPath2(v, size, -1);
			if (p != null) {
				addPath(p);
				return true;
			}
		}

		return false;
	}

	/* (1) Erweitere bestehenden Kantenzug */

	private boolean tryAugmentPath() {

		if (numPaths() == 0)
			return false;

		Vector<Path> myPaths = new Vector<Path>(paths);
		while (myPaths.size() > 0) {

			Path myPath = myPaths.remove(random.nextInt(myPaths.size()));
			Vector<Vertex> verts = new Vector<Vertex>();
			if (Math.random() < 0.5) {
				verts.add(myPath.vStart);
				verts.add(myPath.vEnd);
			} else {
				verts.add(myPath.vEnd);
				verts.add(myPath.vStart);
			}
			for (Vertex v : verts) {

				Vector<Edge> edges = new Vector<Edge>(graph.edgesFor(v));
				while (edges.size() > 0) {

					Edge e = edges.remove(random.nextInt(edges.size()));
					if (e.mark != 0)
						continue;

					if (v == myPath.vStart) {

						Vertex u = e.leadsTo(v);
						if (myPath.contains(u))
							continue;
						Path path = new Path(u);
						path.add(e, v);
						path.append(myPath);
						removePath(myPath);
						addPath(path);
						return true;

					} else { // v = myPath.vEnd

						Vertex u = e.leadsTo(v);
						if (myPath.contains(u))
							continue;
						removePath(myPath);
						myPath.add(e, u);
						addPath(myPath);
						return true;
					}
				}
			}
		}
		return false;
	}

	/* (2) Verbinde zwei Pfade */

	private boolean tryConnectPaths() {

		Path[] paths = findConnectablePaths();
		if (paths == null)
			return false;

		Path p1 = paths[0];
		Path p2 = paths[1];

		removePath(p1);
		removePath(p2);

		if (p1.vStart == p2.vEnd) {
			p2.append(p1);
			addPath(p2);
		} else if (p1.vEnd == p2.vStart) {
			p1.append(p2);
			addPath(p1);
		} else if (p1.vStart == p2.vStart) {
			p1.reverse();
			p1.append(p2);
			addPath(p1);
		} else if (p1.vEnd == p2.vEnd) {
			p2.reverse();
			p1.append(p2);
			addPath(p1);
		} else {
			System.out.println("Lost invariant, very bad.");
		}
		return true;
	}

	/* Finde zwei Pfade, die verbunden werden koennen */

	private Path[] findConnectablePaths() {
		Vector<Path> paths1 = new Vector<Path>(paths);
		while (paths1.size() > 0) {
			Path path1 = paths1.remove(random.nextInt(paths1.size()));
			Vector<Path> paths2 = new Vector<Path>(paths1);
			while (paths2.size() > 0) {
				Path path2 = paths2.remove(random.nextInt(paths2.size()));
				if (!(path1.vStart == path2.vEnd || path1.vEnd == path2.vStart || path1.vStart == path2.vStart
						|| path1.vEnd == path2.vEnd))
					continue;

				if ((path1.contains(path2.vStart) && path1.contains(path2.vEnd))
						|| (path2.contains(path1.vStart) && path2.contains(path1.vEnd)))
					continue;

				return new Path[] { path1, path2 };
			}
		}
		return null;
	}

	/*************************************************************************
	 ** UI Commands
	 *************************************************************************/

	private void uiReset() {
		synchronized (this) {
			reset();
		}
	}
}
