package amdp.taxi.rmaxq;

import amdp.rmaxq.framework.PrimitiveTaskNode;
import burlap.mdp.core.action.ActionType;


public class MoveTaskNode extends PrimitiveTaskNode{

    public MoveTaskNode(ActionType a){
        this.setActionType(a);
    }
    
}
