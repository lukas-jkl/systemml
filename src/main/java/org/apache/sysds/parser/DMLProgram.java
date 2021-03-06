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

package org.apache.sysds.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sysds.runtime.controlprogram.Program;



public class DMLProgram 
{
	private ArrayList<StatementBlock> _blocks;
	private HashMap<String, FunctionStatementBlock> _functionBlocks;
	private HashMap<String,DMLProgram> _namespaces;
	public static final String DEFAULT_NAMESPACE = ".defaultNS";
	public static final String INTERNAL_NAMESPACE = "_internal"; // used for multi-return builtin functions
	
	public DMLProgram(){
		_blocks = new ArrayList<>();
		_functionBlocks = new HashMap<>();
		_namespaces = new HashMap<>();
	}
	
	public DMLProgram(String namespace) {
		this();
		_namespaces.put(namespace, new DMLProgram());
	}
	
	public HashMap<String,DMLProgram> getNamespaces(){
		return _namespaces;
	}

	public void addStatementBlock(StatementBlock b){
		_blocks.add(b);
	}
	
	public int getNumStatementBlocks(){
		return _blocks.size();
	}

	/**
	 * 
	 * @param fkey   function key as concatenation of namespace and function name 
	 *               (see DMLProgram.constructFunctionKey)
	 * @return function statement block
	 */
	public FunctionStatementBlock getFunctionStatementBlock(String fkey) {
		String[] tmp = splitFunctionKey(fkey);
		return getFunctionStatementBlock(tmp[0], tmp[1]);
	}
	
	public void removeFunctionStatementBlock(String fkey) {
		String[] tmp = splitFunctionKey(fkey);
		removeFunctionStatementBlock(tmp[0], tmp[1]);
	}
	
	public FunctionStatementBlock getFunctionStatementBlock(String namespaceKey, String functionName) {
		DMLProgram namespaceProgram = this.getNamespaces().get(namespaceKey);
		if (namespaceProgram == null)
			return null;
	
		// for the namespace DMLProgram, get the specified function (if exists) in its current namespace
		FunctionStatementBlock retVal = namespaceProgram._functionBlocks.get(functionName);
		return retVal;
	}
	
	public void removeFunctionStatementBlock(String namespaceKey, String functionName) {
		DMLProgram namespaceProgram = this.getNamespaces().get(namespaceKey);
		// for the namespace DMLProgram, get the specified function (if exists) in its current namespace
		if (namespaceProgram != null)
			namespaceProgram._functionBlocks.remove(functionName);
	}
	
	public HashMap<String, FunctionStatementBlock> getFunctionStatementBlocks(String namespaceKey) {
		DMLProgram namespaceProgram = this.getNamespaces().get(namespaceKey);
		if (namespaceProgram == null){
			throw new LanguageException("ERROR: namespace " + namespaceKey + " is undefined");
		}
		// for the namespace DMLProgram, get the functions in its current namespace
		return namespaceProgram._functionBlocks;
	}
	
	public boolean hasFunctionStatementBlocks() {
		boolean ret = false;
		for( DMLProgram nsProg : _namespaces.values() )
			ret |= !nsProg._functionBlocks.isEmpty();
		
		return ret;
	}
	
	public ArrayList<FunctionStatementBlock> getFunctionStatementBlocks() {
		ArrayList<FunctionStatementBlock> ret = new ArrayList<>();
		for( DMLProgram nsProg : _namespaces.values() )
			ret.addAll(nsProg._functionBlocks.values());
		return ret;
	}
	
	public Map<String,FunctionStatementBlock> getNamedNSFunctionStatementBlocks() {
		Map<String, FunctionStatementBlock> ret = new HashMap<>();
		for( DMLProgram nsProg : _namespaces.values() )
		for( Entry<String, FunctionStatementBlock> e : nsProg._functionBlocks.entrySet() )
			ret.put(e.getKey(), e.getValue());
		return ret;
	}
	
	public Map<String,FunctionStatementBlock> getNamedFunctionStatementBlocks() {
		Map<String, FunctionStatementBlock> ret = new HashMap<>();
		for( Entry<String, FunctionStatementBlock> e : _functionBlocks.entrySet() )
			ret.put(e.getKey(), e.getValue());
		return ret;
	}

	public boolean containsFunctionStatementBlock(String name) {
		return _functionBlocks.containsKey(name);
	}
	
	public void addFunctionStatementBlock(String fname, FunctionStatementBlock fsb) {
		_functionBlocks.put(fname, fsb);
	}
	
	public void addFunctionStatementBlock( String namespace, String fname, FunctionStatementBlock fsb ) {
		DMLProgram namespaceProgram = this.getNamespaces().get(namespace);
		if (namespaceProgram == null)
			throw new LanguageException( "Namespace does not exist." );
		namespaceProgram._functionBlocks.put(fname, fsb);
	}
	
	public ArrayList<StatementBlock> getStatementBlocks(){
		return _blocks;
	}
	
	public void setStatementBlocks(ArrayList<StatementBlock> passed){
		_blocks = passed;
	}
	
	public StatementBlock getStatementBlock(int i){
		return _blocks.get(i);
	}

	public void mergeStatementBlocks(){
		_blocks = StatementBlock.mergeStatementBlocks(_blocks);
	}
	
	public void hoistFunctionCallsFromExpressions() {
		try {
			//handle statement blocks of all functions
			for( FunctionStatementBlock fsb : getFunctionStatementBlocks() )
				StatementBlock.rHoistFunctionCallsFromExpressions(fsb);
			//handle statement blocks of main program
			ArrayList<StatementBlock> tmp = new ArrayList<>();
			for( StatementBlock sb : _blocks )
				tmp.addAll(StatementBlock.rHoistFunctionCallsFromExpressions(sb));
			_blocks = tmp;
		}
		catch(LanguageException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		// for each namespace, display all functions
		for (String namespaceKey : this.getNamespaces().keySet()){
			
			sb.append("NAMESPACE = " + namespaceKey + "\n");
			DMLProgram namespaceProg = this.getNamespaces().get(namespaceKey);
			
			
			sb.append("FUNCTIONS = ");
			
			for (FunctionStatementBlock fsb : namespaceProg._functionBlocks.values()){
				sb.append(fsb);
				sb.append(", ");
			}
			sb.append("\n");
			sb.append("********************************** \n");
		
		}
		
		sb.append("******** MAIN SCRIPT BODY ******** \n");
		for (StatementBlock b : _blocks){
			sb.append(b);
			sb.append("\n");
		}
		sb.append("********************************** \n");
		return sb.toString();
	}
	
	public static String constructFunctionKey(String fnamespace, String fname) {
		return fnamespace + Program.KEY_DELIM + fname;
	}
	
	public static String[] splitFunctionKey(String fkey) {
		return fkey.split(Program.KEY_DELIM);
	}
}

