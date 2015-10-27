package com.kg.posTagger.objects;

public class ViterbiBackScore {

	private Double score;
	private String state;
	
	public ViterbiBackScore(Double score, String state_came_from){
		setScore(score);
		setState(state_came_from);
	}
	
	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof ViterbiBackScore)){
			return false;
		}
		return this.toString().equals(obj.toString());
	}

	@Override
	public String toString() {
		return this.getState() + "::" + this.getScore();
	}
	
}
