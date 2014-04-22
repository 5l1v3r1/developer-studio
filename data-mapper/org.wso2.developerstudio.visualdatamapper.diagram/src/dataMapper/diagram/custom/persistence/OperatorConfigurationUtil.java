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

import org.apache.commons.lang.WordUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import dataMapper.Element;
import dataMapper.Operator;
import dataMapper.TreeNode;
import dataMapper.diagram.custom.configuration.function.Function;

public class OperatorConfigurationUtil {
	public static String oneToOneMapping(Object input) {
		return null;

	}
	
	
	public static String concatMapping(Object input) {
		return null;
	}

	/**util method for get mapped output tree element
	 * @param inputElement input tree element which is mapped
	 * @return	element
	 */
	public static Element getSimpleMapOutputElement(Element inputElement) {
		return inputElement.getOutNode().getOutgoingLink().get(0).getInNode()
				.getElementParent();
	}

	/**util method for get tree hierarchy to the given element 
	 * @param element which is child element of tree hierarchy
	 * @return	String which gives tree hierarchy
	 */
	public static String getElementHierarchy(Element element) {
		return getSuperparentName(element.getFieldParent()) +"."+element.getName();
	}
	
	/**util method for get one to one map assignment statement
	 * @param inputElement input tree element which is mapped
	 * @return	String one to one map assignment statement
	 */
	public static String getSimpleMappingStatement(Element inputElement) {
		if(isSimpleMap(inputElement)){
			return getElementParentName(getSimpleMapOutputElement(inputElement))+"." +  getSimpleMapOutputElement(inputElement).getName()+ " = " + inputElement.getFieldParent().getName()+ "."+ inputElement.getName()+";";
		}
		return null;
	}
	
	/**util method for check whether an element in one to one map 
	 * @param inputElement element which need to be check for map 
	 * @return	boolean is it one to one map or not
	 */
	public static boolean isSimpleMap(Element inputElement) {
		if(isMaped(inputElement)){
			if(inputElement.getOutNode().getOutgoingLink().get(0).getInNode().getElementParent() != null)
				return true;
		}
		
		return false;
	}
	
	
	public static String getElementParentName(Element element) {
		return element.getFieldParent().getName();
	}
	
	/**util method for find wheather an element is mapped
	 * @param inputElement element which need to be check for map 
	 * @return	boolean is it maped or not
	 */
	public static boolean isMaped(Element inputElement) {
		if(inputElement.getOutNode().getOutgoingLink().size() != 0)
			return true;
		return false;
	}

	
	private static String getSuperparentName(TreeNode node) {
		String paramName = "";
		if (node.getFieldParent() != null) {
			paramName = node.getName();
			while (node.getFieldParent() != null) {
				String name = node.getFieldParent().getName();
				paramName = name + "." + paramName;
				node = node.getFieldParent();
			}
		} else {
			paramName = node.getName();
		}

		return paramName;
	}
	
	public static String jsFunction(String input, String output, ArrayList<String> assignments, boolean recursive) {
		if(recursive){

			String function = "function map_L_"+input+"_L_"+output+"(" + input + ", " + output + "){\n";
			for(String assignment : assignments){
				function.concat(assignment).concat("\n");
			}
			return function.concat("}");
		}
		

		String function = "function map_S_"+input+"_S_"+output+"(" + input + ", " + output + "){\n";
		for(String assignment : assignments){
			function.concat(assignment).concat("\n");
		}
		return function.concat("}");
	}
	
	public static String getForLoop() {
		return null;
	}

	
	/**util method for find wheather  child-elements/child-tree is mapped of a tree node
	 * @param tree  node which need to be check for map 
	 * @return	boolean is it maped or not
	 */
	public static boolean isChildrenMaped(TreeNode tree) {
		for(Element element : tree.getElement()){
			if (isMaped(element)){
				return true;
			}
		}
		EList<TreeNode> trees = tree.getNode();
		for(TreeNode childTree : trees){
			if(isChildrenMaped(childTree)){
				return true;
			}
		}
		
		return false;
	}
	
	/**util method for find wheather  child-elements is mapped of a tree node
	 * @param tree  node which need to be check for map 
	 * @return	boolean is it maped or not
	 */
	public static boolean isChildrenElementMaped(TreeNode tree) {
		for(Element element : tree.getElement()){
			if (isMaped(element)){
				return true;
			}
		}
		return false;
	}
	

	public static EObject getOperatorClass(Element element) {
		return element.getOutNode().getOutgoingLink().get(0).getInNode().eContainer().eContainer().eContainer().eContainer();
	}
	
	/**util method for find wheather exist faunction for a specific tree node mapping
	 * @param functionForElement  new function created
	 * @param functionListForTree all exisiting functions
	 * @return null if does not exisit, else return equal function 
	 */
	public static  Function isFunctionExisit(Function functionForElement,
			List<Function> functionListForTree) {
		for(Function function : functionListForTree){
			if((function.getInputParameter().getName().equals(functionForElement.getInputParameter().getName()) )&& (function.getOutputParameter().getName().equals(functionForElement.getOutputParameter().getName()))){
				return function;
			}
			
		}
		return null;
	}
	
	/**util method for create simple map staement with indexing
	 * @param element input tree node child element which one to one map
	 * @return	assignment statement with arrya indexed for each in/out nodes
	 */
	public static String getSimpleArrayMappingStatement(Element element) {
		if(isSimpleMap(element)){
			String inputParentName = getElementParentName(getSimpleMapOutputElement(element));
			String outputParentName = element.getFieldParent().getName();
			
			/*
			 * If input parameter and output parameter names are identical,
			 * append term 'output' to the output parameter as a convention.
			 */
			if (inputParentName.equals(outputParentName)) {
				outputParentName = "output" + WordUtils.capitalize(outputParentName);
			}
			
			return outputParentName + "[i]." +  getSimpleMapOutputElement(element).getName()+ " = " + inputParentName + "[i]."+ element.getName() + ";";
		}
		return null;
	}
	
	/**util method for get mapped operator eObject of a element
	 * @param element  which is mapped to operator
	 * @return	eObject operator object
	 */
	public static Operator getMappingOperator(Element inputElement) {
		EObject eObject = inputElement.getOutNode().getOutgoingLink().get(0).getInNode().eContainer().eContainer().eContainer().eContainer();
		Operator opertor = (Operator) eObject;
		return opertor;
	}
	
	

}
