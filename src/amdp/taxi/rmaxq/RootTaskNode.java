package amdp.taxi.rmaxq;

import amdp.rmaxq.framework.NonPrimitiveTaskNode;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.TaxiRewardFunction;
import amdp.taxi.TaxiTerminationFunction;
import amdp.rmaxq.framework.GroundedTask;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ngopalan on 8/14/16.
 */
public class RootTaskNode extends NonPrimitiveTaskNode {

    List<String[]> params = new ArrayList<String[]>();
    List<GroundedTask> groundedTasks = new ArrayList<GroundedTask>();
    

    public RootTaskNode(String name, OOSADomain domainIn, TaskNode[] children, int numPas) {
        this.name = name;
        this.params.add(new String[]{"1"});
        this.taskNodes = children;
        this.domain = domainIn;

        this.tf = new TaxiTerminationFunction();
        RewardFunction taxiRF = new TaxiRewardFunction(numPas, tf);
        
        for(String[] param:params){
            groundedTasks.add(new GroundedTask(this, new SimpleAction(name+":"+param), taxiRF
            		));
        }
    }


    @Override
    public Object parametersSet(State s) {
        return params;
    }

    @Override
    public boolean terminal(State s, Action action) {
    	tf = new TaxiTerminationFunction();
        return this.tf.isTerminal(s);
    }

    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        return groundedTasks;
    }

    @Override
    public double pseudoRewardFunction(State s){
        if(tf.isTerminal(s))
        	return 20;
        return -1;
    }
}