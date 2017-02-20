package amdp.rmaxq.framework;

import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.MDPSolverInterface;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.Domain;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.model.SampleModel;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

public class QProviderRmaxQ implements QProvider, MDPSolverInterface{

	private Map<HashableState, QValue> qvals;
	private HashableStateFactory hashingFactory;
	
	public QProviderRmaxQ(HashableStateFactory hsf){
		this.hashingFactory = hsf;
	}
	
	public double qValue(State s, Action a) {

	}

	public double value(State s) {

	}

	public List<QValue> qValues(State s) {

	}
	
	public void solverInit(SADomain domain, double gamma, HashableStateFactory hashingFactory) {
		
	}

	public void resetSolver() {
		
	}

	public void setDomain(SADomain domain) {
		
	}

	public void setModel(SampleModel model) {
		
	}

	public SampleModel getModel() {
		return null;
	}

	public Domain getDomain() {
		return null;
	}

	public void addActionType(ActionType a) {
	
	}

	public void setActionTypes(List<ActionType> actionTypes) {
		
	}

	public List<ActionType> getActionTypes() {
		return null;
	}

	public void setHashingFactory(HashableStateFactory hashingFactory) {
		
	}

	public HashableStateFactory getHashingFactory() {
		return null;
	}

	public double getGamma() {
		return 0;
	}

	public void setGamma(double gamma) {
		
	}

	public void setDebugCode(int code) {
		
	}

	public int getDebugCode() {
		return 0;
	}

	public void toggleDebugPrinting(boolean toggle) {
		
	}
}
