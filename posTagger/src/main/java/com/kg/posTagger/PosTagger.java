package com.kg.posTagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kg.posTagger.objects.StateObservation;
import com.kg.posTagger.objects.StateTransition;
import com.kg.posTagger.objects.ViterbiBackScore;

public class PosTagger {
    
	private Logger log = LoggerFactory.getLogger(PosTagger.class);
	
	private String START_STATE = "$$$$";
	private String END_STATE = "%%%%";
	private String UNKNOWN_STATE = "XXXX";
	
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
	
	public void trainHMM(String trainingSetPath) {
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
					//smoothing
					stateTransitionProbabilities.put(st, 1.0/stateTransitionCounts.size());
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
					//smoothing
					stateObservationProbabilities.put(so, 1.0/stateObservationCounts.size());
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

	public double tag(String testingSetPath, String taggedOutputPath) {
		
		log.info("Taging sentences now...");
		log.info("TestingSetFile: " + testingSetPath);
		log.info("TaggedOutputPath: " + taggedOutputPath);
		long st = System.currentTimeMillis();
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(testingSetPath)));
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(taggedOutputPath)));

			String previousState = null;
			String line;
			List<Map<String, ViterbiBackScore>> v = new ArrayList<Map<String, ViterbiBackScore>>();
			List<String> trueStates = new ArrayList<String>();
			List<String> terms = new ArrayList<String>();
			while((line=reader.readLine())!=null){
				if(previousState == null){
					previousState = START_STATE;
					continue;
				}
				String observation = line.split("/")[0];
				String trueState = (line.split("/").length>1)? line.split("/")[1] : UNKNOWN_STATE;
				if(observation.equals("###")){
					List<String> statesGenerated = backtrack(v);
					int index = 0;
					for(int i=statesGenerated.size()-1; i>=0; i--){
						if(!trueStates.get(index).equals(statesGenerated.get(i))){
							errorCount++;
						}
						writer.write(terms.get(index) + "/" + statesGenerated.get(i) + "\n");
						index++;
					}
					v.clear();
					trueStates.clear();
					terms.clear();
					writer.write("###/###" + "\n");
					previousState = null;
				}else{
					testObservations++;
					terms.add(observation);
					trueStates.add(trueState);
					if(v.size()==0){
						v.add(updateV(observation, null));
					}else{
						v.add(updateV(observation, v.get(v.size()-1)));
					}
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
		Double errorRate = errorCount/testObservations;
		return errorRate;
	}
	

	private List<String> backtrack(List<Map<String, ViterbiBackScore>> v) {
		List<String> states = new ArrayList<String>();
		String prevTag = getMaxScoretag(v.get(v.size()-1));
		states.add(prevTag);
		for(int i=v.size()-2; i>0; i--){
			states.add(v.get(i).get(prevTag).getState());
		}
		return states;
	}

	// Using the viterbi algorithm, predict the state;
	private Map<String, ViterbiBackScore> updateV(String observation, Map<String, ViterbiBackScore> prevV) {
		
		Map<String, ViterbiBackScore> v = new HashMap<String, ViterbiBackScore>();
		//initialization
		if(prevV==null){
			for(String state: stateCounts.keySet()){
				Double a0j = stateTransitionProbabilities.get(new StateTransition(START_STATE, state));
				if(a0j==null) a0j = 1.0/stateTransitionCounts.size();
				Double bj = stateObservationProbabilities.get(new StateObservation(observation, state));
				if(bj==null) bj = 1.0/stateObservationCounts.size();
				v.put(state, new ViterbiBackScore(a0j*bj, START_STATE));
			}
			return v;
		}
		
		for(String state_j: stateCounts.keySet()){
			Double bj = stateObservationProbabilities.get(new StateObservation(observation, state_j));
			if(bj==null) bj = 1.0/stateObservationCounts.size();
			
			Map<String, Double> maxMap = new HashMap<String, Double>();
			for(String state_i: stateCounts.keySet()){
				Double aij = stateTransitionProbabilities.get(new StateTransition(state_i, state_j));
				if(aij==null) aij = 1.0/stateTransitionCounts.size();
				maxMap.put(state_i, prevV.get(state_i).getScore()*aij*bj);
			}
			ViterbiBackScore vitterbiScore = getViterbiBackScore(maxMap);
			v.put(state_j, vitterbiScore);				 
		}
		return v;
	}

	private ViterbiBackScore getViterbiBackScore(Map<String, Double> v) {
		String tag = UNKNOWN_STATE;
		double max = 0;
		for(String state: v.keySet()){
			if(v.get(state)>max){
				max = v.get(state);
				tag = state;
			}
		}
		return new ViterbiBackScore(max, tag);
	}
	
	private String getMaxScoretag(Map<String, ViterbiBackScore> map) {
		String tag = UNKNOWN_STATE;
		double max = 0;
		for(String state: map.keySet()){
			if(map.get(state).getScore()>max){
				max = map.get(state).getScore();
				tag = state;
			}
		}
		return tag;
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

	public static void main(String[] args){
		
		String trainingSetPath = "/Users/KartheekGanesh/Desktop/entrain.txt";
		PosTagger tagger = new PosTagger();
		tagger.trainHMM(trainingSetPath);
		
		String testingSetPath = "/Users/KartheekGanesh/Desktop/entest.txt";
		String taggedOutputPath = "/Users/KartheekGanesh/Desktop/out.txt";
 		tagger.tag(testingSetPath, taggedOutputPath);
    }
}
