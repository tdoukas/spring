package com.td.spring;
/*****************************************************************************
 * Application.java
 *
 * Hauptfenster der Anwendung
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Application.java 245 2016-01-09 12:53:54Z dude $
 *****************************************************************************/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

class Application extends JFrame {

	/*************************************************************************
	 ** Variablen und Konstanten
	 *************************************************************************/

	// Singleton - Instanz

	public static Application instance;

	// Speicherort fuer Konfigurationen

	private final String presetDirName = "preset";

	// Hauptelemente: Model und Drawing

	private Model model = new Model();
	private Drawing drawing = new Drawing();

	// UI-Elemente

	private JPanel userPanel = new JPanel();
	private JPanel embOptPanel = new JPanel();
	private JPanel grOptPanel = new JPanel();
	private JPanel rmOptPanel = new JPanel();
	private JPanel pmOptPanel = new JPanel();
	private JPanel modelOptPanel = new JPanel();
	private JToggleButton runButton = new JToggleButton("Run");
	private JLabel statusLabel = new JLabel(":");
	private JComboBox<Embedder> embSelector;
	private JComboBox<Graph> graphSelector;
	private JComboBox<REModel> rmSelector;
	private JComboBox<PathManager> pmSelector;
	private JMenu presetMenu = new JMenu("Preset");

	// Zur Auswahl stehende Graphen, Embedder, REModelle, PfadManager

	private Graph[] graphs = new Graph[] { new RandomGraph(), new QuadMeshGraph(), new TreeGraph(), new MatrixGraph(),
			new RomeGraph(), new MLGraph() };

	private Embedder[] embedders = new Embedder[] { new FruchtermanReingold(), new EadesEmbedder() };

	private REModel[] reModels = new REModel[] { new NoREModel(), new StraightREModel(), new ConvexREModel(),
			new ConcaveREModel() };

	private PathManager[] pathManagers = new PathManager[] { new NoPathManager(), new RandomPathManager(),
			new ManualPathManager(), new AutoPathManager() };

	// Hilfsvariablen der Anwendung

	private boolean running; // Embedder aktiv?
	private double scale; // Scale-Wert, von externer Komponente
	private long startRunningTime, // Beginn und Ende der aktuellen Berechnung
			stopRunningTime;
	private int nIter = 0; // Aktuelle Anzahl der Iterationen
	private String presetName; // Name des aktuellen Presets
	private long lastStatusUpdate; // Zeitpunkt der letzten Aktualisierung der
									// Statuszeile
	private long lastQualityCheck; // Letzte Qualitaetsmessung des Graphen
	private int numOverlap = 0; // Ergebnis der Qualitaetsmessung:
	private int visComplexity = 0; // Ueberlappungen und vis. Komplexitaet
	private StopWatch watch = // Stopuhr fuer Hauptschleife
			new StopWatch();
	private boolean limitCpu = false; // Weniger CPU-Resourcen benutzen?
	private boolean showQuality = false; // Qualitaet der Zeichnung messen?
	private boolean syncDrawing = true; // Synchron zeichnen?
	private long lastRepaintTime;
	private Object lock = new Object(); // Mutex
	private String saveDimension = "1024x1024";
	private String pngFileName = "drawing.png";
	private String svgFileName = "drawing.svg";

	/*************************************************************************
	 ** Konstruktor
	 *************************************************************************/

	public Application() {
		super("Spring");
		instance = this;

		buildUI();
		buildMenu();

		setGraph(graphs[0]);
		setEmbedder(embedders[0]);
		setREModel(reModels[0]);
		setPathManager(pathManagers[0]);

		pack();
		setSize(getWidth(), 820);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/*************************************************************************
	 ** Hauptschleife
	 *************************************************************************/

	/* Einsprungspunkt, von Main */

	public void run() {
		setRunning(true);
		mainLoop();
	}

	/* Hauptschleife */

	private void mainLoop() {
		while (true) {
			if (running) {
				watch.start();
				nIter++;
				synchronized (lock) {
					model.step();
				}
				watch.stop();
				maybeRepaintDrawing(limitCpu ? 20 : 5);
				if (limitCpu) {
					try {
						Thread.currentThread().sleep(1);
					} catch (Exception e) {
					}
				}
			} else {
				maybeRepaintDrawing(1000);
				try {
					Thread.currentThread().sleep(10);
				} catch (Exception e) {
				}
			}
			synchronized (lock) {
				maybeUpdateStatus();
			}
			Thread.currentThread().yield();
		}
	}

	/*
	 * Aktualisiere die Zeichnung, wenn Zeit dt seit letzter Aktualisierung
	 * vergangen ist
	 */

	private void maybeRepaintDrawing(long dt) {
		long time = System.currentTimeMillis();
		if (time - lastRepaintTime > dt) {
			drawing.repaint();
			lastRepaintTime = time;
		}
	}

	/*
	 * Setze, ob Hauptschleife Iterationen der Berechnung ausfuehrt oder nicht
	 */

	private void setRunning(boolean running) {
		synchronized (lock) {
			this.running = running;
			runButton.setSelected(running);

			long time = System.currentTimeMillis();
			if (running) {
				startRunningTime = time;
				nIter = 0;
			} else
				stopRunningTime = time;
		}
	}

	/*
	 * Zeichne den Graph, wahlweise synchron. Wird nur von Drawing aufgerufen.
	 */

	void paintDrawingSynchronous(Graphics g) {
		if (syncDrawing) {
			synchronized (lock) {
				drawing.doPaintComponent(g);
			}
		} else
			drawing.doPaintComponent(g);
	}

	/*************************************************************************
	 ** Private Setter fuer die Komponenten
	 *************************************************************************/

	private void setGraph(Graph graph) {
		synchronized (lock) {
			graph.generate();
			model.setGraph(graph);
			drawing.setGraph(graph);
			graphSelector.setSelectedItem(graph);
			setupGrOptPanel();
		}
	}

	private void setEmbedder(Embedder embedder) {
		synchronized (lock) {
			nIter = 0;
			model.setEmbedder(embedder);
			setupEmbOptPanel();
		}
	}

	private void setREModel(REModel reModel) {
		synchronized (lock) {
			rmSelector.setSelectedItem(reModel);
			model.setREModel(reModel);
			setupRmOptPanel();
		}
	}

	private void setPathManager(PathManager pathManager) {
		synchronized (lock) {
			pmSelector.setSelectedItem(pathManager);
			model.setPathManager(pathManager);
			setupPMOptPanel();
		}
	}

	/*************************************************************************
	 ** Verschiedene Getter-Methoden
	 *************************************************************************/

	public int getIteration() {
		return nIter;
	}

	public Vector<Path> getSelectedPaths() {
		if (drawing != null)
			return drawing.getSelectedPaths();
		return null;
	}

	public Vertex getSelectedVertex() {
		return drawing.getSelectedVertex();
	}

	public static Application getInstance() {
		return instance;
	}

	/*************************************************************************
	 ** Notify-Methoden
	 *************************************************************************/

	/* Wird von Drawing aufgerufen, nachdem eine Zeichnung fertig ist */

	void notifyScale(double scale) {
		this.scale = scale;
	}

	/* Wird von Drawing aufgerufen, wenn der Benutzer Pfade veraendert hat */

	void notifyPathsChanged() {
		model.notifyPathsChanged();
	}

	/* Wird vom Embedder aufgerufen, wenn er fertig ist */

	void notifyEmbedderFinished() {
		setRunning(false);
	}

	/*************************************************************************
	 ** Statuszeile
	 *************************************************************************/

	/* Setze Text der Statuszeile */

	private void setStatus(String text) {
		statusLabel.setText(text);
	}

	/*
	 * Aktualisiere Status, wenn seit letzter Aktualiserung eine bestimmte Zeit
	 * vergangen ist
	 */

	private void maybeUpdateStatus() {

		long time = System.currentTimeMillis();
		if (time - lastStatusUpdate > 100) {
			lastStatusUpdate = time;
			updateStatus();
		}

		if (showQuality)
			if (time - lastQualityCheck > 1000) {
				lastQualityCheck = time;
				checkQuality();
			}
	}

	/* Berechne die Qualitaet der Zeichnung (aufwendig fuer grosse Graphen) */

	private void checkQuality() {

		Graph g = model.getGraph();
		if (g != null) {
			numOverlap = g.numOverlappingEdges();
			visComplexity = g.visualComplexity();
		}
	}

	/* Aktualisiere Status */

	private void updateStatus() {

		int nv = 0, ne = 0;
		Graph graph = model.getGraph();
		if (graph != null) {
			nv = graph.v.size();
			ne = graph.e.size();
		}

		long time = stopRunningTime - startRunningTime;
		if (time <= 0)
			time = System.currentTimeMillis() - startRunningTime;

		Runtime rt = Runtime.getRuntime();
		long total = rt.totalMemory();
		long used = total - rt.freeMemory();
		int usedMB = (int) (used / (1024 * 1024));
		int totalMB = (int) (total / (1024 * 1024));
		String status = String.format(
				"Scale=%.3e. |V|=%d, |E|=%d." + " Time: %d seconds. %d iterations."
						+ " Mem: %03d/%03d MB used. Compute: %d ms," + " draw: %d ms.",
				scale, nv, ne, (int) (time / 1000), nIter, usedMB, totalMB, watch.meanValue(), drawing.getDrawTime());

		if (showQuality)
			status += String.format(" #=%d, overlap=%d, Q=%d", visComplexity, numOverlap,
					-numOverlap * 3 - visComplexity);
		setStatus(status);
	}

	/*************************************************************************
	 ** UI-Kommandos (Actions). Diese Funktionen werden von externen Stellen
	 * aufgerufen, im allgemeinen nicht aus dem Thread der Hauptschleife, sondern
	 * aus dem Thread der Benutzerobeflaeche. Sie sollen den Zugriff auf die
	 * Resourcen koordinieren und wo noetig synchronisieren.
	 *************************************************************************/

	/* Waehle einen neuen Graph */

	private void uiSelectGraph(Graph g) {
		synchronized (lock) {
			setGraph(g);
			model.resetVertices();
			setRunning(true);
		}
	}

	/* Waehle einen neuen Embedder */

	private void uiSelectEmbedder(Embedder embedder) {
		synchronized (lock) {
			setEmbedder(embedder);
		}
	}

	/* Waehle ein neues Starre-Kanten-Modell */

	private void uiSelectREModel(REModel reModel) {
		synchronized (lock) {
			setREModel(reModel);
		}
	}

	/* Waehle einen neuen Pfadmanager */

	private void uiSelectPathManager(PathManager pathManager) {
		synchronized (lock) {
			setPathManager(pathManager);
		}
	}

	/* Setze die Knotenpositionen zurueck */

	private void uiResetPositions() {
		synchronized (lock) {
			model.resetVertices();
			setRunning(true);
		}
	}

	/* Generiere den Graph neu */

	public void uiGenerate() {
		synchronized (lock) {
			model.generateGraph();
			drawing.repaint();
			setRunning(true);
		}
	}

	/* Setze, ob die Berechnungen ausgefuehrt werden sollen oder nicht */

	private void uiRun(boolean run) {
		synchronized (lock) {
			setRunning(run);
			if (run)
				model.restartEmbedder();
		}
	}

	/* Setze Embedder auf Default-Werte zurueck */

	private void uiEmbedderDefaultValues() {
		synchronized (lock) {
			for (Control ctrl : model.getEmbedder().getControls())
				ctrl.reset();
		}
	}

	/* Lade ein Preset */

	private void uiSetPreset(InputStream in) throws IOException {
		synchronized (lock) {
			setPreset(in);
		}
	}

	/* Speichere die aktuelle Konfiguration in einem Preset */

	private void uiStorePreset(PrintStream out) {
		synchronized (lock) {
			storePreset(out);
		}
	}

	/* Schalte Antialias ein/aus */

	private void uiSetAntialias(boolean antialias) {
		drawing.setAntialias(antialias);
		drawing.repaint();
	}

	/* Schalte Highlighting von interessanten Regionen (Fokus-Regionen) ein */

	private void uiSetHighlight(boolean highlight) {
		drawing.setHighlight(highlight);
		drawing.repaint();
	}

	/* Zeichne Knoten und Kanten in verschiedenen Farben */

	private void uiSetShowMark(boolean showMark) {
		drawing.setShowMark(showMark);
		drawing.repaint();
	}

	/* Zeichne Hintergrundraster */

	private void uiSetDrawGrid(boolean drawGrid) {
		drawing.setDrawGrid(drawGrid);
		drawing.repaint();
	}

	/* Zeichne Raster-Beschriftungen */

	private void uiSetDrawGridText(boolean drawGridText) {
		drawing.setDrawGridText(drawGridText);
		drawing.repaint();
	}

	/* Zeige Kantengruppen */

	private void uiSetDrawEdgeGroups(boolean drawEdgeGroups) {
		drawing.setDrawEdgeGroups(drawEdgeGroups);
		drawing.repaint();
	}

	/* Zeichne Halo */

	private void uiSetDrawHalo(boolean drawHalo) {
		drawing.setDrawHalo(drawHalo);
		drawing.repaint();
	}

	/* Zeichne Kraefte */

	private void uiSetDrawForces(boolean drawForces) {
		drawing.setDrawForces(drawForces);
		drawing.repaint();
	}

	/* Setze Keep Aspect */

	private void uiSetKeepAspect(boolean keepAspect) {
		drawing.setKeepAspect(keepAspect);
		drawing.repaint();
	}

	/*************************************************************************
	 ** UI-Kommandos zum Speichern von Zeichnungen (PNG und SVG)
	 *************************************************************************/

	/*
	 * Abstraktion einer Speicheroperation: die Methode saveDrawing (s.u.) befuellt
	 * eine Instanz dieser Klasse mit Werten und ruft doSave() auf.
	 */

	private abstract class SaveDrawing {

		String fileType, fileExt, fileName;
		String dimension;

		SaveDrawing(String fileType, String fileExt, String fileName, String dimension) {
			this.fileType = fileType;
			this.fileExt = fileExt;
			this.fileName = fileName;
			this.dimension = dimension;
		}

		abstract void doSave(File file, int width, int height);
	}

	/* Speichere Zeichnung als PNG */

	private void uiSaveDrawingPNG() {

		SaveDrawing sd = new SaveDrawing("PNG images", "png", pngFileName, saveDimension) {
			void doSave(File file, int width, int height) {

				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				synchronized (lock) {
					drawing.draw(img);
				}
				try {
					ImageIO.write(img, "png", file);
				} catch (Exception e) {
					showError(e);
				}
			}
		};
		saveDrawing(sd);
		pngFileName = sd.fileName;
		saveDimension = sd.dimension;
	}

	/* Speichere Zeichnung als SVG */

	private void uiSaveDrawingSVG() {

		SaveDrawing sd = new SaveDrawing("SVG images", "svg", svgFileName, saveDimension) {
			void doSave(File file, int width, int height) {
				try {
					OutputStream out = new FileOutputStream(file);
					synchronized (lock) {
						drawing.draw(out, width, height);
					}
					out.close();
				} catch (Exception e) {
					showError(e);
				}
			}
		};
		saveDrawing(sd);
		svgFileName = sd.fileName;
		saveDimension = sd.dimension;
	}

	/* Speichere Zeichnung (Gemeinsamer Ablauf fuer alle Formate) */

	private void saveDrawing(SaveDrawing sd) {

		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(sd.fileType, sd.fileExt);
		chooser.setFileFilter(filter);
		chooser.setSelectedFile(new File(sd.fileName));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File file = chooser.getSelectedFile();
		if (file.exists() && JOptionPane.showConfirmDialog(this, "File exists, overwrite?", "Confirm overwrite",
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;

		int imgWidth = 1024, imgHeight = 1024;
		String dimension = JOptionPane.showInputDialog(this, "Enter dimension (<width>x<height>)", sd.dimension);

		if (dimension != null) {

			String[] tokens = dimension.split("x");
			try {
				int w = Integer.parseInt(tokens[0]);
				int h = Integer.parseInt(tokens[1]);
				imgWidth = w;
				imgHeight = h;
			} catch (Exception e) {
				showError(e);
			}
		}

		sd.doSave(file, imgWidth, imgHeight);
		sd.fileName = file.getName();
		sd.dimension = imgWidth + "x" + imgHeight;
	}

	/* Zeige Fehler waehrend des Speichervorganges */

	private void showError(Exception e) {
		JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	/*************************************************************************
	 ** UI-Elemente: Gesamtlayout und Bedienelemente auf der rechten Seite (User
	 * Panel).
	 *************************************************************************/

	/* Erzeuge das gesamte UI des Hauptfensters */

	private void buildUI() {

		setLayout(new BorderLayout());

		// Drawing

		add(drawing, BorderLayout.CENTER);

		// Status Label

		add(statusLabel, BorderLayout.SOUTH);
		statusLabel.setOpaque(true);
		statusLabel.setBackground(Color.white);

		// User Panel

		JScrollPane scrollPane = new JScrollPane(userPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.EAST);
		buildUserPanel();
	}

	/* Erzeuge das UI der Bedienlemente rechts */

	private void buildUserPanel() {

		userPanel.removeAll();

		userPanel.setLayout(new BorderLayout());
		Box box = Box.createVerticalBox();
		userPanel.add(box, BorderLayout.NORTH);

		box.add(buildGraphPanel());
		box.add(buildEmbedderPanel());
		box.add(buildREModelPanel());
		box.add(buildPathMgrPanel());
	}

	/* Abteilung "Graph" */

	private JComponent buildGraphPanel() {

		Box graphPanel = Box.createVerticalBox();
		setBorderAndTitle(graphPanel, "Graph");

		// Graph Selector

		graphSelector = new JComboBox<Graph>(graphs);
		graphSelector.setSelectedIndex(0);
		graphPanel.add(graphSelector);
		graphSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSelectGraph((Graph) graphSelector.getSelectedItem());
			}
		});

		// Graph Options

		makeOptionsBorder(grOptPanel);
		graphPanel.add(grOptPanel);
		setupGrOptPanel();

		// Buttons

		JPanel grButPanel = new JPanel();
		graphPanel.add(grButPanel);

		JButton reset = new JButton("Reset Positions");
		grButPanel.add(reset);
		reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiResetPositions();
			}
		});

		JButton generateButton = new JButton("Generate");
		grButPanel.add(generateButton);
		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiGenerate();
			}
		});

		return graphPanel;
	}

	/* Abteilung "Embedder" */

	private JComponent buildEmbedderPanel() {

		Box embPanel = Box.createVerticalBox();
		setBorderAndTitle(embPanel, "Embedder");

		// Embedder Selector

		embSelector = new JComboBox<Embedder>(embedders);
		embSelector.setSelectedIndex(0);
		embPanel.add(embSelector);
		embSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSelectEmbedder((Embedder) embSelector.getSelectedItem());
			}
		});

		// Embedder Options

		makeOptionsBorder(embOptPanel);
		embPanel.add(embOptPanel);
		setupEmbOptPanel();

		// Model Options

		embPanel.add(modelOptPanel);
		setupModelOptPanel();

		// Buttons

		JPanel embButPanel = new JPanel();
		embPanel.add(embButPanel);

		Box runPanel = Box.createHorizontalBox();

		runPanel.add(runButton);
		embButPanel.add(runPanel);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiRun(runButton.isSelected());
			}
		});
		// runPanel.setBorder(BorderFactory.createLineBorder(Color.white, 4));

		JButton embDefButton = new JButton("Default values");
		embButPanel.add(embDefButton);
		embDefButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiEmbedderDefaultValues();
			}
		});

		return embPanel;
	}

	/* Abteilung "Rigid Edge Model" */

	private JComponent buildREModelPanel() {

		Box rmPanel = Box.createVerticalBox();
		setBorderAndTitle(rmPanel, "Rigid Edge Model");

		// REModel Selector

		rmSelector = new JComboBox<REModel>(reModels);
		rmSelector.setSelectedIndex(0);
		rmPanel.add(rmSelector);
		rmSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSelectREModel((REModel) rmSelector.getSelectedItem());
			}
		});

		// REModel Options

		makeOptionsBorder(rmOptPanel);
		rmPanel.add(rmOptPanel);
		setupRmOptPanel();

		return rmPanel;
	}

	/* Abteilung PathManager */

	private JComponent buildPathMgrPanel() {

		Box pathMgrPanel = Box.createVerticalBox();
		setBorderAndTitle(pathMgrPanel, "Path Manager");

		// PathManager Selector

		pmSelector = new JComboBox<PathManager>(pathManagers);
		pmSelector.setSelectedIndex(0);
		pathMgrPanel.add(pmSelector);
		pmSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSelectPathManager((PathManager) pmSelector.getSelectedItem());
			}
		});

		// PathMgr Options Panel

		makeOptionsBorder(pmOptPanel);
		pathMgrPanel.add(pmOptPanel);
		setupPMOptPanel();

		return pathMgrPanel;
	}

	/* Hilfsfunktionen fuer alle vier Abteilungen */

	private void setBorderAndTitle(JComponent container, String title) {

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JLabel label = new JLabel(title);
		panel.add(label, BorderLayout.NORTH);
		container.add(panel);
		label.setHorizontalAlignment(SwingConstants.CENTER);

		label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray, 2),
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));

		label.setForeground(Color.white);
		label.setBackground(Color.gray);
		label.setOpaque(true);
	}

	private void makeOptionsBorder(JComponent component) {
		component.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray, 1),
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));
	}

	/* Setup der vier Bereiche entsprechend den ausgewaehlten Modulen */

	private void setupEmbOptPanel() {
		addControls(model.getEmbedder(), embOptPanel);
		revalidate();
	}

	private void setupGrOptPanel() {
		addControls(model.getGraph(), grOptPanel);
		revalidate();
	}

	private void setupRmOptPanel() {
		addControls(model.getREModel(), rmOptPanel);
		revalidate();
	}

	private void setupPMOptPanel() {
		addControls(model.getPathManager(), pmOptPanel);
		revalidate();
	}

	private void setupModelOptPanel() {
		addControls(model, modelOptPanel);
		revalidate();
	}

	/*
	 * Jedes Modell implementiert ControlProvider, und beherbergt seine
	 * Benutzerschnittstellen in einem JComponent. Diese Funktion befuellt das
	 * JComponent container mit den Controls aus cp.
	 */

	private void addControls(ControlProvider cp, JComponent container) {
		container.removeAll();

		if (cp == null)
			return;

		container.setLayout(new BorderLayout());
		Box box = Box.createVerticalBox();
		container.add(box, BorderLayout.NORTH);

		Control[] controls = cp.getControls();
		if (controls.length == 0) {
			// box.add(new JLabel("No Options."));
		} else
			for (int i = 0; i < controls.length; i++) {
				Control ctrl = controls[i];
				box.add(ctrl.getComponent());
			}
	}

	/*************************************************************************
	 ** Menueleiste und Eintraege
	 *************************************************************************/

	/* Erzeuge Menueleiste */

	private void buildMenu() {

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(presetMenu);
		updatePresetMenu();

		JMenu drawingMenu = new JMenu("Drawing");
		menuBar.add(drawingMenu);
		final JCheckBoxMenuItem aaItem = new JCheckBoxMenuItem("Antialias", drawing.getAntialias());
		aaItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetAntialias(aaItem.getState());
			}
		});
		drawingMenu.add(aaItem);

		drawingMenu.addSeparator();

		final JCheckBoxMenuItem dgItem = new JCheckBoxMenuItem("Draw grid", drawing.getDrawGrid());
		dgItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetDrawGrid(dgItem.getState());
			}
		});
		drawingMenu.add(dgItem);

		final JCheckBoxMenuItem dgtItem = new JCheckBoxMenuItem("Draw grid labels", drawing.getDrawGridText());
		dgtItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetDrawGridText(dgtItem.getState());
			}
		});
		drawingMenu.add(dgtItem);

		final JCheckBoxMenuItem degItem = new JCheckBoxMenuItem("Draw edge group ids", drawing.getDrawEdgeGroups());
		degItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetDrawEdgeGroups(degItem.getState());
			}
		});
		drawingMenu.add(degItem);

		final JCheckBoxMenuItem dhItem = new JCheckBoxMenuItem("Draw line halo", drawing.getDrawHalo());
		dhItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetDrawHalo(dhItem.getState());
			}
		});
		drawingMenu.add(dhItem);

		final JCheckBoxMenuItem dfItem = new JCheckBoxMenuItem("Draw forces", drawing.getDrawForces());
		dfItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetDrawForces(dfItem.getState());
			}
		});
		drawingMenu.add(dfItem);

		final JCheckBoxMenuItem kaItem = new JCheckBoxMenuItem("Keep aspect ratio", drawing.getKeepAspect());
		kaItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetKeepAspect(kaItem.getState());
			}
		});
		drawingMenu.add(kaItem);

		drawingMenu.addSeparator();

		final JCheckBoxMenuItem hlItem = new JCheckBoxMenuItem("Highlight interesting regions", drawing.getHighlight());
		hlItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetHighlight(hlItem.getState());
			}
		});
		drawingMenu.add(hlItem);

		final JCheckBoxMenuItem smItem = new JCheckBoxMenuItem("Highlight marked edges / vertices",
				drawing.getShowMark());
		smItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSetShowMark(smItem.getState());
			}
		});
		drawingMenu.add(smItem);

		drawingMenu.addSeparator();

		JMenuItem savePNGItem = new JMenuItem("Save as PNG...");
		savePNGItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSaveDrawingPNG();
			}
		});
		drawingMenu.add(savePNGItem);

		JMenuItem saveSVGItem = new JMenuItem("Save as SVG...");
		saveSVGItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uiSaveDrawingSVG();
			}
		});
		drawingMenu.add(saveSVGItem);

		JMenu modelMenu = new JMenu("Model");
		menuBar.add(modelMenu);
		final JCheckBoxMenuItem ftItem = new JCheckBoxMenuItem("Fix barycenter movement", model.getFixTranslation());
		ftItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.setFixTranslation(ftItem.getState());
			}
		});
		modelMenu.add(ftItem);
		final JCheckBoxMenuItem frItem = new JCheckBoxMenuItem("Fix overall torque", model.getFixRotation());
		frItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.setFixRotation(frItem.getState());
			}
		});
		modelMenu.add(frItem);

		JMenu systemMenu = new JMenu("System");
		menuBar.add(systemMenu);

		final JCheckBoxMenuItem lcItem = new JCheckBoxMenuItem("Limit CPU", limitCpu);
		lcItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				limitCpu = lcItem.getState();
			}
		});
		systemMenu.add(lcItem);

		final JCheckBoxMenuItem sdItem = new JCheckBoxMenuItem("Synchronous drawing", syncDrawing);
		sdItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				syncDrawing = sdItem.getState();
			}
		});
		systemMenu.add(sdItem);

		systemMenu.addSeparator();

		final JCheckBoxMenuItem sqItem = new JCheckBoxMenuItem("Show Quality", showQuality);
		sqItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showQuality = sqItem.getState();
			}
		});
		systemMenu.add(sqItem);
	}

	/*************************************************************************
	 ** Preset Menue
	 *************************************************************************/

	/* Erzeuge das Preset-Menue neu */

	private void updatePresetMenu() {

		presetMenu.removeAll();

		final File dir = Util.findDir(presetDirName);
		JMenuItem menuItem = new JMenuItem("Store preset...");
		presetMenu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = JOptionPane.showInputDialog(Application.this, "Enter name", presetName);
				if (name != null) {
					try {
						PrintStream out = new PrintStream(new File(dir, name));
						uiStorePreset(out);
						out.close();
						presetName = name;
					} catch (Exception exc) {
						exc.printStackTrace();
					}
					updatePresetMenu();
				}
			}
		});

		presetMenu.addSeparator();

		String[] list = dir.list();
		Arrays.sort(list);
		for (final String item : list) {
			menuItem = new JMenuItem(item);
			presetMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						InputStream in = new FileInputStream(new File(dir, item));
						uiSetPreset(in);
						in.close();
						presetName = item;
					} catch (IOException exc) {
						exc.printStackTrace();
					}
					updatePresetMenu();
				}
			});
		}

		if (presetName != null) {

			presetMenu.addSeparator();

			menuItem = new JMenuItem("Delete " + presetName);
			presetMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (JOptionPane.showConfirmDialog(Application.this, "Really delete " + presetName + "?",
							"Confirm delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						new File(dir, presetName).delete();
						updatePresetMenu();
					}
				}
			});
		}
	}

	/*************************************************************************
	 ** Preset laden / speichern
	 *************************************************************************/

	/* Setze Applikationseinstellungen */

	private void setApplicationSetting(String key, String value) {
		if (key.equals("antialias"))
			uiSetAntialias(value.equals("true"));
		if (key.equals("drawGrid"))
			uiSetDrawGrid(value.equals("true"));
		if (key.equals("drawGridText"))
			uiSetDrawGridText(value.equals("true"));
		if (key.equals("drawEdgeGroups"))
			uiSetDrawEdgeGroups(value.equals("true"));
		if (key.equals("drawHalo"))
			uiSetDrawHalo(value.equals("true"));
		if (key.equals("drawForces"))
			uiSetDrawForces(value.equals("true"));
		if (key.equals("keepAspect"))
			uiSetKeepAspect(value.equals("true"));
		if (key.equals("highlight"))
			uiSetHighlight(value.equals("true"));
		if (key.equals("showMark"))
			uiSetShowMark(value.equals("true"));
		if (key.equals("limitCpu"))
			limitCpu = value.equals("true");
		if (key.equals("syncDrawing"))
			syncDrawing = value.equals("true");
		if (key.equals("fixTranslation"))
			model.setFixTranslation(value.equals("true"));
		if (key.equals("fixRotation"))
			model.setFixRotation(value.equals("true"));
	}

	/* Fasse Applikationseinstellungen zusammen */

	private String[][] getApplicationSettings() {
		return new String[][] { { "antialias", "" + drawing.getAntialias() },
				{ "drawGrid", "" + drawing.getDrawGrid() }, { "drawGridText", "" + drawing.getDrawGridText() },
				{ "drawEdgeGroups", "" + drawing.getDrawEdgeGroups() }, { "drawHalo", "" + drawing.getDrawHalo() },
				{ "keepAspect", "" + drawing.getKeepAspect() }, { "highlight", "" + drawing.getHighlight() },
				{ "showMark", "" + drawing.getShowMark() }, { "drawForces", "" + drawing.getDrawForces() },
				{ "limitCpu", "" + limitCpu }, { "syncDrawing", "" + syncDrawing },
				{ "fixTranslation", "" + model.getFixTranslation() }, { "fixRotation", "" + model.getFixRotation() } };
	}

	/* Schreibe den aktuellen Zustand im Preset-Format auf einen Stream out */

	private void storePreset(PrintStream out) {

		for (String[] kv : getApplicationSettings())
			out.println("ApplicationSetting|" + kv[0] + "|" + kv[1]);

		Graph graph = model.getGraph();
		if (graph != null) {
			out.println("Graph|" + graph.getName());
			for (ValueControl c : Util.getValueControls(graph))
				c.serialize(out);
		}

		Embedder embedder = model.getEmbedder();
		if (embedder != null) {
			out.println("Embedder|" + embedder.getName());
			for (ValueControl c : Util.getValueControls(embedder))
				c.serialize(out);
		}

		REModel reModel = model.getREModel();
		if (reModel != null) {
			out.println("REModel|" + reModel.getName());
			for (ValueControl c : Util.getValueControls(reModel))
				c.serialize(out);
		}

		PathManager pathManager = model.getPathManager();
		if (pathManager != null) {
			out.println("PathManager|" + pathManager.getName());
			for (ValueControl c : Util.getValueControls(pathManager))
				c.serialize(out);
		}

		out.println("Model|default");
		for (ValueControl c : Util.getValueControls(model))
			c.serialize(out);
	}

	/* Lade den aktuellen Zustand von einem Stream in */

	private void setPreset(InputStream in) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		ControlProvider cp = null;
		Graph setGraph = null;
		Embedder setEmbedder = null;
		REModel setREModel = null;
		PathManager setPathManager = null;

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] tokens = line.split("\\|");
			if (tokens[0].equals("Graph")) {
				String name = tokens[1];
				for (Graph gr : graphs) {
					if (gr.getName().equals(name)) {
						cp = setGraph = gr;
						break;
					}
				}
			} else if (tokens[0].equals("Embedder")) {
				String name = tokens[1];
				for (Embedder emb : embedders) {
					if (emb.getName().equals(name)) {
						cp = setEmbedder = emb;
						break;
					}
				}
			} else if (tokens[0].equals("REModel")) {
				String name = tokens[1];
				for (REModel rm : reModels) {
					if (rm.getName().equals(name)) {
						cp = setREModel = rm;
						break;
					}
				}
			} else if (tokens[0].equals("PathManager")) {
				String name = tokens[1];
				for (PathManager pm : pathManagers) {
					if (pm.getName().equals(name)) {
						cp = setPathManager = pm;
						break;
					}
				}
			} else if (tokens[0].equals("Model")) {
				cp = model;
			} else if (tokens[0].equals("Control")) {
				String name = tokens[1];
				String value = tokens[2];
				for (ValueControl c : Util.getValueControls(cp)) {
					if (c.getName().equals(name)) {
						c.setValueFromString(value);
						break;
					}
				}
			} else if (tokens[0].equals("ApplicationSetting")) {
				String name = tokens[1];
				String value = tokens[2];
				setApplicationSetting(name, value);
			}
		}

		if (setGraph != null)
			setGraph(setGraph);
		if (setEmbedder != null)
			setEmbedder(setEmbedder);
		if (setREModel != null)
			setREModel(setREModel);
		if (setPathManager != null)
			setPathManager(setPathManager);

		buildMenu();
	}
}
