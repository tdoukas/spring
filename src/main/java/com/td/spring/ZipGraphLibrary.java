package com.td.spring;
/*****************************************************************************
 * ZipGraphLibrary.java
 *
 * Zugriff auf die Graphen in einem Zipfile
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: ZipGraphLibrary.java 228 2015-12-11 13:34:55Z dude $
 *****************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

abstract class ZipGraphLibrary extends Graph {

	private ZipFile zipFile; // Das Zipfile, aus dem gelesen wird

	// Alle Eintraege des Zipfiles
	private Vector<ZipEntry> allEntries = new Vector<ZipEntry>();

	ZipGraphLibrary() {
		setupAllEntries();
		setupDirCtrl();
	}

	/*************************************************************************
	 * Von Unterklasse zu implementieren:
	 *************************************************************************/

	/* Gebe Namen des Zipfiles */

	abstract String getZipFileName();

	/* Generiere den Graphen durch Lesen von einem InputStream */

	abstract void generate(InputStream in) throws IOException;

	/*************************************************************************
	 ** Controls
	 *************************************************************************/

	private OptionControl<ZipEntry> dirCtrl = new OptionControl<ZipEntry>("Directory", "Directory", this, null, null);

	private OptionControl<ZipEntry> fileCtrl = new OptionControl<ZipEntry>("File", "File", this, null, null);

	public Control[] getControls() {
		return new Control[] { dirCtrl, fileCtrl };
	}

	public void valueChanged(Control ctrl) {
		if (ctrl == dirCtrl)
			setupFileCtrl((ZipEntry) ((OptionControl) ctrl).getValue());
		else if (ctrl == fileCtrl) {
			Application application = Application.getInstance();
			if (application != null)
				application.uiGenerate();
		}
	}

	/*************************************************************************
	 ** Generiere Graph
	 *************************************************************************/

	public void generate() {
		clear();

		ZipEntry entry = (ZipEntry) fileCtrl.getValue();
		if (entry != null) {
			try {
				// Rufe generate()-Methode der Unterklasse auf
				generate(zipFile.getInputStream(entry));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*************************************************************************
	 ** Directory- und File-Control
	 *************************************************************************/

	/*
	 * Aktualisiere das Directory-Control mit allen Directory-Eintraegen aus dem
	 * Zipfile
	 */

	private void setupDirCtrl() {

		Vector<ZipEntry> directories = new Vector<ZipEntry>();
		for (ZipEntry entry : allEntries) {

			String name = entry.getName();
			if (entry.isDirectory() && countOccurences(name, '/') > 1 && !name.endsWith("/bad/"))
				directories.add(entry);
		}
		dirCtrl.setOptions(directories.toArray(new ZipEntry[] {}));
	}

	/*
	 * Aktualisiere das File-Control mit den Files aus dem aktuellen Directory
	 */

	private void setupFileCtrl(ZipEntry dirEntry) {

		String dirEntryName = dirEntry.getName();
		if (dirEntryName.endsWith("/"))
			dirEntryName = dirEntryName.substring(0, dirEntryName.length() - 1);

		Vector<ZipEntry> files = new Vector<ZipEntry>();
		for (ZipEntry entry : allEntries) {

			String name = entry.getName();
			String parentName = name.substring(0, name.lastIndexOf("/"));
			if (!entry.isDirectory() && parentName.equals(dirEntryName))
				files.add(entry);
		}

		fileCtrl.setOptions(files.toArray(new ZipEntry[] {}));
	}

	/* Zaehle, wie oft c in s vorkommt */

	private static int countOccurences(String s, char c) {

		int n = 0, i = -1;
		while (true) {
			i = s.indexOf(c, i + 1);
			if (i == -1)
				return n;
			n++;
		}
	}

	/*************************************************************************
	 ** Zip-File
	 *************************************************************************/

	/* Lokalisiere das gesuchte Zip-File. */

	private File findZipFile() {
		String zipFileName = getZipFileName();
		for (String s : new String[] { zipFileName, "../" + zipFileName }) {
			File file = new File(s);
			if (file.exists())
				return file;
		}
		return null;
	}

	/* Lese alle Eintraege aus dem Zip-File. */

	private void setupAllEntries() {
		try {
			zipFile = new ZipFile(findZipFile());
			for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				allEntries.add(entry);
			}
			Collections.sort(allEntries, new Comparator<ZipEntry>() {
				public int compare(ZipEntry s1, ZipEntry s2) {
					return s1.getName().compareTo(s2.getName());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
