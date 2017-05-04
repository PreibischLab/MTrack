package listeners;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import interactiveMT.Interactive_MTDoubleChannel;

public class ShowBitimgListener implements ItemListener {
	
	
final Interactive_MTDoubleChannel parent;
	
	public ShowBitimgListener(final Interactive_MTDoubleChannel parent ){
		
		this.parent = parent;
	}
	
	
	
	@Override
	public void itemStateChanged(final ItemEvent arg0) {

		if (arg0.getStateChange() == ItemEvent.DESELECTED)
			parent.displayBitimg = false;
		else if (arg0.getStateChange() == ItemEvent.SELECTED)
			parent.displayBitimg = true;

	}
}
