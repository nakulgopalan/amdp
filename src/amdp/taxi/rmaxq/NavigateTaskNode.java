package amdp.taxi.rmaxq;

import java.util.ArrayList;
import java.util.List;

import amdp.rmaxq.framework.GroundedTask;
import amdp.rmaxq.framework.NonPrimitiveTaskNode;
import amdp.rmaxq.framework.TaskNode;
import amdp.taxi.state.TaxiLocation;
import amdp.taxi.state.TaxiState;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.SimpleAction;
import burlap.mdp.core.state.State;

/**
 * Created by ngopalan on 8/14/16.
 */
public class NavigateTaskNode extends NonPrimitiveTaskNode{


//    OOSADomain l0Domain;

	public static final String ACTION_NAVIGATE = "navigate";
    protected String[] locations;
    protected List<GroundedTask> gts;
    public NavigateTaskNode(String name, String[] locs, TaskNode[] children){
        this.name = ACTION_NAVIGATE;
        locations = locs;
    	this.taskNodes = children;
    	
    	gts = new ArrayList<GroundedTask>();
    	for( String loc : locations){
        	gts.add(new GroundedTask(this, new SimpleAction(ACTION_NAVIGATE + "_" + loc)));
        }
    }

    @Override
    public Object parametersSet(State s) {
        return locations;
    }


    @Override
    public boolean terminal(State s, Action action) {
        String goal = action.actionName().split("_")[1];
        TaxiState st = (TaxiState) s;
        int tx = st.taxi.x, ty = st.taxi.y;
        for(TaxiLocation l : st.locations){
        	if(l.name().equals(goal)){
        		return tx == l.x && ty == l.y;
        	}
        }
        return false;
    }

    @Override
    public List<GroundedTask> getApplicableGroundedTasks(State s) {
        List<GroundedTask> gtList = new ArrayList<GroundedTask>();
        for(GroundedTask gt : gts){
        	if(!terminal(s, gt.getAction()))
        		gtList.add(gt);
        }
        
        return gtList;
    }
}
