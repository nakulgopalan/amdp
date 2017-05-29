package amdp.taxi.hierarcchyResults;

import java.util.List;

import amdp.amdpframework.AMDPLearningAgent;
import amdp.amdpframework.AMDPModelLearner;
import amdp.amdpframework.GroundedTask;
import amdp.rmaxq.framework.RmaxQLearningAgent;
import amdp.rmaxq.framework.TaskNode;
import amdp.state.hashing.simple.SimpleHashableStateFactory;
import amdp.taxi.TaxiDomain;
import amdp.taxi.TaxiRewardFunction;
import amdp.taxi.TaxiTerminationFunction;
import amdp.taxi.TaxiVisualizer;
import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.LearningAgentFactory;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.VisualActionObserver;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.HashableStateFactory;

public class HierarchyDriver {

	private static OOSADomain domain;
	private static SimulatedEnvironment env;
	
	public static void createEnvirnment(boolean fickle, boolean regularState){
		TerminalFunction taxiTF = new TaxiTerminationFunction();
        RewardFunction taxiRF = new TaxiRewardFunction(1,taxiTF);

        TaxiDomain TDGen = new TaxiDomain(taxiRF, taxiTF);
        
        if(fickle)
        	TDGen.setTransitionDynamicsLikeFickleTaxiProlem();
        TDGen.setFickleTaxi(fickle);
        TDGen.setIncludeFuel(false);
        OOSADomain td = TDGen.generateDomain();
        domain = td;
        State s;
        if(regularState)
        	s = TaxiDomain.getClassicState(false);
        else
        	s = TaxiDomain.getSmallClassicState(false);
        env = new SimulatedEnvironment(td, s);

        VisualActionObserver obs = new VisualActionObserver(td, TaxiVisualizer.getVisualizer(5, 5));
        obs.initGUI();
        env.addObservers(obs);
	}
	
	public static void runTrials(int numTrials, int trialLength, final int threshold, final double discount,
			final double rmax, final double viEpsilon, final int maxVIPasses, final List<AMDPModelLearner> mlList, 
			final TaskNode rmaxqRoot, final GroundedTask amdpRoot, final HashableStateFactory hs){
		
		LearningAgentFactory rmaxAgent = new LearningAgentFactory() {
			public String getAgentName() {
				return "R-Max";
			}
			
			public LearningAgent generateAgent() {
				return new PotentialShapedRMax(domain, discount, hs, rmax, threshold, viEpsilon, maxVIPasses);
			}
		};
		
		LearningAgentFactory rmaxqAgent = new LearningAgentFactory() {
			public String getAgentName() {
				return "R-MaxQ";
			}
			
			public LearningAgent generateAgent() {
				return new RmaxQLearningAgent(rmaxqRoot, hs, env.currentObservation(), rmax, threshold, viEpsilon);
			}
		};
		
		LearningAgentFactory ramdpAgent = new LearningAgentFactory() {
			public String getAgentName() {
				return "R-amdp";
			}
			
			public LearningAgent generateAgent() {
				return new AMDPLearningAgent(amdpRoot, mlList);
			}
		};
		
		LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter(env, numTrials, trialLength, 
				ramdpAgent, rmaxAgent, rmaxqAgent);
		exp.setUpPlottingConfiguration(900, 500, 2, 1000,
				TrialMode.MOST_RECENT_AND_AVERAGE,
				PerformanceMetric.AVERAGE_EPISODE_REWARD,
				PerformanceMetric.CUMULATIVE_REWARD_PER_STEP,
				PerformanceMetric.CUMULATIVE_STEPS_PER_EPISODE,
				PerformanceMetric.MEDIAN_EPISODE_REWARD,
				PerformanceMetric.STEPS_PER_EPISODE,
				PerformanceMetric.CUMULATIVE_REWARD_PER_EPISODE);

		exp.startExperiment();
	}
	public static void main(String[] args) {	
		
		createEnvirnment(false, false);
		TaskNode RMAXQroot = TaxiRMAXQHierarchy.taxiHeirarcy(env.currentObservation(), domain);
		GroundedTask AMDProot = TaxiAMDPHierarchy.taxiHierarchy(env.currentObservation(), domain);
		List<AMDPModelLearner> pgList = TaxiAMDPHierarchy.pgList;
		HashableStateFactory hs = new SimpleHashableStateFactory();
		runTrials(1, 100, 5, 0.99, 100, 0.01, 1, pgList, RMAXQroot, AMDProot, hs);
	}
}
