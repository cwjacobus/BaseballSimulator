package baseball;

public class BaseRunner {
	Integer runnerId;
	Integer responsiblePitcherId;

	public BaseRunner() {
		runnerId = 0;
		responsiblePitcherId = 0;
	}

	public BaseRunner(Integer runnerId, Integer responsiblePitcherId) {
		this.runnerId = runnerId;
		this.responsiblePitcherId = responsiblePitcherId;
	}

	public Integer getRunnerId() {
		return runnerId;
	}

	public void setRunnerId(Integer runnerId) {
		this.runnerId = runnerId;
	}

	public Integer getResponsiblePitcherId() {
		return responsiblePitcherId;
	}

	public void setResponsiblePitcherId(Integer responsiblePitcherId) {
		this.responsiblePitcherId = responsiblePitcherId;
	}

}
