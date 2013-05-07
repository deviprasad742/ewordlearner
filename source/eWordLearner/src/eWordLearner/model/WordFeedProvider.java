package eWordLearner.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

import eWordLearner.eWordLearnerActivator;

public class WordFeedProvider {

	private static final int DEFAULT_POINTER = 1;
	public static final int DEFAULT_AUTO_SAVE_LENGTH = 20;
	public static final int RECALL_SPACE = 10;
	private static final String CURRENT_POINTER_KEY = "current_pointer";

	private int pointer = 1;
	int autoSaveCounter = DEFAULT_AUTO_SAVE_LENGTH;
	private Map<String, Word> wordCache = Collections.synchronizedMap(new HashMap<String, Word>());
	
	private List<String> unreadWords = Collections.synchronizedList(new ArrayList<String>());
	private Map<String, String> recallIndexMap = Collections.synchronizedMap(new HashMap<String, String>());
	private Map<String, String> recallLevelMap = Collections.synchronizedMap(new HashMap<String, String>());
	private Map<String, IWordRepository> repositoryMap = Collections.synchronizedMap(new LinkedHashMap<String, IWordRepository>());

	private String levelsFileName = "recall_levels.txt";
	private String repateIndexFileName = "recall_positions.txt";
	private String unreadFileName = "words_unread.txt";

	private Word lastWord;
	private boolean isLoaded = false;

	private IRepositoryListener repositoryListener;
	private DefaultWordRepository deaultRepository = new DefaultWordRepository();

	public WordFeedProvider(IRepositoryListener repositoryListener) {
		this.repositoryListener = repositoryListener;
	}

	public Word load() {
		Word word = null;
		try {
			loadModel();
			word = getNextWord();
			refreshJob.schedule();
			preFetchImages();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return word;
	}

	Job refreshJob = new Job("Updating word definitions from '" + DefaultWordRepository.BASE_URL + "'") {
		private boolean isFirstTime;

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			File[] listFiles = deaultRepository.getRepositoryFolder().listFiles();
			for (File repo : listFiles) {
				if (repo.isDirectory()) {
					String repoId = repo.getName();
					repositoryMap.put(repoId, new UserRepository(repoId));
				}
			}
			repositoryMap.put(deaultRepository.getId(), deaultRepository);
			isFirstTime = unreadWords.isEmpty();
			boolean saveRepos = false;
			for (Entry<String, IWordRepository> repoEntry : repositoryMap.entrySet()) {
				IWordRepository repository = repoEntry.getValue();
				boolean isUpdated = syncRepository(monitor, repository);
				if (isUpdated) {
					saveRepos = true;
					notifyRepoUpdate();
				}
			}
			
			
			if(saveRepos) {
				save();
			}
			
			isLoaded = true;
			return Status.OK_STATUS;
		}


		private boolean syncRepository(IProgressMonitor monitor, IWordRepository repository) {
			boolean isUpdated = false;
			repository.initialize();
			boolean notify = true; // notify on the load of first word and ignore others
			Map<String, String> definitions = repository.getDefinitions(monitor);
			for (Entry<String, String> definiton : definitions.entrySet()) {
				String wordId = definiton.getKey();
				if (!wordCache.containsKey(wordId)) {
					isUpdated = true;
					Word word = repository.createWord(wordId);
					updateProperties(word);
					wordCache.put(wordId, word);
				}
				// check if repeat index doesn't contain the word
				if (!wordId.isEmpty() && !unreadWords.contains(wordId) && !recallIndexMap.values().contains(wordId)) {
					if (isFirstTime) {
						unreadWords.add(wordId);
					} else {
						unreadWords.add(0, wordId);
					}
					isUpdated = true;
				}
				
				if (notify) {
					notifyRepoUpdate();
					notify = false;
				}
			}
			return isUpdated;
		}
	};

	private void notifyRepoUpdate() {
		if (repositoryListener != null) {
			repositoryListener.repositoryUpdated();
		}
	}
	
	private void loadModel() throws Exception {
		unreadWords.addAll(FileUtils.readListFromFile(new File(getLocation(), unreadFileName)));
		recallLevelMap.putAll(FileUtils.readMapFromFile(new File(getLocation(), levelsFileName)));
		recallIndexMap.putAll(FileUtils.readMapFromFile(new File(getLocation(), repateIndexFileName)));
		String ptrString = recallIndexMap.get(CURRENT_POINTER_KEY);
		if (ptrString != null) {
			pointer = Integer.valueOf(ptrString);
		}
	}
	
	public Word getNextWord() {
		return getNextWord(false);
	}

	public Word getNextWord(boolean skipRecall) {
		return getNextWord(skipRecall, null);
	}

	private Word getNextWord(boolean skipRecall, Word pushWord) {
		if (lastWord != null) {
			recallIndexMap.remove(String.valueOf(pointer));// remove the current position
			if (lastWord.getLevel() != -1) {// update next occurrence position
				if (!recallIndexMap.values().contains(lastWord.getId())) { // check before repeating the word if its already being repeated
					recallIndexMap.put(getRecallPosition(lastWord), lastWord.getId());
				}
				//save present states
				updateModel(lastWord);
			}
			unreadWords.remove(lastWord.getId());// remove the last word from unread words
			skipRecall = skipRecall || pushWord != null;
			boolean incrementPointer = !skipRecall || !recallIndexMap.containsKey(String.valueOf(pointer+1));
			if (incrementPointer) { // increment when recalled or skipped and recalled index in free
				pointer++;
			}
		} else {
			pointer++;
		}
		
		Word nextWord = null;
		if (pushWord != null)  {
			nextWord = pushWord; // set the given word as next word
		} else{
			String repeatId = recallIndexMap.get(String.valueOf(pointer));
			if (repeatId != null && !skipRecall) { // check if new word is requested
				nextWord = wordCache.get(repeatId);
			} else if (unreadWords.size() > 0) {
				String unreadId = unreadWords.get(0);
				nextWord = wordCache.get(unreadId);
			} else {
				nextWord = null;
			}
		}
		recallIndexMap.put(CURRENT_POINTER_KEY, String.valueOf(pointer));
		autoSaveCounter--;
		if (autoSaveCounter == 0) {
			save();
		}
		
		if (nextWord == null && isLoaded) {
			if (unreadWords.size() > 0) {
				unreadWords.remove(0);
				nextWord = getNextWord(skipRecall, pushWord);
			}
		}
		
		if (nextWord != null && nextWord.getImage() == null) {// load image if it already exists
			fetchImage(nextWord, true);
		} 
		preFetchImages();
		lastWord = nextWord;
		return nextWord;
	}

	public String getRecallPosition(Word word) {
		int multiplier = word.getLevel() * word.getLevel();
		int nextRepeatPosition = pointer + multiplier * RECALL_SPACE;
		String nextFreePosition = getNextFreePosition(nextRepeatPosition);
		return nextFreePosition;
	}

	public boolean hasNewWords() {
		return unreadWords.size() > 0;
	}
	
	public void updateModel(Word word) {
		recallLevelMap.put(word.getId(), "" + word.getLevel());
		IWordRepository repository = getRepository(word);
		repository.updateModel(word);
	}

	private IWordRepository getRepository(Word word) {
		IWordRepository repository = repositoryMap.get(word.getRepository());
		return repository;
	}

	private String getNextFreePosition(int nextRepeatPosition) {
		while (recallIndexMap.containsKey("" + nextRepeatPosition)) {
			nextRepeatPosition++;
		}
		return String.valueOf(nextRepeatPosition);
	}

	private void updateProperties(Word word) {
		String levelStr = recallLevelMap.get(word.getId());
		int level = levelStr != null ? Integer.valueOf(levelStr) : 1;
		word.setLevel(level);
	}
	
	
	public List<String> getAvailableWords() {
		List<String> proposals = new ArrayList<String>();
		synchronized (wordCache) {
			proposals.addAll(wordCache.keySet());
		}
		Collections.sort(proposals);
		return proposals;
	}
	
	public void reset() {
		pointer = DEFAULT_POINTER;
		lastWord = null;
		autoSaveCounter = DEFAULT_AUTO_SAVE_LENGTH;
		unreadWords.clear();
		recallIndexMap.clear();
		wordCache.clear();
		disposeCache();
		refreshJob.schedule();
	}

	public void save() {
		autoSaveCounter = DEFAULT_AUTO_SAVE_LENGTH;
		saveJob.schedule();
	}
	
	boolean isReported = false;

	private Job saveJob = new Job("Saving definitons and data") {
		protected IStatus run(IProgressMonitor monitor) {
			try {
				saveModel();
				WordPreferences.getInstance().store();
				System.out.println("Repository saved successfully: " + getLocation());
			} catch (final Exception e) {
				if (!isReported) {
					isReported = true;
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							eWordLearnerActivator.getDefault().logAndShowException(e);
						}
					});
				} else {
					System.out.println(e.getMessage());
				}
			}
			return Status.OK_STATUS;
		};
	};

	private void saveModel() throws Exception {
		for (IWordRepository repository : repositoryMap.values()) {
			repository.save();
		}
		FileUtils.writeListToFile(unreadWords, new File(getLocation(), unreadFileName));
		FileUtils.writeMapToFile(recallLevelMap, new File(getLocation(), levelsFileName));
		FileUtils.writeMapToFile(recallIndexMap, new File(getLocation(), repateIndexFileName));
	}

	private String getLocation() {
		return deaultRepository.getLocation();
	}

	public void unload() {
		repositoryListener = null;
		disposeCache();
	}

	private void disposeCache() {
		for (Word word : wordCache.values()) {
			if (word.getImage() != null) {
				word.getImage().dispose();
			}
		}
	}

	private void preFetchImages() {// prefetch next 10 images
		if (shouldFetchImages()) {
			int count = 0; // fetch next 10 images
			synchronized (unreadWords) {

				for (String id : unreadWords) {
					if (count < DEFAULT_AUTO_SAVE_LENGTH) {
						Word word = wordCache.get(id);
						if (word != null && word.getImage() == null) {
							if (fetchImage(word, true)) {
								count++;
							} else {
								return;
							}
						}
					} else {
						break;
					}
				}
			}
		}

	}

	private Job prefetchImagesJob = new Job("Fetching images from '" + DefaultWordRepository.BASE_URL + "'") {
		protected IStatus run(IProgressMonitor monitor) {
			List<Word> local;
			synchronized (images2Fetch) {
				local = new ArrayList<Word>(images2Fetch);
				images2Fetch.clear();
			}
			for (Word word : local) {
				try {
					IWordRepository repository = getRepository(word);
					File imageFile = repository.fetchImage(word);
					if (imageFile.exists()) {
						monitor.subTask(word.getId());
						updateImage(word, imageFile);
						if (repositoryListener != null) {
							repositoryListener.imagesLoaded(word);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return Status.OK_STATUS;
		};
	};

	private List<Word> images2Fetch = Collections.synchronizedList(new ArrayList<Word>());
	
	public void refreshImage(Word word) {
		if (word.getImage() != null) {
			word.getImage().dispose();
		}
		fetchImage(word, true);
		if (repositoryListener != null) {
			repositoryListener.imagesLoaded(word);
		}
	}
	
	private boolean fetchImage(final Word word, final boolean download) {
		File imageFile = getImageLocation(word);
		if (!imageFile.exists() && download) {
			images2Fetch.add(word);
			prefetchImagesJob.schedule();
		} else if (imageFile.exists()) {
			updateImage(word, imageFile);
		}
		return true;
	}

	public File getUserImagesLocation(final Word word) {
		IWordRepository repository = getRepository(word);
		return repository.getCustomImageFile(word);
	}
	
	public File getImageLocation(final Word word) {
		IWordRepository repository = getRepository(word);
		return repository.getImageFile(word);
	}

	private void updateImage(final Word word, File imageFile) {
		ImageDescriptor descriptor;
		try {
			descriptor = ImageDescriptor.createFromURL(imageFile.toURI().toURL());
			if (word.getImage() != null) {
				word.getImage().dispose();
			}
			word.setImage(descriptor.createImage());
		} catch (MalformedURLException e) {
			eWordLearnerActivator.getDefault().logException(e);
		}
	}

	
	public Word findAndNavigate(String id) {
		if (wordExists(id)) {
			return getNextWord(true, wordCache.get(id));
		}
		return null;
	}
	
	public Word addAndNavigateWord(String id) {
		IWordRepository defaultRepository = repositoryMap.get(IWordRepository.DEFAULT_USER);
		if (defaultRepository != null) {
			Word word = defaultRepository.addWord(id, "");
			updateProperties(word);
			unreadWords.add(0, id);
			wordCache.put(id, word);
			// save if word is Added;
			save();
			notifyRepoUpdate();
			return getNextWord(true, word);
		}
		return null;
	}
	
	public Word removeAndNavigateWord(Word word) {
		IWordRepository repository = getRepository(word);
		repository.removeWord(word);
		recallLevelMap.remove(word.getId());
		wordCache.remove(word.getId());
		lastWord.setLevel(-1);// avoid recall
		save();// save the state
		return getNextWord();
	}
	
	public boolean wordExists(String id) {
		return wordCache.containsKey(id);
	}
	
	
	private boolean shouldFetchImages() {
		if (unreadWords.size() > 0) {
			String nextId = unreadWords.get(0);
			Word word2Check = wordCache.get(nextId);
			if (word2Check != null && word2Check.getImage() == null) {
				return true;
			} else if (unreadWords.size() > 30) { // fetch 10 images in advance
				nextId = unreadWords.get(30);
				word2Check = wordCache.get(nextId);
				return word2Check != null && word2Check.getImage() == null;
			}
		}
		return false;
	}


	public int getPointer() {
		return pointer;
	}

}
