package amdp.rmaxq.framework;

import java.util.List;
import java.util.Map;

import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

public class QProviderRmaxQ implements QProvider{

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
}
