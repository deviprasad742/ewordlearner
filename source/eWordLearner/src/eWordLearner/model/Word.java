package eWordLearner.model;

import org.eclipse.swt.graphics.Image;

/**
 * @author KH509
 *
 */
/**
 * @author KH509
 *
 */
public class Word {

	private String id;
	private String imageUrl;
	private String definition;
	private String siteUrl;
	private Image image;
	private int level;
	private String repository;
	private String soundFile;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getDefinition() {
		if (definition == null) {
			definition = "";
		}
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}
	
	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public String getRepository() {
		return repository;
	}
	
	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getSoundFile() {
		return soundFile;
	}
	
	public void setSoundFile(String soundFile) {
		this.soundFile = soundFile;
	}
	
	@Override
	public String toString() {
		return "Word [id=" + id + ", imageUrl=" + imageUrl + ", definition=" + definition + ", siteUrl=" + siteUrl + ", level=" + level + "]";
	}

	public boolean isLocal() {
		return getSiteUrl() == null;
	}
	
}
