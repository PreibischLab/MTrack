package swingClasses;

import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import ij.IJ;
import interactiveMT.Interactive_MTSingleChannel;
import updateListeners.FinalPoint;

public class SingleProgressSkip extends SwingWorker<Void, Void> {

final Interactive_MTSingleChannel parent;
final int starttime;
final int endtime;
	
	public SingleProgressSkip(final Interactive_MTSingleChannel parent, final int starttime, final int endtime){
	
		this.parent = parent;
		this.starttime = starttime;
		this.endtime = endtime;
	}
	
	@Override
	protected Void doInBackground() throws Exception {

		
		
       

		int next = starttime;

		if (next < 2)
			next = 2;

		
		
		SingleTrack newtrack = new SingleTrack(parent);
		newtrack.Trackobject(next);
		

		return null;

	}

	@Override
	protected void done() {
		try {
			parent.jpb.setIndeterminate(false);
			get();
			parent.frame.dispose();
			IJ.log("Tracking Done and track files written in the chosen folder");

		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}

	}
	
}
