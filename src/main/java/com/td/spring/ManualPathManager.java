package com.td.spring;

/*****************************************************************************
 * ManualPathManager.java
 *
 * Erzeugung und Verwaltung von starren Kantenzuegen. Dies ist der Pfadmanager
 * "V2", vgl. 5.1. und B.5.2.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: ManualPathManager.java 257
 * 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

class ManualPathManager extends PathManager {

	/* Name des Pfadmanagers */

	public String getName() {
		return "V2";
	}

	/*************************************************************************
	 ** Controls (Implementierung von ControlProvider, siehe Control.java)
	 *************************************************************************/

	private IntControl lenCtrl = new IntControl("PathLen", "Maximum path length", this, 5, 2, 20);

	private LinearControl idCtrl = new LinearControl("InitialDirection", "Initial Direction", this, -1.0, -1.0, 1.0,
			"%1.2f");

	private ButtonControl changePathCtrl = new ButtonControl("Change") {
		public void clicked() {
			uiChangePath();
		}
	};

	private ButtonControl storePathCtrl = new ButtonControl("Store") {
		public void clicked() {
			uiStorePath();
		}
	};

	private ButtonControl removeLastCtrl = new ButtonControl("Remove") {
		public void clicked() {
			uiRemoveLastPath();
		}
	};

	public Control[] getControls() {
		return new Control[] { lenCtrl, idCtrl, changePathCtrl, storePathCtrl, removeLastCtrl };
	}

	public void valueChanged(Control ctrl) {
	}

	/*************************************************************************
	 ** Manager
	 *************************************************************************/

	/* Dieser Manager fuehrt keine regelmaessigen Aktivitaeten aus */

	public void doManage() {
	}

	/*
	 * Modifiziere den aktuellen Pfad, ersetze dabei den letzten oder fuege diesen
	 * hinzu. Wird von UI-Actions (s.u.) aufgerufen
	 */

	private void alternatePath(boolean keepLast) {

		double initialDirection = idCtrl.getValue();
		int pathLength = lenCtrl.getValue();

		for (int i = 0; i < 50; i++) {

			Vertex v = graph.getRandomVertex();
			if (v.mark == 1)
				continue;

			Path p = buildPath2(v, pathLength, initialDirection);
			if (p != null) {
				if (!keepLast)
					removeLastPath();
				addPath(p);
				break;
			}
		}
	}

	/*************************************************************************
	 ** UI-Actions, ausgeloest durch Aktionen des Benutzers
	 *************************************************************************/

	/* Ersetze den obersten Pfad durch einen neuen */

	private void uiChangePath() {
		alternatePath(false);
	}

	/* Fuege einen neuen Pfad oben auf den Stapel hinzu */

	private void uiStorePath() {
		alternatePath(true);
	}

	/* Entferne den obersten Pfad vom Stapel */

	private void uiRemoveLastPath() {
		removeLastPath();
	}
}
