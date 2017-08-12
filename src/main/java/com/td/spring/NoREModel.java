package com.td.spring;

/*****************************************************************************
 * NoREModel.java
 *
 * Ein leeres Kraeftemodell fuer starre Kantenzuege, das keine Kraefte ausuebt.
 *
 * Autor: Theo Doukas, Okt - Dez 2015. $Id: NoREModel.java 257 2016-05-29
 * 08:20:04Z dude $
 *****************************************************************************/

class NoREModel extends REModel {

	/* Name des Modells */

	public String getName() {
		return "None";
	}

	/* Implementierung von ControlProvider, siehe Control.java */

	public Control[] getControls() {
		return new Control[] {};
	}

	public void valueChanged(Control ctrl) {
	}

	/* Berechne Kraefte (implementiere REModel) */

	public void computeForces(Path path) {
	}
}
