package com.kg.posTagger.objects;

public class StateObservation {
	
	private String state;
	private String term;
	
	public StateObservation(String term, String state){
		setTerm(term);
		setState(state);
	}
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof StateObservation)){
			return false;
		}
		return this.toString().equals(obj.toString());
	}
	@Override
	public String toString() {
		return this.getState() + "::" + this.getTerm();
	}
	
}
