package com.ibm.bi.dml.hops;

import com.ibm.bi.dml.hops.OptimizerUtils.OptimizationType;
import com.ibm.bi.dml.lops.Aggregate;
import com.ibm.bi.dml.lops.Data;
import com.ibm.bi.dml.lops.Group;
import com.ibm.bi.dml.lops.Lops;
import com.ibm.bi.dml.lops.RangeBasedReIndex;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.parser.Expression.ValueType;
import com.ibm.bi.dml.runtime.matrix.MatrixCharacteristics;
import com.ibm.bi.dml.sql.sqllops.SQLLops;
import com.ibm.bi.dml.utils.HopsException;

//for now only works for range based indexing op
public class IndexingOp extends Hops {

	public static String OPSTRING = "Indexing";
	
	private boolean _rowLowerEqualsUpper = false, _colLowerEqualsUpper = false;
	
	public boolean getRowLowerEqualsUpper(){
		return _rowLowerEqualsUpper;
	}
	
	public boolean getColLowerEqualsUpper() {
		return _colLowerEqualsUpper;
	}
	
	public void setRowLowerEqualsUpper(boolean passed){
		_rowLowerEqualsUpper  = passed;
	}
	
	public void setColLowerEqualsUpper(boolean passed) {
		_colLowerEqualsUpper = passed;
	}
	
	//right indexing doesn't really need the dimensionality of the left matrix
	private static Lops dummy=new Data(null, Data.OperationTypes.READ, null, "-1", DataType.SCALAR, ValueType.INT, false);
	
	private IndexingOp() {
		//default constructor for clone
	}
	
	public IndexingOp(String l, DataType dt, ValueType vt, Hops inpMatrix, Hops inpRowL, Hops inpRowU, Hops inpColL, Hops inpColU, boolean passedRowsLEU, boolean passedColsLEU) {
		super(Kind.Indexing, l, dt, vt);
		/*
		if(inpRowL==null)
			inpRowL=new DataOp("1", DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, "1", -1, -1, -1, -1);
		if(inpRowU==null)
			inpRowU=new DataOp(Long.toString(get_dim1()), DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, Long.toString(get_dim1()), -1, -1, -1, -1);
		if(inpColL==null)
			inpColL=new DataOp("1", DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, "1", -1, -1, -1, -1);
		if(inpColU==null)
			inpColU=new DataOp(Long.toString(get_dim2()), DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, Long.toString(get_dim2()), -1, -1, -1, -1);
	*/
		getInput().add(0, inpMatrix);
		getInput().add(1, inpRowL);
		getInput().add(2, inpRowU);
		getInput().add(3, inpColL);
		getInput().add(4, inpColU);
		
		// create hops if one of them is null
		inpMatrix.getParent().add(this);
		inpRowL.getParent().add(this);
		inpRowU.getParent().add(this);
		inpColL.getParent().add(this);
		inpColU.getParent().add(this);
		
		// set information whether left indexing operation involves row (n x 1) or column (1 x m) matrix
		setRowLowerEqualsUpper(passedRowsLEU);
		setColLowerEqualsUpper(passedColsLEU);
	}

	public Lops constructLops()
			throws HopsException {
		if (get_lops() == null) {
			try {
				ExecType et = optFindExecType();
				if(et == ExecType.MR) {
					
					RangeBasedReIndex reindex = new RangeBasedReIndex(
							getInput().get(0).constructLops(), getInput().get(1).constructLops(), getInput().get(2).constructLops(),
							getInput().get(3).constructLops(), getInput().get(4).constructLops(), dummy, dummy,
							get_dataType(), get_valueType(), et);
	
					reindex.getOutputParameters().setDimensions(get_dim1(), get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
					
					reindex.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
					
					Group group1 = new Group(
							reindex, Group.OperationTypes.Sort, DataType.MATRIX,
							get_valueType());
					group1.getOutputParameters().setDimensions(get_dim1(),
							get_dim2(), get_rows_in_block(), get_cols_in_block(), getNnz());
					
					group1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
	
					Aggregate agg1 = new Aggregate(
							group1, Aggregate.OperationTypes.Sum, DataType.MATRIX,
							get_valueType(), et);
					agg1.getOutputParameters().setDimensions(get_dim1(),
							get_dim2(), get_rows_in_block(), get_cols_in_block(), getNnz());
	
					agg1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
					
					set_lops(agg1);
				}
				else {
					RangeBasedReIndex reindex = new RangeBasedReIndex(
							getInput().get(0).constructLops(), getInput().get(1).constructLops(), getInput().get(2).constructLops(),
							getInput().get(3).constructLops(), getInput().get(4).constructLops(), dummy, dummy,
							get_dataType(), get_valueType(), et);
					reindex.getOutputParameters().setDimensions(get_dim1(), get_dim2(),
							get_rows_in_block(), get_cols_in_block(), getNnz());
					reindex.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
					set_lops(reindex);
				}
			} catch (Exception e) {
				throw new HopsException(this.printErrorLocation() + "In IndexingOp Hop, error constructing Lops " , e);
			}

		}
		return get_lops();
	}

	@Override
	public String getOpString() {
		String s = new String("");
		s += OPSTRING;
		return s;
	}

	public void printMe() throws HopsException {
		if (get_visited() != VISIT_STATUS.DONE) {
			super.printMe();
			for (Hops h : getInput()) {
				h.printMe();
			}
			;
		}
		set_visited(VISIT_STATUS.DONE);
	}

	public SQLLops constructSQLLOPs() throws HopsException {
		throw new HopsException(this.printErrorLocation() + "constructSQLLOPs should not be called for IndexingOp Hop \n");
	}
	
	@Override
	public boolean allowsAllExecTypes()
	{
		return true;
	}
	
	@Override
	protected double computeOutputMemEstimate( long dim1, long dim2, long nnz )
	{		
		double sparsity = OptimizerUtils.getSparsity(dim1, dim2, nnz);
		return OptimizerUtils.estimateSizeExactSparsity(dim1, dim2, sparsity);
	}
	
	@Override
	protected double computeIntermediateMemEstimate( long dim1, long dim2, long nnz )
	{
		return 0;
	}
	
	@Override
	protected long[] inferOutputCharacteristics( MemoTable memo )
	{
		long[] ret = null;
		
		Hops input = getInput().get(0); //original matrix
		MatrixCharacteristics mc = memo.getAllInputStats(input);
		if( mc != null ) 
		{
			//worst-case is input size, but dense
			ret = new long[]{mc.get_rows(), mc.get_cols(), -1};
			if( _rowLowerEqualsUpper ) ret[0]=1;
			if( _colLowerEqualsUpper ) ret[1]=1;	
		}
		
		return ret;
	}

	@Override
	protected ExecType optFindExecType() throws HopsException {
		
		checkAndSetForcedPlatform();

		if( _etypeForced != null ) 			
			_etype = _etypeForced;
		else
		{	
			if ( OptimizerUtils.getOptType() == OptimizationType.MEMORY_BASED ) {
				_etype = findExecTypeByMemEstimate();
			}
			else if ( getInput().get(0).areDimsBelowThreshold() )
				_etype = ExecType.CP;
			else
				_etype = ExecType.MR;
			
			//mark for recompile (forever)
			if( OptimizerUtils.ALLOW_DYN_RECOMPILATION && !dimsKnown() && _etype==ExecType.MR )
				setRequiresRecompile();
		}
		return _etype;
	}
	
	@Override
	public void refreshSizeInformation()
	{
		//TODO MB: generalize this.
		
		Hops input1 = getInput().get(0); //original matrix
		Hops input2 = getInput().get(1); //inpRowL
		Hops input3 = getInput().get(2); //inpRowU
		Hops input4 = getInput().get(3); //inpColL
		Hops input5 = getInput().get(4); //inpColU
		
		//parse input information
		boolean allRows = false;
		if( input2 instanceof LiteralOp )
		{
			if ( ((LiteralOp)input2).get_name().equals("1") && input3 instanceof UnaryOp && ((UnaryOp)input3).get_op() == OpOp1.NROW )
				allRows = true;
		}	
		
		boolean allCols = false;
		if( input4 instanceof LiteralOp )
		{
			if ( ((LiteralOp)input4).get_name().equals("1") && input5 instanceof UnaryOp && ((UnaryOp)input5).get_op() == OpOp1.NCOL )
				allCols = true;
		}
		
		//set dimension information
		if( _rowLowerEqualsUpper )    set_dim1(1);
		else if( allRows ) set_dim1(input1.get_dim1());
		if( _colLowerEqualsUpper )    set_dim2(1);
		else if( allCols ) set_dim2(input1.get_dim2());
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException 
	{
		IndexingOp ret = new IndexingOp();	
		
		//copy generic attributes
		ret.clone(this, false);
		
		//copy specific attributes

		return ret;
	}
}
