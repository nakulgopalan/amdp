package amdp.taxi.rmaxq;

import amdp.rmaxq.framework.PrimitiveTaskNode; 
import burlap.mdp.core.action.ActionType;

/**
 * Created by ngopalan on 8/14/16.
 */
public class PutDownTaskNode extends PrimitiveTaskNode{

    public PutDownTaskNode(ActionType a){
        this.setActionType(a);
    }

}
