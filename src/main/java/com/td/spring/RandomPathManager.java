package com.td.spring;
/*****************************************************************************
 * RandomPathManager.java
 *
 * Erzeugung und Verwaltung von starren Kantenzuegen.
 * Dies ist der Pfadmanager "V1", vgl. B.5.1.
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: RandomPathManager.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

class RandomPathManager extends PathManager {

	private double[] dist = new double[3]; // fuer compDistance()

	/* Name des Pfadmanagers */

	public String getName() {
		return "V1";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private IntControl npCtrl = new IntControl("NumPaths", "Maximum number of paths to generate", this, 0, 0, 100);
	private IntControl lenCtrl = new IntControl("PathLen", "Maximum path length", this, 5, 2, 100);
	private IntControl ttlCtrl = new IntControl("MinTTL", "Minimum time to live for a path", this, 250, 1, 500);
	private RealControl keepCtrl = new LinearControl("KeepRatio", "Amount of straight paths to keep", this, 0.5, 0, 1,
			"%1.2f");
	private RealControl ifCtrl = new LinearControl("PrefDir_Focus", "Edge direction for first vertex (inner product)",
			this, 1.0, -1.0, 1.0, "%1.2f");
	private RealControl inCtrl = new LinearControl("PrefDir_Other", "Edge direction for first vertex (inner product)",
			this, -1.0, -1.0, 1.0, "%1.2f");
	protected RealControl focusCtrl = new LinearControl("Focus", "Focus path creation on problem regions", this, 0, 0,
			1, "%1.2f");

	private Control clearButton = new ButtonControl("Reset Paths") {
		void clicked() {
			synchronized (RandomPathManager.this) {
				clearPaths();
			}
		}
	};

	public Control[] getControls() {
		return new Control[] { npCtrl, lenCtrl, ttlCtrl, keepCtrl, focusCtrl, ifCtrl, inCtrl, clearButton };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Pfade managen
	 *************************************************************************/

	/* Fuehre Management-Schritt aus, siehe PathManager */

	protected void doManage() {

		// Noch nicht genug Pfade da? -> Einen erzeugen

		if (paths.size() < npCtrl.getValue()) {

			boolean focus = Math.random() <= focusCtrl.getValue();
			Vertex v = focus ? getFocussedVertex() : graph.getRandomVertex();

			if (v != null) {

				double ideal = focus ? ifCtrl.getValue() : inCtrl.getValue();

				Path p = buildPath2(v, lenCtrl.getValue(), ideal);
				if (p != null) {
					p.ttl = ttlCtrl.getValue();
					addPath(p);
				}
			}
		}

		// Zu lange Pfade loeschen (wenn sich lenCtrl zwischenzeitlich
		// geaendert hat)

		int len = lenCtrl.getValue();
		for (Iterator<Path> i = paths.iterator(); i.hasNext();) {
			Path path = i.next();
			if (path.length() > len) {
				i.remove();
				pathsChanged = true;
			}
		}

		// Berechne fuer alle Pfade ihre Geradheit und sortiere danach

		for (Path p : paths)
			p.weight = p.getStraightness();

		Collections.sort(paths, new Comparator<Path>() {
			public int compare(Path p, Path q) {
				if (p.weight < q.weight)
					return 1;
				if (p.weight > q.weight)
					return -1;
				return 0;
			}
		});

		// Besonders gerade Pfade sind jetzt vorne in der Liste. Fuer die
		// ersten k Pfade (Wert von keepCtrl) wird TTL hochgezaehlt.

		int n = paths.size();
		int keep = Math.min(n, Math.max(0, (int) (keepCtrl.getValue() * n)));

		for (int i = 0; i < keep; i++)
			paths.elementAt(i).ttl++;

		// Pfade entfernen, deren ttl ablaeuft

		for (Iterator<Path> i = paths.iterator(); i.hasNext();) {
			Path path = i.next();
			if (path.ttl-- < 0) {
				i.remove();
				break;
			}
		}

		// Ueberzaehlige Pfade loeschen

		if (paths.size() > npCtrl.getValue())
			removePath(0);
	}
}
