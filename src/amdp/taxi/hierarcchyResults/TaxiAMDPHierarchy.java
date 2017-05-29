package amdp.taxi.hierarcchyResults;

import java.util.ArrayList;
import java.util.List;

import amdp.amdpframework.AMDPModelLearner;
import amdp.amdpframework.GroundedTask;
import amdp.amdpframework.NonPrimitiveTaskNode;
import amdp.amdpframework.TaskNode;
import amdp.taxi.TaxiDomain;
import amdp.taxiamdpdomains.taxiamdp.GetTaskNode;
import amdp.taxiamdpdomains.taxiamdp.MoveTaskNodes;
import amdp.taxiamdpdomains.taxiamdp.NavigateTaskNode;
import amdp.taxiamdpdomains.taxiamdp.PickupL0TaskNode;
import amdp.taxiamdpdomains.taxiamdp.PickupL1TaskNode;
import amdp.taxiamdpdomains.taxiamdp.PutDownL0TaskNode;
import amdp.taxiamdpdomains.taxiamdp.PutDownL1TaskNode;
import amdp.taxiamdpdomains.taxiamdp.PutTaskNode;
import amdp.taxiamdpdomains.taxiamdp.RootTaskNode;
import amdp.taxiamdpdomains.taxiamdp.TaxiAMDPLearner.l0PolicyGenerator;
import amdp.taxiamdpdomains.taxiamdp.TaxiAMDPLearner.l1PolicyGenerator;
import amdp.taxiamdpdomains.taxiamdp.TaxiAMDPLearner.l2PolicyGenerator;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1TerminalFunction;
import amdp.taxiamdpdomains.taxiamdplevel1.taxil1state.L1StateMapper;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2Domain;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2TerminalFunction;
import amdp.taxiamdpdomains.taxiamdplevel2.taxil2state.L2StateMapper;
import burlap.behavior.singleagent.learning.modellearning.models.FactoredTabularModel;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.UniformCostRF;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.simple.SimpleHashableStateFactory;

public class TaxiAMDPHierarchy {

	public static List<AMDPModelLearner> pgList;
	
	public static GroundedTask taxiHierarchy(State startState, OOSADomain td){
        TerminalFunction tfL1 = new TaxiL1TerminalFunction();
        RewardFunction rfL1 = new UniformCostRF();

        TaxiL1Domain tdL1Gen = new TaxiL1Domain(rfL1, tfL1);

        OOSADomain tdL1 = tdL1Gen.generateDomain();

        TerminalFunction tfL2 = new TaxiL2TerminalFunction();
        RewardFunction rfL2 = new UniformCostRF();

        TaxiL2Domain tdL2Gen = new TaxiL2Domain(rfL2, tfL2);


        OOSADomain tdL2 = tdL2Gen.generateDomain();
        
        L1StateMapper testMapper = new L1StateMapper();

        State sL1 = testMapper.mapState(startState);

        L2StateMapper l2StateMapper = new L2StateMapper();

        State sL2 = l2StateMapper.mapState(sL1);

        ActionType east = td.getAction(TaxiDomain.ACTION_EAST);
        ActionType west = td.getAction(TaxiDomain.ACTION_WEST);
        ActionType south = td.getAction(TaxiDomain.ACTION_SOUTH);
        ActionType north = td.getAction(TaxiDomain.ACTION_NORTH);
        ActionType pickup = td.getAction(TaxiDomain.ACTION_PICKUP);
        ActionType dropoff = td.getAction(TaxiDomain.ACTION_DROPOFF);


        ActionType navigate = tdL1.getAction(TaxiL1Domain.ACTION_NAVIGATE);
        ActionType pickupL1 = tdL1.getAction(TaxiL1Domain.ACTION_PICKUPL1);
        ActionType putdownL1 = tdL1.getAction(TaxiL1Domain.ACTION_PUTDOWNL1);

        ActionType get = tdL2.getAction(TaxiL2Domain.ACTION_GET);
        ActionType put = tdL2.getAction(TaxiL2Domain.ACTION_PUT);

        TaskNode et = new MoveTaskNodes(east);
        TaskNode wt = new MoveTaskNodes(west);
        TaskNode st = new MoveTaskNodes(south);
        TaskNode nt = new MoveTaskNodes(north);
        TaskNode pt = new PickupL0TaskNode(pickup);
        TaskNode dt = new PutDownL0TaskNode(dropoff);


        TaskNode[] navigateSubTasks = new TaskNode[]{et,wt,st,nt};
        TaskNode[] dropOffL1SubTasks = new TaskNode[]{dt};
        TaskNode[] pickupL1SubTasks = new TaskNode[]{pt};

        TaskNode navigateTaskNode = new NavigateTaskNode(navigate,tdL1Gen.generateDomain(),td,navigateSubTasks);
        TaskNode putDownL1TaskNode = new PutDownL1TaskNode(putdownL1,tdL1Gen.generateDomain(),td,dropOffL1SubTasks);
        TaskNode pickupL1TaskNode = new PickupL1TaskNode(pickupL1,tdL1Gen.generateDomain(),td,pickupL1SubTasks);

        FactoredTabularModel navModel = new FactoredTabularModel(((NonPrimitiveTaskNode)navigateTaskNode).getDomain(),new SimpleHashableStateFactory(),5);
        FactoredTabularModel putDownL1Model = new FactoredTabularModel(((NonPrimitiveTaskNode)putDownL1TaskNode).getDomain(),new SimpleHashableStateFactory(),5);
        FactoredTabularModel pickUpL1Model = new FactoredTabularModel(((NonPrimitiveTaskNode)pickupL1TaskNode).getDomain(),new SimpleHashableStateFactory(),5);


//        taskNameToModelMap.put(navigateTaskNode.getName(),navModel);
//        taskNameToModelMap.put(putDownL1TaskNode.getName(),putDownL1Model);
//        taskNameToModelMap.put(pickupL1TaskNode.getName(),pickUpL1Model);

        TaskNode[] getSubTasks = new TaskNode[]{navigateTaskNode, pickupL1TaskNode};
        TaskNode[] putSubTasks = new TaskNode[]{navigateTaskNode, putDownL1TaskNode};

        TaskNode getTaskNode = new GetTaskNode(get, tdL2Gen.generateDomain(), tdL1Gen.generateDomain(), getSubTasks);
        TaskNode putTaskNode = new PutTaskNode(put, tdL2Gen.generateDomain(), tdL1Gen.generateDomain(), putSubTasks);


        TaskNode[] rootSubTasks = new TaskNode[]{getTaskNode,putTaskNode};

        TaskNode root = new RootTaskNode("root",rootSubTasks,tdL2, tfL2,rfL2);
        
        pgList = new ArrayList<AMDPModelLearner>();
        FactoredTabularModel sharedModel = new FactoredTabularModel(td,new SimpleHashableStateFactory(),5);
        pgList.add(0,new l0PolicyGenerator(td, sharedModel,5));
        pgList.add(1,new l1PolicyGenerator(tdL1));
        pgList.add(2,new l2PolicyGenerator(tdL2));
        
        return root.getApplicableGroundedTasks(sL2).get(0);
	}
}
