package thaw.plugins;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;
import thaw.fcp.*;

public class StatusBar implements Runnable, Plugin {
	public final static int INTERVAL = 3000; /* in ms */
	public final static String SEPARATOR = "     ";

	private Core core;
	private boolean running = true;
	private Thread refresher;

	private boolean advancedMode = false;

	public boolean run(Core core) {
		this.core = core;

		this.advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();
		
		this.running = true;
		this.refresher = new Thread(this);

		this.refresher.start();

		return true;
	}

	public void run() {
		while(this.running) {

			try {
				Thread.sleep(INTERVAL);
			} catch(java.lang.InterruptedException e) {
				// pfff :P
			}

			this.updateStatusBar();

		}

	}

	public void updateStatusBar() {
		int progressDone = 0;
		int progressTotal = 0;

		int finished = 0;
		int failed = 0;
		int running = 0;
		int pending = 0;
		int total = 0;
		
		try {
			Vector runningQueue = this.core.getQueueManager().getRunningQueue();

			for(Iterator it = runningQueue.iterator();
			    it.hasNext(); ) {
				FCPTransferQuery query = (FCPTransferQuery)it.next();

				if(query.isRunning() && !query.isFinished()) {
					running++;
					progressTotal += 100;
					progressDone += query.getProgression();
				}

				if(query.isFinished() && query.isSuccessful()) {
					finished++;
					progressTotal += 100;
					progressDone += 100;
				}

				if(query.isFinished() && !query.isSuccessful()) {
					failed++;
				}
			}
			
			Vector[] pendingQueues = this.core.getQueueManager().getPendingQueues();

			for(int i =0 ; i < pendingQueues.length; i++) {
				
				progressTotal += pendingQueues[i].size() * 100;
				pending += pendingQueues[i].size();

			}

		} catch(java.util.ConcurrentModificationException e) {
			Logger.notice(this, "Collision !");
			this.core.getMainWindow().setStatus(this.core.getMainWindow().getStatus()+"*");
			return;
		}
		
		total = finished + failed + running + pending;

		String status = "Thaw "+Main.VERSION;

		if(this.advancedMode) {
			status = status
				+ SEPARATOR + I18n.getMessage("thaw.plugin.statistics.globalProgression") + " "
				+ Integer.toString(progressDone) + "/" + Integer.toString(progressTotal);
		}

		status = status
			+ SEPARATOR + I18n.getMessage("thaw.plugin.statistics.finished")+ " "
			+ Integer.toString(finished) + "/" + Integer.toString(total)
			+ SEPARATOR + I18n.getMessage("thaw.plugin.statistics.failed") + " "
			+ Integer.toString(failed) + "/" + Integer.toString(total)
			+ SEPARATOR + I18n.getMessage("thaw.plugin.statistics.running") + " "
			+ Integer.toString(running) + "/" + Integer.toString(total)
			+ SEPARATOR + I18n.getMessage("thaw.plugin.statistics.pending") + " "
			+ Integer.toString(pending) + "/" + Integer.toString(total);
			
		this.core.getMainWindow().setStatus(status);

	}


	public boolean stop() {
		this.running = false;

		this.core.getMainWindow().setStatus("Thaw "+Main.VERSION);

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.statistics.statistics");
	}


}
