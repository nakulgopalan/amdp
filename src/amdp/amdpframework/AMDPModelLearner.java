package amdp.amdpframework;

import burlap.behavior.policy.Policy;
import burlap.behavior.valuefunction.QProvider;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;

import java.util.List;

/**
 * This is a Policy Generating Interface for AMDPs. The purpose of such policy generators is to
 * generate a policy for a lower level state abstraction in AMDPs given an AMDP action from a higher
 * state abstraction and a lower level state. It also has access to the state mapper allowing generation
 * of abstract states
 * @author ngopalan
 *
 */

public interface AMDPModelLearner extends AMDPPolicyGenerator{


    public void updateModel(State s, Action a, List<Double> rewards, State sPrime, GroundedTask gt);


}
