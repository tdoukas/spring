package com.td.spring;
/*****************************************************************************
 * Main.java
 *
 * Enthaelt main().
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Main.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Properties;

class Main {

	public static void main(String[] args) throws Exception {

		// Aktiviere Hardwarebeschleunigung

		Properties props = System.getProperties();
		props.setProperty("sun.java2d.opengl", "True");

		// Starte Anwendung

		Application app = new Application();
		app.run();
	}
}
