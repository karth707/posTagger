package com.kg.posTagger.objects;

public class StateTransition {

	private String state_i;
	private String state_j;
	
	public StateTransition(String state_i, String state_j){
		setState_i(state_i);
		setState_j(state_j);
	}
	
	public String getState_i() {
		return state_i;
	}
	public void setState_i(String state_i) {
		this.state_i = state_i;
	}
	public String getState_j() {
		return state_j;
	}
	public void setState_j(String state_j) {
		this.state_j = state_j;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof StateTransition)){
			return false;
		}
		return this.toString().equals(obj.toString());
	}
	@Override
	public String toString() {
		return this.getState_i() + "::" + this.getState_j(); 
	}
}
