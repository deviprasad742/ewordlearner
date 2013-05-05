package eWordLearner.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eWordLearner.model.DefaultWordRepository;

public class ELearnerImageDialog extends Dialog {

	Map<String, Image> imagesCache = new HashMap<String, Image>();
	private List<Entry<String, String>> definitons;
	private int currentIndex = 1;
	private Button nextButton;
	private Button prevButton;
	private Canvas canvas;
	private Composite container;
	
	public ELearnerImageDialog(Shell shell, Map<String, String> source) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		definitons = new ArrayList<Map.Entry<String,String>>(source.entrySet());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		canvas = new Canvas(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(canvas);

		Composite btnComposite = new Composite(container, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).applyTo(btnComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(btnComposite);
		
		prevButton = new Button(btnComposite, SWT.PUSH);
		prevButton.setText("<");
		GridDataFactory.swtDefaults().applyTo(prevButton);
		prevButton.addSelectionListener(getButtonListener(false));
		
		nextButton = new Button(btnComposite, SWT.PUSH);
		nextButton.setText(">");
		GridDataFactory.swtDefaults().applyTo(nextButton);
		nextButton.addSelectionListener(getButtonListener(true));
		
		updateUIAndImage();
		
		return container;
	}
	
	private SelectionAdapter getButtonListener(final boolean incrementIndex) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentIndex = incrementIndex ? ++currentIndex : --currentIndex;
				updateUIAndImage();
			}
		};
	}

	private void updateUIAndImage() {
		Entry<String, String> entry = definitons.get(currentIndex);
		String title = entry.getKey();
		String url = DefaultWordRepository.IMAGE_URL_PREFIX + title  + DefaultWordRepository.IMAGE_EXT;
		String definiton = entry.getValue();
		Image image = imagesCache.get(url);
		String index  = "(" + (currentIndex + 1)  + " of " + definitons.size() + ")";
		getShell().setText(title + index);
		if (image == null) {
			ImageDescriptor descriptor;
			try {
				descriptor = ImageDescriptor.createFromURL(new URL(url));
				image = descriptor.createImage();
				imagesCache.put(url, image);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				canvas.setToolTipText(e.getMessage());
				getShell().setText("Failed to load image " + title);
				return;
			}
		}
		canvas.setBackgroundImage(image);
		canvas.update();
		canvas.setToolTipText(definiton);
		Rectangle bounds = image.getBounds();
		int x_hint = Math.max(bounds.width, 100);
		int y_hint = Math.max(bounds.height, 100);
		x_hint = Math.min(x_hint, 400);
		y_hint = Math.min(y_hint, 400);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).hint(x_hint, y_hint).applyTo(canvas);
		GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).applyTo(container);
		//update buttons
		prevButton.setEnabled(currentIndex > 0);
		nextButton.setEnabled(currentIndex < definitons.size() - 1);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getShell().pack();
			}
		});
	}
	
	
	
	@Override
	public boolean close() {
		for (Image image : imagesCache.values()) {
			image.dispose();
		}
		return super.close();
	}

	public static void main(String[] args) {
          new ELearnerImageDialog(null, new DefaultWordRepository().getDefinitions(null)).open();
	}

}
