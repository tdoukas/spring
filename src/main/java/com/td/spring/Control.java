package com.td.spring;
/*****************************************************************************
 * Control.java
 *
 * UI-Elemente zum Anpassen von Parametern
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: Control.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*****************************************************************************
 ** Interface zum Bereitstellen von Controls
 *****************************************************************************/

interface ControlProvider {

	/* Gebe Array aller Controls */

	Control[] getControls();

	/* Wird vom Control aufgerufen, wenn sich dessen Wert geaendert hat */

	void valueChanged(Control control);
}

/*****************************************************************************
 ** Oberklasse aller Kontrollen
 *****************************************************************************/

abstract class Control {

	/* Gebe Java Swing Widget zurueck */

	abstract JComponent getComponent();

	/* Control und Bedienelemente zuruecksetzen */

	void reset() {
	}
}

/*****************************************************************************
 ** Kontrolle fuer einen Wert (Abstrakt)
 *****************************************************************************/

abstract class ValueControl extends Control {

	private String name; // Name des Controls, als Label und zum Speichern
	private String desc; // Kurzbeschreibung fuer Tooltip
	private ControlProvider controlProvider; // Eigentuemer

	/* Konstruktur */

	ValueControl(String name, String desc, ControlProvider controlProvider) {
		this.name = name;
		this.desc = desc;
		this.controlProvider = controlProvider;
	}

	/* Gebe Name des Controls */

	public String getName() {
		return name;
	}

	/* Gebe Kurzbeschreibung des Controls */

	public String getDesc() {
		return desc;
	}

	/* Schreibt den Wert des Controls in einen Stream, zum Speichern. */

	void serialize(PrintStream out) {
		out.print("Control|" + getName() + "|");
		serializeValue(out);
		out.println();
	}

	/* Von Unterklasse zu implementieren: Methoden zum Speichern / Laden */

	void serializeValue(PrintStream out) {
	}

	void setValueFromString(String value) {
	}

	/* Notifier, wird von Unterklassen aufgerufen */

	public void notifyValueChanged() {
		if (controlProvider != null)
			controlProvider.valueChanged(this);
	}
}

/*****************************************************************************
 ** An / Aus Kontrolle
 *****************************************************************************/

class BoolControl extends ValueControl {

	private boolean value, defaultValue;
	private JToggleButton button; // Der eigentliche An/Aus-Button
	private JComponent component; // Das Widget, enthaelt den Button

	/* Konstruktor: konstruiert auch das Widget */

	BoolControl(String name, String desc, ControlProvider e, boolean value) {
		super(name, desc, e);
		this.value = this.defaultValue = value;

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		button = new JToggleButton(name);
		setValue(value);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BoolControl.this.value = button.isSelected();
			}
		});
		panel.add(button);

		component = panel;
		component.setToolTipText(desc);
	}

	/* Gebe Widget zurueck */

	JComponent getComponent() {
		return component;
	}

	/* Setze auf Default-Wert */

	void reset() {
		setValue(defaultValue);
	}

	/* Wert lesen und setzen */

	boolean getValue() {
		return value;
	}

	void setValue(boolean value) {
		this.value = value;
		button.setSelected(value);
	}

	/* Speichern und Laden */

	void serializeValue(PrintStream out) {
		out.print(getValue());
	}

	void setValueFromString(String value) {
		setValue(value.equals("true"));
	}
}

/*****************************************************************************
 ** Kontrolle mit Slider
 *****************************************************************************/

abstract class SliderControl extends ValueControl implements ChangeListener {

	private JSlider slider;
	private JLabel label;
	private int sliderValue;
	private String labelText;

	/* Konstruktor */

	SliderControl(String name, String desc, ControlProvider cp) {
		super(name, desc, cp);
	}

	/*
	 * Das Widget wird bei jeder Anforderung neu konstruiert -> resultiert in einem
	 * stabileren Layout
	 */

	JComponent getComponent() {

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		label = new JLabel(labelText);
		panel.add(label, BorderLayout.NORTH);

		slider = new JSlider(JSlider.HORIZONTAL, 0, 100, sliderValue);
		slider.addChangeListener(this);
		panel.add(slider, BorderLayout.CENTER);

		panel.setToolTipText(getDesc());
		updateLabel();

		return panel;
	}

	/* Java-Callback von ChangeListener */

	public void stateChanged(ChangeEvent e) {
		this.sliderValue = getSliderValue();
	}

	/* Setze den Labeltext */

	void setLabel(String labelText) {
		if (label != null)
			label.setText(labelText);
		this.labelText = labelText;
	}

	/* Setze die Slider-Position */

	void setSliderValue(int sliderValue) {
		if (slider != null)
			slider.setValue(sliderValue);
		this.sliderValue = sliderValue;
	}

	/* Gibt Slider-Position zurueck */

	int getSliderValue() {
		return slider.getValue();
	}

	/* Fordert die Instanz auf, das Label zu aktualisieren */

	abstract void updateLabel();
}

/*****************************************************************************
 ** Kontrolle fuer ganze Zahlen
 *****************************************************************************/

class IntControl extends SliderControl {

	private int min, max, value, defaultValue;

	/* Konstruktor */

	IntControl(String name, String desc, ControlProvider e, int value, int min, int max) {
		super(name, desc, e);
		this.min = min;
		this.max = max;
		this.value = this.defaultValue = value;
		setValue(value);
	}

	/* Java-Callback von ChangeListener */

	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);
		value = Math.round(min + (max - min) * getSliderValue() / 100.0f);
		updateLabel();
	}

	/* Wert lesen und setzen */

	void setValue(int value) {
		this.value = value;
		int sliderValue = ((value - min) * 100 / (max - min));
		setSliderValue(sliderValue);
	}

	int getValue() {
		return value;
	}

	/* Aktualisiere das Label */

	void updateLabel() {
		setLabel(getName() + "=" + getValue());
	}

	/* Setze Wert auf Defaultwert zurueck */

	void reset() {
		setValue(defaultValue);
	}

	/* Speichern und Laden */

	void serializeValue(PrintStream out) {
		out.print(getValue());
	}

	void setValueFromString(String value) {
		setValue(Integer.parseInt(value));
	}
}

/*****************************************************************************
 ** Kontrollen fuer reelle Zahlen (Linear und Logarithmisch)
 *****************************************************************************/

/** Abstrakte Klasse RealControl */

abstract class RealControl extends SliderControl {

	private double min, max, value, defaultValue;
	private String formatString; // Zur Darstellung des Labels

	/* Konstruktoren */

	RealControl(String name, String desc, ControlProvider e, double value, double min, double max,
			String formatString) {
		super(name, desc, e);
		this.min = min;
		this.max = max;
		this.value = this.defaultValue = value;
		this.formatString = formatString;
		setValue(value);
	}

	RealControl(String name, String desc, ControlProvider e, double value, double min, double max) {
		this(name, desc, e, value, min, max, "%.2e");
	}

	/* Auf Defaultwert zuruecksetzen */

	void reset() {
		setValue(defaultValue);
	}

	/* Java-Callback von ChangeListener */

	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);
		value = convert(getSliderValue());
		updateLabel();
	}

	/* Aktualisiere das Label */

	void updateLabel() {
		setLabel(getName() + "=" + String.format(formatString, value));
	}

	/* Die ueblichen getter und setter */

	double getMin() {
		return min;
	}

	double getMax() {
		return max;
	}

	double getValue() {
		return value;
	}

	void setValue(double value) {
		this.value = value;
		setSliderValue(convert(value));
		updateLabel();
	}

	/* Speichern und Laden */

	void serializeValue(PrintStream out) {
		out.print(getValue());
	}

	void setValueFromString(String value) {
		setValue(Double.parseDouble(value));
	}

	/*
	 * Bilde Sliderposition auf Real-Wert ab und umgekehrt, von Unterklassen zu
	 * implementieren
	 */

	abstract double convert(int intValue);

	abstract int convert(double doubleValue);
}

/**
 * Implementation von RealControl: LogarithmicControl, implementiert einen
 ** Slider mit logarithmischer Wirkung
 */

class LogarithmicControl extends RealControl {

	/* Konstruktor */

	LogarithmicControl(String name, String desc, ControlProvider e, double value, double min, double max) {
		super(name, desc, e, value, min, max);
	}

	/* Implementation der abstrakten Methoden von RealControl, s.o. */

	double convert(int intValue) {
		return getMin() * (Math.exp(Math.log(getMax() / getMin()) * 0.01 * intValue));
	}

	int convert(double value) {
		return (int) ((Math.log(value / getMin()) / Math.log(getMax() / getMin())) * 100);
	}
}

/**
 * Implementation von RealControl: LineaerControl, implementiert einen Slider
 ** mit linearer Wirkung
 */

class LinearControl extends RealControl {

	/* Konstruktoren */

	LinearControl(String name, String desc, ControlProvider e, double value, double min, double max) {
		super(name, desc, e, value, min, max);
	}

	LinearControl(String name, String desc, ControlProvider e, double value, double min, double max,
			String formatString) {
		super(name, desc, e, value, min, max, formatString);
	}

	/* Implementation der abstrakten Methoden von RealControl, s.o. */

	double convert(int intValue) {
		return (getMin() + (getMax() - getMin()) * (0.01 * intValue));
	}

	int convert(double value) {
		return (int) ((value - getMin()) * 100 / (getMax() - getMin()));
	}
}

/*****************************************************************************
 ** Kontrolle zur Auswahl einer Option (Dropdown-Menue)
 *****************************************************************************/

class OptionControl<E> extends ValueControl {

	private E[] options; // Ein Array mit den Optionen
	private JComboBox<E> box; // Die Auswahlbox
	private JComponent component; // Das Widget, enthaelt die Auswahlbox

	/* Konstruktor: konstruiert auch das Widget */

	OptionControl(String name, String desc, ControlProvider cp, E[] options, E value) {
		super(name, desc, cp);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(new JLabel(name));
		box = new JComboBox<E>();
		panel.add(box);
		component = panel;

		box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notifyValueChanged();
			}
		});

		if (options != null) {
			setOptions(options);
			if (value != null)
				setValue(value);
		}
	}

	/* Die ueblichen getter und setter */

	public JComponent getComponent() {
		return component;
	}

	void setValue(E value) {
		box.setSelectedItem(value);
	}

	Object getValue() {
		return box.getSelectedItem();
	}

	/* Laden und Speichern */

	public void serializeValue(PrintStream out) {
		out.print(getValue());
	}

	public void setValueFromString(String value) {
		for (E option : options)
			if (option.toString().equals(value)) {
				setValue(option);
				notifyValueChanged();
				break;
			}
	}

	/* Setze die Optionen neu */

	void setOptions(E[] options) {
		this.options = options;
		box.removeAllItems();
		for (E item : options) {
			box.addItem(item);
		}
	}
}

/*****************************************************************************
 ** Kontrolle zur Fileauswahl in einem Verzeichnis. Enthaelt einen Button zum
 * Neuladen des Verzeichnisinhalts.
 *****************************************************************************/

class FileSelectorControl extends ValueControl {

	private JComboBox<String> box; // Die Auswahlbox
	private JPanel panel; // Das Widget, enthaelt die Auswahlbox
	private File dir; // Das Verzeichnis
	private boolean actionsEnabled = true; // Auf Veraenderungen reagieren?

	/* Konstruktor: konstruiert auch das Widget */

	FileSelectorControl(String name, String desc, ControlProvider cp, File dir, String value) {
		super(name, desc, cp);
		this.dir = dir;
		box = new JComboBox<String>();
		setupBoxEntries();
		if (value != null)
			box.setSelectedItem(value);
		box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (actionsEnabled)
					notifyValueChanged();
			}
		});
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(box);
		JButton button = new JButton("Reload Dir.");
		panel.add(button);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setupBoxEntries();
			}
		});
		panel.setToolTipText(desc);
	}

	/* Die ueblichen getter, setter */

	JComponent getComponent() {
		return panel;
	}

	String getValue() {
		return (String) box.getSelectedItem();
	}

	void setValue(String value) {
		box.setSelectedItem(value);
	}

	/* Speichern und Laden */

	void serializeValue(PrintStream out) {
		out.print(getValue());
	}

	void setValueFromString(String value) {
		setValue(value);
	}

	/*
	 * Liest den Inhalt des Verzeichnisses und stellt die Auswahloptionen zusammen
	 */

	private void setupBoxEntries() {
		String[] list = dir.list();
		Arrays.sort(list);
		actionsEnabled = false;
		String selected = (String) box.getSelectedItem();
		box.removeAllItems();
		for (String item : list)
			box.addItem(item);
		if (selected != null)
			box.setSelectedItem(selected);
		actionsEnabled = true;
	}
}

/*****************************************************************************
 ** Ein Einfacher Button
 *****************************************************************************/

abstract class ButtonControl extends Control implements ActionListener {

	private String buttonText;

	/* Konstruktor */

	public ButtonControl(String buttonText) {
		this.buttonText = buttonText;
	}

	/* Liefere das Java-JComponent zurueck */

	JComponent getComponent() {
		JButton button = new JButton(buttonText);
		button.addActionListener(this);
		return button;
	}

	/* Callback: Button wurde geklickt */

	public void actionPerformed(ActionEvent e) {
		clicked();
	}

	/* Von der konkreten Implementation zu ueberschreiben */

	abstract void clicked();
}

/*****************************************************************************
 ** Ein einfacher Statustext
 *****************************************************************************/

class StatusControl extends Control {

	JTextPane textPane = new JTextPane();

	public StatusControl() {
	}

	/* Gebe Swing-JComponent zurueck */

	JComponent getComponent() {
		return textPane;
	}

	/* Setze den Text */

	public void setText(String text) {
		textPane.setText(text);
	}
}
