package bd.ciber.testbed;

public enum CiberIndexKeys {
	DB("ciberCatalog"),
	FILES_COLL("ciberFiles"),
	F_EXTENSION("extension"),
	F_SIZE("size"),
	F_NAME("name"),
	F_FOLDER("folder"),
	F_DEPTH("depth"),
	F_RANDOM("random"),
	F_FULLPATH("fullpath");
	
	private String key;
	CiberIndexKeys(String key) {
		this.key = key;
	}
	
	public String key() {
		return this.key;
	}
	
}
