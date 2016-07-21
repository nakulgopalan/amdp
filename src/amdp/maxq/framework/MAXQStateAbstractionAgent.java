package amdp.maxq.framework;

import burlap.behavior.policy.GreedyDeterministicQPolicy;
import burlap.behavior.policy.SolverDerivedPolicy;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.valuefunction.QValue;
import burlap.debugtools.RandomFactory;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;


/**
 * Created by ngopalan on 5/6/16.
 */
public class MAXQStateAbstractionAgent implements LearningAgent {

    // Map for values
    Map<GroundedTask, HashMap<HashableState, Double>> qValue = new HashMap<GroundedTask, HashMap<HashableState, Double>>();
    //map for completion function
    Map<GroundedTask ,HashMap<GroundedTask,HashMap<HashableState, Double>>> C = new HashMap<GroundedTask ,HashMap<GroundedTask,HashMap<HashableState, Double>>>();

    //cTildeMap
    Map<GroundedTask ,HashMap<GroundedTask,HashMap<HashableState, Double>>> CTilde = new HashMap<GroundedTask ,HashMap<GroundedTask,HashMap<HashableState, Double>>>();

    //map for storing the QProviders
    HashMap<String, QProviderForMAXQ> qProviderMap = new HashMap<String, QProviderForMAXQ>();

    //map for the policy generators for each learner
    HashMap<String, SolverDerivedPolicy> policyMap = new HashMap<String, SolverDerivedPolicy>();

    //map for the action names to grounded tasks!
    HashMap<String, GroundedTask> ActionGroundedTaskMap = new HashMap<String, GroundedTask>();

    //    protected double					epsilon;
    protected Random rand;
    protected double gamma = 0.9;
    protected double VMax = 0.;
    protected double learningRate = 0.1;
    protected int learningEpisodeCount = 0;

    int steps = 0;
    int maxSteps = -1;
    boolean stepsDone = false;
    boolean freezeLearning = false;

    //    List<ArrayList<TaskNode>> taskNodesInLevels = new ArrayList<>();
    TaskNode root;
    HashableStateFactory hsf;
    List<GroundedTask> taskNodesStack = new ArrayList<GroundedTask>();
    int highestCompletedTaskNode;


    /**
     * MAXQ learning agent
     * @param root: the root tasknode
     * @param hashingFactory: a hashing factory to store states
     * @param gamma: discount factor for the Qvalues
     * @param learningRate: learning rate for MAXQ
    //     * @param eps
     */
    public MAXQStateAbstractionAgent(TaskNode root, HashableStateFactory hashingFactory, double gamma, double learningRate){
//        this.epsilon = eps;
        rand = RandomFactory.getMapped(0);
        this.gamma = gamma;
        this.learningRate = learningRate;
        this.root = root;
//        this.taskNodesInLevels = taskNodesInLevels;
        this.hsf = hashingFactory;
    }

    public void setRmax(double Rmax){
        this.VMax = Rmax/(1-gamma);
    }

//    public double getEpsilon() {
//        return epsilon;
//    }

    public double getGamma() {
        return gamma;
    }

    public double getVMax() {
        return VMax;
    }

    public double getLearningRate() {
        return learningRate;
    }

//    public void setEpsilon(double epsilon) {
//        this.epsilon = epsilon;
//    }

    public double getC(GroundedTask parentTask, GroundedTask childTask, State s){
        return C.get(parentTask).get(childTask).get(this.hsf.hashState(s));
    }

    public void setFreezeLearning(boolean freezeLearning) {
        this.freezeLearning = freezeLearning;
    }

    public boolean isFreezeLearning() {
        return freezeLearning;
    }

    public double getVValue(GroundedTask childTask, State s){
        return evaluateMaxNode(childTask,s).getRight();
    }


    /**
     * This method provides a QProvider for each task node that can be queried for the qValues of grounded
     * task nodes under it.
     */
    public void setQProviderForTaskNode(TaskNode t){

        QProviderForMAXQ q =  new QProviderForMAXQ(){


            @Override
            public double value(State s) {
                return Helper.maxQ(this, s);
            }

            @Override
            public double qValue(State s, Action a) {
                // the qValues being returned are those from CTilde and not C, evaluate will return true estimates of C itself
                //TODO: we might be wrong here and this might be a C value

                HashableState hs = hsf.hashState(s);
                // parent grounded task exists with the QProvider
                NonPrimitiveTaskNode currentTaskNode = (NonPrimitiveTaskNode) parentGroundedTask.t;
                TaskNode[] children = currentTaskNode.getChildren();
                for(TaskNode child:children){
                    List<GroundedTask> tempGroundedTaskList = child.getApplicableGroundedTasks(s);
                    for(GroundedTask gt: tempGroundedTaskList){
                        if(gt.action.equals(a)){
                            if (!CTilde.containsKey(parentGroundedTask)) {
                                CTilde.put(parentGroundedTask, new HashMap<GroundedTask, HashMap<HashableState, Double>>());
                            }
                            if (!CTilde.get(parentGroundedTask).containsKey(gt)) {
                                CTilde.get(parentGroundedTask).put(gt, new HashMap<HashableState, Double>());
                            }
                            if (!CTilde.get(parentGroundedTask).get(gt).containsKey(hs)) {
                                CTilde.get(parentGroundedTask).get(gt).put(hs, VMax);
                            }
                            // using the C value from CTildeValue change if needed
//                            double q = evaluateMaxNode(gt, s) + cValue.get(parentGroundedTask).get(gt).get(hs);

                            double q = evaluateMaxNode(gt, s).getRight() + CTilde.get(parentGroundedTask).get(gt).get(hs);
                            return q;
                        }
                    }
                }
                System.err.println("The action does not exist in the list of child tasks!");
                return 0;
            }

            @Override
            public List<QValue> qValues(State s) {
                List<QValue> qValueList = new ArrayList<QValue>();
                NonPrimitiveTaskNode currentTaskNode = (NonPrimitiveTaskNode) parentGroundedTask.t;
                TaskNode[] children = currentTaskNode.getChildren();
                List<GroundedTask> allChildredGroundedTasks = new ArrayList<GroundedTask>();
                for(TaskNode child:children){
                    allChildredGroundedTasks.addAll(child.getApplicableGroundedTasks(s));
                }
//                HashableState hs = hsf.hashState(s);

                for(GroundedTask gt : allChildredGroundedTasks) {
                    double q = this.qValue(s,gt.action);
                    //evaluateMaxNode(gt, s).getRight() + CTilde.get(parentGroundedTask).get(gt).get(hs);
                    qValueList.add(new QValue(s, gt.action,q));

                }
                return qValueList;
            }
        };
        this.qProviderMap.put(t.getName(),q);
    }


    /**
     * This method provides a QProvider for each task node that can be queried for the qValues of grounded
     * task nodes under it.
     */
    public void setSolverDerivedPolicyForTaskNode(TaskNode t, SolverDerivedPolicy policy) {
        this.policyMap.put(t.getName(),policy);
    }



    /**
     * This is an approximation of a GLIE policy (epsilon greedy here) as mentioned in MAXQ
     * @param currentGroundedTask: the chosen grounded task
     * @param s: current state
     * @return
     */
//    public GroundedTask getGLIESubtask(GroundedTask currentGroundedTask, State s){
//        // considering this task node has children it is a non primitive task node
//        NonPrimitiveTaskNode currentTaskNode = (NonPrimitiveTaskNode) currentGroundedTask.t;
//        TaskNode[] children = currentTaskNode.getChildren();
//        List<GroundedTask> allChildredGroundedTasks = new ArrayList<GroundedTask>();
//        for(TaskNode child:children){
//            allChildredGroundedTasks.addAll(child.getApplicableGroundedTasks(s));
//        }
//
//
//        double roll = rand.nextDouble();
//        if(!freezeLearning) {
//            if (roll <= epsilon) {
//                int selected = rand.nextInt(allChildredGroundedTasks.size());
//                GroundedTask gt = allChildredGroundedTasks.get(selected);
//                return gt;
//            }
//        }
////        Map<GroundedTask, Double> qValues = new HashMap<>();
//
//        List<GroundedTask> maxTasks = new ArrayList<GroundedTask>();
//
//        HashableState hs = this.hsf.hashState(s);
//
//        Double maxQ = Double.NEGATIVE_INFINITY;
//        for(GroundedTask gt : allChildredGroundedTasks){
////            double q = getValue(currentGroundedTask, gt, s);
//            if(!cValue.containsKey(currentGroundedTask)){
//                cValue.put(currentGroundedTask,new HashMap<GroundedTask, HashMap<HashableState, Double>>());
//            }
//            if(!cValue.get(currentGroundedTask).containsKey(gt)){
//                cValue.get(currentGroundedTask).put(gt,new HashMap<HashableState, Double>());
//            }
//            if(!cValue.get(currentGroundedTask).get(gt).containsKey(hs)){
//                cValue.get(currentGroundedTask).get(gt).put(hs,VMax);
//            }
//            double q = evaluateMaxNode(gt,s) + cValue.get(currentGroundedTask).get(gt).get(hs);
//            if(maxQ==q){
//                maxTasks.add(gt);
//            }
//            if(maxQ<q){
//                maxTasks.clear();
//                maxTasks.add(gt);
//                maxQ =q;
//            }
//        }
//
//        int selected = rand.nextInt(maxTasks.size());
//        return maxTasks.get(selected);
//    }


    /**
     *This gives the value for a subtask given its parent and current state
     * @param parentTask : parent to the current task
     * @param s : state
     * @return
     */
    public Pair<Action, Double> evaluateMaxNode(GroundedTask parentTask, State s){
        // this task can't be a primitive task, if it is then value should be returned.
        //TODO: need to add pseudo reward function check here??? dunno what this todo is all about
        if(parentTask.t.isTaskPrimitive()){
            HashableState hs = this.hsf.hashState(s);
            // if primitive then value for this node exists!
            if(!qValue.containsKey(parentTask)){
                qValue.put(parentTask,new HashMap<HashableState, Double>());
                qValue.get(parentTask).put(hs,VMax);
            }
            if(!qValue.get(parentTask).containsKey(hs)){
                qValue.get(parentTask).put(hs,VMax);
            }
            return new MutablePair(parentTask.action,qValue.get(parentTask).get(hs));
        }
        // in a non-primitive task list grounded tasks
        List<GroundedTask> maxTasks = new ArrayList<GroundedTask>();
        List<Double> maxValues = new ArrayList<Double>();
        List<GroundedTask> allChildrenGroundedTasks = new ArrayList<GroundedTask>();
        TaskNode[] children = ((NonPrimitiveTaskNode)parentTask.t).getChildren();
        for(TaskNode child:children){
            allChildrenGroundedTasks.addAll(child.getApplicableGroundedTasks(s));
        }
        for(GroundedTask gt : allChildrenGroundedTasks){
            if(!this.ActionGroundedTaskMap.containsKey(gt.action.actionName())) {
                this.ActionGroundedTaskMap.put(gt.action.actionName(), gt);
            }
        }

        Double maxQ = Double.NEGATIVE_INFINITY;
        HashableState hs = this.hsf.hashState(s);
        for(GroundedTask gt : allChildrenGroundedTasks){
//            double q = getValue(parentTask, gt, s) + evaluateMaxNode(gt,s);
            //TODO: make a method for returns here
            if(!C.containsKey(parentTask)){
                C.put(parentTask,new HashMap<GroundedTask, HashMap<HashableState, Double>>());
            }
            if(!C.get(parentTask).containsKey(gt)){
                C.get(parentTask).put(gt,new HashMap<HashableState, Double>());
            }
            if(!C.get(parentTask).get(gt).containsKey(hs)){
                C.get(parentTask).get(gt).put(hs,VMax);
            }
            if(!CTilde.containsKey(parentTask)){
                CTilde.put(parentTask,new HashMap<GroundedTask, HashMap<HashableState, Double>>());
            }
            if(!CTilde.get(parentTask).containsKey(gt)){
                CTilde.get(parentTask).put(gt,new HashMap<HashableState, Double>());
            }
            if(!CTilde.get(parentTask).get(gt).containsKey(hs)){
                CTilde.get(parentTask).get(gt).put(hs,VMax);
            }
            double subtreeEvaluation  = evaluateMaxNode(gt,s).getRight();
            double explorationQ = CTilde.get(parentTask).get(gt).get(hs) + subtreeEvaluation;
            double returnedQ = C.get(parentTask).get(gt).get(hs) + subtreeEvaluation;
            if(maxQ==explorationQ){
                maxTasks.add(gt);
                maxValues.add(returnedQ);
            }
            if(maxQ<explorationQ){
                maxTasks.clear();
                maxTasks.add(gt);
                maxValues.clear();
                maxValues.add(returnedQ);
                maxQ = explorationQ;
            }
        }
        // return the true value to the outside subtree of the chosen explored action!
        double maxReturnQ =Double.NEGATIVE_INFINITY;
        int maxValueIndex = -1;
        for(int i =0;i< maxValues.size();i++){
            if(maxValues.get(i) > maxReturnQ){
                maxValueIndex = i;
            }
        }
        return new MutablePair<Action, Double>(maxTasks.get(maxValueIndex).action,maxValues.get(maxValueIndex));
    }

//    /**
//     *This gives the value for a subtask given its parent and current state
//     * @param parentTask : parent to the current task
//     * @param childTask : current task
//     * @param s : state
//     * @return
//     */
//    public double getValue(GroundedTask parentTask, GroundedTask childTask, State s){
//        HashableState hs = this.hsf.hashState(s);
//        if(childTask.t.isTaskPrimitive()){
//            // if primitive then value for this node exists!
//            childTask.hashCode();
//            if(!qValue.containsKey(childTask)){
//                qValue.put(childTask,new HashMap<HashableState, Double>());
//                qValue.get(childTask).put(hs,VMax);
//            }
//            if(!qValue.get(childTask).containsKey(hs)){
//                qValue.get(childTask).put(hs,VMax);
//            }
//            return qValue.get(childTask).get(hs);
//        }
//        else{
//            List<GroundedTask> maxTasks = new ArrayList<>();
//            List<GroundedTask> allChildredGroundedTasks = new ArrayList<>();
//            TaskNode[] children = ((NonPrimitiveTaskNode)childTask.t).getChildren();
//            for(TaskNode child:children){
//                allChildredGroundedTasks.addAll(child.getGroundedTasks(s));
//            }
//
//            Double maxQ = Double.NEGATIVE_INFINITY;
//            for(GroundedTask gt : allChildredGroundedTasks){
//                double q = getValue(childTask, gt, s);
//                if(maxQ==q){
//                    maxTasks.add(gt);
//                }
//                if(maxQ<q){
//                    maxTasks.clear();
//                    maxTasks.add(gt);
//                    maxQ =q;
//                }
//            }
//            // to check if things are initialized else add initializations for all states as seen
//            if(!cValue.containsKey(parentTask)){
//                cValue.put(parentTask,new HashMap<GroundedTask, HashMap<HashableState, Double>>());
//            }
//            if(!cValue.get(parentTask).containsKey(childTask)){
//                cValue.get(parentTask).put(childTask,new HashMap<HashableState, Double>());
//            }
//            if(!cValue.get(parentTask).get(childTask).containsKey(hs)){
//                cValue.get(parentTask).get(childTask).put(hs,VMax);
//            }
//            return maxQ + cValue.get(parentTask).get(childTask).get(hs);
//        }
//    }

    /**
     * this is the MAXQ-Q function from the paper
     * @param parentGroundedTask
     * @param env
     * @param ea
     * @param level
     * @return List of observed states
     */
    public List<State> MAXQ_Q(GroundedTask parentGroundedTask, Environment env, Episode ea, int level){
        // maintaining a stack
        if(this.taskNodesStack.size()<=level){
            this.taskNodesStack.add(parentGroundedTask);
        }
        else{
            this.taskNodesStack.set(level, parentGroundedTask);
        }

        // if not primitive get all possible children grounded tasks for current state
        if(!parentGroundedTask.t.isTaskPrimitive()){
            //here we fill up the grounded tasks to the action map
            TaskNode[] children = ((NonPrimitiveTaskNode)parentGroundedTask.t).getChildren();
            List<GroundedTask> allChildrenGroundedTasks = new ArrayList<GroundedTask>();
            for(TaskNode child:children){
                allChildrenGroundedTasks.addAll(child.getApplicableGroundedTasks(env.currentObservation()));
            }
            for(GroundedTask gt : allChildrenGroundedTasks){
                if(!this.ActionGroundedTaskMap.containsKey(gt.action.actionName())) {
                    this.ActionGroundedTaskMap.put(gt.action.actionName(), gt);
                }
            }
        }

        if(parentGroundedTask.t.isTaskPrimitive()){
            if(steps > maxSteps && maxSteps != -1){
                //if max steps have been taken return empty array!
                this.stepsDone = true;
                return new ArrayList<State>();
            }
            Action ga = parentGroundedTask.getAction();
            EnvironmentOutcome eo = env.executeAction(ga);
            steps++;


            HashableState hs = this.hsf.hashState(eo.o);

            if(!freezeLearning) {
                if (!qValue.containsKey(parentGroundedTask)) {
                    qValue.put(parentGroundedTask, new HashMap<HashableState, Double>());
                }
                if (!qValue.get(parentGroundedTask).containsKey(hs)) {
                    qValue.get(parentGroundedTask).put(hs, this.VMax);
                }
                double value = (1 - learningRate) * qValue.get(parentGroundedTask).get(hs)
                        + learningRate * eo.r;
                qValue.get(parentGroundedTask).put(hs, value);
            }

            //record result
            ea.transition(eo);

            this.highestCompletedTaskNode = this.taskNodesStack.size();
            // to see if a higher task node has been completed
            for(int i=0;i<this.taskNodesStack.size();i++){
                GroundedTask tempTask = this.taskNodesStack.get(i);
                if(tempTask.t.terminal(eo.op,tempTask.action)){
                    this.highestCompletedTaskNode = i;
                    break;
                }
            }
            List<State> returnStateList = new ArrayList<State>();
            returnStateList.add(eo.o.copy());
            return returnStateList;
        }
        else{
            // return state list is a linked list as the sequence will be prepended to it
            List<State> returnStateList = new ArrayList<State>();

            while(!parentGroundedTask.t.terminal(env.currentObservation(),parentGroundedTask.action)){
                State startState =env.currentObservation().copy();
                HashableState hashedStartState  = this.hsf.hashState(startState);
                //get policy and get task from policy!
                // first set Qprovider
                QProviderForMAXQ QP = this.qProviderMap.get(parentGroundedTask.getT().getName());
                QP.setGt(parentGroundedTask);
                SolverDerivedPolicy p;
                if(!freezeLearning){
                    p = policyMap.get(parentGroundedTask.t.getName());
                }
                else {
                    // this happens when testing!
                    p = new GreedyDeterministicQPolicy();
                }
                p.setSolver(QP);

                Action ga = p.action(env.currentObservation());

                if(!this.ActionGroundedTaskMap.containsKey(ga.actionName())){
                    if(!parentGroundedTask.t.isTaskPrimitive()){
                        //here we fill up the grounded tasks
                        TaskNode[] children = ((NonPrimitiveTaskNode)parentGroundedTask.t).getChildren();
                        List<GroundedTask> allChildredGroundedTasks = new ArrayList<GroundedTask>();
                        for(TaskNode child:children){
                            allChildredGroundedTasks.addAll(child.getApplicableGroundedTasks(startState));
                        }
                        for(GroundedTask gt : allChildredGroundedTasks){
                            if(!this.ActionGroundedTaskMap.containsKey(gt.action.actionName())) {
                                this.ActionGroundedTaskMap.put(gt.action.actionName(), gt);
                            }
                        }
                    }
                }

                GroundedTask currentGroundedSubTask = this.ActionGroundedTaskMap.get(ga.actionName());

                List<State> childSeq = MAXQ_Q(currentGroundedSubTask,env,ea,level+1);
                returnStateList.addAll(new ArrayList<State>(childSeq));
                if(this.stepsDone){
                    return returnStateList;
                }

                //if a higher task is terminated we update only the tasks in the stack that were finished!!
                if(this.highestCompletedTaskNode<=level)
                {
                    if(!freezeLearning) {

                        State lastState = env.currentObservation();
                        HashableState hashedLastState  =this.hsf.hashState(lastState);
                        boolean childTaskComplete = currentGroundedSubTask.t.terminal(lastState, currentGroundedSubTask.action);

                        // choose max action from the current subtask
                        Action a_star = evaluateMaxNode(parentGroundedTask,lastState).getLeft();

                        GroundedTask gt_star = this.ActionGroundedTaskMap.get(a_star.actionName());
//                        int N = 1;

                        if (childTaskComplete) {
                            for(int i=childSeq.size()-1;i>=0;i--) {
                                //update CTildeValue if values not present

                                State currentState = childSeq.get(i);
                                HashableState hashedCurrentState = this.hsf.hashState(currentState);
                                if (!CTilde.containsKey(parentGroundedTask)) {
                                    CTilde.put(parentGroundedTask, new HashMap<GroundedTask, HashMap<HashableState, Double>>());
                                }
                                if (!CTilde.get(parentGroundedTask).containsKey(currentGroundedSubTask)) {
                                    CTilde.get(parentGroundedTask).put(currentGroundedSubTask, new HashMap<HashableState, Double>());
                                }
                                if (!CTilde.get(parentGroundedTask).get(currentGroundedSubTask).containsKey(hashedCurrentState)) {
                                    CTilde.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, VMax);
                                }


                                //update CValue if values not present
                                if (!C.containsKey(parentGroundedTask)) {
                                    C.put(parentGroundedTask, new HashMap<GroundedTask, HashMap<HashableState, Double>>());
                                }
                                if (!C.get(parentGroundedTask).containsKey(currentGroundedSubTask)) {
                                    C.get(parentGroundedTask).put(currentGroundedSubTask, new HashMap<HashableState, Double>());
                                }
                                if (!C.get(parentGroundedTask).get(currentGroundedSubTask).containsKey(hashedCurrentState)) {
                                    C.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, VMax);
                                }


                                double updateCTildeValue=0.;
                                double updateCValue=0.;

                                if(parentGroundedTask.t.terminal(lastState,parentGroundedTask.action)){
                                    // if the parent task is finished the completion function only updates w.r.t. the pseudo reward!
                                    updateCTildeValue = (1 - learningRate)
                                            * CTilde.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                            + learningRate * Math.pow(gamma, childSeq.size()-i)
                                            * (((NonPrimitiveTaskNode)parentGroundedTask.t).pseudoRewardFunction(lastState,currentGroundedSubTask));

                                    // this value is slowly moving to 0. as the part missing the operation is + learningRate*0.
                                    updateCValue = (1 - learningRate)
                                            *C.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState);
                                }
                                else{
                                    double V_aStar_sTick  = this.evaluateMaxNode(gt_star, lastState).getRight();
                                    // SARSA kind of update for CTilde
                                    updateCTildeValue = (1 - learningRate)
                                            * CTilde.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                            + learningRate * Math.pow(gamma, childSeq.size()-i)
                                            * (((NonPrimitiveTaskNode)parentGroundedTask.t).pseudoRewardFunction(lastState,currentGroundedSubTask)
                                            + this.CTilde.get(parentGroundedTask).get(gt_star).get(hashedLastState)
                                            + V_aStar_sTick);

                                    updateCValue = (1 - learningRate)
                                            *C.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                            + learningRate * Math.pow(gamma, childSeq.size()-i)
                                            *(C.get(parentGroundedTask).get(gt_star).get(hashedLastState)
                                            + V_aStar_sTick);

                                }

                                CTilde.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, updateCTildeValue);


                                C.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, updateCValue);

//                                N++;
                            }
                        }
                    }
                    return returnStateList;
                }
                // since no higher tasks terminated update currently terminated task
                if(!freezeLearning) {

                    State lastState = env.currentObservation();
                    HashableState hashedLastState  =this.hsf.hashState(lastState);

                    // choose max action from the current subtask
                    Action a_star = evaluateMaxNode(parentGroundedTask,lastState).getLeft();

                    GroundedTask gt_star = this.ActionGroundedTaskMap.get(a_star.actionName());
//                    int N = 1;

                    for(int i=0;i<childSeq.size();i++) {
                        //update CTildeValue
                        State currentState = childSeq.get(i);
                        HashableState hashedCurrentState = this.hsf.hashState(currentState);
                        if (!CTilde.containsKey(parentGroundedTask)) {
                            CTilde.put(parentGroundedTask, new HashMap<GroundedTask, HashMap<HashableState, Double>>());
                        }
                        if (!CTilde.get(parentGroundedTask).containsKey(currentGroundedSubTask)) {
                            CTilde.get(parentGroundedTask).put(currentGroundedSubTask, new HashMap<HashableState, Double>());
                        }
                        if (!CTilde.get(parentGroundedTask).get(currentGroundedSubTask).containsKey(hashedCurrentState)) {
                            CTilde.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, VMax);
                        }


                        //update CValue if values not present
                        if (!C.containsKey(parentGroundedTask)) {
                            C.put(parentGroundedTask, new HashMap<GroundedTask, HashMap<HashableState, Double>>());
                        }
                        if (!C.get(parentGroundedTask).containsKey(currentGroundedSubTask)) {
                            C.get(parentGroundedTask).put(currentGroundedSubTask, new HashMap<HashableState, Double>());
                        }
                        if (!C.get(parentGroundedTask).get(currentGroundedSubTask).containsKey(hashedCurrentState)) {
                            C.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, VMax);
                        }


                        double updateCTildeValue=0.;
                        double updateCValue=0.;

                        if(parentGroundedTask.t.terminal(lastState,parentGroundedTask.action)){
                            // if the parent task is finished the completion function only updates w.r.t. the pseudo reward!
                            updateCTildeValue = (1 - learningRate)
                                    * CTilde.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                    + learningRate * Math.pow(gamma, childSeq.size()-i)
                                    * (((NonPrimitiveTaskNode)parentGroundedTask.t).pseudoRewardFunction(lastState,currentGroundedSubTask));

                            // this value is slowly moving to 0. as the part missing the operation is + learningRate*0.
                            updateCValue = (1 - learningRate)
                                    *C.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState);
                        }
                        else{
                            double V_aStar_sTick  = this.evaluateMaxNode(gt_star, lastState).getRight();
                            // SARSA kind of update for CTilde
                            updateCTildeValue = (1 - learningRate)
                                    * CTilde.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                    + learningRate * Math.pow(gamma, childSeq.size()-i)
                                    * (((NonPrimitiveTaskNode)parentGroundedTask.t).pseudoRewardFunction(lastState,currentGroundedSubTask)
                                    + this.CTilde.get(parentGroundedTask).get(gt_star).get(hashedLastState)
                                    + V_aStar_sTick);

                            updateCValue = (1 - learningRate)
                                    *C.get(parentGroundedTask).get(currentGroundedSubTask).get(hashedCurrentState)
                                    + learningRate * Math.pow(gamma, childSeq.size()-i)
                                    *(C.get(parentGroundedTask).get(gt_star).get(hashedLastState)
                                    + V_aStar_sTick);

                        }

                        CTilde.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, updateCTildeValue);


                        C.get(parentGroundedTask).get(currentGroundedSubTask).put(hashedCurrentState, updateCValue);


//                        N++;
                    }

                }
            }
            return returnStateList;
        }


    }

//    private GroundedTask getMaxAction(State currentState, GroundedTask parentTask) {
//        TaskNode[] children = ((NonPrimitiveTaskNode)parentTask.t).getChildren();
//        List<GroundedTask> allChildrenGroundedTasks = new ArrayList<GroundedTask>();
//        for(TaskNode child:children){
//            allChildrenGroundedTasks.addAll(child.getApplicableGroundedTasks(currentState));
//        }
//        for(GroundedTask gt : allChildrenGroundedTasks){
//            if(!this.ActionGroundedTaskMap.containsKey(gt.action.actionName())) {
//                this.ActionGroundedTaskMap.put(gt.action.actionName(), gt);
//            }
//        }
//
//        HashableState hs = this.hsf.hashState(currentState);
//        for(GroundedTask gt : allChildrenGroundedTasks){
////            double q = getValue(parentTask, gt, s) + evaluateMaxNode(gt,s);
//
//            if(!cTildeValue.containsKey(parentTask)){
//                cTildeValue.put(parentTask,new HashMap<GroundedTask, HashMap<HashableState, Double>>());
//            }
//            if(!cTildeValue.get(parentTask).containsKey(gt)){
//                cTildeValue.get(parentTask).put(gt,new HashMap<HashableState, Double>());
//            }
//            if(!cTildeValue.get(parentTask).get(gt).containsKey(hs)){
//                cTildeValue.get(parentTask).get(gt).put(hs,VMax);
//            }
//            double subtreeEvaluation  = evaluateMaxNode(gt,s).getRight();
//            double explorationQ = cTildeValue.get(parentTask).get(gt).get(hs) + subtreeEvaluation;
//            double returnedQ = cValue.get(parentTask).get(gt).get(hs) + subtreeEvaluation;
//            if(maxQ==explorationQ){
//                maxTasks.add(gt);
//                maxValues.add(returnedQ);
//            }
//            if(maxQ<explorationQ){
//                maxTasks.clear();
//                maxTasks.add(gt);
//                maxValues.clear();
//                maxValues.add(returnedQ);
//                maxQ = explorationQ;
//            }
//        }
//        // return the true value to the outside subtree of the chosen explored action!
//        double maxReturnQ =Double.NEGATIVE_INFINITY;
//        int maxValueIndex = -1;
//        for(int i =0;i< maxValues.size();i++){
//            if(maxValues.get(i) > maxReturnQ){
//                maxValueIndex = i;
//            }
//        }
//        return new MutablePair<Action, Double>(maxTasks.get(maxValueIndex).action,maxValues.get(maxValueIndex));
//
//
//
//    }


    public int numberOfParams(){
        int numParams =0;

        for(GroundedTask pgt : C.keySet()){
            for(GroundedTask cgt : C.get(pgt).keySet()){
                numParams+=C.get(pgt).get(cgt).size();
            }
        }

        int CSize = numParams;
        System.out.println("Params in C " + numParams);

        for(GroundedTask pgt : CTilde.keySet()){
            for(GroundedTask cgt : CTilde.get(pgt).keySet()){
                numParams+=CTilde.get(pgt).get(cgt).size();
            }
        }

        int CTildeSize = numParams -CSize ;
        System.out.println("Params in CTilde " + CTildeSize);

        for(GroundedTask gt : qValue.keySet()){
            numParams += qValue.get(gt).size();
        }

        int VSize = numParams - CTildeSize - CSize;
        System.out.println("Params in V" +VSize);

        return numParams;
    }


    @Override
    public Episode runLearningEpisode(Environment environment) {
        return runLearningEpisode(environment, -1);
    }

    @Override
    public Episode runLearningEpisode(Environment env, int maxSteps) {
        this.maxSteps = maxSteps;
        this.steps =0;
        this.stepsDone=false;
        Episode ea = new Episode(env.currentObservation());
        System.out.println(learningEpisodeCount);
        if(!this.freezeLearning){
            this.learningEpisodeCount++;
        }
        //behave until a terminal state or max steps is reached
        State curState = env.currentObservation();



        while(!env.isInTerminalState() && (steps < maxSteps || maxSteps == -1)){
            // there is only one root task node in MAXQ
            GroundedTask gtRoot = ((NonPrimitiveTaskNode)(this.root)).getApplicableGroundedTasks(curState).get(0);
            this.taskNodesStack.add(gtRoot);
            int level=0;
            int executedSteps = MAXQ_Q(gtRoot, env, ea, level).size();
//            steps+=executedSteps;
        }

        return ea;
    }





}

