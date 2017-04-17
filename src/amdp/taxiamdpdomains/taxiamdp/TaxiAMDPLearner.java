package amdp.taxiamdpdomains.taxiamdp;

import amdp.amdpframework.*;
import amdp.taxi.TaxiDomain;
import amdp.taxi.TaxiRewardFunction;
import amdp.taxi.TaxiTerminationFunction;
import amdp.taxi.TaxiVisualizer;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1Domain;
import amdp.taxiamdpdomains.taxiamdplevel1.TaxiL1TerminalFunction;
import amdp.taxiamdpdomains.taxiamdplevel1.taxil1state.L1StateMapper;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2Domain;
import amdp.taxiamdpdomains.taxiamdplevel2.TaxiL2TerminalFunction;
import amdp.taxiamdpdomains.taxiamdplevel2.taxil2state.L2StateMapper;
import amdp.taxiamdpdomains.testingtools.BoundedRTDPForTests;
import amdp.taxiamdpdomains.testingtools.GreedyReplan;
import amdp.taxiamdpdomains.testingtools.MutableGlobalInteger;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.auxiliary.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.auxiliary.StateEnumerator;
import burlap.behavior.singleagent.learning.modellearning.models.FactoredTabularModel;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMaxWithoutEnvironmentSharedModel;
import burlap.behavior.singleagent.shaping.potential.PotentialFunction;
import burlap.behavior.valuefunction.ConstantValueFunction;
import burlap.behavior.valuefunction.QProvider;
import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.mdp.auxiliary.StateMapping;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.common.UniformCostRF;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.mdp.singleagent.model.FactoredModel;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.model.TransitionProb;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import burlap.visualizer.Visualizer;

import java.util.*;

/**
 * Created by ngopalan on 8/14/16.
 */
public class TaxiAMDPLearner {

    public static List<BoundedRTDPForTests> brtdpList= new ArrayList<BoundedRTDPForTests>();
//    DPrint.(3214986, false);

    public static Map<GroundedTask, MDPSolver> groundedTaskToSolverMap = new HashMap<>();
    public static Map<String, FactoredTabularModel> taskNameToModelMap = new HashMap<>();


    static int l0Budget = 50;//0.001
    static int l1Budget = 40;//0.001
    static int l2Budget = 30;//0.01

    static int l0Depth = 30;
    static int l1Depth = 5;
    static int l2Depth = 15;

    static protected MutableGlobalInteger bellmanBudget = new MutableGlobalInteger(-1);




    static int  maxTrajectoryLength = 101;


    public static void main(String[] args) {

        DPrint.toggleCode(23987345, false);
        RandomFactory randomFactory = new RandomFactory();
        Random rand = randomFactory.getMapped(100);

        boolean randomStart = true;
        boolean singlePassenger = true;

        bellmanBudget.setValue(-1);

//        for(int i =0;i<args.length;i++){
//            String str = args[i];
////            System.out.println(str);
//            if(str.equals("-r")){
//                randomStart = Boolean.parseBoolean(args[i+1]);
//            }
//            if(str.equals("-b")){
//                bellmanBudget.setValue(Integer.parseInt(args[i+1]));
//            }
//            if(str.equals("-s")){
//                singlePassenger = Boolean.parseBoolean(args[i+1]);
//            }
//        }

        TerminalFunction tf = new TaxiTerminationFunction();
        RewardFunction rf = new TaxiRewardFunction(1,tf);

        TaxiDomain tdGen = new TaxiDomain(rf,tf);



//        tdGen.setTransitionDynamicsLikeFickleTaxiProlem();

        tdGen.setFickleTaxi(false);
        tdGen.setIncludeFuel(false);


        OOSADomain td = tdGen.generateDomain();

        OOSADomain tdEnv = tdGen.generateDomain();


        State startState;// = TaxiDomain.getComplexState(false);

        if(randomStart){
            if(singlePassenger){
                startState = TaxiDomain.getRandomClassicState(rand, td, false);
            }
            else{
                startState = TaxiDomain.getComplexState(false);
            }

        }
        else{
            if(singlePassenger) {
                startState = TaxiDomain.getClassicState(td, false);
            }
            else{
                startState = TaxiDomain.getComplexState(false);
            }
        }


        TerminalFunction tfL1 = new TaxiL1TerminalFunction();
        RewardFunction rfL1 = new UniformCostRF();

        TaxiL1Domain tdL1Gen = new TaxiL1Domain(rfL1, tfL1, rand);

        OOSADomain tdL1 = tdL1Gen.generateDomain();


        L1StateMapper testMapper = new L1StateMapper();

        State sL1 = testMapper.mapState(startState);

        L2StateMapper l2StateMapper = new L2StateMapper();

        State sL2 = l2StateMapper.mapState(sL1);


        TerminalFunction tfL2 = new TaxiL2TerminalFunction();
        RewardFunction rfL2 = new UniformCostRF();

        TaxiL2Domain tdL2Gen = new TaxiL2Domain(rfL2, tfL2,rand);


        OOSADomain tdL2 = tdL2Gen.generateDomain();


        ActionType east = td.getAction(TaxiDomain.ACTION_EAST);
        ActionType west = td.getAction(TaxiDomain.ACTION_WEST);
        ActionType south = td.getAction(TaxiDomain.ACTION_SOUTH);
        ActionType north = td.getAction(TaxiDomain.ACTION_NORTH);
        ActionType pickup = td.getAction(TaxiDomain.ACTION_PICKUP);
        ActionType dropoff = td.getAction(TaxiDomain.ACTION_DROPOFF);


        ActionType navigate = tdL1.getAction(TaxiL1Domain.ACTION_NAVIGATE);
        ActionType pickupL1 = tdL1.getAction(TaxiL1Domain.ACTION_PICKUPL1);
        ActionType putdownL1 = tdL1.getAction(TaxiL1Domain.ACTION_PUTDOWNL1);

        ActionType get = tdL2.getAction(TaxiL2Domain.ACTION_GET);
        ActionType put = tdL2.getAction(TaxiL2Domain.ACTION_PUT);

        TaskNode et = new MoveTaskNodes(east);
        TaskNode wt = new MoveTaskNodes(west);
        TaskNode st = new MoveTaskNodes(south);
        TaskNode nt = new MoveTaskNodes(north);
        TaskNode pt = new PickupL0TaskNode(pickup);
        TaskNode dt = new PutDownL0TaskNode(dropoff);


        TaskNode[] navigateSubTasks = new TaskNode[]{et,wt,st,nt};
        TaskNode[] dropOffL1SubTasks = new TaskNode[]{dt};
        TaskNode[] pickupL1SubTasks = new TaskNode[]{pt};

        TaskNode navigateTaskNode = new NavigateTaskNode(navigate,tdL1Gen.generateDomain(),tdGen.generateDomain(),navigateSubTasks);
        TaskNode putDownL1TaskNode = new PutDownL1TaskNode(putdownL1,tdL1Gen.generateDomain(),tdGen.generateDomain(),dropOffL1SubTasks);
        TaskNode pickupL1TaskNode = new PickupL1TaskNode(pickupL1,tdL1Gen.generateDomain(),tdGen.generateDomain(),pickupL1SubTasks);

        FactoredTabularModel navModel = new FactoredTabularModel(((NonPrimitiveTaskNode)navigateTaskNode).getDomain(),new SimpleHashableStateFactory(),5);
        FactoredTabularModel putDownL1Model = new FactoredTabularModel(((NonPrimitiveTaskNode)putDownL1TaskNode).getDomain(),new SimpleHashableStateFactory(),5);
        FactoredTabularModel pickUpL1Model = new FactoredTabularModel(((NonPrimitiveTaskNode)pickupL1TaskNode).getDomain(),new SimpleHashableStateFactory(),5);


        taskNameToModelMap.put(navigateTaskNode.getName(),navModel);
        taskNameToModelMap.put(putDownL1TaskNode.getName(),putDownL1Model);
        taskNameToModelMap.put(pickupL1TaskNode.getName(),pickUpL1Model);

        TaskNode[] getSubTasks = new TaskNode[]{navigateTaskNode, pickupL1TaskNode};
        TaskNode[] putSubTasks = new TaskNode[]{navigateTaskNode, putDownL1TaskNode};

        TaskNode getTaskNode = new GetTaskNode(get, tdL2Gen.generateDomain(), tdL1Gen.generateDomain(), getSubTasks);
        TaskNode putTaskNode = new PutTaskNode(put, tdL2Gen.generateDomain(), tdL1Gen.generateDomain(), putSubTasks);


        TaskNode[] rootSubTasks = new TaskNode[]{getTaskNode,putTaskNode};

        TaskNode root = new RootTaskNode("root",rootSubTasks,tdL2, tfL2,rfL2);

//        BoundedRTDPForTests brtdp = new BoundedRTDPForTests(((NonPrimitiveTaskNode)root).domain(), 0.99,  new SimpleHashableStateFactory(false),new ConstantValueFunction(0.),
//                new ConstantValueFunction(1.), 0.01, l2Budget);
//        brtdp.setRemainingNumberOfBellmanUpdates(new MutableGlobalInteger(50));
//
//
//        brtdp.setMaxRolloutDepth(l2Depth);//5
//        brtdp.toggleDebugPrinting(false);
//            Policy p = brtdp.planFromState(new L2StateMapper().mapState(new L1StateMapper().mapState(startState)));
////        brtdpList.add(brtdp);
//        Episode ea = PolicyUtils.rollout(p, new L2StateMapper().mapState(new L1StateMapper().mapState(startState)), tdL2.getModel());
//        for (Action a:ea.actionSequence){
//            System.out.println(a.actionName());
//        }

        List<AMDPModelLearner> pgList = new ArrayList<AMDPModelLearner>();
        FactoredTabularModel sharedModel = new FactoredTabularModel(td,new SimpleHashableStateFactory(),5);
        pgList.add(0,new l0PolicyGenerator(td, sharedModel,5));
        pgList.add(1,new l1PolicyGenerator(tdL1));
        pgList.add(2,new l2PolicyGenerator(tdL2));

        AMDPLearningAgent agent = new AMDPLearningAgent(root.getApplicableGroundedTasks(sL2).get(0),pgList);
        List<Double> rewardsPerEpisode = new ArrayList<>();


        Visualizer v = TaxiVisualizer.getVisualizer(5, 5);
        List<Episode> eaList = new ArrayList<Episode>();

//        startState = TaxiDomain.getStartStateFromTaxiPosition(3,3,rand,td,false);
//        startState = TaxiDomain.getRandomClassicState(rand,td,false);

        StateEnumerator se = new StateEnumerator(((NonPrimitiveTaskNode) navigateTaskNode).domain(),new SimpleHashableStateFactory());
        se.findReachableStatesAndEnumerate(startState);

        List<ActionType> atType = ((NonPrimitiveTaskNode) navigateTaskNode).getDomain().getActionTypes();

        HashMap<State,HashMap<String, ArrayList<TransitionProb>>> transitionTableWhenWorking = new HashMap<>();
        HashMap<State,HashMap<String, Double>> rewardTableWhenWorking = new HashMap<>();

        HashMap<State,HashMap<String, ArrayList<TransitionProb>>> transitionTableWhenNotWorking = new HashMap<>();
        HashMap<State,HashMap<String, Double>> rewardTableWhenNotWorking = new HashMap<>();


        for(int i=0;i<100;i++) {

//            startState = TaxiDomain.getRandomClassicState(rand, td, false);
            SimulatedEnvironment envN = new SimulatedEnvironment(tdEnv, startState);

//            if(i==45){
//                FactoredTabularModel model = taskNameToModelMap.get(navigateTaskNode.getName());
//                for(int j=0;j<se.numStatesEnumerated();j++){
//                    State s = se.getStateForEnumerationId(j);
//                    for(ActionType at:atType){
//                        List<Action> actionList = at.allApplicableActions(s);
//                        if(actionList.size()==0){
//                            continue;
//                        }
//
//                        Action a = actionList.get(0);
//                        List<TransitionProb> tempTPList = model.transitions(s,a);
//
//                        if(!transitionTableWhenWorking.containsKey(s)){
//                            transitionTableWhenWorking.put(s, new HashMap<String, ArrayList<TransitionProb>>());
//                        }
//                        if(!transitionTableWhenWorking.get(s).containsKey(a.actionName())){
//                            transitionTableWhenWorking.get(s).put(a.actionName(), new ArrayList<TransitionProb>());
//                        }
//                        transitionTableWhenWorking.get(s).get(a.actionName()).addAll(tempTPList);
//
//                        if(!rewardTableWhenWorking.containsKey(s)){
//                            rewardTableWhenWorking.put(s, new HashMap<String, Double>());
//                        }
//
//                        rewardTableWhenWorking.get(s).put(a.actionName(), model.sample(s,a).r);
//
//                    }
//
//                }
//            }


            Episode e = agent.runLearningEpisode(envN, 100);
            eaList.add(e);
            if(e.numActions()>99 && i>45) {
                new EpisodeSequenceVisualizer(v, td, eaList);
            }
            System.out.println("TaxiAMDPLearner: episode: "+i + " num actions: " + e.numActions());
            rewardsPerEpisode.add(e.discountedReturn(1.));
        }


//        FactoredTabularModel model = taskNameToModelMap.get(navigateTaskNode.getName());
//        for(int j=0;j<se.numStatesEnumerated();j++){
//            State s = se.getStateForEnumerationId(j);
//            for(ActionType at:atType){
//                List<Action> actionList = at.allApplicableActions(s);
//                if(actionList.size()==0){
//                    continue;
//                }
//
//                Action a = actionList.get(0);
//                List<TransitionProb> tempTPList = model.transitions(s,a);
//
//                if(!transitionTableWhenNotWorking.containsKey(s)){
//                    transitionTableWhenNotWorking.put(s, new HashMap<String, ArrayList<TransitionProb>>());
//                }
//                if(!transitionTableWhenNotWorking.get(s).containsKey(a.actionName())){
//                    transitionTableWhenNotWorking.get(s).put(a.actionName(), new ArrayList<TransitionProb>());
//                }
//                transitionTableWhenNotWorking.get(s).get(a.actionName()).addAll(tempTPList);
//
//                if(!rewardTableWhenNotWorking.containsKey(s)){
//                    rewardTableWhenNotWorking.put(s, new HashMap<String, Double>());
//                }
//
//                rewardTableWhenNotWorking.get(s).put(a.actionName(), model.sample(s,a).r);
//
//            }
//
//        }

//        for(int i=0;i<se.numStatesEnumerated();i++){
//
//            State s = se.getStateForEnumerationId(i);
//            for(ActionType at:atType){
//                List<Action> actionList = at.allApplicableActions(s);
//                if(actionList.size()==0){
//                    continue;
//                }
//
//                Action a = actionList.get(0);
////                List<TransitionProb> tempTPList = model.transitions(s,a);
//
//                if(!transitionTableWhenNotWorking.containsKey(s)){
//                    continue;
//                }
//                if(!transitionTableWhenNotWorking.get(s).containsKey(a.actionName())){
//                    continue;
//                }
//                for(int j=0;j<transitionTableWhenNotWorking.get(s).get(a.actionName()).size();j++) {
//                    if(!transitionTableWhenWorking.get(s).get(a.actionName()).get(j).eo.op.equals(transitionTableWhenNotWorking.get(s).get(a.actionName()).get(j).eo.op)) {
//                        System.out.println("-------------------------------------------------------------------------------------------");
//                        System.out.println("///////////////////////////////////!!!transitions!!!!///////////////////////////////////////////////");
//                        System.out.println("original state: " + s);
//                        System.out.println("action taken: " + a.actionName());
//                        System.out.println("-------------------------------------------------------------------------------------------");
//                        System.out.println(transitionTableWhenWorking.get(s).get(a.actionName()).get(j).eo.op);
//                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
//                        System.out.println(transitionTableWhenNotWorking.get(s).get(a.actionName()).get(j).eo.op);
//                    }
//
//                    if(!rewardTableWhenWorking.get(s).get(a.actionName()).equals(rewardTableWhenNotWorking.get(s).get(a.actionName()))) {
//                        System.out.println("-------------------------------------------------------------------------------------------");
//                        System.out.println("//////////////////////////////!!!reward!!!!///////////////////////////////////");
//                        System.out.println("original state: " + s);
//                        System.out.println("action taken: " + a.actionName());
//                        System.out.println("-------------------------------------------------------------------------------------------");
//                        System.out.println(rewardTableWhenWorking.get(s).get(a.actionName()));
//                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
//                        System.out.println(rewardTableWhenNotWorking.get(s).get(a.actionName()));
//                    }
//                }
//
//            }
//
//        }


        int count = 0;
        int count0 =0;
        int count1 =0;
        int count2 =0;
        double sumRewards =0.;
        for(int i=0;i<rewardsPerEpisode.size();i++){
            System.out.println("reward in episode " + i + ": " + rewardsPerEpisode.get(i));
            sumRewards+= rewardsPerEpisode.get(i);
//			System.out.println("Level: " + brtdpLevelList.get(i) + ", count: " + numUpdates);
//            if(brtdpList.get(i)==0){
//                count0 += numUpdates;
//            }
//            else if(brtdpList.get(i)==1){
//                count1 += numUpdates;
//            }
//            else{
//                count2 += numUpdates;
//            }
        }

        System.out.println(sumRewards);
//        System.out.println("actions taken: " + e.actionSequence.size());
//        System.out.println("rewards: " + e.discountedReturn(1.));
////        System.out.println("Total updates used: " + count);
////        System.out.println(e.actionSequence.size());
////        System.out.println(e.discountedReturn(1.));
//        System.out.println( count);
//        System.out.println("Total planners used: " + brtdpList.size());
//        System.out.println("Taxi with AMDPs \n Backups by individual planners:");
//        for(BoundedRTDPForTests b:brtdpList){
//            System.out.println(b.getNumberOfBellmanUpdates());
//        }
//        System.out.println("random start state: " + randomStart);






    }

    public static class l2PolicyGenerator implements AMDPModelLearner {

        private OOSADomain l2;
        double gamma = 0.99;
        private StateMapping sm = new L2StateMapper();
        public l2PolicyGenerator(OOSADomain l2In){
            l2 = l2In;
        }


        @Override
        public Policy generatePolicy(State s, GroundedTask groundedTask) {
            l2 = ((NonPrimitiveTaskNode)groundedTask.getT()).domain();
            l2.setModel(new FactoredModel(((FactoredModel)l2.getModel()).getStateModel(),groundedTask.rewardFunction(), groundedTask.terminalFunction()));
            BoundedRTDPForTests brtdp = new BoundedRTDPForTests(l2, gamma,  new SimpleHashableStateFactory(false),new ConstantValueFunction(0.),
                    new ConstantValueFunction(1.), 0.1, l2Budget);
            brtdp.setRemainingNumberOfBellmanUpdates(bellmanBudget);


            brtdp.setMaxRolloutDepth(l2Depth);//5
            brtdp.toggleDebugPrinting(false);
            brtdp.planFromState(s);
            brtdpList.add(brtdp);
//            return brtdp.planFromState(s);
            return new GreedyReplan(brtdp);
        }

        @Override
        public State generateAbstractState(State s) {
            return sm.mapState(s);
        }


        @Override
        public QProvider getQProvider(State s, GroundedTask groundedTask){return null;};

        @Override
        public void updateModel(State s, Action a, List<Double> rewards, State sPrime, GroundedTask gt) {

        }
    }


    public static class l1PolicyGenerator implements AMDPModelLearner{

        private OOSADomain l1;
        protected final double discount = 0.99;
        StateMapping sm = new L1StateMapper();

        public l1PolicyGenerator(OOSADomain l2In){

        }

        @Override
        public Policy generatePolicy(State s, GroundedTask gt) {
            l1 = ((NonPrimitiveTaskNode)gt.getT()).domain();

            l1.setModel(new FactoredModel(((FactoredModel)l1.getModel()).getStateModel(),gt.rewardFunction(), gt.terminalFunction()));


            BoundedRTDPForTests brtdp = new BoundedRTDPForTests(l1, discount, new SimpleHashableStateFactory(false),
                    new ConstantValueFunction(0.),   new ConstantValueFunction(1.),
                    0.01,
                    l1Budget);//10
            brtdp.setRemainingNumberOfBellmanUpdates(bellmanBudget);
            brtdpList.add(brtdp);

            brtdp.setMaxRolloutDepth(l1Depth);//10
            brtdp.toggleDebugPrinting(false);
            brtdp.planFromState(s);
            return new GreedyReplan(brtdp);

        }

        @Override
        public State generateAbstractState(State s) {
            return sm.mapState(s);
        }

        @Override
        public QProvider getQProvider(State s, GroundedTask groundedTask){return null;};

        @Override
        public void updateModel(State s, Action a, List<Double> rewards, State sPrime, GroundedTask gt) {
            
        }

    }

    public static class l0PolicyGenerator implements AMDPModelLearner{

        //        private OOSADomain l0;
        private final double discount = 0.99;

        HashMap<GroundedTask, FactoredTabularModel> gtModel = new HashMap<>();

//        FactoredTabularModel sharedModel;
//        RMaxModel sharedRMaxModel;
        int nconf;

        public l0PolicyGenerator(OOSADomain l0In, FactoredTabularModel sharedModel, int nconf){
//            this.sharedModel = sharedModel;

//            sharedRMaxModel = new RMaxModel(sharedModel,pf,1.,l0In.getActionTypes());
            this.nconf = nconf;
        }

        @Override
        public Policy generatePolicy(State s, GroundedTask gt) {

            OOSADomain l0 = gt.groundedDomain();



            if(!groundedTaskToSolverMap.containsKey(gt)){
                FactoredTabularModel sharedModel = taskNameToModelMap.get(gt.getT().getName());
                FactoredTabularModel ft = new FactoredTabularModel(gt.groundedDomain(),new SimpleHashableStateFactory(),nconf,sharedModel.getStateNodes());
//                taskNameToModelMap.put(gt,ft);

                PotentialFunction pf = new PotentialShapedRMaxWithoutEnvironmentSharedModel.RMaxPotential(-1, 0.99);
                l0.setModel(ft);


                PotentialShapedRMaxWithoutEnvironmentSharedModel learner = new PotentialShapedRMaxWithoutEnvironmentSharedModel(l0,
                        new SimpleHashableStateFactory(), pf, sharedModel,5, 0.1, 100,1.);
                groundedTaskToSolverMap.put(gt,learner);
            }

//            l0.setModel(new FactoredModel(((FactoredModel)l0.getModel()).getStateModel(), gt.rewardFunction(), gt.terminalFunction()));


//            BoundedRTDPForTests brtdp = new BoundedRTDPForTests(l0, discount, new SimpleHashableStateFactory(false),
//                    new ConstantValueFunction(0.),
//                    new ConstantValueFunction(1.),
//                    0.01,
//                    l0Budget);//50
//
//            brtdp.setRemainingNumberOfBellmanUpdates(bellmanBudget);
//            brtdpList.add(brtdp);
//
//            brtdp.setMaxRolloutDepth(l0Depth);//15
//            brtdp.toggleDebugPrinting(false);
//            brtdp.planFromState(s);
//            return new GreedyReplan(brtdp);

            PotentialShapedRMaxWithoutEnvironmentSharedModel modelLearner = (PotentialShapedRMaxWithoutEnvironmentSharedModel) groundedTaskToSolverMap.get(gt);


            return modelLearner.getModelPlanner().planFromState(s);

        }

        @Override
        public State generateAbstractState(State s) {
            return null;
        }


        @Override
        public QProvider getQProvider(State s, GroundedTask groundedTask){return null;}


        @Override
        public void updateModel(State s, Action a, List<Double> rewards, State sPrime, GroundedTask gt) {
            PotentialShapedRMaxWithoutEnvironmentSharedModel modelLearner = (PotentialShapedRMaxWithoutEnvironmentSharedModel) groundedTaskToSolverMap.get(gt);
            modelLearner.updateStateAndModel(s,a,sPrime,rewards);
        }
    }

}
