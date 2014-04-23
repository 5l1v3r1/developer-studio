/*
 * Copyright 2014 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dataMapper.diagram.custom.persistence;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import dataMapper.DataMapperRoot;
import dataMapper.Element;
import dataMapper.Operator;
import dataMapper.SchemaDataType;
import dataMapper.TreeNode;
import dataMapper.diagram.custom.configuration.function.AssignmentStatement;
import dataMapper.diagram.custom.configuration.function.ForLoop;
import dataMapper.diagram.custom.configuration.function.Function;
import dataMapper.diagram.custom.configuration.function.FunctionBody;
import dataMapper.diagram.custom.configuration.operators.DataMapperTransformerRegistry;
import dataMapper.diagram.custom.configuration.operators.OperatorsTransformer;


public class MappingModelTraverser {

	private static MappingModelTraverser instance;
	private static List<Integer> operatorsList;

	private MappingModelTraverser() {
		
	}

	public static MappingModelTraverser getInstance() {
		if (instance == null) {
			instance = new MappingModelTraverser();
		}

		return instance;
	}

	public void traverse(DataMapperRoot rootDiagram,DataMapperConfiguration mappingConfig) {
		 Function mainFunction = createMainFunction(rootDiagram.getInput()
		 .getTreeNode().get(0), rootDiagram.getOutput().getTreeNode()
		 .get(0));

		 mappingConfig.getFunctionList().add(mainFunction);
		 operatorsList = new ArrayList<Integer>();
		 traverse(rootDiagram.getInput().getTreeNode().get(0), mappingConfig, mainFunction);

	}

	/**
	 * @param inputTreeNode 	main input tree
	 * @param outputTreeNode	main output tree
	 * @return	Main function for configuration
	 */
	private Function createMainFunction(TreeNode inputTreeNode,TreeNode outputTreeNode) {
		Function mainFunction = null;
		if (OperatorConfigurationUtil.isChildrenMaped(inputTreeNode)) {
			mainFunction = new Function();

			mainFunction.setInputParameter(inputTreeNode);
			mainFunction.setOutputParameter(outputTreeNode);
			mainFunction.setSingle(true);
			mainFunction.setMainFunction(true);

		}

		return mainFunction;
	}

	/**
	 * @param tree 				tree element which would be traverse
	 * @param config			full configuration 
	 * @param parentFunction	function which would be the function call statement execute
	 */
	private static void traverse(TreeNode tree, DataMapperConfiguration config, Function parentFunction) {
		List<Function> functionListForTree = new ArrayList<Function>();
		
		/*if (parentFunction.isMainFunction()) {
			functionListForTree.add(parentFunction);
		}*/

		for(Element element : tree.getElement()){
			if(OperatorConfigurationUtil.isMaped(element)){
				Function functionForElement = new Function();
				functionForElement.setInputParameter(tree);
				if(tree.getSchemaDataType().equals(SchemaDataType.ARRAY)){
					
					if(OperatorConfigurationUtil.isSimpleMap(element)){
						String mapAssignmennt = OperatorConfigurationUtil.getSimpleArrayMappingStatement(element);
						AssignmentStatement assign = new AssignmentStatement();
						assign.setStatement(mapAssignmennt);
						
						functionForElement.setOutputParameter(OperatorConfigurationUtil.getSimpleMapOutputElement(element).getFieldParent());
						functionForElement.setSingle(false);
						Function oldFunction = OperatorConfigurationUtil.isFunctionExisit(functionForElement , functionListForTree);
						
						if( oldFunction == null){
							ArrayList<AssignmentStatement> assignmentList = new ArrayList<AssignmentStatement>();
							assignmentList.add(assign);
							FunctionBody body = new FunctionBody();
							ForLoop loop = new ForLoop();
							loop.setArrayTree(tree);
							loop.setAssignmentStatements(assignmentList);
							ArrayList<ForLoop> forLoop = new ArrayList<ForLoop>();
							forLoop.add(loop);
							body.setForLoop(forLoop);
							functionForElement.setFunctionBody(body);
							functionListForTree.add(functionForElement);
						}
						else {
							oldFunction.getFunctionBody().getForLoop().get(0).getAssignmentStatements().add(assign);
						}
						
					}
					// array type element map with operators
					else {
						EObject eObjectoperator = OperatorConfigurationUtil.getOperatorClass(element);
						Operator operator = (Operator) eObjectoperator;
						if (operator != null) {
							if(!operatorsList.contains(System.identityHashCode(operator))){
								operatorsList.add(System.identityHashCode(operator));
								
								OperatorsTransformer transformer = DataMapperTransformerRegistry.getInstance().getTransformer(operator);
								AssignmentStatement assign = transformer.transform(operator); //FIXME wrong assignment get for array. should handle in each operator class
								functionForElement.setOutputParameter(transformer.getOutputElementParent(operator));
								functionForElement.setSingle(false);
								Function oldFunction = OperatorConfigurationUtil.isFunctionExisit(functionForElement , functionListForTree);
								
								if( oldFunction == null){
									ArrayList<AssignmentStatement> assignmentList = new ArrayList<AssignmentStatement>();
									FunctionBody body = new FunctionBody();
									assignmentList.add(assign);
									ForLoop loop = new ForLoop();
									loop.setArrayTree(tree);
									loop.setAssignmentStatements(assignmentList);
									ArrayList<ForLoop> forLoop = new ArrayList<ForLoop>();
									forLoop.add(loop);
									body.setForLoop(forLoop);

									functionForElement.setFunctionBody(body);
									functionListForTree.add(functionForElement);
								}
								else {
									oldFunction.getFunctionBody().getForLoop().get(0).getAssignmentStatements().add(assign);
								}
							}
						}	
					}
				}
				//Record type mapping
				else{
					//record type one to one mapping
					if(OperatorConfigurationUtil.isSimpleMap(element)){
						String mapAssignmennt = OperatorConfigurationUtil.getSimpleMappingStatement(element);
						AssignmentStatement assign = new AssignmentStatement();
						assign.setStatement(mapAssignmennt);
						
						functionForElement.setOutputParameter(OperatorConfigurationUtil.getSimpleMapOutputElement(element).getFieldParent());
						functionForElement.setSingle(true);
						Function oldFunction = OperatorConfigurationUtil.isFunctionExisit(functionForElement , functionListForTree);
						
						if( oldFunction == null){
							ArrayList<AssignmentStatement> assignmentList = new ArrayList<AssignmentStatement>();
							FunctionBody body = new FunctionBody();
							assignmentList.add(assign);
							body.setAssignmentStatements(assignmentList);
							functionForElement.setFunctionBody(body);
							functionListForTree.add(functionForElement);
						}
						else {
							oldFunction.getFunctionBody().getAssignmentStatements().add(assign);
						}
						
					}
					//record type with operator mapping
					else {
						EObject eObjectoperator = OperatorConfigurationUtil.getOperatorClass(element);
						Operator operator = (Operator) eObjectoperator;
						if(!operatorsList.contains(System.identityHashCode(operator))){
							operatorsList.add(System.identityHashCode(operator));
							
							OperatorsTransformer transformer = DataMapperTransformerRegistry.getInstance().getTransformer(operator);
							AssignmentStatement assign = transformer.transform(operator);
							functionForElement.setOutputParameter(transformer.getOutputElementParent(operator));
							functionForElement.setSingle(true);
							Function oldFunction = OperatorConfigurationUtil.isFunctionExisit(functionForElement , functionListForTree);
							
							if( oldFunction == null){
								ArrayList<AssignmentStatement> assignmentList = new ArrayList<AssignmentStatement>();
								FunctionBody body = new FunctionBody();
								assignmentList.add(assign);
								body.setAssignmentStatements(assignmentList);
								functionForElement.setFunctionBody(body);
								functionListForTree.add(functionForElement);
							}
							else {
								oldFunction.getFunctionBody().getAssignmentStatements().add(assign);
							}
						}
					}
				}
			}
		}//for element end
	
		//all functions for the current tree node should copy to DataMapperConfiguration
		if(!functionListForTree.isEmpty()){
			config.getFunctionList().addAll(functionListForTree);
			for (Function function : functionListForTree) {
				function.setParentFunction(parentFunction);
				//set function call statement for appropriate parent function
				
					parentFunction.getFunctionBody().getFunctionCallStatements().add(function.getFunctionCall());
				
			}
		}

		//iterate in child trees
		if(tree.getNode() != null){
			for(TreeNode childTree : tree.getNode()){
				if(OperatorConfigurationUtil.isChildrenElementMaped(childTree)){
					if(functionListForTree.size() != 0){
						traverse(childTree , config, functionListForTree.get(0));						
					}
					else {
						traverse(childTree, config, parentFunction);
					}
				}
				else{
					traverse(childTree, config, parentFunction);
				}
			}
		}
		
	}

}
