package amdp.taxi.rmaxq;

import burlap.mdp.core.action.ActionType; 
import amdp.rmaxq.framework.PrimitiveTaskNode;

/**
 * Created by ngopalan on 8/14/16.
 */
public class PickupTaskNode extends PrimitiveTaskNode{
    public PickupTaskNode(ActionType a){
        this.setActionType(a);
    }
}
