package amdp.ramdp.framework;

import java.util.ArrayList;
import java.util.List;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.oo.OOSADomain;

public abstract class Task {


	/**
	 * domain - the MDP representing the level of abstraction
	 */
	private OOSADomain domain;
	
	/**
	 * actionType - the general action type acossiated with the task 
	 */
	private ActionType actionType;
	
	/**
	 * children - the subtasks in a hierarchy
	 */
	private Task[] children;
	
	/**
	 * get the domain
	 * @return the abstract domain for the task
	 */
	public OOSADomain getDomain(){
		return domain;
	}
		
	/**
	 * a unique ID
	 * @return unique ID for the task in the hierarchy
	 */
	public abstract String getName();
	
	/**
	 * tells whether this task is in the base MDP
	 * @return boolean indicating whether the task is 
	 * primitive (true) or composite (false)
	 */
	public abstract boolean isPrimitive();
	
	/**
	 * Gets all parameterizations of the task availibe in s
	 * @param s the current state
	 * @return list of grounded tasks which gives all variations 
	 * of the task in the current state
	 */
	public List<GroundedTask> getAllGroundedTasks(State s){
		List<Action> acts = actionType.allApplicableActions(s);
		List<GroundedTask> gts = new ArrayList<GroundedTask>();
		for(Action a : acts){
			gts.add(new GroundedTask(a, this));
		}
		return gts;
	}
	
	/**
	 * Gets the subtasks of the current task
	 * @return array of subtasks of the task
	 */
	public Task[] getChildren(){
		return children;
	}
	
	/**
	 * determines if the current task is terminated in state s which parameterization a 
	 * @param s the current state
	 * @param a the action from the specific grounding
	 * @return boolean indicating if the action a is terminated in state s
	 */
	public abstract boolean isTerminal(State s, Action a);
}
