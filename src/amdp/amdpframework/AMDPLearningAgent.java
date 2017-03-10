package amdp.amdpframework;

import amdp.tools.StackObserver;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.debugtools.DPrint;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.oo.ObjectParameterizedAction;
import burlap.mdp.core.oo.state.generic.GenericOOState;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.mdp.singleagent.oo.OOSADomain;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AMDPLearningAgent implements LearningAgent{

    //TODO: create a high level environment for a learning agent made up of high level states entirely, this would allow swapping out learners at each level!


    List<AMDPModelLearner> ModelLearners;
    Map<String, GroundedTask> actionToGroundedTaskMap = new HashMap<String, GroundedTask>();
    Map<String, OOSADomain> actionToDomainMap = new HashMap<String, OOSADomain>();

    // This is a stack of states storing states at each level.
    List<State> StateStack = new ArrayList<State>();
    List<Episode> EpisodeStack = new ArrayList<Episode>();

    GroundedTask rootGroundedTask;

    protected int debugCode = 23987345;

    protected int stepCount = 0;

    protected int maxLevel;

    protected List<List<Action>> policyStack;

    protected StackObserver onlineStackObserver;




    public AMDPLearningAgent(GroundedTask rootGroundedTask, List<AMDPModelLearner> inputModelLearners){

        this.rootGroundedTask = rootGroundedTask;

        this.actionToGroundedTaskMap.put(rootGroundedTask.action.actionName(), rootGroundedTask);
        this.actionToDomainMap.put(rootGroundedTask.action.actionName(),rootGroundedTask.groundedDomain());



//		if(inputDomainList.size()!=inputPolicyGenerators.size()){
//			System.err.print("The number of domains ("+ inputDomainList + "), is not equal to the number of policy generators(" + inputPolicyGenerators+")");
//			System.exit(-10);
//		}


        this.ModelLearners = inputModelLearners;

        for (int i = 0; i < this.ModelLearners.size(); i++) {
            StateStack.add(new GenericOOState());
        }
        this.maxLevel = this.ModelLearners.size()-1;

        this.policyStack = new ArrayList<List<Action>>(ModelLearners.size());
        for(int i = 0; i < ModelLearners.size(); i++){
            this.policyStack.add(new ArrayList<Action>());
        }
    }

    public List<List<Action>> getPolicyStack() {
        return policyStack;
    }

    public StackObserver getOnlineStackObserver() {
        return onlineStackObserver;
    }

    public void setOnlineStackObserver(StackObserver onlineStackObserver) {
        this.onlineStackObserver = onlineStackObserver;
    }




    @Override
    public Episode runLearningEpisode(Environment environment) {
        return this.runLearningEpisode(environment, -1);
    }

    @Override
    public Episode runLearningEpisode(Environment env, int maxSteps) {
        stepCount = 0;

        State baseState = env.currentObservation();

        StateStack.set(0, baseState);



        Episode ea = new Episode(baseState);

        for(int i = 1; i< ModelLearners.size(); i++){
            AMDPPolicyGenerator p = this.ModelLearners.get(i);
            baseState = p.generateAbstractState(baseState);
            StateStack.set(i, baseState);
        }


        //TODO: need a root task to start decomposing!
        decompose(env, ModelLearners.size()-1, rootGroundedTask, maxSteps, ea);


        return ea;
    }



    protected List<Double> decompose(Environment env, int level, GroundedTask gt, int maxSteps, Episode ea){
        State s = StateStack.get(level);
        List<Double> rewardList  = new ArrayList<>();




        if(level !=0){


            while(!gt.terminalFunction().isTerminal(s) && (stepCount < maxSteps || maxSteps == -1)){
                Policy pi = ModelLearners.get(level).generatePolicy(s, gt);
                State startState = s.copy();
                TaskNode[] childTaskNodes = ((NonPrimitiveTaskNode)gt.t).childTaskNodes;
//					List<GroundedTask> childGroundedTaskList = gt.t.getApplicableGroundedTasks(s);

                addTasksToMap(childTaskNodes, s, level);
                Action a = pi.action(s);
                //TODO: get child grounded task
                String str = StringUtils.repeat("	", maxLevel - level);
                str = str + a.toString();
                DPrint.cl(debugCode , str);
                this.policyStack.get(level).add(a);
                if(this.onlineStackObserver != null){
                    this.onlineStackObserver.updatePolicyStack(this.policyStack);
                }
                String tempStr ="";
                if(a instanceof ObjectParameterizedAction){
                    String[] params = ((ObjectParameterizedAction)a).getObjectParameters();
                    if(params!=null) {
                        for (int i = 0; i < params.length; i++) {
                            tempStr = tempStr + "_" + params[i];
                        }
                    }
                }
                else{
                    tempStr+="_"+a.hashCode();
                }


                List<Double> tempRewards = decompose(env, level - 1, actionToGroundedTaskMap.get(a.actionName() + tempStr + "_" + level), maxSteps, ea);
                rewardList.addAll(tempRewards);
                s = StateStack.get(level);
                ModelLearners.get(level).updateModel(startState, a, rewardList, s, gt);

            }
        }
        else{
            while((!env.isInTerminalState() && !gt.terminalFunction().isTerminal(s) )&& (stepCount < maxSteps || maxSteps == -1)){
                // this is a grounded action at the base level
                Policy pi = ModelLearners.get(level).generatePolicy(s, gt);
                Action ga = pi.action(s);
                this.policyStack.get(level).add(ga);
                if(this.onlineStackObserver != null){
                    this.onlineStackObserver.updatePolicyStack(this.policyStack);
                }

                EnvironmentOutcome eo = env.executeAction(ga);

                String str = StringUtils.repeat("	", maxLevel - level);
                str = str + ga.toString();
                DPrint.cl(debugCode , str);
                ea.transition(eo);
//				ea.recordTransitionTo(ga, eo.op, eo.r);
                StateStack.set(level, eo.op);
                s = eo.op;
                stepCount++;
                rewardList.add(eo.r);
                List<Double> tempRewardList = new ArrayList<>();
                tempRewardList.add(eo.r);
                ModelLearners.get(level).updateModel(eo.o, ga,tempRewardList, eo.op, gt);
            }



        }

        if(level < ModelLearners.size() -1){
            // project state up and getting new next state after running a policy to termination
            State projectState = ModelLearners.get(level+1).generateAbstractState(StateStack.get(level));
            String str = StringUtils.repeat("	", maxLevel - level);
            str = str + level + projectState.toString();
            DPrint.cl(debugCode , str);
            str = level + StateStack.get(level).toString();
            DPrint.cl(debugCode , str);

            StateStack.set(level+1, projectState);
        }

        this.policyStack.get(level).clear();

        //TODO: return things
        return rewardList;
    }

    private void addTasksToMap(TaskNode[] childTaskNodes, State s, int level) {
        List<GroundedTask> childGroundedTaskList = new ArrayList<GroundedTask>();
        for(int i=0;i<childTaskNodes.length;i++){
            TaskNode t = childTaskNodes[i];
            childGroundedTaskList.addAll(t.getApplicableGroundedTasks(s));
        }
        for(GroundedTask gt:childGroundedTaskList){
            Action a =gt.action;
            String tempStr ="";
            if(a instanceof ObjectParameterizedAction){
                String[] params = ((ObjectParameterizedAction)a).getObjectParameters();
                if(params!=null) {
                    for (int i = 0; i < params.length; i++) {
                        tempStr = tempStr + "_" + params[i];
                    }
                }
            }
            else{
                tempStr+="_"+a.hashCode();
            }
            if(!actionToGroundedTaskMap.containsKey(a.actionName() + tempStr + "_" + level)){
                actionToGroundedTaskMap.put(a.actionName() + tempStr + "_" + level, gt);
            }
        }
    }



}
