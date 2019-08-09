/*
 * Modifications Copyright 2019 Graz University of Technology
 *
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


package org.tugraz.sysds.runtime.meta;

import org.tugraz.sysds.hops.OptimizerUtils;
import org.tugraz.sysds.runtime.matrix.data.MatrixBlock;

import java.util.Arrays;


public class MatrixCharacteristics extends DataCharacteristics
{
	private static final long serialVersionUID = 8300479822915546000L;

	private long numRows = -1;
	private long numColumns = -1;
	private int numRowsPerBlock = 1;
	private int numColumnsPerBlock = 1;
	private long nonZero = -1;
	private boolean ubNnz = false;
	
	public MatrixCharacteristics() {}
	
	public MatrixCharacteristics(long nr, long nc, long nnz) {
		set(nr, nc, -1, -1, nnz);
	}
	
	public MatrixCharacteristics(long nr, long nc, int bnr, int bnc) {
		set(nr, nc, bnr, bnc);
	}

	public MatrixCharacteristics(long nr, long nc, int bnr, int bnc, long nnz) {
		set(nr, nc, bnr, bnc, nnz);
	}
	
	public MatrixCharacteristics(DataCharacteristics that) {
		set(that);
	}

	@Override
	public DataCharacteristics set(long nr, long nc, int bnr, int bnc) {
		numRows = nr;
		numColumns = nc;
		numRowsPerBlock = bnr;
		numColumnsPerBlock = bnc;
		return this;
	}

	@Override
	public DataCharacteristics set(long nr, long nc, int bnr, int bnc, long nnz) {
		set(nr, nc, bnr, bnc);
		nonZero = nnz;
		ubNnz = false;
		return this;
	}

	@Override
	public DataCharacteristics set(DataCharacteristics that) {
		set(that.getRows(), that.getCols(), that.getRowsPerBlock(), that.getColsPerBlock(), that.getNonZeros());
		ubNnz = (that instanceof MatrixCharacteristics && ((MatrixCharacteristics)that).ubNnz);
		return this;
	}

	@Override
	public long getRows(){
		return numRows;
	}

	@Override
	public void setRows(long rlen) {
		numRows = rlen;
	}

	@Override
	public long getCols(){
		return numColumns;
	}

	@Override
	public void setCols(long clen) {
		numColumns = clen;
	}

	@Override
	public long getLength() {
		return numRows * numColumns;
	}

	@Override
	public int getRowsPerBlock() {
		return numRowsPerBlock;
	}

	@Override
	public void setRowsPerBlock( int brlen){
		numRowsPerBlock = brlen;
	}

	@Override
	public int getColsPerBlock() {
		return numColumnsPerBlock;
	}

	@Override
	public void setColsPerBlock( int bclen){
		numColumnsPerBlock = bclen;
	}

	@Override
	public long getNumBlocks() {
		return getNumRowBlocks() * getNumColBlocks();
	}

	@Override
	public long getNumRowBlocks() {
		//number of row blocks w/ awareness of zero rows
		return Math.max((long) Math.ceil((double)getRows() / getRowsPerBlock()), 1);
	}

	@Override
	public long getNumColBlocks() {
		//number of column blocks w/ awareness of zero columns
		return Math.max((long) Math.ceil((double)getCols() / getColsPerBlock()), 1);
	}
	
	@Override
	public String toString() {
		return "["+numRows+" x "+numColumns+", nnz="+nonZero+" ("+ubNnz+")"
		+", blocks ("+numRowsPerBlock+" x "+numColumnsPerBlock+")]";
	}

	@Override
	public void setDimension(long nr, long nc) {
		numRows = nr;
		numColumns = nc;
	}

	@Override
	public MatrixCharacteristics setBlockSize(int blen) {
		return setBlockSize(blen, blen);
	}

	@Override
	public MatrixCharacteristics setBlockSize(int bnr, int bnc) {
		numRowsPerBlock = bnr;
		numColumnsPerBlock = bnc;
		return this;
	}

	@Override
	public void setNonZeros(long nnz) {
		ubNnz = false;
		nonZero = nnz;
	}

	@Override
	public long getNonZeros() {
		return !ubNnz ? nonZero : -1;
	}

	@Override
	public void setNonZerosBound(long nnz) {
		ubNnz = true;
		nonZero = nnz;
	}

	@Override
	public long getNonZerosBound() {
		return nonZero;
	}

	@Override
	public double getSparsity() {
		return OptimizerUtils.getSparsity(this);
	}

	@Override
	public boolean dimsKnown() {
		return ( numRows >= 0 && numColumns >= 0 );
	}

	@Override
	public boolean dimsKnown(boolean includeNnz) {
		return ( numRows >= 0 && numColumns >= 0
			&& (!includeNnz || nnzKnown()));
	}

	@Override
	public boolean rowsKnown() {
		return ( numRows >= 0 );
	}

	@Override
	public boolean colsKnown() {
		return ( numColumns >= 0 );
	}

	@Override
	public boolean nnzKnown() {
		return ( !ubNnz && nonZero >= 0 );
	}

	@Override
	public boolean isUltraSparse() {
		return dimsKnown(true) && OptimizerUtils.getSparsity(this)
			< MatrixBlock.ULTRA_SPARSITY_TURN_POINT;
	}

	@Override
	public boolean mightHaveEmptyBlocks() {
		long singleBlk = Math.max(Math.min(numRows, numRowsPerBlock),1) 
				* Math.max(Math.min(numColumns, numColumnsPerBlock),1);
		return !nnzKnown() || numRows==0 || numColumns==0
			|| (nonZero < numRows*numColumns - singleBlk);
	}

	@Override
	public boolean equals (Object anObject) {
		if( !(anObject instanceof MatrixCharacteristics) )
			return false;
		MatrixCharacteristics mc = (MatrixCharacteristics) anObject;
		return ((numRows == mc.numRows)
			&& (numColumns == mc.numColumns)
			&& (numRowsPerBlock == mc.numRowsPerBlock)
			&& (numColumnsPerBlock == mc.numColumnsPerBlock)
			&& (nonZero == mc.nonZero));
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(new long[]{numRows,numColumns,
			numRowsPerBlock,numColumnsPerBlock,nonZero});
	}
}
