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

package com.ibm.bi.dml.lops;

import com.ibm.bi.dml.hops.AggBinaryOp.SparkAggType;
import com.ibm.bi.dml.lops.LopProperties.ExecLocation;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.lops.compile.JobType;
import com.ibm.bi.dml.parser.Expression.*;


/**
 * Lop to perform cross product operation
 */
public class MMCJ extends Lop 
{

		
	public enum MMCJType {
		AGG,
		NO_AGG,
	}
	
	//optional attribute for mr exec type
	private MMCJType _type = MMCJType.AGG;
	
	//optional attribute for spark exec type
	private SparkAggType _aggtype = SparkAggType.MULTI_BLOCK;
		
	/**
	 * Constructor to perform a cross product operation.
	 * @param et 
	 * @param input
	 * @param op
	 */

	public MMCJ(Lop input1, Lop input2, DataType dt, ValueType vt, MMCJType type, ExecType et) 
	{
		super(Lop.Type.MMCJ, dt, vt);		
		this.addInput(input1);
		this.addInput(input2);
		input1.addOutput(this);
		input2.addOutput(this);
		
		_type = type;
		
		if( et == ExecType.MR )
		{
			boolean breaksAlignment = true;
			boolean aligner = false;
			boolean definesMRJob = true;
			lps.addCompatibility(JobType.MMCJ);
			this.lps.setProperties( inputs, ExecType.MR, ExecLocation.MapAndReduce, breaksAlignment, aligner, definesMRJob );
			this.lps.setProducesIntermediateOutput(true);
		}
		else //if( et == ExecType.SPARK )
		{
			boolean breaksAlignment = false;
			boolean aligner = false;
			boolean definesMRJob = false;
			lps.addCompatibility(JobType.INVALID);
			lps.setProperties( inputs, et, ExecLocation.ControlProgram, breaksAlignment, aligner, definesMRJob );
		}
	}

	/**
	 * 
	 * @param input1
	 * @param input2
	 * @param dt
	 * @param vt
	 * @param aggtype
	 * @param et
	 */
	public MMCJ(Lop input1, Lop input2, DataType dt, ValueType vt, SparkAggType aggtype, ExecType et) 
	{
		this(input1, input2, dt, vt, MMCJType.NO_AGG, et);
		
		_aggtype = aggtype;
	}
	
	
	@Override
	public String toString() {
	
		return "Operation = MMCJ";
	}

	//MR instruction generation
	@Override
	public String getInstructions(int input_index1, int input_index2, int output_index)
	{
		StringBuilder sb = new StringBuilder();
		sb.append( getExecType() );
		sb.append( Lop.OPERAND_DELIMITOR );
		sb.append( "cpmm" );
		sb.append( OPERAND_DELIMITOR );
		
		sb.append( getInputs().get(0).prepInputOperand(input_index1));
		sb.append( OPERAND_DELIMITOR );
		sb.append( getInputs().get(1).prepInputOperand(input_index2));
		sb.append( OPERAND_DELIMITOR );
		sb.append( this.prepOutputOperand(output_index));
		
		sb.append( OPERAND_DELIMITOR );
		sb.append( _type );
		
		
		return sb.toString();
	}

	//SPARK instruction generation
	@Override
	public String getInstructions(String input1, String input2, String output)
	{
		StringBuilder sb = new StringBuilder();
		sb.append( getExecType() );
		sb.append( Lop.OPERAND_DELIMITOR );
		sb.append( "cpmm" );
		sb.append( OPERAND_DELIMITOR );
		
		sb.append( getInputs().get(0).prepInputOperand(input1) );
		sb.append( OPERAND_DELIMITOR );
		sb.append( getInputs().get(1).prepInputOperand(input2) );
		sb.append( OPERAND_DELIMITOR );
		sb.append( this.prepOutputOperand(output) );
		
		sb.append( OPERAND_DELIMITOR );
		sb.append( _aggtype );
				
		return sb.toString();
	}
}