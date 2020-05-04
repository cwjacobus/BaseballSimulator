package baseball;

public class FieldingStats {
	private int assists;
	private int putOuts;
	private int errors;

	public FieldingStats() {
	}
	
	public FieldingStats(int assists, int putOuts, int errors) {
		this.assists = assists;
		this.putOuts = putOuts;
		this.errors = errors;
	}

	public int getAssists() {
		return assists;
	}

	public void setAssists(int assists) {
		this.assists = assists;
	}

	public int getPutOuts() {
		return putOuts;
	}

	public void setPutOuts(int putOuts) {
		this.putOuts = putOuts;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

}
