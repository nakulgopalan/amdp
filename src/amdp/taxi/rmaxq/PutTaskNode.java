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
public class PutTaskNode extends NonPrimitiveTaskNode{

//    OOSADomain l1Domain;

	public static String ACTION_PUT = "put";
    ActionType putType;
    protected String[] passenders;
    public PutTaskNode(OOSADomain taxiL1Domain, String[] passes, TaskNode[] children){
        this.domain = taxiL1Domain;
        this.name = ACTION_PUT;
        this.taskNodes = children;
        passenders = passes;
    }

    @Override
    public Object parametersSet(State s) {
        List<String[]> params = new ArrayList<String[]>();
        List<Action> gtActions = putType.allApplicableActions(s);
        for(Action a:gtActions){
            params.add(new String[]{a.actionName().split("_")[1],a.actionName().split("_")[2]});
        }
        return null;
    }

    @Override
    public boolean terminal(State s, Action action) {
        String passName = ((PutType.PutAction)action).passenger;
        StateConditionTest sc =  new GroundedPropSC(new GroundedProp(domain.propFunction(TaxiDomain.PASSENGERATGOALLOCATIONPF), new String[]{passName}));
        return new GoalConditionTF(sc).isTerminal(s);
    }


    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gtList = new ArrayList<GroundedTask>();
        for(String pass : passenders){
        	StateConditionTest sc =  new GroundedPropSC(new GroundedProp(domain.propFunction(TaxiDomain.PASSENGERATGOALLOCATIONPF), new String[]{pass}));
            gtList.add(new GroundedTask(this, new PutType.PutAction(pass), new GoalBasedRF(sc, 10)));
        }
        return gtList;
    }
    
    public static class PutType implements ActionType {


        public PutType() {
//            actions = new ArrayList<Action>(locations.size());
//            for(String location : locations){
//                actions.add(new NavigateAction(location));
//            }
        }

        @Override
        public String typeName() {
            return ACTION_PUT;
        }

        @Override
        public Action associatedAction(String strRep) {
//            return new PutAction(strRep.split("_")[0], strRep.split("_")[1]);
            return new PutAction(strRep.split("_")[0]);
        }

        @Override
        public List<Action> allApplicableActions(State s) {
            List<Action> actions = new ArrayList<Action>();
            List<TaxiPassenger> passengers = ((TaxiState) s).passengers;
            List<TaxiLocation> locations = ((TaxiState) s).locations;
            for (TaxiPassenger passenger : passengers) {
                for (TaxiLocation loc : locations) {
                    actions.add(new PutAction(passenger.name()));
                }

            }
            return actions;
        }

        public static class PutAction implements Action {

            public String passenger;
//            public String location;

            public PutAction(String passenger) {
                this.passenger = passenger;
//                this.location = location;
            }

            @Override
            public String actionName() {
//                return ACTION_PUT + "_" + passenger + "_" + location;
                return ACTION_PUT + "_" + passenger;
            }

            @Override
            public Action copy() {
                return new PutAction(passenger);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                PutAction that = (PutAction) o;

                return that.passenger.equals(passenger) ;//&& that.location.equals(location);

            }

            @Override
            public String toString() {
                return this.actionName();
            }
        }
    }

}
