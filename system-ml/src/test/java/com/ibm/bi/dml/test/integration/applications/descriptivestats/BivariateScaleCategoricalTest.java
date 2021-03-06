/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.ibm.bi.dml.test.integration.applications.descriptivestats;


import java.util.HashMap;

import org.junit.Test;

import com.ibm.bi.dml.runtime.matrix.data.MatrixValue.CellIndex;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.test.utils.TestUtils;


public class BivariateScaleCategoricalTest extends AutomatedTestBase 
{

	
	private final static String TEST_DIR = "applications/descriptivestats/";
	private final static String TEST_SCALE_NOMINAL = "ScaleCategorical";
	private final static String TEST_SCALE_NOMINAL_WEIGHTS = "ScaleCategoricalWithWeightsTest";

	private final static double eps = 1e-9;
	private final static int rows = 10000;
	private final static int ncatA = 100; // # of categories in A
	private final static double minVal = 0; // minimum value in Y
	private final static double maxVal = 250; // minimum value in Y
	private int maxW = 10;    // maximum weight
	
	@Override
	public void setUp() {
		addTestConfiguration(TEST_SCALE_NOMINAL, 
				new TestConfiguration(TEST_DIR, TEST_SCALE_NOMINAL, 
					new String[] { "Eta"+".scalar", "AnovaF"+".scalar", "VarY"+".scalar", "MeanY"+".scalar", "CFreqs", "CMeans", "CVars" }));
		//addTestConfiguration(TEST_SCALE_NOMINAL_WEIGHTS, new TestConfiguration(TEST_DIR, "ScaleCategoricalWithWeightsTest", new String[] { "outEta", "outAnovaF", "outVarY", "outMeanY", "outCatFreqs", "outCatMeans", "outCatVars" }));
		addTestConfiguration(TEST_SCALE_NOMINAL_WEIGHTS, new TestConfiguration(TEST_DIR, "ScaleCategoricalWithWeightsTest", new String[] { "Eta"+".scalar", "AnovaF"+".scalar", "VarY"+".scalar", "MeanY"+".scalar", "CFreqs", "CMeans", "CVars" }));
	}
	
	@Test
	public void testScaleCategorical() {
		
		TestConfiguration config = getTestConfiguration(TEST_SCALE_NOMINAL);
		
		config.addVariable("rows", rows);
		
		/* This is for running the junit test the new way, i.e., construct the arguments directly */
		String SC_HOME = SCRIPT_DIR + TEST_DIR;	
		fullDMLScriptName = SC_HOME + TEST_SCALE_NOMINAL + ".dml";
		programArgs = new String[]{"-args",  SC_HOME + INPUT_DIR + "A" , 
	                        Integer.toString(rows),
	                         SC_HOME + INPUT_DIR + "Y" , 
	                         SC_HOME + OUTPUT_DIR + "VarY" ,
	                         SC_HOME + OUTPUT_DIR + "MeanY" ,
	                         SC_HOME + OUTPUT_DIR + "CFreqs" ,
	                         SC_HOME + OUTPUT_DIR + "CMeans" ,
	                         SC_HOME + OUTPUT_DIR + "CVars" ,
	                         SC_HOME + OUTPUT_DIR + "Eta" ,
	                         SC_HOME + OUTPUT_DIR + "AnovaF" };
		fullRScriptName = SC_HOME + TEST_SCALE_NOMINAL + ".R";
		rCmd = "Rscript" + " " + SC_HOME + TEST_SCALE_NOMINAL + ".R" + " " + 
		       SC_HOME + INPUT_DIR + " " + SC_HOME + EXPECTED_DIR;

		loadTestConfiguration(config);

        double[][] A = getRandomMatrix(rows, 1, 1, ncatA, 1, System.currentTimeMillis()) ; 
        round(A);
        double[][] Y = getRandomMatrix(rows, 1, minVal, maxVal, 0.1, System.currentTimeMillis()) ; 

		writeInputMatrix("A", A, true);
		writeInputMatrix("Y", Y, true);

 
		boolean exceptionExpected = false;
		/*
		 * Expected number of jobs:
		 */
		// int expectedNumberOfJobs = 5;
		runTest(true, exceptionExpected, null, -1);
		
		runRScript(true);
		
		for(String file: config.getOutputFiles())
		{
			/* NOte that some files do not contain matrix, but just a single scalar value inside */
			HashMap<CellIndex, Double> dmlfile;
			HashMap<CellIndex, Double> rfile;
			if (file.endsWith(".scalar")) {
				file = file.replace(".scalar", "");
				dmlfile = readDMLScalarFromHDFS(file);
				rfile = readRScalarFromFS(file);
			}
			else {
				dmlfile = readDMLMatrixFromHDFS(file);
				rfile = readRMatrixFromFS(file);
			}
			TestUtils.compareMatrices(dmlfile, rfile, eps, file+"-DML", file+"-R");
		}
	}
	
	private void round(double[][] weight) {
		for(int i=0; i<weight.length; i++)
			weight[i][0]=Math.floor(weight[i][0]);
	}

	@Test
	public void testScaleCategoricalWithWeights() {
		TestConfiguration config = getTestConfiguration(TEST_SCALE_NOMINAL_WEIGHTS);
		
		config.addVariable("rows", rows);

		/* This is for running the junit test the new way, i.e., construct the arguments directly */
		String SC_HOME = SCRIPT_DIR + TEST_DIR;	
		fullDMLScriptName = SC_HOME + TEST_SCALE_NOMINAL_WEIGHTS + ".dml";
		programArgs = new String[]{"-args",  SC_HOME + INPUT_DIR + "A" , 
	                        Integer.toString(rows),
	                         SC_HOME + INPUT_DIR + "Y" , 
	                         SC_HOME + INPUT_DIR + "WM" , 
	                         SC_HOME + OUTPUT_DIR + "VarY" ,
	                         SC_HOME + OUTPUT_DIR + "MeanY" ,
	                         SC_HOME + OUTPUT_DIR + "CFreqs" ,
	                         SC_HOME + OUTPUT_DIR + "CMeans" ,
	                         SC_HOME + OUTPUT_DIR + "CVars" ,
	                         SC_HOME + OUTPUT_DIR + "Eta" ,
	                         SC_HOME + OUTPUT_DIR + "AnovaF" };
		fullRScriptName = SC_HOME + TEST_SCALE_NOMINAL_WEIGHTS + ".R";
		rCmd = "Rscript" + " " + SC_HOME + TEST_SCALE_NOMINAL_WEIGHTS + ".R" + " " + 
		       SC_HOME + INPUT_DIR + " " + SC_HOME + EXPECTED_DIR;
		
		loadTestConfiguration(config);

        double[][] A = getRandomMatrix(rows, 1, 1, ncatA, 1, System.currentTimeMillis());
        double[][] Y = getRandomMatrix(rows, 1, minVal, maxVal, 0.1, System.currentTimeMillis());
        double[][] WM = getRandomMatrix(rows, 1, 1, maxW, 1, System.currentTimeMillis());
        round(A);
        round(WM);
        
		writeInputMatrix("A", A, true);
		writeInputMatrix("Y", Y, true);
		writeInputMatrix("WM", WM, true);
		
		runTest(true, false, null, -1);
		
		runRScript(true);
		
		for(String file: config.getOutputFiles())
		{
			/* NOte that some files do not contain matrix, but just a single scalar value inside */
			HashMap<CellIndex, Double> dmlfile;
			HashMap<CellIndex, Double> rfile;
			if (file.endsWith(".scalar")) {
				file = file.replace(".scalar", "");
				dmlfile = readDMLScalarFromHDFS(file);
				rfile = readRScalarFromFS(file);
			}
			else {
				dmlfile = readDMLMatrixFromHDFS(file);
				rfile = readRMatrixFromFS(file);
			}
			TestUtils.compareMatrices(dmlfile, rfile, eps, file+"-DML", file+"-R");
		}

	}
	

}
