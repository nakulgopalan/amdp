package amdp.taxi.rmaxq;

import java.util.ArrayList;
import java.util.List;

import amdp.amdpframework.GroundedPropSC;
import amdp.rmaxq.framework.GroundedTask;
import amdp.rmaxq.framework.NonPrimitiveTaskNode;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.TaxiDomain;
import amdp.taxi.state.TaxiLocation;
import amdp.taxi.state.TaxiPassenger;
import amdp.taxi.state.TaxiState;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import amdp.taxiamdpdomains.taxiamdplevel1.taxil1state.TaxiL1Location;
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
public class PutTaskNode extends NonPrimitiveTaskNode{

//    OOSADomain l1Domain;

	public static String ACTION_PUT = "put";
    ActionType putType;
    protected String[] passenders, locations;
    protected List<GroundedTask> gts;
    public PutTaskNode(OOSADomain taxiL1Domain, String[] passes, String[] locs, TaskNode[] children){
        this.domain = taxiL1Domain;
        this.name = ACTION_PUT;
        this.taskNodes = children;
        this.passenders = passes;
        this.locations = locs;
        
        gts = new ArrayList<GroundedTask>();
        RewardFunction urf = new UniformCostRF();
        for(String pass : passenders){
        	for(String loc : locations){
        		gts.add(new GroundedTask(this, new SimpleAction(ACTION_PUT + "_" + pass +"_" + loc), urf));
        	}
        }
    }

    @Override
    public Object parametersSet(State s) {
        List<String[]> params = new ArrayList<String[]>();
        for(String loc: locations){
        	for(String pass: passenders){
        		params.add(new String[]{loc,pass});
        		}
        }
        return null;
    }

    @Override
    public boolean terminal(State s, Action action) {
        String[] act = action.actionName().split("_");
        String passName = act[1], goalName = act[2];
        TaxiState st = (TaxiState)s;
        for(TaxiPassenger p : st.passengers){
        	for(TaxiLocation l : st.locations){
        		if(p.name().equals(passName) && l.name().equals(goalName)){
        			if(p.x == l.x && p.y == l.y){
        				return true;
        			}
        		}
        	}
        }
        return false;
    }


    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gtList = new ArrayList<GroundedTask>();
        TaxiState st = (TaxiState)s;
        for(GroundedTask gt: gts){
        	String pass = gt.actionName().split("_")[1];
        	TaxiPassenger p = st.passengers.get(st.passengerInd(pass));
        	if(p.inTaxi && !terminal(s, gt.getAction())){
        		gtList.add(gt);
        	}
        }
        return gtList;
    }
    
}
