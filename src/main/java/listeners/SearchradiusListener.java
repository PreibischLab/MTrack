package listeners;

import java.awt.Label;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import interactiveMT.Interactive_MTDoubleChannel;
import interactiveMT.Interactive_MTDoubleChannel.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class SearchradiusListener implements AdjustmentListener {
	final Label label;
	final float min, max;
	 final Interactive_MTDoubleChannel parent;
	public SearchradiusListener( final Interactive_MTDoubleChannel parent,final Label label, final float min, final float max) {
		this.label = label;
		this.min = min;
		this.max = max;
		this.parent = parent;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		parent.initialSearchradius = parent.computeValueFromScrollbarPosition(event.getValue(), min, max, parent.scrollbarSize);
		label.setText("Initial Search Radius:  = " + parent.initialSearchradius);

		if (!parent.isComputing) {
			parent.updatePreview(ValueChange.iniSearch);
		} else if (!event.getValueIsAdjusting()) {
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(ValueChange.iniSearch);
		}
	}
}
