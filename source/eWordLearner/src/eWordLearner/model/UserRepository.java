package eWordLearner.model;

import java.io.File;


public class UserRepository extends DefaultWordRepository {

	private final String id;
	
	public UserRepository(String id) {
		this.id = id;
	}
	
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	protected boolean isWebRepository() {
		return false;
	}
	
	@Override
	protected String computeLocation() {
		String location = super.computeLocation();
		File repos = new File(location, REPOSITORIES);
		File repoFile = new File(repos, getId());
		return repoFile.getAbsolutePath();
	}
	
}
