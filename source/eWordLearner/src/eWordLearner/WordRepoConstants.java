package eWordLearner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


public class WordRepoConstants {

	private static final List<Image> createdImages = new ArrayList<Image>();
	public static final Image IMAGE_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	public static final Image IMAGE_FILE = createImage("icons/file.jpg");
	public static final Image IMAGE_UP = createImage("icons/up.gif");
	public static final Image IMAGE_DOWN = createImage("icons/down.gif");
	public static final Image IMAGE_PLAY_URL = createImage("icons/play_url.png");
	public static final Image FAV_MUSIC_FILE = createImage("icons/fav.png");
	public static final Image IMAGE_GOOGLE = createImage("icons/google.png");
	public static final Image IMAGE_PHOCAB_LOGO = createImage("icons/phocab.png");
	public static final Image IMAGE_NOT_AVAILABLE = createImage("icons/noimage.jpg");
	public static final Image IMAGE_REFRESH = createImage("icons/refresh.gif");
	public static final Image IMAGE_SEARCH = createImage("icons/search.png");
	public static final Image IMAGE_SOUND = createImage("icons/sound.png");


	static {

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				Display.getDefault().disposeExec(new Runnable() {
					@Override
					public void run() {
						for (Image image : createdImages) {
							image.dispose();
						}
					}
				});
			}
		});

	}

	private static Image createImage(String filePath) {
		Image image = ImageDescriptor.createFromFile(eWordLearnerActivator.class, filePath).createImage();
		createdImages.add(image);
		return image;
	}

}
