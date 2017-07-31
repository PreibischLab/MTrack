package singleListeners;

import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;


import interactiveMT.Interactive_MTSingleChannel;
import interactiveMT.Interactive_MTSingleChannel.ValueChange;
import interactiveMT.Interactive_PSFAnalyze;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class SingleUnstability_ScoreHoughListener implements AdjustmentListener {
	final Label label;
	final float min, max;
	final int scrollbarSize;
	Interactive_MTSingleChannel parent;
	final Scrollbar Unstability_ScoreScrollbar;

	public SingleUnstability_ScoreHoughListener(final Interactive_MTSingleChannel parent, final Label label, final float min, final float max, final int scrollbarSize,
			final Scrollbar Unstability_ScoreScrollbar) {
		this.label = label;
		this.min = min;
		this.max = max;
		this.parent = parent;
		this.scrollbarSize = scrollbarSize;
		this.Unstability_ScoreScrollbar = Unstability_ScoreScrollbar;
		Unstability_ScoreScrollbar.addMouseListener( new StandardMouseListener( parent, ValueChange.SHOWMSERinHough ) );
	}
	
	
	

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		parent.Unstability_Score = (parent.computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize));

		Unstability_ScoreScrollbar.setValue(parent.computeScrollbarPositionFromValue((float) parent.Unstability_Score, min, max, scrollbarSize));

		label.setText("Unstability Score = " + parent.Unstability_Score);

		
	}
}