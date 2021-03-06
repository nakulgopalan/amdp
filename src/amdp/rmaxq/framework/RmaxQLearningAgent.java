package amdp.rmaxq.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.policy.SolverDerivedPolicy;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

public class RmaxQLearningAgent implements LearningAgent {

	//Pa(s', x)
	private Map<GroundedTask, Map<HashableState, Map<HashableState, Double>>> transition;

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

	//envolope(a)
	private Map<GroundedTask, List<HashableState>> envolope;

	//ta 
	private Map<GroundedTask, List<HashableState>> terminal;

	private Map<GroundedTask, Integer> actionTimestaps;
	
	private double dynamicPrgEpsilon;
	private int threshold;
	private TaskNode root;
	private HashableStateFactory hashingFactory;
	private double Vmax;
	private Environment env;
	private State initialState;	
	private List<HashableState> reachableStates = new ArrayList<HashableState>();
	private long time = 0;
	private int timestep;

	public RmaxQLearningAgent(TaskNode root, HashableStateFactory hs, State initState, double vmax, int threshold, double maxDelta){
		this.root = root;
		this.reward = new HashMap<GroundedTask, Map<HashableState,Double>>();
		this.transition = new HashMap<GroundedTask, Map<HashableState, Map<HashableState, Double>>>();
		this.totalReward = new HashMap<HashableState, Map<GroundedTask,Double>>();
		this.actionCount = new HashMap<HashableState, Map<GroundedTask,Integer>>();
		this.qProvider = new HashMap<GroundedTask, QProviderRmaxQ>();
		this.envolope = new HashMap<GroundedTask, List<HashableState>>();
		this.resultingStateCount = new HashMap<HashableState, Map<GroundedTask,Map<HashableState,Integer>>>();
		this.terminal = new HashMap<GroundedTask, List<HashableState>>();
		this.qPolicy = new HashMap<GroundedTask, SolverDerivedPolicy>();
		this.groundedTaskMap = new HashMap<String, GroundedTask>();
		this.dynamicPrgEpsilon = maxDelta;
		this.hashingFactory = hs;
		this.Vmax = vmax;
		this.threshold = threshold;
		this.initialState = initState;
		this.actionTimestaps = new HashMap<GroundedTask, Integer>();
		reachableStates = StateReachability.getReachableStates(initialState, root.getDomain(), hashingFactory);
	}

	public long getTime(){
		return time;
	}
	public Episode runLearningEpisode(Environment env) {
		return runLearningEpisode(env, -1);
	}

	public Episode runLearningEpisode(Environment env, int maxSteps) {
		this.env = env;
		Episode e = new Episode(initialState);
		GroundedTask rootSolve = root.getApplicableGroundedTasks(env.currentObservation()).get(0);
		timestep = 0;
		actionTimestaps.clear();
		
		//look at equals in grounded task
		time = System.currentTimeMillis();
		HashableState hs = hashingFactory.hashState(env.currentObservation());
		e = R_MaxQ(hs, rootSolve, e);
		time = System.currentTimeMillis() - time;
		return e;
	}

	protected Episode R_MaxQ(HashableState hs, GroundedTask task, Episode e){
		if(task.t.isTaskPrimitive()){
			Action a = task.getAction();
			EnvironmentOutcome outcome = env.executeAction(a);
			e.transition(outcome);
			State sprime = outcome.op;
			HashableState hsprime = hashingFactory.hashState(sprime);

			//r(s,a) += r
			Map<GroundedTask, Double> rewrd = totalReward.get(hs);
			if(rewrd == null){
				rewrd = new HashMap<GroundedTask, Double>();
				totalReward.put(hs, rewrd);
			}
			Double totR = rewrd.get(task);
			if(totR == null){
				totR = 0.;
			}			
			totR = totR + outcome.r;
			rewrd.put(task, totR);

			//n(s,a) ++
			Map<GroundedTask, Integer> count = actionCount.get(hs);
			if(count == null){
				count = new HashMap<GroundedTask, Integer>();
				actionCount.put(hs, count);
			}
			Integer sum = count.get(task);
			if(sum == null){
				sum = 0;
			}
			sum = sum + 1;
			count.put(task, sum);

			//n(s,a,s')++
			Map<GroundedTask, Map<HashableState,Integer>> stateCount = resultingStateCount.get(hs);
			if(stateCount == null){
				stateCount = new HashMap<GroundedTask, Map<HashableState,Integer>>();
				resultingStateCount.put(hs, stateCount);
			}
			Map<HashableState, Integer> Scount = stateCount.get(task);
			if(Scount == null){
				Scount = new HashMap<HashableState, Integer>();
				stateCount.put(task, Scount);
			}
			Integer resCount = Scount.get(hsprime);
			if(resCount == null){
				resCount = 0;
			}
			resCount = resCount + 1;
			Scount.put(hsprime, resCount);

			//add pa(s, sprime) =0 in order to perform the update in compute model
			Map<HashableState, Map<HashableState,Double>> actionTSA = transition.get(task); 
			if(actionTSA == null){
				actionTSA = new HashMap<HashableState, Map<HashableState,Double>>();
				transition.put(task, actionTSA);
			}
			Map<HashableState, Double> endProb = actionTSA.get(hs);
			if(endProb == null){
				endProb = new HashMap<HashableState, Double>();
				actionTSA.put(hs, endProb);
			}
			Double prob = endProb.get(hsprime);
			if(prob == null)
				endProb.put(hsprime, 0.);

			timestep++;
			return e;
		}else{ //task is composute
			boolean terminal = false;
			QProviderRmaxQ qvalues = qProvider.get(task);
			if(qvalues == null){
				qvalues = new QProviderRmaxQ(hashingFactory, task);
				qProvider.put(task, qvalues);
			}			

			SolverDerivedPolicy taskFromPolicy = qPolicy.get(task);
			if(taskFromPolicy == null){
				taskFromPolicy = new GreedyQPolicy();
				qPolicy.put(task, taskFromPolicy);
			}			
			taskFromPolicy.setSolver(qvalues);

			do{
				computePolicy(hs, task);
				Action maxqAction = taskFromPolicy.action(hs.s());
				GroundedTask childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
				if(childFromPolicy == null){
					addChildTasks(task, hs.s());
					childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
				}
				//R pia(s') (s')
				e = R_MaxQ(hs, childFromPolicy , e);
				State s = e.stateSequence.get(e.stateSequence.size() - 1);
				hs = hashingFactory.hashState(s);

				terminal = task.t.terminal(s, task.action);
			}while(!terminal);

			return e;
		}

	}

	public void computePolicy(HashableState hs, GroundedTask task){
		
		Integer aTime = actionTimestaps.get(task);
		if(aTime == null){
			aTime = 0;
			actionTimestaps.put(task, aTime);
		}
		
		List<HashableState> envolopA = envolope.get(task);
		if(envolopA == null){
			envolopA = new ArrayList<HashableState>();
			envolope.put(task, envolopA);
		}

		if(aTime < timestep){
			actionTimestaps.put(task, timestep);
			envolopA.clear();
		}
		prepareEnvolope(hs, task);

		QProviderRmaxQ qp = qProvider.get(task);
		if(qp == null){
			qp = new QProviderRmaxQ(hashingFactory, task);
			qProvider.put(task, qp);
		}

		boolean converged = false;
		while(!converged){   
			double maxDelta = 0;
			for(HashableState hsprime : envolopA){

				List<GroundedTask> ActionIns = getTaskActions(task, hsprime.s());
				for(GroundedTask a : ActionIns){
					double oldQ = qp.qValue(hsprime.s(), a.action);

					double newQ = epuation_1(qp, hsprime, a);
					qp.update(hsprime.s(), a.action, newQ);

					if(Math.abs(oldQ - newQ) > maxDelta)
						maxDelta = Math.abs(oldQ - newQ);
				}
			}
			if(maxDelta < dynamicPrgEpsilon)
				converged = true;
		}
	}

	

	public void prepareEnvolope(HashableState hs, GroundedTask task){
		List<HashableState> envelope = envolope.get(task);
		if(!envelope.contains(hs)){
			envelope.add(hs);
			List<GroundedTask> ActionIns = getTaskActions(task, hs.s());
			for(GroundedTask a : ActionIns){
				compute_model(hs, a); 

				//get function forPa'(s, .)
				Map<HashableState, Map<HashableState,Double>> Pa = transition.get(a);
				if(Pa == null){
					Pa = new HashMap<HashableState, Map<HashableState,Double>>();
					transition.put(a, Pa);
				}
				Map<HashableState, Double> psa = Pa.get(hs);
				if(psa == null){
					psa = new HashMap<HashableState, Double>();
					Pa.put(hs, psa);
				}

				for(HashableState hsp : psa.keySet()){
					if(psa.get(hsp) > 0)
						prepareEnvolope(hsp, task);
				}
			}
		}
	}

	private void compute_model(HashableState hs, GroundedTask task){
		if(task.t.isTaskPrimitive()){
			//n(s, a)
			Map<GroundedTask, Integer> scount = actionCount.get(hs);
			if(scount == null){
				scount = new HashMap<GroundedTask, Integer>();
				actionCount.put(hs, scount);
			}
			Integer n_sa = scount.get(task);
			if(n_sa == null){
				n_sa = 0;
				scount.put(task, 0);
			}			
			if(n_sa >= threshold){
				//r(s, a)
				Map<GroundedTask, Double> rewards = totalReward.get(hs);
				if(rewards == null){
					rewards = new HashMap<GroundedTask, Double>();
					totalReward.put(hs, rewards);
				}
				Double r_sa = rewards.get(task);
				if(r_sa == null){
					r_sa = 0.;
					rewards.put(task, r_sa);
				}			

				//set Ra(s) to r(s,a) / n(s,a)
				Map<HashableState, Double> Ra = reward.get(task);
				if(Ra == null){
					Ra = new HashMap<HashableState, Double>();
					reward.put(task, Ra);
				}
				double newR = (double)r_sa / n_sa;
				Ra.put(hs, newR);

				//get Pa(s, .)
				Map<HashableState, Map<HashableState,Double>> Ptask = transition.get(task);
				if(Ptask == null){
					Ptask = new HashMap<HashableState, Map<HashableState,Double>>();
					transition.put(task, Ptask);
				}
				Map<HashableState, Double> pas = Ptask.get(hs); 
				if(pas == null){
					pas = new HashMap<HashableState, Double>();
					Ptask.put(hs, pas);
				}

				Map<GroundedTask, Map<HashableState,Integer>> nS = resultingStateCount.get(hs);
				if(nS == null){
					nS = new HashMap<GroundedTask, Map<HashableState,Integer>>();
					resultingStateCount.put(hs, nS);
				}
				Map<HashableState, Integer> nSA = nS.get(task);
				if(nSA == null){
					nSA = new HashMap<HashableState, Integer>();
					nS.put(task, nSA);
				}
				for(HashableState hsprime : pas.keySet()){
					//get n(s, a, s')
					Integer n_sasp = nSA.get(hsprime);
					if(n_sasp == null){
						n_sasp = 0;
						nSA.put(hsprime, n_sasp);
					}

					//set Pa(s, s') = n(s,a,s') / n(s, a)
					double p_assp = (double)n_sasp / n_sa;
					pas.put(hsprime, p_assp);
				}
			}
		}else{
			computePolicy(hs, task);
			
			QProviderRmaxQ qvalues = qProvider.get(task);
			SolverDerivedPolicy taskFromPolicy = qPolicy.get(task);
			if(taskFromPolicy == null){
				taskFromPolicy = new GreedyQPolicy();
				qPolicy.put(task, taskFromPolicy);
			}
			taskFromPolicy.setSolver(qvalues);

			Map<HashableState, Map<HashableState,Double>> probtask = transition.get(task);
			if(probtask == null){
				probtask = new HashMap<HashableState, Map<HashableState,Double>>();
				transition.put(task, probtask);
			}					

			Map<HashableState, Double> rewtask = reward.get(task);
			if(rewtask == null){
				rewtask = new HashMap<HashableState, Double>();
				reward.put(task, rewtask);
			}
			
			List<HashableState> envelopeA = envolope.get(task);
			if(envelopeA == null){
				envelopeA = new ArrayList<HashableState>();
				envolope.put(task, envelopeA);
			}
			
			boolean converged = false;
			while(!converged){
				double maxChange = 0;
				//temporary holders for batch updates
				for(HashableState hsprime : envelopeA){
					Action maxqAction = taskFromPolicy.action(hsprime.s());
					GroundedTask childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
					if(childFromPolicy == null){
						addChildTasks(task, hsprime.s());
						childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
					}
					
					//p pia(s') (s',.)
					Map<HashableState, Map<HashableState,Double>> pfrompia = transition.get(childFromPolicy);
					if(pfrompia == null){
						pfrompia = new HashMap<HashableState, Map<HashableState,Double>>();
						transition.put(childFromPolicy, pfrompia);
					}
					Map<HashableState, Double> childProbabilities = pfrompia.get(hsprime);
					if(childProbabilities == null){
						childProbabilities = new HashMap<HashableState, Double>();
						pfrompia.put(hsprime, childProbabilities);
					}
					
					Double prevReward = rewtask.get(hsprime);
					if(prevReward == null){
						prevReward = Vmax;
						rewtask.put(hsprime, prevReward);
					}					
					double newReward = equation_4(task, taskFromPolicy, rewtask, hsprime, childProbabilities);

					//find max change for value iteration
					if(Math.abs(newReward - prevReward) > maxChange)
						maxChange = Math.abs(newReward - prevReward);

					Map<HashableState, Double> Pstosp = probtask.get(hsprime);
					if(Pstosp == null){
						Pstosp = new HashMap<HashableState, Double>();
						probtask.put(hsprime, Pstosp);
					}

					//for all s in ta
					// equation 7
					List<HashableState> terminal = getTerminalStates(task);
					for(HashableState hx :terminal){
						//get current pa(s',x)
						Double oldPrabability = Pstosp.get(hx);
						if(oldPrabability == null){
							oldPrabability = 0.;
							Pstosp.put(hx, oldPrabability);
						}

						double newProb = equation_5(task, probtask, childProbabilities, hx);

						if(Math.abs(newProb - oldPrabability) > maxChange)
							maxChange = Math.abs(newProb - oldPrabability);

						//set pa(s',x)
						Pstosp.put(hx, newProb);
					}
					if(maxChange < dynamicPrgEpsilon)
						converged = true;
				}
			}	
		}
	}

	private double epuation_1(QProviderRmaxQ qp, HashableState hsprime, GroundedTask a) {
		//Ra'(s')
		Map<HashableState, Double> actionAR = reward.get(a);
		if(actionAR == null){
			actionAR = new HashMap<HashableState, Double>();
			reward.put(a, actionAR);
		}
		Double actionReward = actionAR.get(hsprime);
		if(actionReward == null){
			actionReward = Vmax;
			actionAR.put(hsprime, actionReward);
		}

		Map<HashableState, Map<HashableState,Double>> Pchildaction = transition.get(a);
		if(Pchildaction == null){
			Pchildaction = new HashMap<HashableState, Map<HashableState,Double>>();
			transition.put(a, Pchildaction);
		}
		Map<HashableState, Double> fromsp = Pchildaction.get(hsprime);
		if(fromsp == null){
			fromsp = new HashMap<HashableState, Double>();
			Pchildaction.put(hsprime, fromsp);
		}

		double weightedQ = 0;
		for(HashableState hspprime : fromsp.keySet()){
			double value = qp.value(hspprime.s());
			weightedQ += fromsp.get(hspprime) * value;
		}
		double newQ = actionReward + weightedQ;
		return newQ;
	}


	private double equation_4(GroundedTask task, SolverDerivedPolicy taskFromPolicy, Map<HashableState, Double> rewtask,
			HashableState hsprime, Map<HashableState, Double> childProbabilities) {
		Action maxqAction = taskFromPolicy.action(hsprime.s());
		GroundedTask childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
		if(childFromPolicy == null){
			addChildTasks(task, hsprime.s());
			childFromPolicy = groundedTaskMap.get(maxqAction.actionName());
		}

		//R pia(s') (s')
		Map<HashableState, Double> childrew = reward.get(childFromPolicy);
		if(childrew == null){
			childrew = new HashMap<HashableState, Double>();
			reward.put(childFromPolicy, childrew);
		}
		Double actionReward = childrew.get(hsprime);
		if(actionReward == null){
			actionReward = Vmax;
			childrew.put(hsprime, actionReward);
		}

		double weightedReward = 0;
		for(HashableState hnext : childProbabilities.keySet()){
			//get Ra(nextstate)
			if(task.t.terminal(hnext.s(), task.action))
				continue;

			Double nextReward = rewtask.get(hnext);
			if(nextReward == null){
				nextReward = Vmax;
				rewtask.put(hnext, nextReward);
			}
			weightedReward += childProbabilities.get(hnext) * nextReward;
		}
		double newReward = actionReward + weightedReward;
		rewtask.put(hsprime, newReward);
		return newReward;
	}

	private double equation_5(GroundedTask task, Map<HashableState, Map<HashableState, Double>> probtask,
			Map<HashableState, Double> childProbabilities, HashableState hx) {
		Double childProbability = childProbabilities.get(hx);
		if(childProbability == null){
			childProbability = 0.;
			childProbabilities.put(hx, childProbability);
		}

		double weightedTransition = 0;
		//sum over all p pia(s) (s',.)
		for(HashableState hnext: childProbabilities.keySet()){
			if(task.t.terminal(hnext.s(), task.action))
				continue;

			double psprimeTospprime = childProbabilities.get(hnext);
			//pa (s'',x)
			Map<HashableState, Double> tohnext = probtask.get(hnext);
			if(tohnext == null){
				tohnext = new HashMap<HashableState, Double>();
				probtask.put(hnext, tohnext);
			}
			Double pspptohx = tohnext.get(hx);
			if(pspptohx == null){
				pspptohx = 0.;
				tohnext.put(hx, pspptohx);
			}

			weightedTransition += psprimeTospprime * pspptohx;
		}
		double newProb = childProbability + weightedTransition;
		return newProb;
	}
		
	protected void addChildTasks(GroundedTask task, State s){
		if(!task.t.isTaskPrimitive()){
			List<GroundedTask> childGroundedTasks =  getTaskActions(task, s);

			for(GroundedTask gt : childGroundedTasks){
				if(!groundedTaskMap.containsKey(gt.action.actionName()))
					groundedTaskMap.put(gt.action.actionName(), gt);
			}
		}
	}

	protected List<HashableState> getTerminalStates(GroundedTask t){
		if(terminal.containsKey(t))
			return terminal.get(t);
		List<HashableState> terminals = new ArrayList<HashableState>();
		for(HashableState s :reachableStates){
			if(t.t.terminal(s.s(), t.getAction()))
				terminals.add(s);
		}

		terminal.put(t, terminals);
		if(terminals.size() == 0)
			throw new RuntimeException("no terminal");
		return terminals;
	}

	public List<GroundedTask> getTaskActions(GroundedTask task, State s){
		TaskNode[] children = task.t.getChildren();
		List<GroundedTask> childTasks = new ArrayList<GroundedTask>();
		for(TaskNode t: children){
			childTasks.addAll(t.getApplicableGroundedTasks(s));
		}
		return childTasks;
	}
}