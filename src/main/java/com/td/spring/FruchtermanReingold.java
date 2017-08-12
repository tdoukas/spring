package com.td.spring;
/*****************************************************************************
 * FruchtermanReingold.java
 *
 * Implementierung des Sping-Embedders von Fruchterman und Reingold (1991).
 *
 * Autor: Theo Doukas, Okt - Dez 2015.
 * $Id: FruchtermanReingold.java 257 2016-05-29 08:20:04Z dude $
 *****************************************************************************/

import java.util.Vector;

class FruchtermanReingold extends Embedder {

    private double k;                      // Die Konstante k, vgl. Gl. (6)
    private int faExp = 2;                 // Exponent der anziehenden Kraft
    private double[] dist = new double[3]; // fuer compDistance()
    private boolean firstRun = true;       // Erster Durchlauf?

    // Fuer gridbox-Algorithmus
    private Vector<Vector<Vertex>> gridBoxes = new Vector<Vector<Vertex>>();

    private double[] scaleRecord = new double[20]; // fuer simulierte
    private int scaleRecordPtr = 0;                // Abkuehlung

    /* Name des Embedders */

    String getName() { return "Fruchterman & Reingold (91)";  }

    /*************************************************************************
     **  Controls (Implementierung von ControlProvider, siehe Control.java)
     *************************************************************************/

    private RealControl CCtrl = 
	new LogarithmicControl("C", "Force constant", this, 5E2, 1E-1, 1E5);
    private IntControl faCtrl =
	new IntControl("faExp", "Attractive force exponent", this, 2, 1, 10);
    private RealControl tCtrl = 
	new LinearControl("Tmax", "Maximum temperature", this, 1, 0, 5);
    private BoolControl autoCtrl = 
	new BoolControl("Auto Temp", "Enable simulated annealing", this, 
			true);
    private BoolControl gridCtrl = 
	new BoolControl("Grid", "Use grid algorithm", this, false);

    public Control[] getControls() {
	return new Control[] { CCtrl, tCtrl, faCtrl, autoCtrl, gridCtrl };
    }

    public void valueChanged (Control ctrl) {}

    /*************************************************************************
     ** Abstossende und anziehende Kraft
     *************************************************************************/
    
    private double fr (double d) {
	return k*k/d;
    }

    private double fa (double d) {
	for (int i = 1; i < faExp; i++)
	    d *= d;
	return d/k;
    }

    /*************************************************************************
     **  Berechnungsschritt  
     *************************************************************************/

    /* Vorbereitung zur Berechnung, wird von Model aufgerufen */

    public void prepareStep (Graph g) {
	k = CCtrl.getValue() * Math.sqrt(1.0/g.v.size());
	faExp = faCtrl.getValue();
	if (autoCtrl.getValue())
	    controlTemp(g);
    }

    /* Berechne Kraefte dieses Embedders */

    public void computeForces(Graph g) {
	
	// Abstossende Kraefte: wirken zwischen allen Knoten

	if (gridCtrl.getValue()) {

	    // Variante: mit Grid

	    double[] bounds = g.getBounds();
	    double xmin = bounds[0];
	    double ymin = bounds[1];
	    double boundsWidth = (bounds[2] - xmin) * 1.1;
	    double boundsHeight = (bounds[3] - ymin) * 1.1;

	    // boxSize bestimmt Anzahl der Gridboxen, darum Mindestwert
	    // festlegen, so dass nicht mehr als maxBoxesPerAxis^2 generiert
	    // werden. Idealwert ist 2*k, aber das kann im Verhaeltnis sehr 
	    // klein werden, wenn die Zeichnung "davonlaeuft"

	    final int maxBoxesPerAxis = 100;
	    double boxSize =
		Math.max(Math.max(boundsWidth, boundsHeight) / maxBoxesPerAxis,
			 2*k);

	    int numX = 1 + (int) (boundsWidth / boxSize);
	    int numY = 1 + (int) (boundsHeight / boxSize);
	    int numGridBoxes = numX*numY;

	    for (int i = gridBoxes.size(); i < numGridBoxes; i++)
		gridBoxes.add(new Vector<Vertex>());
	    for (int i = 0; i < numGridBoxes; i++)
		gridBoxes.elementAt(i).clear();

	    // Knoten in GridBoxen einsortieren
	    
	    for (Vertex v: g.v) {

		int x = (int) ((v.x-xmin) / boxSize);
		int y = (int) ((v.y-ymin) / boxSize);
		gridBoxes.elementAt(y*numX+x).add(v);
	    }

	    // Berechne abstossende Kraefte nur fuer Knoten, die innerhalb
	    // der Distanz boxSize = 2*k liegen. Betrachte dazu nur Knoten
	    // innerhalb von angrenzenden GridBoxen.

	    for (Vertex v: g.v) {

		int x = (int) ((v.x-xmin) / boxSize);
		int y = (int) ((v.y-ymin) / boxSize);

		for (int i = -1; i <= 1; i++)
		    for (int j = -1; j <= 1; j++) {

			int bx = x+i;
			int by = y+j;

			if (bx < 0 || bx >= numX || by < 0 || by >= numY)
			    continue;

			for (Vertex u: gridBoxes.elementAt(by*numX+bx)) {
			    if (u!=v) {
				Util.compDistance(v, u, dist);
				double dx = dist[0], dy = dist[1];
				double d2 = dist[2];
				double d = Math.sqrt(d2);
				if (d>boxSize)
				    continue;
				double f = fr(d);

				// Abweichend von [FrRe91, S.1132] wird hier
				// Actio = Reactio implementiert

				double ddx = f * dx / d;
				double ddy = f * dy / d;
				ddx /= 2;
				ddy /= 2;
				v.dx += ddx;
				v.dy += ddy;
				u.dx -= ddx;
				u.dy -= ddy;
			    }
			}
		    }
	    }
	    
	} else {

	    // Variante: ohne Grid, Laufzeit O(|V|^2).
	    
	    for (Vertex v: g.v) {

		for (Vertex u: g.v)
		    if (u!=v) {
			Util.compDistance(v, u, dist);
			double dx = dist[0], dy = dist[1], d2 = dist[2];
			double d = Math.sqrt(d2);
			double f = fr(d);

			// Abweichend von [FrRe91, S.1132] wird hier
			// Actio = Reactio implementiert
			
			double ddx = f * dx / d;
			double ddy = f * dy / d;
			ddx /= 2;
			ddy /= 2;
			v.dx += ddx;
			v.dy += ddy;
			u.dx -= ddx;
			u.dy -= ddy;
		    }
	    }
	}

	// Anziehende Kraefte: wirken nur zwischen Kanten

	for (Edge e: g.e) {
	    Util.compDistance(e.v1, e.v2, dist);

	    double dx = dist[0], dy = dist[1], d2 = dist[2];
	    double d = Math.sqrt(d2);
	    double f = fa(d);
	    double ddx = f * dx / d;
	    double ddy = f * dy / d;

	    e.v1.dx -= ddx;
	    e.v1.dy -= ddy;
	    e.v2.dx += ddx;
	    e.v2.dy += ddy;
	}
    }

    /*************************************************************************
     **  Automatische Temperaturkontrolle 
     *************************************************************************/

    /* Begrenze die Kraefte, wird von Model aufgerufen */
    
    public void limitForces (Graph g) {
	limitForces(g, tCtrl.getValue());
    }

    /* Passe den Temperaturregler an, wird in jedem Durchgang aufgerufen
       (-> simulierte Abkuehlung, siehe Anhang D) */

    private void controlTemp(Graph g) {

	double[] bounds = g.getBounds();
	double boundsW = bounds[2] - bounds[0];
	double boundsH = bounds[3] - bounds[1];
	double scale = Math.sqrt(boundsW*boundsW + boundsH*boundsH);
	int n = scaleRecord.length;

	/* scale-Wert aufzeichnen */

	scaleRecord[scaleRecordPtr] = scale;
	scaleRecordPtr = (scaleRecordPtr + 1) % n;

	/* n Werte gesammelt: auswerten */

	if (scaleRecordPtr==0) {

	    /* Bilde zwei zeitlich aufeinanderfolgende Mittelwerte (lowSum,
	       highSum) */
	    
	    double lowSum = 0;
	    double highSum = 0;

	    for (int i = 0; i < n/2; i++)
		lowSum += scaleRecord[(scaleRecordPtr + i)%n];
	    for (int i = n/2; i < n; i++)
		highSum += scaleRecord[(scaleRecordPtr + i)%n];

	    /* Das Verhaeltnis r = highSum / lowSum kennzeichnet die zeitliche
	       Entwicklung von scale */

	    double r = highSum / lowSum;
	    double t = tCtrl.getValue();

	    /* Wenn sich die Groesse nur wenig veraendert -> langsam abkuehlen, 
	       sonst Temperatur erhoehen */
	    
	    t *= (Math.abs(r-1.0) < 0.01) ? 0.9 : 1.1;

	    /* Temperatur nahe null -> fertig */

	    if (t < 0.1) {
		t = 0;
		finished ();
	    }

	    tCtrl.setValue(Math.max(tCtrl.getMin(), 
				    Math.min(tCtrl.getMax(), t)));
	}

	/* Beim ersten Durchlauf: Temperatur auf Maximum setzen */

	if (firstRun)
	    tCtrl.setValue(tCtrl.getMax());

	firstRun = false;
    }

    /*************************************************************************
     **  Restart-Aktion
     *************************************************************************/

    /* Starte Embedder neu */

    void restart () {
	firstRun = true;
    }

}
