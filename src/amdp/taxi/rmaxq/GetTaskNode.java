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
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.oo.propositional.GroundedProp;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.GoalBasedRF;
import burlap.mdp.singleagent.oo.OOSADomain;

/**
 * Created by ngopalan on 8/14/16.
 */
public class GetTaskNode extends NonPrimitiveTaskNode{

//    domain l1Domain;
	public static final String ACTION_GET = "get";
	
    protected String[] passengers;
    public GetTaskNode(OOSADomain taxiL2Domain, String[] passes,  TaskNode[] children){
        this.domain = taxiL2Domain;
        this.name = ACTION_GET;
        this.taskNodes = children;
        this.passengers = passes;
    }

    @Override
    public Object parametersSet(State s) {
        return passengers;
    }

    @Override
    public boolean terminal(State s, Action action) {

        String passName = ((GetType.GetAction)action).passenger;
        StateConditionTest sc =  new GroundedPropSC(new GroundedProp(domain.propFunction(TaxiDomain.PASSENGERINTAXI), new String[]{passName}));
        return new GoalConditionTF(sc).isTerminal(s);
    }

    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gtList = new ArrayList<GroundedTask>();
        for(String pass : passengers){
        	StateConditionTest sc =  new GroundedPropSC(new GroundedProp
        			(domain.propFunction(TaxiDomain.PASSENGERINTAXI), new String[]{pass}));
            gtList.add(new GroundedTask(this, new GetType.GetAction(pass), new GoalBasedRF(sc, 10)));
        }
        return gtList;
    }
    
    public static class GetType implements ActionType {


        public GetType() {
//            actions = new ArrayList<Action>(locations.size());
//            for(String location : locations){
//                actions.add(new NavigateAction(location));
//            }
        }

        @Override
        public String typeName() {
            return ACTION_GET;
        }

        @Override
        public Action associatedAction(String strRep) {
            return new GetAction(strRep);
        }

        @Override
        public List<Action> allApplicableActions(State s) {
            List<Action> actions = new ArrayList<Action>();
            List<TaxiPassenger> passengers = ((TaxiState) s).passengers;
            for (TaxiPassenger passenger : passengers) {
                actions.add(new GetAction(passenger.name()));

            }
            return actions;
        }

        public static class GetAction implements Action {

            public String passenger;

            public GetAction(String passenger) {
                this.passenger = passenger;
            }

            @Override
            public String actionName() {
                return ACTION_GET + "_" + passenger;
            }

            @Override
            public Action copy() {
                return new GetAction(passenger);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                GetAction that = (GetAction) o;

                return that.passenger.equals(passenger);

            }


            @Override
            public String toString() {
                return this.actionName();
            }
        }


    }
}
