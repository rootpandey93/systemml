package dml.runtime.matrix.mapred;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import dml.runtime.instructions.MRInstructions.MRInstruction;
import dml.runtime.instructions.MRInstructions.TertiaryInstruction;
import dml.runtime.matrix.io.MatrixCell;
import dml.runtime.matrix.io.MatrixIndexes;
import dml.runtime.matrix.io.MatrixPackedCell;
import dml.runtime.matrix.io.MatrixValue;
import dml.runtime.matrix.io.TaggedMatrixValue;
import dml.runtime.matrix.io.MatrixValue.CellIndex;
import dml.utils.DMLRuntimeException;
import dml.utils.DMLUnsupportedOperationException;

public class GMRReducer extends ReduceBase
implements Reducer<MatrixIndexes, TaggedMatrixValue, MatrixIndexes, MatrixValue>{
	
	private MatrixValue realOutValue;
	private HashMap<Byte, HashMap<CellIndex, Double>> cacheForCtable=new HashMap<Byte, HashMap<CellIndex, Double>>();
	public void reduce(MatrixIndexes indexes,
			Iterator<TaggedMatrixValue> values,
			OutputCollector<MatrixIndexes, MatrixValue> out,
			Reporter reporter) throws IOException {
		
		long start=System.currentTimeMillis();
		commonSetup(reporter);
		
		cachedValues.reset();
	//	LOG.info("before aggregation: \n"+cachedValues);
		//perform aggregate operations first
		processAggregateInstructions(indexes, values);
		
//		LOG.info("after aggregation: \n"+cachedValues);
		
		//perform mixed operations
		try {
			processReducerInstructionsInGMR();
		} catch (Exception e) {
			throw new IOException(e);
		} 
		
//		LOG.info("after mixed operations: \n"+cachedValues);

		//output the final result matrices
		outputResultsFromCachedValuesForGMR(reporter);

		reporter.incrCounter(Counters.COMBINE_OR_REDUCE_TIME, System.currentTimeMillis()-start);
	}
	
	//process mixture of instructions
	protected void processReducerInstructionsInGMR() throws DMLUnsupportedOperationException, DMLRuntimeException 
	{
		if(mixed_instructions==null)
			return;
		for(MRInstruction ins: mixed_instructions)
		{
			if(ins instanceof TertiaryInstruction)
			{
				((TertiaryInstruction) ins).processInstruction(valueClass, cachedValues, zeroInput, cacheForCtable);
			}else
				processOneInstruction(ins, valueClass, cachedValues, tempValue, zeroInput);
		}
	}
	
	protected void outputResultsFromCachedValuesForGMR(Reporter reporter) throws IOException
	{
		for(int i=0; i<resultIndexes.length; i++)
		{
			byte output=resultIndexes[i];
			IndexedMatrixValue outValue=cachedValues.get(output);
			if(outValue==null)
				continue;
			if(valueClass.equals(MatrixPackedCell.class))
			{
				realOutValue.copy(outValue.getValue());
				collectOutput_N_Increase_Counter(outValue.getIndexes(), 
						realOutValue, i, reporter);
			}
			else
				collectOutput_N_Increase_Counter(outValue.getIndexes(), 
					outValue.getValue(), i, reporter);
	//		LOG.info("output: "+outValue.getIndexes()+" -- "+outValue.getValue()+" ~~ tag: "+output);
		//	System.out.println("Reducer output: "+outValue.getIndexes()+" -- "+outValue.getValue()+" ~~ tag: "+output);
		}
	}
	
	public void close()throws IOException
	{
		MatrixIndexes key=new MatrixIndexes();
		MatrixCell value=new MatrixCell();
		for(Entry<Byte, HashMap<CellIndex, Double>> ctable: cacheForCtable.entrySet())
		{
			Vector<Integer> resultIDs=getOutputIndexes(ctable.getKey());
			//long maxRows=Long.MIN_VALUE, maxCols=Long.MIN_VALUE, maxRowIndex=Long.MIN_VALUE, maxColIndex=Long.MIN_VALUE;
			for(Entry<CellIndex, Double> e: ctable.getValue().entrySet())
			{
				key.setIndexes(e.getKey().row, e.getKey().column);
				value.setValue(e.getValue());
				for(Integer i: resultIDs)
				{
					collectFinalMultipleOutputs.collectOutput(key, value, i, cachedReporter);
					resultsNonZeros[i]++;
					if ( resultDimsUnknown[i] == (byte) 1 ) {
						if(key.getRowIndex()>resultsMaxRowDims[i] )
							resultsMaxRowDims[i] = key.getRowIndex();
						if ( key.getColumnIndex() > resultsMaxColDims[i] )
							resultsMaxColDims[i] = key.getColumnIndex();
					}
				}
			}
		}
		super.close();
	}
	
	public void configure(JobConf job)
	{
		super.configure(job);
		try {
			realOutValue=valueClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		//this is to make sure that aggregation works for GMR
		if(valueClass.equals(MatrixCell.class))
			valueClass=MatrixPackedCell.class;
	}
}
