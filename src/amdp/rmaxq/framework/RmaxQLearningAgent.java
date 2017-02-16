package amdp.rmaxq.framework;

import java.util.HashMap;
import java.util.Map;

import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.valuefunction.QProvider;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

public class RmaxQLearningAgent implements LearningAgent {

	//Qa(s', a')
//	private Map<GroundedTask, Map<HashableState, Map<GroundedTask, Double>>> qValue;
	
	//Va(s)
//	private Map<GroundedTask, Map<HashableState, Double>> value;
	
	//Pa(s', x)
	private Map<GroundedTask, Map<HashableState, Map<HashableState, Double>>> transition;
	
	//Ra(s) 
	private Map<GroundedTask, Map<HashableState, Double>> reward;
	
	//pi a
	
	//r(s,a)
	private Map<HashableState, Map< GroundedTask, Double>> totalReward;
	
	//n(s,a)
	private Map<HashableState, Map< GroundedTask, Double>> actionCount;
	
	//n(s,a,s')
	private Map<HashableState, Map< GroundedTask, Map<HashableState, Double>>> resultingStateCount;
	
	//QProviders for each grounded task
	private Map<GroundedTask, QProvider> qPolicy;
	
	//t
	private int timestamp;
	
	private TaskNode root;
	private HashableStateFactory hashingFactory;
	private double Vmax;
	private Environment env;
	
	public RmaxQLearningAgent(TaskNode root, HashableStateFactory hs, double vmax){
//		this.qValue = new HashMap<GroundedTask, Map<HashableState,Map<GroundedTask,Double>>>();
//		this.value = new HashMap<GroundedTask, Map<HashableState,Double>>();
		
		this.root = root;
		this.reward = new HashMap<GroundedTask, Map<HashableState,Double>>();
		this.transition = new HashMap<GroundedTask, Map<HashableState,Map<HashableState,Double>>>();
		this.reward = new HashMap<GroundedTask, Map<HashableState,Double>>();
		this.totalReward = new HashMap<HashableState, Map<GroundedTask,Double>>();
		this.actionCount = new HashMap<HashableState, Map<GroundedTask,Double>>();
		this.qPolicy = new HashMap<GroundedTask, QProvider>();
		this.timestamp = 0;
		this.hashingFactory = hs;
		this.Vmax = vmax;
	}
	 
	public Episode runLearningEpisode(Environment env) {
		return runLearningEpisode(env, -1);
	}

	public Episode runLearningEpisode(Environment env, int maxSteps) {
		this.env = env;
		
		return R_MaxQ(env.currentObservation(), );
	}

	protected Episode R_MaxQ(State s, GroundedTask task){
		Episode e = new Episode(env.currentObservation());
		HashableState hs = hashingFactory.hashState(s);
		if(task.t.isTaskPrimitive()){
			Action a = task.getAction();
			EnvironmentOutcome outcome = env.executeAction(a);
			e.transition(outcome);
			State sprime = outcome.op;
			HashableState hsprime = hashingFactory.hashState(sprime);
			
			//r(s,a) ++
			if(!totalReward.containsKey(hs))
				totalReward.put(hs, new HashMap<GroundedTask, Double>());
			if(!totalReward.get(hs).containsKey(task))
				totalReward.get(hs).put(task, 0.);
			double r = totalReward.get(hs).get(task) + 1;
			totalReward.get(hs).put(task, r);
			
			//n(sa) ++
			if(!actionCount.containsKey(hs))
				actionCount.put(hs, new HashMap<GroundedTask, Double>());
			if(!actionCount.get(hs).containsKey(task))
				actionCount.get(hs).put(task, 0.);
			double n = actionCount.get(hs).get(task) + 1;
			actionCount.get(hs).put(task, n);
			
			//n(s,a,s')++
			
		}else{
			
		}
	}
}
