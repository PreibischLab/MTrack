package mt.listeners;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import fit.AbstractFunction2D;
import fit.PointFunctionMatch;
import fit.polynomial.HigherOrderPolynomialFunction;
import fit.polynomial.InterpolatedPolynomial;
import fit.polynomial.LinearFunction;
import fit.polynomial.Polynomial;
import fit.polynomial.QuadraticFunction;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import mpicbg.models.Point;
import mt.DisplayPoints;
import mt.FLSobject;
import mt.LengthCounter;
import mt.LengthDistribution;
import mt.RansacFileChooser;
import mt.Tracking;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import ransacBatch.BatchRANSAC;
import trackerType.TrackModel;

public class InteractiveRANSAC implements PlugIn {
	public static int MIN_SLIDER = 0;
	public static int MAX_SLIDER = 500;

	public static double MIN_ERROR = 0.0;
	public static double MAX_ERROR = 30.0;

	public static double MIN_RES = 1.0;
	public static double MAX_RES = 30.0;

	public static double MAX_ABS_SLOPE = 100.0;
	ResultsTable rtAll;
	public static double MIN_CAT = 0.0;
	public static double MAX_CAT = 100.0;
	public File inputfile;
	public File[] inputfiles;
	public String inputdirectory;
	public NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);

	public ArrayList<Pair<LinearFunction, ArrayList<PointFunctionMatch>>> linearlist;
	final Frame  jFreeChartFrame;
	public int functionChoice = 1; // 0 == Linear, 1 == Quadratic interpolated, 2 ==
								// cubic interpolated
	AbstractFunction2D function;
	public double lambda;
	ArrayList<Pair<Integer, Double>> mts;
	ArrayList<Point> points;
	public final int numTimepoints, minTP, maxTP;

	public int countfile;
	Scrollbar lambdaSB;
	Label lambdaLabel;

	final XYSeriesCollection dataset;
	final JFreeChart chart;
	int updateCount = 0;
	public ArrayList<Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>>> segments;
	public ArrayList<LinearFunction> linearsegments;
	public
	// for scrollbars
	int maxErrorInt, lambdaInt, minSlopeInt, maxSlopeInt, minDistCatInt, restoleranceInt;

	public double maxError = 3.0;
	public double minSlope = 0.1;
	public double maxSlope = 100;
	public double restolerance = 5;
	public double tptolerance = 2;
	public int maxDist = 300;
	public int minInliers = 50;
	public boolean detectCatastrophe = true;
	ArrayList<Pair<Integer, Double>> lifecount  ;
	public double minDistanceCatastrophe = 2;
	public final boolean serial;
	ArrayList<File> AllMovies;
	protected boolean wasCanceled = false;

	public InteractiveRANSAC(final ArrayList<Pair<Integer, Double>> mts, File file) {
		this(mts, 0, 300, 3.0, 0.1, 10.0, 10, 50, 1, 0.1, file);
		nf.setMaximumFractionDigits(5);
	}

	
	public InteractiveRANSAC(File[] file) {
		this(0, 300, 3.0, 0.1, 10.0, 10, 50, 1, 0.1, file);
		nf.setMaximumFractionDigits(5);
	}
	
	
	public InteractiveRANSAC(final ArrayList<Pair<Integer, Double>> mts, final int minTP, final int maxTP,
			final double maxError, final double minSlope, final double maxSlope, final int maxDist,
			final int minInliers, final int functionChoice, final double lambda, final File file) {
		this.minTP = minTP;
		this.maxTP = maxTP;
		this.numTimepoints = maxTP - minTP + 1;
		this.functionChoice = functionChoice;
		this.lambda = lambda;
		this.mts = mts;
		this.points = Tracking.toPoints(mts);
		this.inputfile = file;
		this.inputdirectory = file.getParent();
		this.maxError = maxError;
		this.minSlope = minSlope;
		this.maxSlope = maxSlope;
		this.maxDist = Math.min(maxDist, numTimepoints);
		this.minInliers = Math.min(minInliers, numTimepoints);

		this.serial = false;
		if (this.minSlope >= this.maxSlope)
			this.minSlope = this.maxSlope - 0.1;

		this.maxErrorInt = computeScrollbarPositionFromValue(MAX_SLIDER, this.maxError, MIN_ERROR, MAX_ERROR);
		this.lambdaInt = computeScrollbarPositionFromValue(MAX_SLIDER, this.lambda, 0.0, 1.0);
		this.minSlopeInt = computeScrollbarPositionValueFromDoubleExp(MAX_SLIDER, this.minSlope, MAX_ABS_SLOPE);
		this.maxSlopeInt = computeScrollbarPositionValueFromDoubleExp(MAX_SLIDER, this.maxSlope, MAX_ABS_SLOPE);
		this.maxError = computeValueFromScrollbarPosition(this.maxErrorInt, MAX_SLIDER, MIN_ERROR, MAX_ERROR);
		this.minSlope = computeValueFromDoubleExpScrollbarPosition(this.minSlopeInt, MAX_SLIDER, MAX_ABS_SLOPE);
		this.maxSlope = computeValueFromDoubleExpScrollbarPosition(this.maxSlopeInt, MAX_SLIDER, MAX_ABS_SLOPE);
		this.dataset = new XYSeriesCollection();
		this.chart = Tracking.makeChart(dataset, "Microtubule Length Plot", "Timepoint", "MT Length");
		this.jFreeChartFrame = Tracking.display(chart, new Dimension(500, 400));

	};

	public InteractiveRANSAC(final int minTP, final int maxTP,
			final double maxError, final double minSlope, final double maxSlope, final int maxDist,
			final int minInliers, final int functionChoice, final double lambda, final File[] file) {
		this.minTP = minTP;
		this.maxTP = maxTP;
		this.numTimepoints = maxTP - minTP + 1;
		this.functionChoice = functionChoice;
		this.lambda = lambda;
		this.mts = null;
		this.points= null;
		this.inputfiles = file;
		this.inputdirectory = file[0].getParent();
		this.maxError = maxError;
		this.minSlope = minSlope;
		this.maxSlope = maxSlope;
		this.maxDist = Math.min(maxDist, numTimepoints);
		this.minInliers = Math.min(minInliers, numTimepoints);

		this.serial = true;
		if (this.minSlope >= this.maxSlope)
			this.minSlope = this.maxSlope - 0.1;

		this.maxErrorInt = computeScrollbarPositionFromValue(MAX_SLIDER, this.maxError, MIN_ERROR, MAX_ERROR);
		this.lambdaInt = computeScrollbarPositionFromValue(MAX_SLIDER, this.lambda, 0.0, 1.0);
		this.minSlopeInt = computeScrollbarPositionValueFromDoubleExp(MAX_SLIDER, this.minSlope, MAX_ABS_SLOPE);
		this.maxSlopeInt = computeScrollbarPositionValueFromDoubleExp(MAX_SLIDER, this.maxSlope, MAX_ABS_SLOPE);
		this.maxError = computeValueFromScrollbarPosition(this.maxErrorInt, MAX_SLIDER, MIN_ERROR, MAX_ERROR);
		this.minSlope = computeValueFromDoubleExpScrollbarPosition(this.minSlopeInt, MAX_SLIDER, MAX_ABS_SLOPE);
		this.maxSlope = computeValueFromDoubleExpScrollbarPosition(this.maxSlopeInt, MAX_SLIDER, MAX_ABS_SLOPE);
		this.dataset = new XYSeriesCollection();
		this.chart = Tracking.makeChart(dataset, "Microtubule Length Plot", "Timepoint", "MT Length");
		this.jFreeChartFrame = Tracking.display(chart, new Dimension(500, 400));

	};
	
	
	
	@Override
	public void run(String arg) {
		/* JFreeChart */
		
	rtAll = new ResultsTable();
	 lifecount = new ArrayList<Pair<Integer, Double>>();
	 countfile = 0;
	 AllMovies = new ArrayList<File>();
		if (!serial){
			
			
			linearlist = new ArrayList<Pair<LinearFunction, ArrayList<PointFunctionMatch>>>();
			this.dataset.addSeries(Tracking.drawPoints(mts));
			Tracking.setColor(chart, 0, new Color(64, 64, 64));
			Tracking.setStroke(chart, 0, 0.2f);
		Card();
		updateRANSAC();
		}
		
		if (serial){
			
			
			
			
	    CardTable();
	    
	    
		}
		

	}

	
	public JFrame Cardframe = new JFrame("Welcome to Ransac Rate and Statistics Analyzer ");
	public JPanel panelCont = new JPanel();
	public JPanel panelFirst = new JPanel();
	public JPanel panelSecond = new JPanel();
	JFileChooser chooserA;
	String choosertitleA;
	public void Card() {
		
		CardLayout cl = new CardLayout();

		panelCont.setLayout(cl);
		
		panelCont.add(panelFirst, "1");
		panelCont.add(panelSecond, "2");
		
		panelFirst.setName("Ransac fits for rates and frequency analysis");
		
		panelSecond.setName("Statistical Analysis");

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		final Scrollbar maxErrorSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxErrorInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar minInliersSB = new Scrollbar(Scrollbar.HORIZONTAL, this.minInliers, 1, 2, numTimepoints + 1);
		final Scrollbar maxDistSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxDist, 1, 0, numTimepoints + 1);
		final Scrollbar minSlopeSB = new Scrollbar(Scrollbar.HORIZONTAL, this.minSlopeInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar maxSlopeSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxSlopeInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);

		final Choice choice = new Choice();
		choice.add("Linear Function only");
		choice.add("Quadratic function regularized with Linear Function");
		choice.add("Cubic Function regularized with Linear Function");

		this.lambdaSB = new Scrollbar(Scrollbar.HORIZONTAL, this.lambdaInt, 1, MIN_SLIDER, MAX_SLIDER + 1);

		final Label maxErrorLabel = new Label("Max. Error (px) = " + this.maxError, Label.CENTER);
		final Label minInliersLabel = new Label("Min. #Points (tp) = " + this.minInliers, Label.CENTER);
		final Label maxDistLabel = new Label("Max. Gap (tp) = " + this.maxDist, Label.CENTER);
		this.lambdaLabel = new Label("Linearity (fraction) = " + this.lambda, Label.CENTER);
		final Label minSlopeLabel = new Label("Min. Segment Slope (px/tp) = " + this.minSlope, Label.CENTER);
		final Label maxSlopeLabel = new Label("Max. Segment Slope (px/tp) = " + this.maxSlope, Label.CENTER);
		final Label maxResLabel = new Label(
				"MT is rescued if the start of event# i + 1 > start of event# i by px =  " + this.restolerance,
				Label.CENTER);

		final Checkbox findCatastrophe = new Checkbox("Detect Catastrophies", this.detectCatastrophe);
		final Scrollbar minCatDist = new Scrollbar(Scrollbar.HORIZONTAL, this.minDistCatInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar maxRes = new Scrollbar(Scrollbar.HORIZONTAL, this.restoleranceInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Label minCatDistLabel = new Label("Min. Catatastrophy height (tp) = " + this.minDistanceCatastrophe,
				Label.CENTER);
		final Button done = new Button("Done");
		final Button batch = new Button("Save Parameters for Batch run");
		final Button cancel = new Button("Cancel");
		final Button Write = new Button("Save Rates and Frequencies to File");
		choice.select(functionChoice);
		setFunction();

		// Location
		panelFirst.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		
		
		panelFirst.add(maxErrorSB, c);

		++c.gridy;
		panelFirst.add(maxErrorLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelFirst.add(minInliersSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(minInliersLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelFirst.add(maxDistSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(maxDistLabel, c);

		++c.gridy;
		c.insets = new Insets(30, 0, 0, 0);
		panelFirst.add(choice, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelFirst.add(lambdaSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(lambdaLabel, c);

		++c.gridy;
		c.insets = new Insets(30, 0, 0, 0);
		panelFirst.add(minSlopeSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(minSlopeLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelFirst.add(maxSlopeSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(maxSlopeLabel, c);

		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelFirst.add(findCatastrophe, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelFirst.add(minCatDist, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelFirst.add(minCatDistLabel, c);

		++c.gridy;
		c.insets = new Insets(30, 150, 0, 150);
		panelFirst.add(Write, c);

	

		++c.gridy;
		c.insets = new Insets(30, 150, 0, 150);
		panelFirst.add(batch, c);

	

		maxErrorSB.addAdjustmentListener(new MaxErrorListener(this, maxErrorLabel, maxErrorSB));
		minInliersSB.addAdjustmentListener(new MinInliersListener(this, minInliersLabel, minInliersSB));
		maxDistSB.addAdjustmentListener(new MaxDistListener(this, maxDistLabel, maxDistSB));
		choice.addItemListener(new FunctionItemListener(this));
		lambdaSB.addAdjustmentListener(new LambdaListener(this, lambdaLabel, lambdaSB));
		minSlopeSB.addAdjustmentListener(new MinSlopeListener(this, minSlopeSB, minSlopeLabel));
		maxSlopeSB.addAdjustmentListener(new MaxSlopeListener(this, maxSlopeSB, maxSlopeLabel));
		findCatastrophe
				.addItemListener(new CatastrophyCheckBoxListener(this, findCatastrophe, minCatDistLabel, minCatDist));
		minCatDist.addAdjustmentListener(new MinCatastrophyDistanceListener(this, minCatDistLabel, minCatDist));

		Write.addActionListener(new WriteRatesListener(this));
		done.addActionListener(new FinishButtonListener(this, false));
		batch.addActionListener(new RansacBatchmodeListener(this));
		cancel.addActionListener(new FinishButtonListener(this, true));
		
		
		panelFirst.setVisible(true);
		cl.show(panelCont, "1");
		Cardframe.add(panelCont, BorderLayout.CENTER);
	
		Cardframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Cardframe.pack();
		Cardframe.setVisible(true);
		updateRANSAC();
	}
	
	
         public void CardTable() {
		
		CardLayout cl = new CardLayout();
		DefaultTableModel userTableModel = new DefaultTableModel(new Object[]{"Track File"}, 0) {
		    @Override
		    public boolean isCellEditable(int row, int column) {
		        return false;
		    }
		};
		
		
				
		
		for (int i = 0; i < inputfiles.length; ++i) {
			
			String[] currenttrack = {(inputfiles[i].getName())};
			userTableModel.addRow(currenttrack);
		}
		
		
		 JTable table = new JTable(userTableModel);
			
		 JScrollPane scrollPane = new JScrollPane(table);
		 scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		 
		panelCont.setLayout(cl);
		
		panelCont.add(panelFirst, "1");
		panelCont.add(panelSecond, "2");
		
		panelFirst.setName("Ransac fits for rates and frequency analysis");
		
	

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		final Scrollbar maxErrorSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxErrorInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar minInliersSB = new Scrollbar(Scrollbar.HORIZONTAL, this.minInliers, 1, 2, numTimepoints + 1);
		final Scrollbar maxDistSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxDist, 1, 0, numTimepoints + 1);
		final Scrollbar minSlopeSB = new Scrollbar(Scrollbar.HORIZONTAL, this.minSlopeInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar maxSlopeSB = new Scrollbar(Scrollbar.HORIZONTAL, this.maxSlopeInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);

		final Choice choice = new Choice();
		choice.add("Linear Function only");
		choice.add("Quadratic function regularized with Linear Function");
		choice.add("Cubic Function regularized with Linear Function");

		this.lambdaSB = new Scrollbar(Scrollbar.HORIZONTAL, this.lambdaInt, 1, MIN_SLIDER, MAX_SLIDER + 1);

		final Label maxErrorLabel = new Label("Max. Error (px) = " + this.maxError, Label.CENTER);
		final Label minInliersLabel = new Label("Min. #Points (tp) = " + this.minInliers, Label.CENTER);
		final Label maxDistLabel = new Label("Max. Gap (tp) = " + this.maxDist, Label.CENTER);
		this.lambdaLabel = new Label("Linearity (fraction) = " + this.lambda, Label.CENTER);
		final Label minSlopeLabel = new Label("Min. Segment Slope (px/tp) = " + this.minSlope, Label.CENTER);
		final Label maxSlopeLabel = new Label("Max. Segment Slope (px/tp) = " + this.maxSlope, Label.CENTER);
		final Label maxResLabel = new Label(
				"MT is rescued if the start of event# i + 1 > start of event# i by px =  " + this.restolerance,
				Label.CENTER);

		final Checkbox findCatastrophe = new Checkbox("Detect Catastrophies", this.detectCatastrophe);
		final Scrollbar minCatDist = new Scrollbar(Scrollbar.HORIZONTAL, this.minDistCatInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Scrollbar maxRes = new Scrollbar(Scrollbar.HORIZONTAL, this.restoleranceInt, 1, MIN_SLIDER,
				MAX_SLIDER + 1);
		final Label minCatDistLabel = new Label("Min. Catatastrophy height (tp) = " + this.minDistanceCatastrophe,
				Label.CENTER);
		final Button done = new Button("Done");
		final Button batch = new Button("Save Parameters for Batch run");
		final Button cancel = new Button("Cancel");
		final Button Write = new Button("Save Rates and Frequencies to File");
		final Button WriteStats = new Button("Compute Length and Lifetime distribution");
		final Button WriteAgain = new Button("Save Rates and Frequencies to File");
		choice.select(functionChoice);
		setFunction();

		// Location
		panelFirst.setLayout(layout);
		panelSecond.setLayout(layout);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 50);
		panelFirst.add(scrollPane, c);
		
		
		++c.gridy;
		panelSecond.add(maxErrorSB, c);

		++c.gridy;
		panelSecond.add(maxErrorLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelSecond.add(minInliersSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(minInliersLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelSecond.add(maxDistSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(maxDistLabel, c);

		++c.gridy;
		c.insets = new Insets(30, 0, 0, 0);
		panelSecond.add(choice, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelSecond.add(lambdaSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(lambdaLabel, c);

		++c.gridy;
		c.insets = new Insets(30, 0, 0, 0);
		panelSecond.add(minSlopeSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(minSlopeLabel, c);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelSecond.add(maxSlopeSB, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(maxSlopeLabel, c);

		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelSecond.add(findCatastrophe, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		c.insets = new Insets(10, 0, 0, 0);
		panelSecond.add(minCatDist, c);
		c.insets = new Insets(0, 0, 0, 0);

		++c.gridy;
		panelSecond.add(minCatDistLabel, c);

		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelFirst.add(Write, c);
	
		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelFirst.add(WriteStats, c);
		
		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelSecond.add(WriteAgain, c);
	

		++c.gridy;
		c.insets = new Insets(20, 120, 0, 120);
		panelSecond.add(batch, c);

		
		table.addMouseListener(new MouseAdapter() {
			  public void mouseClicked(MouseEvent e) {
			    if (e.getClickCount() == 1) {
			      JTable target = (JTable)e.getSource();
			      int row = target.getSelectedRow();
			      // do some action if appropriate column
			      if (row > 0)
			      displayclicked(row);
			      else
			      displayclicked(0);	  
			    }
			  }
			});

		maxErrorSB.addAdjustmentListener(new MaxErrorListener(this, maxErrorLabel, maxErrorSB));
		minInliersSB.addAdjustmentListener(new MinInliersListener(this, minInliersLabel, minInliersSB));
		maxDistSB.addAdjustmentListener(new MaxDistListener(this, maxDistLabel, maxDistSB));
		choice.addItemListener(new FunctionItemListener(this));
		lambdaSB.addAdjustmentListener(new LambdaListener(this, lambdaLabel, lambdaSB));
		minSlopeSB.addAdjustmentListener(new MinSlopeListener(this, minSlopeSB, minSlopeLabel));
		maxSlopeSB.addAdjustmentListener(new MaxSlopeListener(this, maxSlopeSB, maxSlopeLabel));
		findCatastrophe
				.addItemListener(new CatastrophyCheckBoxListener(this, findCatastrophe, minCatDistLabel, minCatDist));
		minCatDist.addAdjustmentListener(new MinCatastrophyDistanceListener(this, minCatDistLabel, minCatDist));

		Write.addActionListener(new WriteRatesListener(this));
		WriteStats.addActionListener(new WriteStatsListener(this));
		WriteAgain.addActionListener(new WriteRatesListener(this));
		done.addActionListener(new FinishButtonListener(this, false));
		batch.addActionListener(new RansacBatchmodeListener(this));
		cancel.addActionListener(new FinishButtonListener(this, true));
		
		
		panelFirst.setVisible(true);
		panelSecond.setVisible(true);
		
		
		
		cl.show(panelCont, "1");
		
		JPanel control = new JPanel();
		control.add(new JButton(new AbstractAction("\u22b2Prev") {

			@Override
			public void actionPerformed(ActionEvent e) {
				CardLayout cl = (CardLayout) panelCont.getLayout();
				cl.previous(panelCont);
			}
		}));
		control.add(new JButton(new AbstractAction("Next\u22b3") {

			@Override
			public void actionPerformed(ActionEvent e) {
				CardLayout cl = (CardLayout) panelCont.getLayout();
				cl.next(panelCont);
			}
		}));
		Cardframe.add(panelCont, BorderLayout.CENTER);
	
	
		Cardframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Cardframe.pack();
		Cardframe.add(control, BorderLayout.SOUTH);
		Cardframe.setVisible(true);
		Cardframe.pack();
		
	}
	
	
         public void displayclicked(final int trackindex){
     		

        	 this.inputfile = inputfiles[trackindex];
        	 this.inputdirectory = inputfiles[trackindex].getParent();
     		this.mts = Tracking.loadMT(inputfiles[trackindex]);
     		this.points = Tracking.toPoints(mts);
     		linearlist = new ArrayList<Pair<LinearFunction, ArrayList<PointFunctionMatch>>>();
     		dataset.removeAllSeries();
            this.dataset.addSeries(Tracking.drawPoints(mts));
			Tracking.setColor(chart, 0, new Color(64, 64, 64));
			Tracking.setStroke(chart, 0, 0.2f);
     		updateRANSAC();
     		
     		
     	}
     	
	
	
	public void setFunction() {
		if (functionChoice == 0) {
			this.function = new LinearFunction();
			this.setLambdaEnabled(false);
		} else if (functionChoice == 1) {
			this.setLambdaEnabled(true);
			// this.function = new QuadraticFunction();
			this.function = new InterpolatedPolynomial<LinearFunction, QuadraticFunction>(new LinearFunction(),
					new QuadraticFunction(), 1 - this.lambda);
		} else {
			this.setLambdaEnabled(true);
			this.function = new InterpolatedPolynomial<LinearFunction, HigherOrderPolynomialFunction>(
					new LinearFunction(), new HigherOrderPolynomialFunction(3), 1 - this.lambda);
		}

	}

	public void setLambdaEnabled(final boolean state) {
		if (state) {
			if (!lambdaSB.isEnabled()) {
				lambdaSB.setEnabled(true);
				lambdaLabel.setEnabled(true);
				lambdaLabel.setForeground(Color.BLACK);
			}
		} else {
			if (lambdaSB.isEnabled()) {
				lambdaSB.setEnabled(false);
				lambdaLabel.setEnabled(false);
				lambdaLabel.setForeground(Color.GRAY);
			}
		}
	}

	public void updateRANSAC() {
		++updateCount;

		for (int i = dataset.getSeriesCount() - 1; i > 0; --i)
			dataset.removeSeries(i);

		segments = Tracking.findAllFunctions(points, function, maxError, minInliers, maxDist);

		sort(segments);
		if (segments == null || segments.size() == 0) {
			--updateCount;
			return;
		}

		LinearFunction linear = new LinearFunction();
		int i = 1, segment = 1;

		for (final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> result : segments) {

			if (LinearFunction.slopeFits(result.getB(), linear, minSlope, maxSlope)) {

				final Pair<Double, Double> minMax = Tracking.fromTo(result.getB());

				dataset.addSeries(Tracking.drawFunction((Polynomial) result.getA(), minMax.getA(), minMax.getB(), 0.5,
						"Segment " + segment));
                
				if (functionChoice > 0) {
					Tracking.setColor(chart, i, new Color(255, 0, 0));
					Tracking.setStroke(chart, i, 1f);
				} else {
					Tracking.setColor(chart, i, new Color(0, 128, 0));
					Tracking.setStroke(chart, i, 0.5f);
				}

				++i;

				if (functionChoice > 0) {

					dataset.addSeries(Tracking.drawFunction(linear, minMax.getA(), minMax.getB(), 0.5,
							"Linear Segment " + segment));

					Tracking.setColor(chart, i, new Color(0, 128, 0));
					Tracking.setStroke(chart, i, 0.5f);

					++i;

				}
				
				

				dataset.addSeries(Tracking.drawPoints(Tracking.toPairList(result.getB()), "Inliers " + segment));

				Tracking.setColor(chart, i, new Color(255, 0, 0));
				Tracking.setDisplayType(chart, i, false, true);
				Tracking.setSmallUpTriangleShape(chart, i);

				++i;
				++segment;
			} else {
				System.out.println("Removed segment because slope is wrong.");
			}

		}

		if (this.detectCatastrophe) {
			if (segments.size() < 2) {
				System.out.println(
						"We have only " + segments.size() + " segments, need at least two to detect catastrophies.");
			} else {
				for (int catastrophy = 0; catastrophy < segments.size() - 1; ++catastrophy) {
					final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> start = segments.get(catastrophy);
					final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> end = segments.get(catastrophy + 1);

					final double tStart = start.getB().get(start.getB().size() - 1).getP1().getL()[0];
					final double tEnd = end.getB().get(0).getP1().getL()[0];

					final double lStart = start.getB().get(start.getB().size() - 1).getP1().getL()[1];
					final double lEnd = end.getB().get(0).getP1().getL()[1];

					final ArrayList<Point> catastropyPoints = new ArrayList<Point>();

					for (final Point p : points)
						if (p.getL()[0] >= tStart && p.getL()[0] <= tEnd)
							catastropyPoints.add(p);

					/*
					 * System.out.println( "\ncatastropy" ); for ( final Point p
					 * : catastropyPoints) System.out.println( p.getL()[ 0 ] +
					 * ", " + p.getL()[ 1 ] );
					 */

					if (catastropyPoints.size() > 2) {
						if (Math.abs(lStart - lEnd) >= this.minDistanceCatastrophe) {
							// maximally 1.1 timepoints between points on a line
							final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> fit = Tracking
									.findFunction(catastropyPoints, new LinearFunction(), 0.75, 3, 1.1);

							if (fit != null) {
								if (((LinearFunction) fit.getA()).getM() < 0) {
									sort(fit);

									segments.add(fit);

									double minY = Math.min(fit.getB().get(0).getP1().getL()[1],
											fit.getB().get(fit.getB().size() - 1).getP1().getL()[1]);
									double maxY = Math.max(fit.getB().get(0).getP1().getL()[1],
											fit.getB().get(fit.getB().size() - 1).getP1().getL()[1]);

									final Pair<Double, Double> minMax = Tracking.fromTo(fit.getB());

									dataset.addSeries(Tracking.drawFunction((Polynomial) fit.getA(), minMax.getA() - 1,
											minMax.getB() + 1, 0.1, minY - 2.5, maxY + 2.5, "C " + catastrophy));

									Tracking.setColor(chart, i, new Color(0, 0, 255));
									Tracking.setDisplayType(chart, i, true, false);
									Tracking.setStroke(chart, i, 2f);

									++i;

									dataset.addSeries(Tracking.drawPoints(Tracking.toPairList(fit.getB()),
											"C(inl) " + catastrophy));

									Tracking.setColor(chart, i, new Color(0, 0, 255));
									Tracking.setDisplayType(chart, i, false, true);
									Tracking.setShape(chart, i, ShapeUtilities.createDownTriangle(4f));

									++i;
								} else {
									System.out.println("Slope not negative: " + fit.getA());
								}
							} else {
								System.out.println("No function found.");
							}
						} else {
							System.out.println("Catastrophy height not sufficient " + Math.abs(lStart - lEnd) + " < "
									+ this.minDistanceCatastrophe);
						}
					} else {
						System.out.println("We have only " + catastropyPoints.size()
								+ " points, need at least three to detect this catastrophy.");
					}
				}
			}
		}

		--updateCount;
	}

	protected void sort(final Pair<? extends AbstractFunction2D, ArrayList<PointFunctionMatch>> segment) {
		Collections.sort(segment.getB(), new Comparator<PointFunctionMatch>() {

			@Override
			public int compare(final PointFunctionMatch o1, final PointFunctionMatch o2) {
				final double t1 = o1.getP1().getL()[0];
				final double t2 = o2.getP1().getL()[0];

				if (t1 < t2)
					return -1;
				else if (t1 == t2)
					return 0;
				else
					return 1;
			}
		});
	}
	
	protected void sortPoints(final ArrayList<Point> points) {
		Collections.sort(points, new Comparator<Point>() {

			@Override
			public int compare(final Point o1, final Point o2) {
				final double t1 = o1.getL()[0];
				final double t2 = o2.getL()[0];

				if (t1 < t2)
					return -1;
				else if (t1 == t2)
					return 0;
				else
					return 1;
			}
		});
	}

	protected void sort(final ArrayList<Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>>> segments) {
		for (final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> segment : segments)
			sort(segment);

		Collections.sort(segments, new Comparator<Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>>>() {
			@Override
			public int compare(Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> o1,
					Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> o2) {
				final double t1 = o1.getB().get(0).getP1().getL()[0];
				final double t2 = o2.getB().get(0).getP1().getL()[0];

				if (t1 < t2)
					return -1;
				else if (t1 == t2)
					return 0;
				else
					return 1;
			}
		});

		/*
		for (final Pair<AbstractFunction2D, ArrayList<PointFunctionMatch>> segment : segments) {
			System.out.println("\nSEGMENT");
			for (final PointFunctionMatch pm : segment.getB())
				System.out.println(pm.getP1().getL()[0] + ", " + pm.getP1().getL()[1]);
		}
		*/
	}

	public void close() {
		panelFirst.setVisible(false);
		Cardframe.dispose();
		jFreeChartFrame.setVisible(false);
		jFreeChartFrame.dispose();
	}

	protected static double computeValueFromDoubleExpScrollbarPosition(final int scrollbarPosition,
			final int scrollbarMax, final double maxValue) {
		final int maxScrollHalf = scrollbarMax / 2;
		final int scrollPos = scrollbarPosition - maxScrollHalf;

		final double logMax = Math.log10(maxScrollHalf + 1);

		final double value = Math.min(maxValue,
				((logMax - Math.log10(maxScrollHalf + 1 - Math.abs(scrollPos))) / logMax) * maxValue);

		if (scrollPos < 0)
			return -value;
		else
			return value;
	}

	protected static int computeScrollbarPositionValueFromDoubleExp(final int scrollbarMax, final double value,
			final double maxValue) {
		final int maxScrollHalf = scrollbarMax / 2;
		final double logMax = Math.log10(maxScrollHalf + 1);

		int scrollPos = (int) Math
				.round(maxScrollHalf + 1 - Math.pow(10, logMax - (Math.abs(value) / maxValue) * logMax));

		if (value < 0)
			scrollPos *= -1;

		return scrollPos + maxScrollHalf;
	}

	protected static double computeValueFromScrollbarPosition(final int scrollbarPosition, final int scrollbarMax,
			final double minValue, final double maxValue) {
		return minValue + (scrollbarPosition / (double) scrollbarMax) * (maxValue - minValue);
	}

	protected static int computeScrollbarPositionFromValue(final int scrollbarMax, final double value,
			final double minValue, final double maxValue) {
		return (int) Math.round(((value - minValue) / (maxValue - minValue)) * scrollbarMax);
	}

	
		    
	public static void main(String[] args) {
		
		new ImageJ();
		

		    JFrame frame = new JFrame("");
		    RansacFileChooser panel = new RansacFileChooser();
		 
		    frame.getContentPane().add(panel,"Center");
		    frame.setSize(panel.getPreferredSize());

	}
}
