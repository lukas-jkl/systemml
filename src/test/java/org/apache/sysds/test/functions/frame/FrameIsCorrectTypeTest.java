/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.test.functions.frame;

import org.apache.sysds.api.DMLScript;
import org.apache.sysds.common.Types;
import org.apache.sysds.common.Types.ValueType;
import org.apache.sysds.hops.OptimizerUtils;
import org.apache.sysds.lops.LopProperties;
import org.apache.sysds.runtime.io.FrameWriter;
import org.apache.sysds.runtime.io.FrameWriterFactory;
import org.apache.sysds.runtime.matrix.data.FrameBlock;
import org.apache.sysds.runtime.matrix.data.InputInfo;
import org.apache.sysds.runtime.matrix.data.OutputInfo;
import org.apache.sysds.runtime.util.UtilFunctions;
import org.apache.sysds.test.AutomatedTestBase;
import org.apache.sysds.test.TestConfiguration;
import org.apache.sysds.test.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class FrameIsCorrectTypeTest extends AutomatedTestBase
{
	private final static String TEST_NAME = "DropInvalid";
	private final static String TEST_DIR = "functions/frame/";
	private static final String TEST_CLASS_DIR = TEST_DIR + FrameIsCorrectTypeTest.class.getSimpleName() + "/";

	private final static int rows = 20;
	private final static ValueType[] schemaStrings = {ValueType.FP64, ValueType.STRING};

	public static void init() {
		TestUtils.clearDirectory(TEST_DATA_DIR + TEST_CLASS_DIR);
	}

	public static void cleanUp() {
		if (TEST_CACHE_ENABLED) {
			TestUtils.clearDirectory(TEST_DATA_DIR + TEST_CLASS_DIR);
		}
	}

	@Override
	public void setUp() {
		TestUtils.clearAssertionInformation();
		addTestConfiguration(TEST_NAME, new TestConfiguration(TEST_CLASS_DIR, TEST_NAME, new String[] {"B"}));
		if (TEST_CACHE_ENABLED) {
			setOutAndExpectedDeletionDisabled(true);
		}
	}

	@Test
	public void testDoubleinStringCP() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 3, 1, LopProperties.ExecType.CP);
	}

	@Test
	public void testDoubleinStringSpark() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 3, 1, LopProperties.ExecType.SPARK);
	}

	@Test
	public void testStringInDouble() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 3, 2, LopProperties.ExecType.CP);
	}

	@Test
	public void testStringInDoubleSpark() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 3, 2, LopProperties.ExecType.SPARK);
	}

	@Test
	public void testDoubleInFloat() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 5, 3, LopProperties.ExecType.CP);
	}

	@Test
	public void testDoubleInFloatSpark() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 5, 3, LopProperties.ExecType.SPARK);
	}

	@Test
	public void testLongInInt() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 5, 4, LopProperties.ExecType.CP);
	}

	@Test
	public void testLongInIntSpark() {
		runIsCorrectTest(schemaStrings, rows, schemaStrings.length, 5, 4, LopProperties.ExecType.SPARK);
	}
	private void runIsCorrectTest(ValueType[] schema, int rows, int cols,
		int badValues, int test, LopProperties.ExecType et)
	{
		Types.ExecMode platformOld = setExecMode(et);
		boolean oldFlag = OptimizerUtils.ALLOW_ALGEBRAIC_SIMPLIFICATION;
		boolean sparkConfigOld = DMLScript.USE_LOCAL_SPARK_CONFIG;
		try {
			getAndLoadTestConfiguration(TEST_NAME);
			String HOME = SCRIPT_DIR + TEST_DIR;
			fullDMLScriptName = HOME + TEST_NAME + ".dml";
			programArgs = new String[] {"-args", input("A"), input("M"),
				String.valueOf(rows), Integer.toString(cols), output("B")};
			FrameBlock frame1 = new FrameBlock(schema);
			FrameWriter writer = FrameWriterFactory.createFrameWriter(OutputInfo.CSVOutputInfo);
			FrameBlock frame2 = new FrameBlock(UtilFunctions.nCopies(cols, Types.ValueType.STRING));
			String[] meta = new String[]{"FP64", "STRING"};

			initFrameDataString(frame1); // initialize a frame with one column

			switch (test) { //Double in String
				case 1:
					String[] S = new String[rows];
					Arrays.fill(S, "string_value");
					for (int i = 0; i < badValues; i++)
						S[i] = "0.1345672225";
					frame1.appendColumn(S);
					break;

				case 2: { // String in double
					double[][] D = getRandomMatrix(rows, 1, 1, 10, 0.7, 2373);
					String[] tmp1 = new String[rows];
					for (int i = 0; i < rows; i++)
						tmp1[i] = (String) UtilFunctions.doubleToObject(ValueType.STRING, D[i][0], false);
					frame1.appendColumn(tmp1);
					for (int i = 0; i < badValues; i++)
						frame1.set(i, 1, "string_value");
					meta[meta.length - 1] = "FP64";
					break;
				}
				case 3: {//Double in float
					double[][] D = getRandomMatrix(rows, 1, 1, 10, 0.7, 2373);
					String[] tmp1 = new String[rows];
					for (int i = 0; i < rows; i++)
						tmp1[i] = (String) UtilFunctions.doubleToObject(ValueType.STRING, D[i][0], false);
					frame1.appendColumn(tmp1);
					for (int i = 0; i < badValues; i++)
						frame1.set(i, 1,  "1234567890123456768E40");
					meta[meta.length - 1] = "FP32";
					break;
				}
				case 4: { // long in int
					String[] tmp1 = new String[rows];
					for (int i = 0; i < rows; i++)
						tmp1[i] = String.valueOf(i);
					for (int i = 0; i < badValues; i++)
						tmp1[i] = "12345678910111212";
					frame1.appendColumn(tmp1);
					meta[meta.length - 1] = "INT32";
					break;
				}
			}
			writer.writeFrameToHDFS(
				frame1.slice(0, rows - 1, 0, 1, new FrameBlock()),
				input("A"), rows, schema.length);

			frame2.appendRow(meta);
			writer.writeFrameToHDFS(frame2, input("M"), 1, schema.length);
			runTest(true, false, null, -1);
			FrameBlock frameout = readDMLFrameFromHDFS("B", InputInfo.BinaryBlockInputInfo);

			//read output data and compare results
			ArrayList<Object> data = new ArrayList<>();
			for (int i = 0; i < frameout.getNumRows(); i++)
				data.add(frameout.get(i, 1));

			int nullNum = Math.toIntExact(data.stream().filter(s -> s == null).count());
			//verify output schema
			Assert.assertEquals("Wrong result: " + nullNum + ".", badValues, nullNum);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			rtplatform = platformOld;
			DMLScript.USE_LOCAL_SPARK_CONFIG = sparkConfigOld;
			OptimizerUtils.ALLOW_ALGEBRAIC_SIMPLIFICATION = oldFlag;
			OptimizerUtils.ALLOW_AUTO_VECTORIZATION = true;
			OptimizerUtils.ALLOW_OPERATOR_FUSION = true;
		}
	}
	private  void initFrameDataString(FrameBlock frame1) {
		double[][] A = getRandomMatrix(rows, 1, Float.MAX_VALUE, Double.MAX_VALUE, 0.7, 2373);
		double[] tmp6 = new double[rows];
		for (int i = 0; i < rows; i++)
			tmp6[i] = (Double) UtilFunctions.doubleToObject(ValueType.FP64, A[i][0], false);
		frame1.appendColumn(tmp6);
	}
}
