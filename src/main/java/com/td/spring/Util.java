package com.td.spring;
/*****************************************************************************
 * Util.java
 *
 * Verschiedene Hilfsfunktionen
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Util.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.io.File;
import java.util.Vector;

class Util {

	/*
	 * Liefert nur die Controls eines ControlProviders, die vom Typ ValueControl
	 * sind.
	 */

	static Vector<ValueControl> getValueControls(ControlProvider cp) {
		Vector<ValueControl> vcs = new Vector<ValueControl>();
		for (Control ctrl : cp.getControls())
			if (ctrl instanceof ValueControl)
				vcs.add((ValueControl) ctrl);
		return vcs;
	}

	/*
	 * Sucht ein Verzeichnis mit Namen <name> in der Naehe des aktuellen
	 * Arbeitsverzeichnisses.
	 */

	static File findDir(String name) {
		for (String s : new String[] { name, "../" + name }) {
			File file = new File(s);
			if (file.isDirectory())
				return file;
		}
		return null;
	}

	/*
	 * Berechnet das normierte Skalarprodukt <u,v>/(|u||v|), mit u = u1-u0 und v =
	 * v1-v0, unter Verwendung von compDistance (s.u.)
	 */

	static double normalizedInnerProduct(Vertex u0, Vertex u1, Vertex v0, Vertex v1) {

		double[] dist = new double[3];
		compDistance(u1, u0, dist);
		double ux = dist[0], uy = dist[1], u2 = dist[2];
		double ur = Math.sqrt(u2);
		compDistance(v1, v0, dist);
		double vx = dist[0], vy = dist[1], v2 = dist[2];
		double vr = Math.sqrt(v2);

		return (ux * vx + uy * vy) / (ur * vr);
	}

	/*
	 * Haeufig benutzte Funktion: berechne den Abstand zwischen zwei Knoten v1 und
	 * v2. Rueckgabe: in result[0] und result[1] der Distanzvektor (dx, dy); in
	 * result[2] das Quadrat des Abstandes. Sind die zwei Knoten zu nah beieinander,
	 * werden sie in eine zufaellige Richtung voneinander entfernt.
	 */

	static void compDistance(Vertex v1, Vertex v2, double[] result) {

		double dx = 0, dy = 0, d2 = 0;

		for (boolean valid = false; !valid;) {

			dx = v1.x - v2.x;
			dy = v1.y - v2.y;
			d2 = dx * dx + dy * dy;

			if (d2 < 1E-10) {

				double angle = Math.random() * 2 * Math.PI;
				double ddx = Math.cos(angle);
				double ddy = Math.sin(angle);

				v1.x -= ddx;
				v1.y -= ddy;
				v2.x += ddx;
				v2.y += ddy;
			} else
				valid = true;
		}

		if (result != null) {
			result[0] = dx;
			result[1] = dy;
			result[2] = d2;
		}
	}
}

/*****************************************************************************
 ** Eine Stopuhr, geeignet zum Messen von Durchschnittsdauern von Prozessen.
 ** Bedienung: Zu Beginn des (regelmaessig ablaufenden) Prozesses start()
 ** aufrufen, bei Ende stop(). meanValue() gibt stets den Mittelwert ueber die
 ** letzten recordSize Prozesse.
 *****************************************************************************/

class StopWatch {

	private Vector<Integer> values = new Vector<Integer>();
	private int recordSize;
	private long startTime;

	/* Stopwatch mit angegebener Speichergroesse erzeugen */

	StopWatch() {
		this(50);
	}

	StopWatch(int recordSize) {
		this.recordSize = recordSize;
	}

	/* Start der Messung */

	void start() {
		startTime = System.currentTimeMillis();
	}

	/* Ende der Messung */

	void stop() {
		int time = (int) (System.currentTimeMillis() - startTime);
		record(time);
	}

	/* Messwert aufzeichnen */

	private void record(int value) {
		synchronized (this) {
			values.add(value);
			while (values.size() > recordSize)
				values.remove(0);
		}
	}

	/* Gib Mittelwert ueber alle Messwerte */

	int meanValue() {
		synchronized (this) {
			int sum = 0;
			int size = values.size();
			if (size == 0)
				return 0;
			for (int value : values)
				sum += value;
			sum /= size;
			return sum;
		}
	}
}
