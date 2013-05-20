package eWordLearner.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eWordLearner.WordRepoConstants;
import eWordLearner.eWordLearnerActivator;
import eWordLearner.model.IRepositoryListener;
import eWordLearner.model.IWordRepository;
import eWordLearner.model.MP3File;
import eWordLearner.model.Word;
import eWordLearner.model.WordFeedProvider;
import eWordLearner.model.WordPreferences;

public class WordView extends ViewPart {

	
	private static final String ID = "eWordLearner.ui.WordView";
	private Button nextButton;
	private Button prevButton;
	private Button incNextButton;

	private Canvas canvas;
	private Composite container;
	private WordFeedProvider feedProvider;
	private Word currentWord;
	private Text wordText;
	private Text definitionText;
	private Spinner recallSpinner;
	private Stack<Word> forwardStack = new Stack<Word> ();
	private Stack<Word> backwardStack = new Stack<Word> ();
	private Composite topComposite;
	private Text searchWordText;

	
	@Override
	public void createPartControl(Composite parent) {
		feedProvider = new WordFeedProvider(repositoryListener);
		currentWord = feedProvider.load();

		container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		
		topComposite = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(topComposite);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(topComposite);

		Composite btnComposite = new Composite(topComposite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).applyTo(btnComposite);
		GridLayoutFactory.fillDefaults().numColumns(7).applyTo(btnComposite);
		
		/*Button addButton = new Button(btnComposite, SWT.PUSH);
		addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addButton.setToolTipText("Add/Search Word");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addWord();
			}
		});*/

		
		prevButton = new Button(btnComposite, SWT.PUSH);
		prevButton.setText("<");
		prevButton.setToolTipText("Previous (Key Left)");
		GridDataFactory.swtDefaults().applyTo(prevButton);
		prevButton.addSelectionListener(getButtonListener(true));
		prevButton.setEnabled(false);


		nextButton = new Button(btnComposite, SWT.PUSH);
		nextButton.setText(">");
		nextButton.setToolTipText("Next (Key Right)");
		GridDataFactory.swtDefaults().applyTo(nextButton);
		nextButton.addSelectionListener(getButtonListener(false));
		
		fetchNewButton = new Button(btnComposite, SWT.PUSH);
		fetchNewButton.setText(">*");
		fetchNewButton.setToolTipText("Next New Word (Key Up/Down)");
		GridDataFactory.swtDefaults().applyTo(fetchNewButton);
		fetchNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fetchNewWord();
			}
			
		});
		fetchNewButton.setEnabled(false);
		
		
		recallLabel = new Label(btnComposite, SWT.NONE);
		recallLabel.setText("Recall Level:");
		GridDataFactory.swtDefaults().applyTo(recallLabel);
		adapt(recallLabel);

		recallSpinner = new Spinner(btnComposite, SWT.BORDER);
		recallSpinner.setMinimum(0);
		recallSpinner.setMaximum(100);
		recallSpinner.setIncrement(1);
		recallSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setRecallLevel();
			}

			private void setRecallLevel() {
				int level = recallSpinner.getSelection();
				currentWord.setLevel(level == 0 ? - 1 : level);
				feedProvider.updateModel(currentWord);
				updateSpinnerToolTip();
			}
		});
		
		incNextButton = new Button(btnComposite, SWT.PUSH);
		incNextButton.setText(">+");
		incNextButton.setToolTipText("Next (Increment Repeat Level)");
		GridDataFactory.swtDefaults().applyTo(incNextButton);
		incNextButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getNextIncremented();
			}

			private void getNextIncremented() {
				if (currentWord != null) {
					int level = recallSpinner.getSelection();
					currentWord.setLevel(level == 0 ? - 1 : ++level);
					feedProvider.updateModel(currentWord);
				}
			}
			
		});
		incNextButton.addSelectionListener(getButtonListener(false));
        incNextButton.setEnabled(false);
        
        pronounceAlwaysButton = new Button(btnComposite, SWT.CHECK);
        pronounceAlwaysButton.setImage(WordRepoConstants.IMAGE_SOUND);
        pronounceAlwaysButton.setToolTipText("Pronounce word while navigating between words");
		GridDataFactory.swtDefaults().applyTo(pronounceAlwaysButton);

		final IPreferenceStore preferenceStore = eWordLearnerActivator.getDefault().getPreferenceStore();
		preferenceStore.setDefault(WordRepoConstants.PREF_PRONOUNCE_WORD, true);
		pronounceAlwaysButton.setSelection(preferenceStore.getBoolean(WordRepoConstants.PREF_PRONOUNCE_WORD));
		pronounceAlwaysButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				preferenceStore
						.setValue(WordRepoConstants.PREF_PRONOUNCE_WORD, pronounceAlwaysButton.getSelection());
			}
		});
		
		
		SashForm sashContainer = new SashForm(container, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(sashContainer);
		
		definitionText = new Text(sashContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		definitionText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 60).applyTo(definitionText);
		definitionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentWord != null) {
					currentWord.setDefinition(definitionText.getText());
					feedProvider.updateModel(currentWord);
				}
			}
		});
		
		Composite canvasComposite = new Composite(sashContainer, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(canvasComposite);
		GridLayoutFactory.fillDefaults().applyTo(canvasComposite);
		sashContainer.setWeights(new int[]{2, 7});
		
		createCanvasTitle(canvasComposite);
		
		canvas = new Canvas(canvasComposite, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).applyTo(canvas);
		canvasComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
		adapt(container);
		addKeyListeners();
		
		
		addPaintListener();
		updateUIAndImage();

	}

	private void addPaintListener() {
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (image != null) {
					GC gc = new GC(canvas);
					gc.setAntialias(SWT.ON);
					gc.setInterpolation(SWT.HIGH);
					gc.drawImage(image, 0, 0, 
							image.getBounds().width, image.getBounds().height, 
							0, 0, 350, 350);
					gc.dispose();
				}
			}
		});
	}

	private void addKeyListeners() {
		canvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {

			}

			@Override
			public void mouseDown(MouseEvent e) {
				canvas.setFocus();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {

			}
		});
		canvas.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_LEFT) {
					navigateWord(true);
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					navigateWord(false);
				} else if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
					fetchNewWord();
				} else if (e.keyCode == '\r') {
					playWord();
				}
			}
		});
	}

	private void createCanvasTitle(Composite parent) {
		Composite btnComposite = new Composite(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(btnComposite);
		
		Composite leftBtnComposite = new Composite(btnComposite, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(leftBtnComposite);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(leftBtnComposite);
		
		Composite rightBtnComposite = new Composite(btnComposite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(rightBtnComposite);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(rightBtnComposite);
		
		Composite wordComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(wordComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(wordComposite);
		
		wordText = new Text(wordComposite, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(wordText);
		wordText.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		
		playWordButton = new Button(wordComposite, SWT.FLAT);
		GridDataFactory.swtDefaults().applyTo(playWordButton);
		playWordButton.setImage(WordRepoConstants.IMAGE_SOUND);
		playWordButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				playWord();
			}
		});
		playWordButton.setToolTipText("Pronounce Word (Key Enter)");
		
		/*********************************** Left Toolbar Section *****************************************************/
		
		copyImageButton = createLeftToolButton(leftBtnComposite);
		copyImageButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
		copyImageButton.setToolTipText("Copy the image url from the web and click on the button to update the image.");
		copyImageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				downloadImage();
			}

			private void downloadImage() {
				Clipboard clipboard = new Clipboard(Display.getDefault());
				try {
					Object contents = clipboard.getContents(TextTransfer.getInstance());
					boolean isValid = false;
					if (contents != null) {
						String url = contents.toString();
						Matcher matcher = IWordRepository.IMAGE_URL_PATTERN.matcher(url);
						if (matcher.matches()) {
							isValid = true;
							try {
								boolean proceed = currentWord.getImage() == null;
								proceed = proceed || MessageDialog.openQuestion(getSite().getShell(), "Confirm", "Are you sure you want to replace the existing image?");
								if (proceed) {
									feedProvider.setImage(currentWord, url);
									refreshImage();
								}
							} catch (IOException e) {
								eWordLearnerActivator.getDefault().logAndShowException(e);
							}
						} 
					}
					if (!isValid) {
						String message = "Clipboard doesn't contain a valid image url. Copy the image url from web (ending with jpeg,jpg,png,bmp,gif) and select the button again to update the image.";
						MessageDialog.openInformation(getSite().getShell(), "Information", message);
					}
				} finally {
					clipboard.dispose();
				}
			}
		});
		
		
		refreshButton = createLeftToolButton(leftBtnComposite);
		refreshButton.setImage(WordRepoConstants.IMAGE_REFRESH);
		refreshButton.setToolTipText("Refresh Image");
		refreshButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refreshImage();
			}
		});
		
		
		searchButton = createLeftToolButton(leftBtnComposite);
		searchButton.setImage(WordRepoConstants.IMAGE_GOOGLE);
		searchButton.setToolTipText("Search for definition of selected text from definition field or current word if selection is empty");
		searchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				searchUrl();
			}
			
		});
		
		/*Button searchImageButton = createLeftToolButton(leftBtnComposite);
		searchImageButton.setImage(WordRepoConstants.IMAGE_SEARCH);
		searchImageButton.setToolTipText("Search for related images");
		searchImageButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				searchImageUrl();
			}
		});*/
		
		wordOriginButton = createLeftToolButton(leftBtnComposite);
		wordOriginButton.setImage(WordRepoConstants.IMAGE_PHOCAB_LOGO);
		wordOriginButton.setToolTipText("Vist Phocabulary page of the word");
		wordOriginButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				if (currentWord != null) {
					String url = currentWord.getSiteUrl();
					try {
						PlatformUI.getWorkbench().getBrowserSupport().createBrowser("Sample").openURL(new URL(url));
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}
		});

		/*********************************** Right Toolbar Section *****************************************************/
		
		searchWordText = new Text(rightBtnComposite, SWT.BORDER);
		GridDataFactory.swtDefaults().hint(100, SWT.DEFAULT).align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(searchWordText);
		searchWordText.setToolTipText("Press enter to find the word");
		searchWordText.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == '\r') {
					findWord(searchWordText.getText());
				}
			}
		});
		searchWordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateAddButton();
			}
		});
		
		updateSearchProposals();
		addWordButton = creaeteRightToolButton(rightBtnComposite);
		addWordButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addWordButton.setToolTipText("Add Word");
		addWordButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addWord(searchWordText.getText().trim());
			}
		});
		
		removeWordButton = creaeteRightToolButton(rightBtnComposite);
		removeWordButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
		removeWordButton.setToolTipText("Remove Word");
		removeWordButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeWord();
			}

			private void removeWord() {
				String message = "Are you sure you want to remove '" + currentWord.getId() + "'?";
				boolean confirm = MessageDialog.openConfirm(getSite().getShell(), "Confirm", message);
				if (confirm) {
					currentWord = feedProvider.removeAndNavigateWord(currentWord);
					updateSearchProposals();
					updateUIAndImage();
				}
			}
		});
		removeWordButton.setEnabled(false);
		
		Button saveButton = creaeteRightToolButton(rightBtnComposite);
		saveButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveButton.setToolTipText("Save current state. By default repository will be automatically saved for every "
				+ WordFeedProvider.DEFAULT_AUTO_SAVE_LENGTH + " iterations or when view is closed");

		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				saveRepository();
			}

			private void saveRepository() {
				feedProvider.save();
			}
		});
		
		Button clearPlaylistButton = creaeteRightToolButton(rightBtnComposite);
		clearPlaylistButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_CLEAR));
		clearPlaylistButton.setToolTipText("Reset");
		clearPlaylistButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				resetRepository();
			}

			private void resetRepository() {
				String message = "This action will reset the order in which words are displayed to initial state. Do you wish to proceed?";
				boolean confirm = MessageDialog.openConfirm(getSite().getShell(), "Reset", message);
				if (confirm) {
					//save current state
					wordText.setText("");
					feedProvider.save();
					forwardStack.clear();
					backwardStack.clear();
					currentWord = null;
					feedProvider.reset();
				}
			}
		});
	}
	
	private void refreshImage() {
		feedProvider.refreshImage(currentWord);
	}

	private void updateSearchProposals() {
		List<String> proposals = feedProvider.getAvailableWords();
		String[] array = proposals.toArray(new String[0]);
		if (autoCompleteField == null) {
			autoCompleteField = new AutoCompleteField(searchWordText, new TextContentAdapter(), array);
		} else {
			autoCompleteField.setProposals(array);
		}
	}

	private Button createLeftToolButton(Composite leftBtnComposite) {
		Button copyImageLocationButton = new Button(leftBtnComposite, SWT.PUSH);
		GridDataFactory.swtDefaults().applyTo(copyImageLocationButton);
		return copyImageLocationButton;
	}

	private Button creaeteRightToolButton(Composite rightBtnComposite) {
		Button saveButton = new Button(rightBtnComposite, SWT.PUSH);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(saveButton);
		return saveButton;
	}
	
	
	private static final Pattern definition_pattern = Pattern.compile("[a-zA-Z- ]*");

	private void findWord(String value) {
		Word findWord = feedProvider.findAndNavigate(value);
		if (findWord != null) {
			currentWord = findWord;
			updateUIAndImage();
		}
	}
	
	private void addWord(String wordId) {
		boolean valid = definition_pattern.matcher(wordId).matches();
		if (valid) {
			currentWord = feedProvider.addAndNavigateWord(wordId);
			updateUIAndImage();
		} else {
			String message = "Only word containing one of the alphabetical characters, aiphen and space can be added.";
			MessageDialog.openInformation(getSite().getShell(), "Invalid Word", message);
		}
	}

	private void playWord() {
		if (currentWord != null) {
			try {
				feedProvider.fetchSoundFile(currentWord);
				String soundFile = currentWord.getSoundFile();
				if (new File(soundFile).exists()) {
					new MP3File(soundFile).play();
				}
			} catch (IOException e) {
				eWordLearnerActivator.getDefault().logAndShowException(e);
			}
		}
	}
	
	private void fetchNewWord() {
		currentWord = feedProvider.getNextWord(true);
		updateUIAndImage();
	}
	
	private void adapt(Control control) {
		if (control instanceof Label || control instanceof Composite) {
			control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		}
		if (control instanceof Composite) {
			Control[] children = ((Composite) control).getChildren();
			for (Control child : children) {
				adapt(child);
			}
		}
	}

	private IRepositoryListener repositoryListener = new IRepositoryListener() {
		@Override
		public void repositoryUpdated() {
			if (currentWord == null) {
				currentWord = feedProvider.getNextWord();
			}
			updateUIAndImageAsync(false);
		}

		@Override
		public void imagesLoaded(Word word) {
			if (word == currentWord) {
				updateUIAndImageAsync(true);
			}
		}
	};
	private Button searchButton;
	private Label recallLabel;
	private Button wordOriginButton;
	private Button fetchNewButton;
	private Button refreshButton;
	private Button copyImageButton;
	private AutoCompleteField autoCompleteField;
	private Button addWordButton;
	private Button removeWordButton;
	private Image image;
	private Button playWordButton;
	private Button pronounceAlwaysButton;

	private SelectionAdapter getButtonListener(final boolean isBackButton) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				navigateWord(isBackButton);
			}

		};
	}

	private void navigateWord(final boolean isBackButton) {
		boolean fetchFromRepo = false;
		if (!isBackButton && !forwardStack.isEmpty()) {
			if (currentWord != null) {
				backwardStack.push(currentWord);
			}
			currentWord = forwardStack.pop();
		} else if (isBackButton) {
            forwardStack.push(currentWord);
            currentWord = backwardStack.pop();
		} else {
			fetchFromRepo = true;
			backwardStack.push(currentWord);
		}
		
		if (fetchFromRepo) {
			currentWord = feedProvider.getNextWord();
		}
		updateUIAndImage();
		if (pronounceAlwaysButton.getSelection()) {
			playWord();
		}
	}
	
	private void updateUIAndImageAsync(final boolean isImageUpdate) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!isImageUpdate) {
					updateSearchProposals();
				}
				updateUIAndImage();
			}
		});

	}
	
	private void updateUIAndImage() {
		image = null;
		if (canvas.isDisposed()) {
			return;
		}
		
		String id = "";
		String definition= "";
		if (currentWord != null) {
			id = currentWord.getId();
			definition = currentWord.getDefinition();
			topComposite.layout(true);
			image = currentWord.getImage();
			// update buttons
			updateSpinnerToolTip();
			recallSpinner.setSelection(currentWord.getLevel());
		}
		// update fields
		wordText.setText(id);
		String progressText = "Words covered: " + feedProvider.getCompletedWords() +  " of " + feedProvider.getTotalWords();
		wordText.setToolTipText(progressText);
		
 		definitionText.setText(definition);
		canvas.setToolTipText(definition);
		updateWidgetEnablement();
		
		//clear search text;
		searchWordText.setText("");

		if (image == null) {
			image = WordRepoConstants.IMAGE_NOT_AVAILABLE; // show dummy image
		}
		
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).hint(350, 350).applyTo(canvas);
		canvas.redraw();
		canvas.getParent().layout(true);
		wordText.getParent().layout(true);
		
		container.layout(true);
	}

	private void updateWidgetEnablement() {
		prevButton.setEnabled(!backwardStack.isEmpty());
		nextButton.setEnabled(true);
		fetchNewButton.setEnabled(feedProvider.hasNewWords());
		incNextButton.setEnabled(forwardStack.isEmpty());
		wordOriginButton.setEnabled(currentWord != null && currentWord.getSiteUrl() != null);
		refreshButton.setEnabled(currentWord != null);
		copyImageButton.setEnabled(currentWord != null);
		searchButton.setEnabled(currentWord != null);
		recallSpinner.setEnabled(currentWord != null);
		removeWordButton.setEnabled(currentWord != null && currentWord.isLocal() && forwardStack.isEmpty());
		playWordButton.setEnabled(currentWord != null);
		updateAddButton();
	}
	
	private void updateAddButton() {
		addWordButton.setEnabled(!searchWordText.getText().trim().isEmpty() && !feedProvider.wordExists(searchWordText.getText()));
	}

	private void updateSpinnerToolTip() {
		String toolTip = "Current Position: " + feedProvider.getPointer() + "/" + "Recall Position: ";
		toolTip = toolTip + (recallSpinner.getSelection() == 0 ? "Never" : feedProvider.getRecallPosition(currentWord));
		recallSpinner.setToolTipText(toolTip);
		recallLabel.setToolTipText(toolTip);
	}

	
/*	private void searchImageUrl() {
		if (currentWord != null) {
			String url = WordPreferences.getInstance().getSearchUrl();
			String searchableWord = currentWord.getId();
			url = url.replace(WordPreferences.REPLACABLE_WORD, searchableWord);
			System.out.println(url);
			try {
				PlatformUI.getWorkbench().getBrowserSupport().createBrowser("Sample").openURL(new URL(url));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}*/
	
	private void searchUrl() {
		if (currentWord != null) {
			String url = WordPreferences.getInstance().getSearchUrl();
			String searchableWord = currentWord.getId();
			String foucsText = ((Text) definitionText).getSelectionText().trim();
			if (!foucsText.isEmpty()) {
				searchableWord = foucsText;
			}
			url = url.replace(WordPreferences.REPLACABLE_WORD, searchableWord);
			System.out.println(url);
			try {
				PlatformUI.getWorkbench().getBrowserSupport().createBrowser("Sample").openURL(new URL(url));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}


	public static WordView showView() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			return (WordView) activePage.showView(ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void setFocus() {
       container.setFocus();
	}
	
	@Override
	public void dispose() {
		forwardStack.clear();
		backwardStack.clear();
		feedProvider.save();
		feedProvider.unload();
		super.dispose();
	}

	
	
}
