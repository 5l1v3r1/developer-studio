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

package org.wso2.developerstudio.datamapper.diagram.custom.configuration.operators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.wso2.developerstudio.datamapper.Element;
import org.wso2.developerstudio.datamapper.Operator;
import org.wso2.developerstudio.datamapper.OperatorRightConnector;
import org.wso2.developerstudio.datamapper.SchemaDataType;
import org.wso2.developerstudio.datamapper.Split;
import org.wso2.developerstudio.datamapper.TreeNode;
import org.wso2.developerstudio.datamapper.diagram.custom.configuration.function.AssignmentStatement;

public class SplitTransformer extends OneToManyTransformer {

	private static final String INDEX = "[i]";

	@Override
	public AssignmentStatement transform(Operator operator) {
		AssignmentStatement assign = new AssignmentStatement();
		StringBuilder statement = new StringBuilder();
		ArrayList<Element> splitOutputs = getOutputElements(operator);
		// getOutputEObjects(operator);
		Element splitInput = getInputElement(operator);
		String index = "";
		if (splitInput.getFieldParent().getSchemaDataType().equals(SchemaDataType.ARRAY)) {
			index = INDEX;
		}
		Split split = (Split) operator;
		int i = 0;
		int splitArrayMapIndex = split.getArrayOutput() - 1;

		TreeNode rootForMap = getOutputElementParent(operator);

		/**
		 * due to there can be different requirement with number of output
		 * connectors, it needs to iterate through all output connectors and
		 * build config assignments
		 */
		for (Element output : splitOutputs) {

			// input parent name can be same as the output parent name. in that
			// case we needs to append "output" string to output parent name.
			String outputParentName = "";
			String inputParentName = "";
			if (output != null) {

				outputParentName = output.getFieldParent().getName();
				inputParentName = splitInput.getFieldParent().getName();
			}

			/*
			 * If input parameter and output parameter names are identical,
			 * append term 'output' to the output parameter as a convention.
			 */
			if (outputParentName.equals(inputParentName)) {
				outputParentName = "output" + WordUtils.capitalize(outputParentName);
			}

			if (output != null && splitArrayMapIndex != i) {
				if (split.getDelimiter() != null) {
					statement.append(getTreeHierarchy(output.getFieldParent(), rootForMap) + "." + output.getName() + " = " + inputParentName + index + "." + splitInput.getName() + ".split(\"" + split.getDelimiter() + "\")" + "[" + i + "];");
					statement.append(System.lineSeparator());
					statement.append("\t");
				} else {
					statement.append(getTreeHierarchy(output.getFieldParent(), rootForMap) + "." + output.getName() + " = " + inputParentName + index + "." + splitInput.getName() + ".split(\"\")" + "[" + i + "];");
					statement.append(System.lineSeparator());
					statement.append("\t");
				}
			} else if (output != null && splitArrayMapIndex == i) {

				statement.append(getTreeHierarchy(output.getFieldParent(), rootForMap) + "." + output.getName() + " = new Array();");
				statement.append(System.lineSeparator());

				StringBuilder builder = new StringBuilder();
				if (split.getDelimiter() != null) {
					builder.append("var unmappedResultArray = ").append(inputParentName).append(index).append(".").append(splitInput.getName()).append(".split(\"").append(split.getDelimiter()).append("\");");
				} else {
					builder.append("var unmappedResultArray = ").append(inputParentName).append(index).append(".").append(splitInput.getName()).append(".split(\"\");");
				}
				statement.append("\t");
				statement.append(builder.toString());
				statement.append(System.lineSeparator());

				List<Integer> mappedIndexes = getMappedOutputNodes(operator);
				builder.setLength(0);
				builder.append("var mappedIndexArray = new Array(");
				for (Integer each : mappedIndexes) {
					builder.append(each).append(",");
				}
				builder.deleteCharAt(builder.length() - 1);
				builder.append(");");
				statement.append("\t");
				statement.append(builder.toString());
				statement.append(System.lineSeparator());

				builder.setLength(0);
				builder.append("for (var j in mappedIndexArray ){ \n \t\t newArray.splice( mappedIndexArray[j],1); \n\t}");
				statement.append("\t");
				statement.append(builder.toString());
				statement.append(System.lineSeparator());

				statement.append("\t");
				statement.append(getTreeHierarchy(output.getFieldParent(), rootForMap)).append(".").append(output.getName()).append(" = unmappedResultArray;");
				statement.append(System.lineSeparator());
			}

			i++;
		}

		assign.setStatement(statement.toString());

		return assign;
	}

	/**
	 * mapped output nodes index needs when unmapped result array mapped to an
	 * element
	 * 
	 * @param operator
	 *            split operator
	 * @return unmapped output connector indexes of the operator
	 */
	private List<Integer> getMappedOutputNodes(Operator operator) {
		EList<OperatorRightConnector> rightConnectors = operator.getBasicContainer().getRightContainer().getRightConnectors();
		ArrayList<Integer> connectorList = new ArrayList<Integer>();
		int i = 0;
		int j = ((Split) operator).getArrayOutput() - 1;
		for (OperatorRightConnector connector : rightConnectors) {
			if (connector.getOutNode().getOutgoingLink().size() != 0 && j != i) {
				connectorList.add(i);
			}
			i++;
		}
		return connectorList;
	}

	private List<EObject> getOutputEObjects(Operator operator) {
		EList<OperatorRightConnector> rightConnectors = operator.getBasicContainer().getRightContainer().getRightConnectors();
		ArrayList<EObject> eObjectList = new ArrayList<EObject>();
		for (OperatorRightConnector connector : rightConnectors) {
			if (connector.getOutNode().getOutgoingLink().size() != 0) {
				if (connector.getOutNode().getOutgoingLink().get(0).getInNode().getElementParent() != null) {
				}
				// EObject nextOperator =
				// connector.getOutNode().getOutgoingLink().get(0).getInNode().eContainer().eContainer().eContainer().eContainer()
				// ;

			} else {
				eObjectList.add(null);
			}
		}
		return eObjectList;
	}

	private String getTreeHierarchy(TreeNode tree, TreeNode parent) {
		StringBuilder hierarchy = new StringBuilder();

		while (!(tree.equals(parent))) {
			hierarchy.insert(0, tree.getName());
			hierarchy.insert(0, ".");
			tree = tree.getFieldParent();
		}

		if (tree.getSchemaDataType().equals(SchemaDataType.ARRAY)) {
			hierarchy.insert(0, (tree.getName() + INDEX));
		} else {
			hierarchy.insert(0, tree.getName());
		}

		return hierarchy.toString();

	}

	@Override
	public TreeNode getInputElementParent(Operator operator) {
		return getInputElement(operator).getFieldParent();
	}
}
