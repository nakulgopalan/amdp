package amdp.taxiamdpdomains.taxiamdp;

import amdp.taxi.TaxiDomain;
import amdp.taxi.TaxiRewardFunction;
import amdp.taxi.TaxiTerminationFunction;
import amdp.taxi.TaxiVisualizer;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.auxiliary.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.modellearning.rmax.PotentialShapedRMax;
import burlap.debugtools.RandomFactory;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.shell.visual.VisualExplorer;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import burlap.visualizer.Visualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by ngopalan on 3/3/17.
 */
public class TaxiBaseLearner {
    public static void main(String[] args) {


        RandomFactory randomFactory = new RandomFactory();
        Random rand = randomFactory.getMapped(100);

        boolean randomStart = true;
        boolean singlePassenger = true;

        TerminalFunction tf = new TaxiTerminationFunction();
        RewardFunction rf = new TaxiRewardFunction(1,tf);



        TaxiDomain tdGen = new TaxiDomain(rf,tf);


        tdGen.setTransitionDynamicsLikeFickleTaxiProlem();
        tdGen.setFickleTaxi(true);
        tdGen.setIncludeFuel(false);

        OOSADomain td = tdGen.generateDomain();


        //setup initial state
//    State s = new GridWorldState(new GridAgent(0, 0), new GridLocation(10, 10, "loc0"));
        State s = TaxiDomain.getRandomClassicState(rand, td, false);

        //create visualizer and explorer


        Visualizer v = TaxiVisualizer.getVisualizer(5, 5);
        List<Episode> eaList = new ArrayList<Episode>();
//    SampleModel model = new
        Environment env = new SimulatedEnvironment(td, s);
        VisualExplorer exp = new VisualExplorer(td, env, v);

        //set control keys to use w-s-a-d
//    exp.addKeyAction("w", GridWorldDomain.ACTION_NORTH, "");
//    exp.addKeyAction("s", GridWorldDomain.ACTION_SOUTH, "");
//    exp.addKeyAction("a", GridWorldDomain.ACTION_WEST, "");
//    exp.addKeyAction("d", GridWorldDomain.ACTION_EAST, "");

//        exp.initGUI();
//        exp.startLiveStatePolling(1000/60);

        // Learn it
        PotentialShapedRMax rMax = new PotentialShapedRMax(td, 0.9, new SimpleHashableStateFactory(), 100, 5, 0.1, 100);

        for(int i=0;i<100;i++) {
            Episode e = rMax.runLearningEpisode(env,1000);
            env.resetEnvironment();
            System.out.println(e.numTimeSteps());
            eaList.add(e);
        }

        new EpisodeSequenceVisualizer(v, td, eaList);

    }
}
