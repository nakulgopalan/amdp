package amdp.taxi.rmaxq;

import java.util.ArrayList;
import java.util.List;

import amdp.amdpframework.GroundedPropSC;
import amdp.rmaxq.framework.GroundedTask;
import amdp.rmaxq.framework.NonPrimitiveTaskNode;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.TaxiDomain;
import amdp.taxi.state.TaxiLocation;
import amdp.taxi.state.TaxiState;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import burlap.mdp.auxiliary.common.GoalConditionTF;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.oo.propositional.GroundedProp;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.GoalBasedRF;
import burlap.mdp.singleagent.oo.OOSADomain;

/**
 * Created by ngopalan on 8/14/16.
 */
public class NavigateTaskNode extends NonPrimitiveTaskNode{


//    OOSADomain l0Domain;

	public static final String ACTION_NAVIGATE = "navigate";
    protected List<GroundedTask> gtasks;
    protected String[] locations;
    
    public NavigateTaskNode(String[] locs, TaskNode[] children, OOSADomain dom){
        gtasks = new ArrayList<GroundedTask>();
        name = ACTION_NAVIGATE;
        this.domain = dom;
        locations = locs;
    	this.taskNodes = children;
    }

    @Override
    public Object parametersSet(State s) {
        return locations;
    }


    @Override
    public boolean terminal(State s, Action action) {
        String location = ((NavigateType.NavigateAction)action).location;
        StateConditionTest sc =  new GroundedPropSC(new GroundedProp(domain.propFunction(TaxiDomain.TAXIATLOCATIONPF), new String[]{location}));
        return new GoalConditionTF(sc).isTerminal(s);
    }

    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gtList = new ArrayList<GroundedTask>();
        
        for( String loc : locations){
        	StateConditionTest st = new GroundedPropSC
        			(new GroundedProp(domain.propFunction(TaxiDomain.TAXIATLOCATIONPF), new String[]{loc}));
        	
            gtList.add(new GroundedTask(this, new NavigateType.NavigateAction(loc), new GoalBasedRF(st, 10) ));
        }
        return gtList;
    }
    
    public static class NavigateType implements ActionType {


        public NavigateType() {
//            actions = new ArrayList<Action>(locations.size());
//            for(String location : locations){
//                actions.add(new NavigateAction(location));
//            }
        }

        @Override
        public String typeName() {
            return ACTION_NAVIGATE;
        }

        @Override
        public Action associatedAction(String strRep) {
            return new NavigateAction(strRep);
        }

        @Override
        public List<Action> allApplicableActions(State s) {
            List<Action> actions = new ArrayList<Action>();
            List<TaxiLocation> locations = ((TaxiState)s).locations;
            for(TaxiLocation location: locations){
                actions.add(new NavigateAction(location.colour));
            }
            return actions;
        }

        public static class NavigateAction implements Action{

            public String location;

            public NavigateAction(String location) {
                this.location= location;
            }

            @Override
            public String actionName() {
                return ACTION_NAVIGATE + "_" + location;
            }

            @Override
            public Action copy() {
                return new NavigateAction(location);
            }

            @Override
            public boolean equals(Object o) {
                if(this == o) return true;
                if(o == null || getClass() != o.getClass()) return false;

                NavigateAction that = (NavigateAction) o;

                return that.location.equals(location) ;

            }

            @Override
            public int hashCode() {
                String str = ACTION_NAVIGATE + "_" + location;
                return str.hashCode();
            }

            @Override
            public String toString() {
                return this.actionName();
            }
        }
    }
}
