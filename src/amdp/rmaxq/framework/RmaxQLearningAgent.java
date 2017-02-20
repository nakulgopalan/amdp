package amdp.rmaxq.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import amdp.maxq.framework.QProviderForMAXQ;
import amdp.rmaxq.framework.RmaxQLearningAgent.StateTransition;
import amdp.utilities.BoltzmannQPolicyWithCoolingSchedule;
import burlap.behavior.policy.SolverDerivedPolicy;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
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
	private Map<GroundedTask, Map<HashableState, List<StateTransition>>> transition;
	
	//Ra(s) 
	private Map<GroundedTask, Map<HashableState, Double>> reward;
	//pi a
	
	//r(s,a)
	private Map<HashableState, Map< GroundedTask, Double>> totalReward;
	
	//n(s,a)
	private Map<HashableState, Map< GroundedTask, Integer>> actionCount;
	
	//n(s,a,s')
	private Map<HashableState, Map< GroundedTask, Map<HashableState, Integer>>> resultingStateCount;
	
	//grounded task map
	private Map<String, GroundedTask> groundedTaskMap;
	
	//QProviders for each grounded task
	private Map<GroundedTask, QProviderRmaxQ> qProvider;
	
	//policies
	private Map<GroundedTask, SolverDerivedPolicy> qPolicy;
	
	//t
	private int timestamp;
	
	//envolope(a)
	private Map<GroundedTask, List<State>> envolope;
	
	//ta 
	private Map<GroundedTask, List<State>> terminal;
	
	private double dynamicPrgEpsilon;
	private int threshold;
	private TaskNode root;
	private HashableStateFactory hashingFactory;
	private double Vmax;
	private Environment env;
	private State initialState;
	
	public RmaxQLearningAgent(TaskNode root, HashableStateFactory hs, double vmax, int threshold, double maxDelta){
//		this.qValue = new HashMap<GroundedTask, Map<HashableState,Map<GroundedTask,Double>>>();
//		this.value = new HashMap<GroundedTask, Map<HashableState,Double>>();
		
		this.root = root;
		this.reward = new HashMap<GroundedTask, Map<HashableState,Double>>();
		this.transition = new HashMap<GroundedTask, Map<HashableState, List<StateTransition>>>();
		this.reward = new HashMap<GroundedTask, Map<HashableState,Double>>();
		this.totalReward = new HashMap<HashableState, Map<GroundedTask,Double>>();
		this.actionCount = new HashMap<HashableState, Map<GroundedTask,Integer>>();
		this.qProvider = new HashMap<GroundedTask, QProviderRmaxQ>();
		this.envolope = new HashMap<GroundedTask, List<State>>();
		this.resultingStateCount = new HashMap<HashableState, Map<GroundedTask,Map<HashableState,Integer>>>();
		this.terminal = new HashMap<GroundedTask, List<State>>();
		this.qPolicy = new HashMap<GroundedTask, SolverDerivedPolicy>();
		this.groundedTaskMap = new HashMap<String, GroundedTask>();
		this.dynamicPrgEpsilon = maxDelta;
		this.timestamp = 0;
		this.hashingFactory = hs;
		this.Vmax = vmax;
		this.threshold = threshold;
	}
	 
	public Episode runLearningEpisode(Environment env) {
		return runLearningEpisode(env, -1);
	}

	public Episode runLearningEpisode(Environment env, int maxSteps) {
		this.env = env;
		this.initialState = env.currentObservation();
		Episode e = new Episode(env.currentObservation());
		GroundedTask rootSolve = root.getApplicableGroundedTasks(env.currentObservation()).get(0);
		return R_MaxQ(env.currentObservation(), rootSolve, e);
	}

	protected Episode R_MaxQ(State s, GroundedTask task, Episode e){
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
			double r = totalReward.get(hs).get(task) + outcome.r;
			totalReward.get(hs).put(task, r);
			
			//n(sa) ++
			if(!actionCount.containsKey(hs))
				actionCount.put(hs, new HashMap<GroundedTask, Integer>());
			if(!actionCount.get(hs).containsKey(task))
				actionCount.get(hs).put(task, 0);
			int n = actionCount.get(hs).get(task) + 1;
			actionCount.get(hs).put(task, n);
			
			//n(s,a,s')++
			if(!resultingStateCount.containsKey(hs))
				resultingStateCount.put(hs, new HashMap<GroundedTask, Map<HashableState,Integer>>());
			if(!resultingStateCount.get(hs).containsKey(task))
				resultingStateCount.get(hs).put(task, new HashMap<HashableState, Integer>());
			if(!resultingStateCount.get(hs).get(task).containsKey(hsprime))
				resultingStateCount.get(hs).get(task).put(hsprime, 0);
			n = resultingStateCount.get(hs).get(task).get(hsprime) + 1;
			
			timestamp++;
			
			return e;
		}else{
			
		}
	}
	
	public void computePolicy(State s, GroundedTask task){
		
	}
	
	public void prepareEnvolope(State s, GroundedTask task){
		if(!envolope.containsKey(task))
			envolope.put(task, new ArrayList<State>());
		List<State> envelope = envolope.get(task);
		HashableState hs = hashingFactory.hashState(s);
		
		if(!envelope.contains(s)){
			envelope.add(s);
			List<GroundedTask> ActionIns = task.t.getApplicableGroundedTasks(s);
			for(GroundedTask a : ActionIns){
				computeModel(s, a);
				
				//get function forPa'(s, .)
				if(!transition.containsKey(a))
					transition.put(a, new HashMap<HashableState, List<StateTransition>>);
				if(!transition.get(a).containsKey(hs)){
					transition.get(a).put(hs, new ArrayList<RmaxQLearningAgent.StateTransition>)
				}
				List<StateTransition> aProbFroms = transition.get(a).get(hs);
				for(StateTransition st : aProbFroms ){
					if(st.probability > 0){
						prepareEnvolope(st.s, task);
					}
				}
			}
		}
	}
	
	public void computeModel(State s, GroundedTask task){
		HashableState hs = hashingFactory.hashState(s);
		if(task.t.isTaskPrimitive()){
			//n(s, a)
			if(!actionCount.containsKey(hs))
				actionCount.put(hs, new HashMap<GroundedTask, Integer>());
			if(!actionCount.get(hs).containsKey(task))
				actionCount.get(hs).put(task, 0);
			int n_sa = actionCount.get(hs).get(task);
			
			if(n_sa >= threshold){
				//r(s, a)
				if(!totalReward.containsKey(hs))
					totalReward.put(hs, new HashMap<GroundedTask, Double>());
				if(!totalReward.get(hs).containsKey(task))
					totalReward.get(hs).put(task, 0.);
				double r_sa = totalReward.get(hs).get(task);
				
				//set Ra(s) to r(s,a) / n(s,a)
				if(!reward.containsKey(task))
					reward.put(task, new HashMap<HashableState, Double>());
				
				double newR = r_sa / n_sa;
				reward.get(task).put(hs, newR);
				
				//get Pa(s, .)
				if(!transition.containsKey(task))
					transition.put(task, new HashMap<HashableState, List<StateTransition>>());
				if(!transition.get(task).containsKey(hs))
					transition.get(task).put(hs, new ArrayList<StateTransition>());
				
				List<StateTransition> sprimes = transition.get(task).get(hs);
				for(StateTransition st : sprimes){
					HashableState hsprime = hashingFactory.hashState(st.s);
					//get n(s, a, s')
					if(!resultingStateCount.containsKey(hs))
						resultingStateCount.put(hs, new HashMap<GroundedTask, Map<HashableState,Integer>>());
					if(!resultingStateCount.get(hs).containsKey(task))
						resultingStateCount.get(hs).put(task, new HashMap<HashableState, Integer>());
					if(!resultingStateCount.get(hs).get(task).containsKey(hsprime))
						resultingStateCount.get(hs).get(task).put(hsprime, 0);
					int n_sasp = resultingStateCount.get(hs).get(task).get(hsprime);
					
					//set Pa(s, s') = n(s,a,s') / n(s, a)
					double p_assp = n_sasp / n_sa;
					if(!transition.containsKey(task))
						transition.put(task, new HashMap<HashableState, List<StateTransition>>());
					if(!transition.get(task).containsKey(hs))
						transition.get(task).put(hs, new ArrayList<StateTransition>());
					transition.get(task).get(hs).add(new StateTransition(st.s,  p_assp));
				}
			}
		}else{
			computePolicy(s, task);
			
			boolean converged = false;
			while(!converged){
				double maxChange = 0;
				
				if(!envolope.containsKey(task))
					envolope.put(task, new ArrayList<State>());
				List<State> envelopeA = envolope.get(task);
				for(State sprime : envelopeA){
					HashableState hsprime = hashingFactory.hashState(sprime);
					//Ra(sp)
					if(!reward.containsKey(task))
						reward.put(task, new HashMap<HashableState, Double>());
					if(!reward.get(task).containsKey(hsprime))
						reward.get(task).put(hsprime, Vmax);
					double prevReward = reward.get(task).get(hsprime);
					
					// calculate new reward
					//get qprovider
					if(!qProvider.containsKey(task))
						qProvider.put(task, new QProviderRmaxQ(hashingFactory));
					QProviderRmaxQ qvalues = qProvider.get(task);
					
					if(!qPolicy.containsKey(task))
						qPolicy.put(task, new BoltzmannQPolicyWithCoolingSchedule(50, 0.99879));
					SolverDerivedPolicy taskFromPolicy = qPolicy.get(task);
					taskFromPolicy.setSolver(qvalues);
					Action maxqAction = taskFromPolicy.action(sprime);
					if(!groundedTaskMap.containsKey(maxqAction.actionName()))
						addChildTasks(task);
					
					//R pia(s') (s')
					GroundedTask childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
					if(!reward.containsKey(childFromPolicy))
						reward.put(childFromPolicy, new HashMap<HashableState, Double>());
					if(!reward.get(childFromPolicy).containsKey(hsprime))
						reward.get(childFromPolicy).put(hsprime, Vmax);
					double actionReward = reward.get(childFromPolicy).get(hsprime);
					
					//p pia(s') (s',.)
					if(!transition.containsKey(childFromPolicy))
						transition.put(childFromPolicy, new HashMap<HashableState, List<StateTransition>>());
					if(!transition.get(childFromPolicy).containsKey(hsprime))
						transition.get(childFromPolicy).put(hsprime, new ArrayList<StateTransition>());
					List<StateTransition> childProbabilities = this.transition.get(childFromPolicy).get(hsprime);
					
					double weightedReward = 0;
					for(StateTransition nextState : childProbabilities){
						//get Ra(nextstate)
						if(task.t.terminal(nextState.s, task.action))
							continue;
						
						HashableState hnext = hashingFactory.hashState(nextState.s);
						if(!reward.containsKey(task))
							reward.put(task, new HashMap<HashableState, Double>());
						if(!reward.get(task).containsKey(hnext))
							reward.get(task).put(hnext, Vmax);
						double nextReward = reward.get(task).get(hnext);
						
						weightedReward += nextState.probability * nextReward;
					}
					
					if(!reward.containsKey(task))
						reward.put(task, new HashMap<HashableState, Double>());
					
					double newReward = actionReward + weightedReward;
					reward.get(task).put(hsprime, newReward);
					
					//find max change for value iteration
					if(Math.abs(newReward - prevReward) > maxChange)
						maxChange = Math.abs(newReward - prevReward);
					
					//for all s in ta
					List<State> terminal = getTerminalStates(task);
					for(State x :terminal){
						HashableState hx = hashingFactory.hashState(x);
						//get current pa(s',x)
						if(transition.containsKey(task))
							transition.put(task, new HashMap<HashableState, List<StateTransition>>());
						if(!transition.get(task).containsKey(hsprime))
							transition.get(task).put(hsprime, new ArrayList<StateTransition>());
						List<StateTransition> PaOfs = transition.get(task).get(hsprime);
						
						//look for transition from s' to x
						double oldPrabability = 0;
						for(StateTransition st: PaOfs){
							HashableState sthState = hashingFactory.hashState(st.s);
							if(sthState.equals(hx)){
								oldPrabability = st.probability;
								break;
							}
						}
						
						//p pia(s) (s',x)
						if(!transition.containsKey(childFromPolicy))
							transition.put(childFromPolicy, new HashMap<HashableState, List<StateTransition>>());
						if(!transition.get(childFromPolicy).containsKey(hsprime))
							transition.get(childFromPolicy).put(hsprime, new ArrayList<StateTransition>());
						List<StateTransition> piafroms = transition.get(childFromPolicy).get(hsprime);
						
						double childProbability = 0;
						for(StateTransition st: piafroms){
							HashableState hststate = hashingFactory.hashState(st.s);
							if(hststate.equals(hx)){
								childProbability = st.probability;
								break;
							}
						}
						
						double weightedTransition = 0;
						//sum over all p pia(s) (s',.)
						for(StateTransition st: piafroms){
							HashableState hspprime = hashingFactory.hashState(st.s);
							
							//pa (s'',x)
							if(!transition.containsKey(task))
								transition.put(task, new HashMap<HashableState, List<StateTransition>>());
							if(!transition.get(task).containsKey(hspprime))
								transition.get(task).put(hspprime, new ArrayList<StateTransition>());
							
							double probspptox = 0;
							List<StateTransition> fromspp = transition.get(task).get(hspprime);
							for(StateTransition sfromspp: fromspp){
								HashableState hsx = hashingFactory.hashState(sfromspp.s);
								if(hsx.equals(hx)){
									probspptox = sfromspp.probability;
									break;
								}
							}
							
							weightedTransition += st.probability * probspptox;
						}
						double newProb = childProbability + weightedTransition;
						
						if(Math.abs(newProb - oldPrabability) > maxChange)
							maxChange = Math.abs(newProb - oldPrabability);
						
						//set pa(s',x)
						if(!transition.containsKey(task))
							transition.put(task, new HashMap<HashableState, List<StateTransition>>());
						if(!transition.get(task).containsKey(hsprime))
							transition.get(task).put(hsprime, new ArrayList<StateTransition>());
						List<StateTransition> paspx = transition.get(task).get(hsprime);
						
						for(StateTransition st : paspx){
							HashableState hsx = hashingFactory.hashState(st.s);
							if(hsx.equals(hx)){
								st.probability = newProb;
								break;
							}
						}
					}
					
				}
				if(maxChange < dynamicPrgEpsilon)
					converged = true;
			}
		}
	}
	
	public class StateTransition{
		public State s;
		public Double probability;
		
		public StateTransition(State s, Double pr){
			this.s = s;
			this.probability = pr;
		}
	}
	
	protected void addChildTasks(GroundedTask task){
		if(!task.t.isTaskPrimitive()){
			TaskNode[] children = ((NonPrimitiveTaskNode)task.t).getChildren();
			List<GroundedTask> childGroundedTasks = new ArrayList<GroundedTask>();
			for(TaskNode child: children){
				childGroundedTasks.addAll(child.getApplicableGroundedTasks(env.currentObservation()));
			}
			
			for(GroundedTask gt : childGroundedTasks){
				if(!groundedTaskMap.containsKey(gt.action.actionName()))
					groundedTaskMap.put(gt.action.actionName(), gt);
			}
		}
	}
	
	protected List<State> getTerminalStates(GroundedTask t){
		if(terminal.containsKey(t))
			return terminal.get(t);
		List<State> reachable = StateReachability.getReachableStates(initialState, t.t.getDomain(), hashingFactory);
		List<State> terminals = new ArrayList<State>();
		for(State s :reachable){
			if(t.t.terminal(s, t.getAction()))
				terminals.add(s);
		}

		terminal.put(t, terminals);
		return terminals;
	}
}
