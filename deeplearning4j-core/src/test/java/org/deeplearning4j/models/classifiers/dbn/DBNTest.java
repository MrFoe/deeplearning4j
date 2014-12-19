package org.deeplearning4j.models.classifiers.dbn;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.distributions.Distributions;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.models.featuredetectors.rbm.RBM;
import org.deeplearning4j.nn.api.NeuralNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.nd4j.linalg.api.activation.Activations;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.nn.WeightInit;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by agibsonccc on 8/28/14.
 */
public class DBNTest {

    private static Logger log = LoggerFactory.getLogger(DBNTest.class);



    @Test
    public void testIris() {
        RandomGenerator gen = new MersenneTwister(123);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .iterations(1000)
                .activationFunction(Activations.tanh())
                .visibleUnit(RBM.VisibleUnit.GAUSSIAN).hiddenUnit(RBM.HiddenUnit.RECTIFIED)
                .lossFunction(LossFunctions.LossFunction.RMSE_XENT).weightInit(WeightInit.VI)
                .optimizationAlgo(NeuralNetwork.OptimizationAlgorithm.ITERATION_GRADIENT_DESCENT)
                .rng(gen).constrainGradientToUnitNorm(true)
                .learningRate(1e-3f)
                .nIn(4).nOut(3).list(3).hiddenLayerSizes(new int[]{3,2})
                .override(new NeuralNetConfiguration.ConfOverride() {
                    @Override
                    public void override(int i, NeuralNetConfiguration.Builder builder) {

                        if (i == 2) {
                            builder.weightInit(WeightInit.ZERO);
                            builder.activationFunction(Activations.softMaxRows());
                            builder.lossFunction(LossFunctions.LossFunction.MCXENT);

                        }
                    }
                }).build();



        DBN d = new DBN.Builder().layerWiseConfiguration(conf)
                .build();



        DataSetIterator iter = new IrisDataSetIterator(150, 150);

        DataSet next = iter.next(100);
        next.normalizeZeroMeanZeroUnitVariance();
        d.fit(next);



        Evaluation eval = new Evaluation();
        INDArray output = d.output(next.getFeatureMatrix());
        eval.eval(next.getLabels(),output);
        log.info("Score " + eval.stats());


    }

    @Test
    public void testDbn() throws IOException {
        RandomGenerator gen = new MersenneTwister(123);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .withActivationType(NeuralNetConfiguration.ActivationType.HIDDEN_LAYER_ACTIVATION)
                .momentum(9e-1f).weightInit(WeightInit.UNIFORM)
                .optimizationAlgo(NeuralNetwork.OptimizationAlgorithm.ITERATION_GRADIENT_DESCENT)
                .lossFunction(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY).rng(gen).iterations(1000)
                .learningRate(1e-1f).nIn(784).nOut(10).list(4).hiddenLayerSizes(new int[]{500, 400, 300}).override(new NeuralNetConfiguration.ConfOverride() {
                    @Override
                    public void override(int i, NeuralNetConfiguration.Builder builder) {
                        if(i == 3) {
                            builder.weightInit(WeightInit.ZERO);
                            builder.activationFunction(Activations.softMaxRows());
                            builder.lossFunction(LossFunctions.LossFunction.MCXENT);

                        }
                    }
                })
                .build();



        DBN d = new DBN.Builder().layerWiseConfiguration(conf)
                .build();

        MnistDataFetcher fetcher = new MnistDataFetcher(true);
        fetcher.fetch(10);
        DataSet d2 = fetcher.next();
        d.fit(d2);


        INDArray predict2 = d.output(d2.getFeatureMatrix());

        Evaluation eval = new Evaluation();
        eval.eval(d2.getLabels(),predict2);
        log.info(eval.stats());
        int[] predict = d.predict(d2.getFeatureMatrix());
        log.info("Predict " + Arrays.toString(predict));


    }

}
