package eWordLearner.model;

public interface IRepositoryListener {
	
	void repositoryUpdated();
	
	void imagesLoaded(Word word);
}
