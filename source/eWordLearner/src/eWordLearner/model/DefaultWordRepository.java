package eWordLearner.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eWordLearner.eWordLearnerActivator;

public class DefaultWordRepository implements IWordRepository {

	public static final String BASE_URL = "http://www.phocabulary.com/feed.php";
	public static final String IMAGE_URL_PREFIX = "http://www.phocabulary.com/words/";
	public static final String WORD_URL_PREFIX = "http://www.phocabulary.com/";
	private static Pattern TITLE_PATTERN = Pattern.compile(".*<title>(.*)</title>");
	private static Pattern DEFINITION_PATTERN = Pattern.compile(".*<!\\[CDATA\\[(.*)\\]\\]>.*");
	private static final String ID = "$default$";

	protected Map<String, String> cachedDefinitions = Collections.synchronizedMap(new HashMap<String, String>());
	protected Set<String> wordsList = new LinkedHashSet<String>();
	private boolean isInitialized = false;

	public DefaultWordRepository() {

	}

	public synchronized void initialize() {
		isInitialized = false;
		File definitionsFile = new File(getLocation(), IWordRepository.DEFINITIONS_FILE_NAME);
		getImagesDirectory();// initialize images folder
		try {
			cachedDefinitions.putAll(FileUtils.readMapFromFile(definitionsFile));
			String wordOrder = cachedDefinitions.get(WORD_ORDER);
			cachedDefinitions.remove(WORD_ORDER);// should not be part of definitions
			if (wordOrder != null && !wordOrder.trim().isEmpty()) {
				String[] split = wordOrder.split(WORD_SEPARATOR);
				wordsList.addAll(Arrays.asList(split));
			}
			isInitialized = true;
		} catch (Exception e) {
			eWordLearnerActivator.getDefault().logException(e);
		}

	}

	public String getId() {
		return ID;
	}


	@Override
	public synchronized Map<String, String> getDefinitions(IProgressMonitor monitor) {
		StringBuffer urlFeedBuffer = new StringBuffer();
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		if (isInternetConnected() && isWebRepository()) {
			try {
				URL url = new URL(BASE_URL);
				monitor.beginTask("Fetching definitons from '" + url + "'", IProgressMonitor.UNKNOWN);
				HttpURLConnection connection = null;
				try {
					connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					InputStreamReader inStream = new InputStreamReader(connection.getInputStream());
					BufferedReader buff = null;
					try {
						buff = new BufferedReader(inStream);
						String lastId = null;
						while (true) {
							String nextLine = buff.readLine();
							if (nextLine != null) {
								urlFeedBuffer.append(nextLine.trim());
								Matcher titleMatcher = TITLE_PATTERN.matcher(nextLine);
								if (titleMatcher.matches()) {
									lastId = titleMatcher.group(1);
									monitor.subTask("Loading definiton '" + lastId + "'");
								}
								Matcher definitonMatcher = DEFINITION_PATTERN.matcher(nextLine);
								if (definitonMatcher.matches()) {
									String defintion = definitonMatcher.group(1).trim();
									//add to local cached definitions
									if (!cachedDefinitions.containsKey(lastId)) { 
										System.out.println("New word '" + lastId + "' added to dictionary");
										cachedDefinitions.put(lastId, defintion);
									}
									if (!wordsList.contains(lastId)) {
										wordsList.add(lastId);
									}
								}
							} else {
								break;
							}
						}
					} finally {
						if (buff != null) {
							buff.close();
						}
					}
				} catch (IOException e) {
					eWordLearnerActivator.getDefault().logException(e);
				} finally {
					connection.disconnect();
				}

			} catch (MalformedURLException e) {
				eWordLearnerActivator.getDefault().logException(e);
			}
		}

		return getOrderedDefinitons(cachedDefinitions);
	}

	private Map<String, String> getOrderedDefinitons(Map<String, String> definitions) {
		Map<String, String> orderedMap = new LinkedHashMap<String, String>();
		if (wordsList.isEmpty()) {
			synchronized (definitions) {
				orderedMap.putAll(definitions);
			}
		}
		for (String id : wordsList) {
			safeAdd(definitions, orderedMap, id);
		}
		// add words not found in ordered list
		for (String id : definitions.keySet()) {
			if (!wordsList.contains(id)) {
				wordsList.add(id);
				safeAdd(definitions, orderedMap, id);
			}
		}

		return orderedMap;
	}

	private void safeAdd(Map<String, String> definitions, Map<String, String> linkedMap, String id) {
		String value = definitions.get(id);
		if (value == null) {
			value = "";
		}
		linkedMap.put(id, value);
	}

	public synchronized Word createWord(String id) {
		Word word = new Word();
		word.setId(id);
		word.setImageUrl(DefaultWordRepository.IMAGE_URL_PREFIX + id + DefaultWordRepository.IMAGE_EXT);
		if (isWebRepository()) {
			word.setSiteUrl(DefaultWordRepository.WORD_URL_PREFIX + id);
		}
		word.setDefinition(cachedDefinitions.get(id));
		word.setRepository(getId());
		return word;
	}

	public synchronized Word addWord(String id, String definition) {
		Word word = new Word();
		word.setId(id);
		word.setDefinition(definition);
		word.setRepository(getId());
		if (!wordsList.contains(id)) {
			wordsList.add(id);
		}
		cachedDefinitions.put(id, definition);
		return word;
	}

	public File fetchImage(Word word) throws IOException {
		File imageFile = getImageFile(word);
		if (isWebRepository()) {
			FileUtils.writeFile(word.getImageUrl(), imageFile);
			System.out.println("File copied: " + imageFile);
		}
		return imageFile;
	}

	public File getImageFile(Word word) {
		File imagesDirectory = getImagesDirectory();
		File imageFile = new File(imagesDirectory, word.getId() + DefaultWordRepository.IMAGE_EXT);
		return imageFile;
	}

	public synchronized void updateModel(Word word) {
		cachedDefinitions.put(word.getId(), word.getDefinition());
	}

	public synchronized void save() throws Exception {
		if (isInitialized) {
			StringBuilder wordOrder = new StringBuilder();
			for (String word : wordsList) {
				wordOrder.append(word);
				wordOrder.append(WORD_SEPARATOR);
			}
			removeCommaAtEndofBuffer(wordOrder);
			cachedDefinitions.put(WORD_ORDER, wordOrder.toString());
			if (cachedDefinitions.size() != 1) {
				FileUtils.writeMapToFile(cachedDefinitions, new File(getLocation(), IWordRepository.DEFINITIONS_FILE_NAME));
			} else {
				try {
					throw new Exception("Definitons are empty. May be a bug in implementation. If repository is created for first time this can be ignored.");
				} catch (Exception e) {
					eWordLearnerActivator.getDefault().logException(e);
				}
			}
		}
	}

	private String location;
	public String getLocation() {
		if (location == null) {
			location = computeLocation();
		}
		return location;
	}

	protected String computeLocation() {
		File directory = new File(WordPreferences.getInstance().getRepoLocation());
		directory.mkdirs();
		return directory.getAbsolutePath();
	}
	
	public File getRepositoryFolder() {
		// create user repositories folder
		File repos = new File(getLocation(), REPOSITORIES);
		if (repos.exists() && repos.isFile()) {
			repos.delete();
		}
		repos.mkdirs();

		File userDir = new File(repos, DEFAULT_USER);
		if (!userDir.exists()) {
			userDir.mkdir();
		}
		return repos;
	}

	private File imagesDir;

	private File getImagesDirectory() {
		if (imagesDir == null) {
			imagesDir = new File(getLocation(), "images");
			imagesDir.mkdirs();
		}
		return imagesDir;
	}

	private boolean isInternetConnected() {
		try {
			// make a URL to a known source
			URL url = new URL("http://www.google.com");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(5000);
			// trying to retrieve data from the source. If there
			// is no connection, this line will fail
			urlConnection.getContent();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected boolean isWebRepository() {
		return true;
	}
	
	@Override
	public synchronized void removeWord(Word word) {
		String id = word.getId();
		cachedDefinitions.remove(id);
		wordsList.remove(id);
	}
	
	private static void removeCommaAtEndofBuffer(StringBuilder buffer) {
		if (buffer.toString().endsWith(",")) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
	}

	public static final String IMAGE_EXT = ".jpg";
}
