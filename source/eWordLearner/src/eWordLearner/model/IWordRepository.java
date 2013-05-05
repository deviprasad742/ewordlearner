package eWordLearner.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IWordRepository {

	String DEFINITIONS_FILE_NAME = "word_definitions.txt";
	String WORD_ORDER = "word_order";
	String WORD_SEPARATOR = ",";
	String REPOSITORIES = "repositories";
	String DEFAULT_USER = "default_user";


	Map<String, String> getDefinitions(IProgressMonitor monitor);

	String getId();

	String getLocation();

	void initialize();

	Word createWord(String id);

	Word addWord(String id, String definition);

	void save() throws Exception;

	void updateModel(Word word);

	File getImageFile(Word word);

	File fetchImage(Word word) throws IOException;

	void removeWord(Word word);

}