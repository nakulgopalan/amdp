package amdp.taxi.rmaxq;

import java.util.ArrayList;
import java.util.List;

import amdp.amdpframework.GroundedPropSC;
import amdp.rmaxq.framework.GroundedTask;
import amdp.rmaxq.framework.NonPrimitiveTaskNode;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.TaxiDomain;
import amdp.taxi.state.TaxiPassenger;
import amdp.taxi.state.TaxiState;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2Domain;
import burlap.mdp.auxiliary.common.GoalConditionTF;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.oo.propositional.GroundedProp;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.GoalBasedRF;
import burlap.mdp.singleagent.common.UniformCostRF;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;

/**
 * Created by ngopalan on 8/14/16.
 */
public class GetTaskNode extends NonPrimitiveTaskNode{

//    domain l1Domain;
	public static final String ACTION_GET = "get";
	protected List<GroundedTask> gts;
    protected String[] passengers;
    public GetTaskNode(OOSADomain taxiL2Domain, String[] passes,  TaskNode[] children){
        this.domain = taxiL2Domain;
        this.name = ACTION_GET;
        this.taskNodes = children;
        this.passengers = passes;
        
        RewardFunction rf = new UniformCostRF();
        gts = new ArrayList<GroundedTask>();
        for(String pass : passengers){
        	gts.add(new GroundedTask(this, new SimpleAction(ACTION_GET + "_" + pass), rf));
        }
    }

    @Override
    public Object parametersSet(State s) {
        return passengers;
    }

    @Override
    public boolean terminal(State s, Action action) {
    	 String passName = action.actionName().split("_")[1];
    	 TaxiState st = (TaxiState)s;
    	 int tx = st.taxi.x, ty=st.taxi.y;
    	 for(TaxiPassenger p : st.passengers){
    		 if(p.name().equals(passName)){
    			 if(p.inTaxi && p.x == tx && p.y == ty)
    				 return true;
    		 }
    	 }
    	 return false;
    }

    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gt = new ArrayList<GroundedTask>();
        for(GroundedTask g: gts){
        	if(!terminal(s, g.getAction()))
        		gt.add(g);
        }
        return gt;
    }
}
