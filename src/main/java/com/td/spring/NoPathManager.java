package com.td.spring;

/*****************************************************************************
 * NoPathManager.java
 *
 * Ein leerer Pfadmanager, der keine Aktionen ausfuehrt.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: NoPathManager.java 228 2015-12-11
 * 13:34:55Z dude $
 *****************************************************************************/

class NoPathManager extends PathManager {

	/* Name des Pfadmanagers */

	public String getName() {
		return "None";
	}

	/* Implementierung von ControlProvider, siehe Control.java */

	public void valueChanged(Control ctrl) {
	}

	public Control[] getControls() {
		return new Control[] {};
	}

	/* Management ausfuehren (implementiere PathManager) */

	protected void doManage() {
	}
}
