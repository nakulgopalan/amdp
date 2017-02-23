package amdp.taxi.rmaxq;

import java.util.ArrayList;
import java.util.List;

import amdp.rmaxq.framework.RmaxQLearningAgent;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.TaxiDomain;
import amdp.taxi.TaxiRewardFunction;
import amdp.taxi.TaxiTerminationFunction;
import amdp.taxi.TaxiVisualizer;
import amdp.taxi.state.TaxiLocation;
import amdp.taxi.state.TaxiPassenger;
import amdp.taxi.state.TaxiState;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2Domain;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.auxiliary.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.mdp.core.Domain;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import burlap.visualizer.Visualizer;

public class TaxiRmaxQDriver {
	
	private static SimulatedEnvironment env;
	private static Domain domain;
	
	public static TaskNode setupHeirarcy(){
        TerminalFunction taxiTF = new TaxiTerminationFunction();
        RewardFunction taxiRF = new TaxiRewardFunction(1,taxiTF);

        TaxiDomain TDGen = new TaxiDomain(taxiRF, taxiTF);
        
        TDGen.setTransitionDynamicsLikeFickleTaxiProlem();
        TDGen.setFickleTaxi(true);
        TDGen.setIncludeFuel(false);
        OOSADomain td = TDGen.generateDomain();
        domain = td;
        State s = TaxiDomain.getClassicState(td, false);
        env = new SimulatedEnvironment(td, s);
        
        List<TaxiPassenger> passengers = ((TaxiState)s).passengers;
        List<TaxiLocation> locations = ((TaxiState)s).locations;
        String[] locs = new String[locations.size()], passengerNames = new String[passengers.size()];
        int i = 0;
        for(TaxiPassenger pass : passengers){
        	passengerNames[i] = pass.name();
        	i++;
        }
        i = 0;
        for(TaxiLocation loc : locations){
        	locs[i] = loc.colour;
        	i++;
        }
        
        
        ActionType east = td.getAction(TaxiDomain.ACTION_EAST);
        ActionType west = td.getAction(TaxiDomain.ACTION_WEST);
        ActionType south = td.getAction(TaxiDomain.ACTION_SOUTH);
        ActionType north = td.getAction(TaxiDomain.ACTION_NORTH);
        ActionType pickup = td.getAction(TaxiDomain.ACTION_PICKUP);
        ActionType dropoff = td.getAction(TaxiDomain.ACTION_DROPOFF);
        
        TaskNode te = new MoveTaskNode(east);
        TaskNode tw = new MoveTaskNode(west);
        TaskNode ts = new MoveTaskNode(south);
        TaskNode tn = new MoveTaskNode(north);
        TaskNode tp = new PickupTaskNode(pickup);
        TaskNode tdp = new PutDownTaskNode(dropoff);
        
        TaskNode[] navigateSubTasks = new TaskNode[]{te, tw, ts, tn};
        
        TaskNode navigate = new NavigateTaskNode(locs, navigateSubTasks, td);
        TaskNode[] getNodeSubTasks = new TaskNode[]{tp,navigate};
        TaskNode[] putNodeSubTasks = new TaskNode[]{tdp,navigate};
        
        TaskNode getNode = new GetTaskNode(td, passengerNames, getNodeSubTasks);
        TaskNode putNode = new PutTaskNode(td, passengerNames, putNodeSubTasks);
        
        TaskNode[] rootTasks = new TaskNode[]{getNode, putNode};
        
        TaskNode root = new RootTaskNode("root", td, rootTasks, passengerNames.length );
        
        return root;
	}
	
	
	public static void main(String[] args) {
		TaskNode root = setupHeirarcy();
		HashableStateFactory hs = new SimpleHashableStateFactory();
		
		LearningAgent RmaxQ = new RmaxQLearningAgent(root, hs, 100, 5, 0.001);
 		Episode e = RmaxQ.runLearningEpisode(env);
		List<Episode> episodes = new ArrayList<Episode>();
		episodes.add(e);
		
		Visualizer v = TaxiVisualizer.getVisualizer(5, 5);
		new EpisodeSequenceVisualizer(v, domain, episodes );
	}

}
