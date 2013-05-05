package eWordLearner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class eWordLearnerActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "eWordLearner"; //$NON-NLS-1$

	// The shared instance
	private static eWordLearnerActivator plugin;
	
	/**
	 * The constructor
	 */
	public eWordLearnerActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static eWordLearnerActivator getDefault() {
		return plugin;
	}
	

	public void logAndShowException(Exception e) {
		logAndShowException(null, e);
	}
	
	public void logAndShowException(String message, Exception e) {
		IStatus status = logException(message, e);
		ErrorDialog.openError(null, "Exception", status.getMessage(), status);
	}
	
	public IStatus logException(Exception e) {
		return logException(null, e);
	}
	
	public IStatus logException(String message, Exception e) {
		message = message == null ? e.getMessage() : message;
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
		getLog().log(status);
		return status;
	}

}
