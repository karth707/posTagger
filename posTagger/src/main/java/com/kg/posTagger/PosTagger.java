package com.kg.posTagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kg.posTagger.objects.StateObservation;
import com.kg.posTagger.objects.StateTransition;

public class PosTagger {
    
	private Logger log = LoggerFactory.getLogger(PosTagger.class);
	//private Gson gson = new Gson();
	
	private String START_STATE = "$";
	private String END_STATE = "%";
	
	// Used for calculating state transition probabilities
	Map<StateTransition, Double> stateTransitionCounts;
	Map<String, Double> stateCounts;
	Map<StateTransition, Double> stateTransitionProbabilities;
	
	// Used for calculating observation probabilities
	Map<StateObservation, Double> stateObservationCounts;
	Set<String> observations;
	Map<StateObservation, Double> stateObservationProbabilities;
	
	// Error Metrics
	double testObservations;
	double errorCount;
	
	public PosTagger(){
		stateTransitionCounts = new HashMap<StateTransition, Double>();
		stateCounts = new HashMap<String, Double>();
		stateTransitionProbabilities = new HashMap<StateTransition, Double>();
		
		stateObservationCounts = new HashMap<StateObservation, Double>();
		observations = new HashSet<String>();
		stateObservationProbabilities = new HashMap<StateObservation, Double>();
		
		testObservations = 0;
		errorCount = 0;
	}
	
	private void trainHMM(String trainingSetPath) {
		log.info("Training HMM now...");
		log.info("TrainingSetFile: " + trainingSetPath);
		long st = System.currentTimeMillis();
		
		fillCountMaps(trainingSetPath);
		learnparameters();
		
		long et = System.currentTimeMillis();
		log.info("Time taken for training: " + (et-st) + " ms");
	}
	
	// Learn aij and bj using the countMaps to fill in the probabilities
	private void learnparameters() {
		
		// for aij
		for(String state1: stateCounts.keySet()){
			for(String state2: stateCounts.keySet()){
				StateTransition st = new StateTransition(state1, state2);
				Double trasnitionCount = stateTransitionCounts.get(st);
				if(trasnitionCount!=null){
					Double stateCount = stateCounts.get(state1);
					if(stateCount!=null && stateCount!=0){
						stateTransitionProbabilities.put(st, trasnitionCount/stateCount);
					}
				}else{
					stateTransitionProbabilities.put(st, 0.0);
				}
			}
		}
		
		// for bjs
		for(String state: stateCounts.keySet()){
			for(String observation: observations){
				StateObservation so = new StateObservation(observation, state);
				Double observationCount = stateObservationCounts.get(so);
				if(observationCount!=null){
					Double stateCount = stateCounts.get(state);
					if(stateCount!=null && stateCount!=0){
						stateObservationProbabilities.put(so, observationCount/stateCount);
					}
				}else{
					stateObservationProbabilities.put(so, 0.0);
				}
			}
		}
	}

	private void fillCountMaps(String trainingSetPath){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(trainingSetPath)));
			
			String previousState = null;
			String line;
			while((line=reader.readLine())!=null){
				if(previousState == null){
					previousState = START_STATE;
					incrementStateCounts(previousState);
					continue;
				}
				String observation = line.split("/")[0];
				String currentState = (line.split("/")[1].equals("###"))? END_STATE : line.split("/")[1];
				StateObservation so = new StateObservation(observation, currentState);
	
				observations.add(observation);
				
				//Increment the stateMaps
				incrementStateObservationCounts(so);
				incrementStateCounts(currentState);
				
				StateTransition st;
				if(previousState.equals(END_STATE)){
					incrementStateCounts(START_STATE);
					st = new StateTransition(START_STATE, currentState);
				}else{
					st = new StateTransition(previousState, currentState);
				}
				// Increment observationMap
				imcrementStateTransitionCounts(st);
				previousState = currentState;
			}
			reader.close();
		}catch(Exception ex){
			log.error("Error while filling CountMaps");
			ex.printStackTrace();
		}
	}

	private void imcrementStateTransitionCounts(StateTransition st) {
		if(!stateTransitionCounts.containsKey(st)){
			stateTransitionCounts.put(st, 0.0);
		}
		stateTransitionCounts.put(st, stateTransitionCounts.get(st) + 1);
	}

	private void incrementStateObservationCounts(StateObservation so) {
		if(!stateObservationCounts.containsKey(so)){
			stateObservationCounts.put(so, 0.0);
		}
		stateObservationCounts.put(so, stateObservationCounts.get(so) + 1);
	}

	private void incrementStateCounts(String currentState) {
		if(!stateCounts.containsKey(currentState)){
			stateCounts.put(currentState, 0.0);
		}
		stateCounts.put(currentState, stateCounts.get(currentState) + 1);
	}
	
	private void tag(String testingSetPath, String taggedOutputPath) {
		
		log.info("Taging sentences now...");
		log.info("TestingSetFile: " + testingSetPath);
		log.info("TaggedOutputPath: " + taggedOutputPath);
		long st = System.currentTimeMillis();
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(testingSetPath)));
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(taggedOutputPath)));

			String previousState = null;
			String line;
			Map<String, Double> v = new HashMap<String, Double>();
			while((line=reader.readLine())!=null){
				if(previousState == null){
					writer.write("###/###" + "\n");
					previousState = START_STATE;
					continue;
				}
				String state;
				String observation = line.split("/")[0];
				String trueState = line.split("/")[1];
				if(observation.equals("###/###")){
					state = END_STATE;
					previousState = START_STATE;
					writer.write("###/###" + "\n");
				}else{
					testObservations++;
					state = getState(previousState, observation, v);
					if(!state.equals(trueState)){
						errorCount++;
					}
					writer.write(observation + "/" + state + "\n");
					previousState = state;
				}
				
			}
			reader.close();
			writer.close();
			
		}catch(Exception ex){
			log.error("Error while tagging");
			ex.printStackTrace();
		}
		
		long et = System.currentTimeMillis();
		log.info("Time taken for testing: " + (et-st) + " ms");
		
		log.info("Done with Taging sentences");
		log.info("!!!!!--ERROR RATE: " + errorCount/testObservations + " --!!!!!");
	}
	

	// Using the viterbi algorithm, predict the state;
	private String getState(String previousState, String observation, Map<String, Double> v) {
		
		//initialization
		if(previousState.equals(START_STATE)){
			for(String state: stateCounts.keySet()){
				Double a0j = stateTransitionProbabilities.get(new StateTransition(START_STATE, state));
				if(a0j==null) a0j = 0.0;
				Double bj = stateObservationProbabilities.get(new StateObservation(observation, state));
				if(bj==null) bj = 0.0;
				v.put(state, a0j*bj);
			}
			return getTagWithMaxScore(v);
		}
		
		for(String state: stateCounts.keySet()){
			
			Double aij = stateTransitionProbabilities.get(new StateObservation(previousState, state));
			if(aij==null) aij = 0.0;
			Double bj = stateObservationProbabilities.get(new StateObservation(observation, state));
			if(bj==null) bj = 0.0;
			v.put(state, (v.get(state)*aij*bj));				 
		}
		return getTagWithMaxScore(v);
	}

	private String getTagWithMaxScore(Map<String, Double> v) {
		String tag = "XXXX";
		double max = 0;
		for(String state: v.keySet()){
			if(v.get(state)>max){
				max = v.get(state);
				tag = state;
			}
		}
		return tag;
	}

	public static void main(String[] args){
		
		String trainingSetPath = "/Users/KartheekGanesh/Desktop/entrain.txt";
		PosTagger tagger = new PosTagger();
		tagger.trainHMM(trainingSetPath);
		
		String testingSetPath = "/Users/KartheekGanesh/Desktop/entest.txt";
		String taggedOutputPath = "/Users/KartheekGanesh/Desktop/out.txt";
 		tagger.tag(testingSetPath, taggedOutputPath);
    }
}
