package eWordLearner.model;

import java.io.File;
import java.util.prefs.Preferences;

import eWordLearner.eWordLearnerActivator;

public class WordPreferences {

	public static final String REPLACABLE_WORD= "$word";
	private static final String SEARCH_URL = "search_url";
	private static final String REPOSITORY_LOCATION = "repository_location";
	private static final WordPreferences instance = new WordPreferences();
	private Preferences preferences;
	private String defaultUrl = "https://www.google.com/search?as_q=" + REPLACABLE_WORD + " definition";
	
	private WordPreferences() {
		preferences = Preferences.systemRoot().node("eclipse").node("plugins").node("prefs").node("eWordLearner");
	}

	public static WordPreferences getInstance() {
		return instance;
	}

	public String getRepoLocation() {
      return preferences.get(REPOSITORY_LOCATION, getDefaultRepoLocation());
	}

	public void setRepoLocation(String location) {
		preferences.put(REPOSITORY_LOCATION, location);
	}
	
	public String getSearchUrl() {
	      return preferences.get(SEARCH_URL, defaultUrl);
	}
	
	public void setSearchUrl(String url) {
		preferences.put(SEARCH_URL, url);
	}
	

	public void store() {
		try {
			preferences.exportSubtree(System.out);
		} catch (Exception e) {
			eWordLearnerActivator.getDefault().logException(e);
		}
	}

	private String defaultRepoLocation;
	private String getDefaultRepoLocation() {
		if (defaultRepoLocation == null) {
			String userDir = System.getProperty("user.home");
			File dropBoxDir = new File(userDir, "Dropbox");
			File directory = new File(dropBoxDir, "eWordLearner");
			defaultRepoLocation = directory.getAbsolutePath();
		}
		return defaultRepoLocation;
	}

}
