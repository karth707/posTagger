package com.kg.posTagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunPosTagger {

	private static Logger log = LoggerFactory.getLogger(RunPosTagger.class);
	
	public static void main(String[] args){
		
		String TRAINING_SET_PATH = "/Users/KartheekGanesh/Desktop/entrain.txt";
		String TESTING_SET_PATH = "/Users/KartheekGanesh/Desktop/entest.txt";
		String TAGGED_OUTPUT_PATH = "/Users/KartheekGanesh/Desktop/out.txt";
		
		PosTagger tagger = new PosTagger();
		tagger.trainHMM(TRAINING_SET_PATH);
 		double errorRate = tagger.tag(TESTING_SET_PATH, TAGGED_OUTPUT_PATH);
 		log.info("!!!!!--ERROR RATE: " + errorRate + " --!!!!!");
	}
}
