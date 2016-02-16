/*
 * Copyright 2016 WSO2, Inc. (http://wso2.com)
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
package org.wso2.developerstudio.datamapper.diagram.custom.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.wso2.developerstudio.datamapper.DataMapperLink;
import org.wso2.developerstudio.datamapper.DataMapperRoot;
import org.wso2.developerstudio.datamapper.Input;
import org.wso2.developerstudio.datamapper.OperatorLeftConnector;
import org.wso2.developerstudio.datamapper.OperatorRightConnector;
import org.wso2.developerstudio.datamapper.Output;
import org.wso2.developerstudio.datamapper.SchemaDataType;
import org.wso2.developerstudio.datamapper.impl.ConcatImpl;
import org.wso2.developerstudio.datamapper.impl.ElementImpl;
import org.wso2.developerstudio.datamapper.impl.LowerCaseImpl;
import org.wso2.developerstudio.datamapper.impl.OperatorImpl;
import org.wso2.developerstudio.datamapper.impl.SplitImpl;
import org.wso2.developerstudio.datamapper.impl.TreeNodeImpl;
import org.wso2.developerstudio.datamapper.impl.UpperCaseImpl;

/**
 * This class represent the object model of the Data-Mapper Diagram.
 */
public class DataMapperDiagramModel {

    private List<DMVariable> variablesArray = new ArrayList<>();
    private List<Integer> inputVariablesArray = new ArrayList<>();
    private List<Integer> outputVariablesArray = new ArrayList<>();
    private List<DMOperation> operationsList = new ArrayList<>();
    private List<Integer> resolvedVariableArray = new ArrayList<>();
    private List<Integer> resolvedOutputVariableArray = new ArrayList<>();
    private List<OperatorImpl> graphOperationElements = new ArrayList<>();

    private List<ArrayList<Integer>> inputAdjList = new ArrayList<>();
    private List<ArrayList<Integer>> outputAdjList = new ArrayList<>();
    private List<Integer> executionSeq = new ArrayList<>();
    private String inputRootName;
    private String outputRootName;

    public DataMapperDiagramModel(DataMapperRoot rootDiagram) {
        setInputAndOutputRootNames(rootDiagram);
        populateOutputVariables(rootDiagram.getOutput());
        populateInputVariables(rootDiagram.getInput());
        resetDiagramTraversalProperties();
        updateExecutionSequence();
    }

    private void setInputAndOutputRootNames(DataMapperRoot rootDiagram) {
        EObject inRootElement = rootDiagram.getInput().eContents().get(0);
        if (inRootElement instanceof TreeNodeImpl) {
            setInputRootName(((TreeNodeImpl) inRootElement).getName());
        } else {
            throw new IllegalArgumentException("Invalid Input root element found");
        }
        EObject outRootElement = rootDiagram.getOutput().eContents().get(0);
        if (outRootElement instanceof TreeNodeImpl) {
            setOutputRootName(((TreeNodeImpl) outRootElement).getName());
        } else {
            throw new IllegalArgumentException("Invalid Output root element found");
        }

    }

    private void updateExecutionSequence() {
        List<Integer> unexecutedOperationList = new ArrayList<>();
        List<Integer> tempUnexecutedOperationList = new ArrayList<>();
        int numberOfOperations = operationsList.size();
        for (int i = 0; i < numberOfOperations; i++) {
            unexecutedOperationList.add(i);
        }
        while (executionSeq.size() < numberOfOperations) {
            for (int index = 0; unexecutedOperationList.size() > index; ++index) {
                if (operationIsExecutable(unexecutedOperationList.get(index))) {
                    executionSeq.add(unexecutedOperationList.get(index));
                    addOutputsToResolvedVariables(unexecutedOperationList.get(index));
                } else {
                    tempUnexecutedOperationList.add(unexecutedOperationList.get(index));
                }
            }
            unexecutedOperationList = tempUnexecutedOperationList;
            tempUnexecutedOperationList = new ArrayList<>();
        }
    }

    private void addOutputsToResolvedVariables(Integer index) {
        ArrayList<Integer> outputVariables = outputAdjList.get(index);
        for (Integer operationOutputVariable : outputVariables) {
            if (outputVariablesArray.indexOf(operationOutputVariable) >= 0) {
                resolvedOutputVariableArray.add(operationOutputVariable);
            } else {
                resolvedVariableArray.add(operationOutputVariable);
            }
        }
    }

    /**
     * Checks whether all input variables for the operation in inputAdjList are
     * in the resolvedVariable list
     * 
     * @param index
     * @return
     */
    private boolean operationIsExecutable(Integer index) {
        ArrayList<Integer> inputVariables = inputAdjList.get(index);
        for (Integer integer : inputVariables) {
            if (resolvedVariableArray.indexOf(integer) < 0) {
                return false;
            }
        }
        return true;
    }

    private void resetDiagramTraversalProperties() {
        for (OperatorImpl operation : graphOperationElements) {
            operation.setVisited(false);
            operation.setMarked(false);
            operation.setIndex(-1);
            operation.setPortVariableIndex(new ArrayList<Integer>());
        }
        graphOperationElements = new ArrayList<>();
    }

    private void populateInputVariables(Input input) {
        TreeIterator<EObject> variableIterator = input.eAllContents();
        Stack<EObject> parentVariableStack = new Stack<>();
        List<EObject> tempNodeArray = new ArrayList<>();
        for (Iterator<EObject> iterator = variableIterator; iterator.hasNext();) {
            EObject objectElement = (EObject) iterator.next();
            if (objectElement instanceof ElementImpl) {
                ElementImpl element = (ElementImpl) objectElement;
                if (element.getLevel() <= parentVariableStack.size()) {
                    while (parentVariableStack.size() >= element.getLevel()) {
                        parentVariableStack.pop();
                    }
                } else if (element.getLevel() > (parentVariableStack.size() + 1)) {
                    throw new IllegalArgumentException("Illegal element level detected : element level- "
                            + element.getLevel() + " , parents level- " + parentVariableStack.size());
                }
                int index = variablesArray.size();
                variablesArray.add(new DMVariable(getVariableName(DMVariableType.INPUT, parentVariableStack,
                        ((ElementImpl) objectElement).getName()), getUniqueId(objectElement), DMVariableType.INPUT,
                        index));
                ((ElementImpl) objectElement).setIndex(index);
                resolvedVariableArray.add(index);
                inputVariablesArray.add(index);
                tempNodeArray.add(element);
            } else if (objectElement instanceof TreeNodeImpl) {
                TreeNodeImpl element = (TreeNodeImpl) objectElement;
                if (element.getLevel() == parentVariableStack.size()) {
                    parentVariableStack.pop();
                    parentVariableStack.push(element);
                } else if (element.getLevel() > parentVariableStack.size()) {
                    parentVariableStack.push(element);
                } else {
                    while (parentVariableStack.size() >= element.getLevel()) {
                        parentVariableStack.pop();
                    }
                    parentVariableStack.push(element);
                }
            }
        }
        populateAdjacencyLists(tempNodeArray);
    }

    private void populateAdjacencyLists(List<EObject> tempNodeArray) {
        while (tempNodeArray.size() > 0) {
            EObject nextElement = tempNodeArray.remove(0);
            if (nextElement instanceof ElementImpl) {
                EList<DataMapperLink> outgoingLinks = ((ElementImpl) nextElement).getOutNode().getOutgoingLink();
                for (DataMapperLink dataMapperLink : outgoingLinks) {
                    EObject linkedNode = getLinkedElement(dataMapperLink);
                    if (linkedNode instanceof ElementImpl) {
                        operationsList.add(new DMOperation(DMOperatorType.DIRECT, getUniqueDirectId(linkedNode,
                                operationsList.size()), operationsList.size()));
                        outputAdjList.add(new ArrayList<Integer>());
                        outputAdjList.get(operationsList.size() - 1).add(((ElementImpl) linkedNode).getIndex());
                        inputAdjList.add(new ArrayList<Integer>());
                        inputAdjList.get(operationsList.size() - 1).add(((ElementImpl) nextElement).getIndex());
                    } else if (linkedNode instanceof OperatorImpl && !((OperatorImpl) linkedNode).isMarked()) {
                        ((OperatorImpl) linkedNode).setMarked(true);
                        tempNodeArray.add(linkedNode);
                    }
                }
            } else if (nextElement instanceof OperatorImpl && !((OperatorImpl) nextElement).isVisited()) {
                int index = operationsList.size();
                OperatorImpl operatorElement = (OperatorImpl) nextElement;
                DMOperation operator = new DMOperation(getOperatorType(operatorElement), getUniqueId(operatorElement),
                        index);
                operationsList.add(operator);
                graphOperationElements.add(operatorElement);
                ((OperatorImpl) nextElement).setIndex(index);

                outputAdjList.add(new ArrayList<Integer>());
                inputAdjList.add(new ArrayList<Integer>());
                // populate outputAdjList
                List<DataMapperLink> outlinks = getOutLinksOfOperator(operatorElement);
                int indexOfLink = 0;
                for (DataMapperLink dataMapperLink : outlinks) {
                    indexOfLink++;
                    EObject linkedElement = getLinkedElement(dataMapperLink);
                    if (linkedElement instanceof ElementImpl) {
                        outputAdjList.get(operatorElement.getIndex()).add(((ElementImpl) linkedElement).getIndex());
                    } else if (linkedElement instanceof OperatorImpl) {
                        OperatorImpl operationElement = (OperatorImpl) linkedElement;
                        if (!operationElement.isMarked()) {
                            tempNodeArray.add(linkedElement);
                            operationElement.setMarked(true);
                        }
                        String variablePrefix = operator.getOperatorType() + "_" + operator.getIndex() + "_"
                                + indexOfLink;
                        if (operatorElement.getPortVariableIndex().size() <= indexOfLink) {
                            int variableIndex = variablesArray.size();
                            DMVariable tempVar = new DMVariable(variablePrefix, getUniqueDirectId(operatorElement,
                                    indexOfLink), DMVariableType.INTERMEDIATE, variableIndex);
                            variablesArray.add(tempVar);
                            operatorElement.getPortVariableIndex().add(variableIndex);
                            outputAdjList.get(operator.getIndex()).add(variableIndex);
                            if (operationElement.isVisited()) {
                                inputAdjList.get(operationElement.getIndex()).add(variableIndex);
                            }
                        }
                    }
                }
                // populate inputAdjList
                List<DataMapperLink> inlinks = getInLinksOfOperator(operatorElement);
                for (DataMapperLink dataMapperLink : inlinks) {
                    EObject linkedElement = getPreviousLinkedElement(dataMapperLink);
                    if (linkedElement instanceof ElementImpl) {
                        inputAdjList.get(operatorElement.getIndex()).add(((ElementImpl) linkedElement).getIndex());
                    } else if (linkedElement instanceof OperatorImpl) {
                        OperatorImpl sourceElement = (OperatorImpl) linkedElement;
                        if (!sourceElement.isMarked()) {
                            sourceElement.setMarked(true);
                            tempNodeArray.add(sourceElement);
                        } else if (sourceElement.isVisited()) {
                            List<DataMapperLink> outLinksOfSourceElement = getOutLinksOfOperator(sourceElement);
                            int indexOfSourceLink = outLinksOfSourceElement.indexOf(dataMapperLink);
                            inputAdjList.get(operator.getIndex()).add(
                                    sourceElement.getPortVariableIndex().get(indexOfSourceLink));
                        }
                    }
                }
                operatorElement.setVisited(true);
            }
        }
    }

    private EObject getPreviousLinkedElement(DataMapperLink dataMapperLink) {
        EObject element = dataMapperLink.eContainer().eContainer();
        if (element instanceof ElementImpl) {
            return element;
        } else {
            while (!(element instanceof OperatorImpl)) {
                element = element.eContainer();
            }
            return element;
        }
    }

    private List<DataMapperLink> getInLinksOfOperator(OperatorImpl operatorElement) {
        EList<OperatorLeftConnector> leftContainers = operatorElement.getBasicContainer().getLeftContainer()
                .getLeftConnectors();
        List<DataMapperLink> linkList = new ArrayList<>();
        for (OperatorLeftConnector operatorLeftConnector : leftContainers) {
            linkList.addAll(operatorLeftConnector.getInNode().getIncomingLink());
        }
        return linkList;
    }

    private List<DataMapperLink> getOutLinksOfOperator(OperatorImpl operatorElement) {
        EList<OperatorRightConnector> rightContainers = operatorElement.getBasicContainer().getRightContainer()
                .getRightConnectors();
        List<DataMapperLink> linkList = new ArrayList<>();
        for (OperatorRightConnector operatorRightConnector : rightContainers) {
            linkList.addAll(operatorRightConnector.getOutNode().getOutgoingLink());
        }
        return linkList;
    }

    private String getUniqueDirectId(EObject parent, int size) {
        return parent.toString() + " " + size;
    }

    private String getUniqueId(EObject nextElement) {
        return nextElement.toString();
    }

    private DMOperatorType getOperatorType(OperatorImpl nextElement) {
        if (nextElement instanceof ConcatImpl) {
            return DMOperatorType.CONCAT;
        } else if (nextElement instanceof SplitImpl) {
            return DMOperatorType.SPLIT;
        } else if (nextElement instanceof UpperCaseImpl) {
            return DMOperatorType.UPPERCASE;
        } else if (nextElement instanceof LowerCaseImpl) {
            return DMOperatorType.LOWERCASE;
        } else {
            throw new IllegalArgumentException("Unknown operator detected : " + nextElement.toString());
        }
    }

    private EObject getLinkedElement(DataMapperLink dataMapperLink) {
        EObject element = dataMapperLink.getInNode().eContainer();
        if (element instanceof ElementImpl) {
            return element;
        } else {
            while (!(element instanceof OperatorImpl)) {
                element = element.eContainer();
            }
            return element;
        }
    }

    /**
     * This method will populate the outputVariables array field from diagram
     * output tree
     * 
     * @param output
     */
    private void populateOutputVariables(Output output) {
        TreeIterator<EObject> variableIterator = output.eAllContents();
        Stack<EObject> parentVariableStack = new Stack<EObject>();
        for (Iterator<EObject> iterator = variableIterator; iterator.hasNext();) {
            EObject objectElement = (EObject) iterator.next();
            if (objectElement instanceof ElementImpl) {
                ElementImpl element = (ElementImpl) objectElement;
                if (element.getLevel() <= parentVariableStack.size()) {
                    while (parentVariableStack.size() >= element.getLevel()) {
                        parentVariableStack.pop();
                    }
                } else if (element.getLevel() > (parentVariableStack.size() + 1)) {
                    throw new IllegalArgumentException("Illegal element level detected : element level- "
                            + element.getLevel() + " , parents level- " + parentVariableStack.size());
                }
                int index = variablesArray.size();
                variablesArray.add(new DMVariable(getVariableName(DMVariableType.OUTPUT, parentVariableStack,
                        ((ElementImpl) objectElement).getName()), objectElement.toString(), DMVariableType.OUTPUT,
                        index));
                outputVariablesArray.add(index);
                ((ElementImpl) objectElement).setIndex(index);
            } else if (objectElement instanceof TreeNodeImpl) {
                TreeNodeImpl element = (TreeNodeImpl) objectElement;
                if (element.getLevel() == parentVariableStack.size()) {
                    parentVariableStack.pop();
                    parentVariableStack.push(element);
                } else if (element.getLevel() > parentVariableStack.size()) {
                    parentVariableStack.push(element);
                } else {
                    while (parentVariableStack.size() >= element.getLevel()) {
                        parentVariableStack.pop();
                    }
                    parentVariableStack.push(element);
                }
            }
        }
    }

    private String getVariableName(DMVariableType prefix, Stack<EObject> parentVariableStack, String name) {
        String variableName = prefix.toString().toLowerCase();
        for (EObject eObject : parentVariableStack) {
            if (eObject instanceof TreeNodeImpl) {
                if (!((TreeNodeImpl) eObject).getSchemaDataType().equals(SchemaDataType.ARRAY)) {
                    variableName = variableName + ((TreeNodeImpl) eObject).getName() + ".";
                }
            } else if (eObject instanceof ElementImpl) {
                variableName = variableName + ((ElementImpl) eObject).getName() + ".";
            } else {
                throw new IllegalArgumentException("Illegal element type found : " + eObject.toString());
            }
        }
        return variableName + name;
    }

    public List<Integer> getInputVariablesArray() {
        return inputVariablesArray;
    }

    public void setInputVariablesArray(List<Integer> inputVariablesArray) {
        this.inputVariablesArray = inputVariablesArray;
    }

    public String getInputRootName() {
        return inputRootName;
    }

    public void setInputRootName(String inputRootName) {
        this.inputRootName = inputRootName;
    }

    public String getOutputRootName() {
        return outputRootName;
    }

    public void setOutputRootName(String outputRootName) {
        this.outputRootName = outputRootName;
    }

    public List<DMOperation> getOperationsList() {
        return operationsList;
    }

    public void setOperationsArray(List<DMOperation> operationsArray) {
        this.operationsList = operationsArray;
    }

    public List<Integer> getResolvedVariableArray() {
        return resolvedVariableArray;
    }

    public void setResolvedVariableArray(List<Integer> resolvedVariableArray) {
        this.resolvedVariableArray = resolvedVariableArray;
    }

    public List<Integer> getResolvedOutputVariableArray() {
        return resolvedOutputVariableArray;
    }

    public void setResolvedOutputVariableArray(List<Integer> resolvedOutputVariableArray) {
        this.resolvedOutputVariableArray = resolvedOutputVariableArray;
    }

    public List<ArrayList<Integer>> getInputAdjList() {
        return inputAdjList;
    }

    public void setInputAdjList(List<ArrayList<Integer>> inputAdjList) {
        this.inputAdjList = inputAdjList;
    }

    public List<ArrayList<Integer>> getOutputAdjList() {
        return outputAdjList;
    }

    public void setOutputAdjList(List<ArrayList<Integer>> outputAdjList) {
        this.outputAdjList = outputAdjList;
    }

    public List<OperatorImpl> getGraphOperationElements() {
        return graphOperationElements;
    }

    public List<Integer> getExecutionSequence() {
        return executionSeq;
    }

    public void setExecutionSeq(List<Integer> executionSeq) {
        this.executionSeq = executionSeq;
    }

    public List<DMVariable> getVariablesArray() {
        return variablesArray;
    }

    public void setVariablesArray(List<DMVariable> variablesArray) {
        this.variablesArray = variablesArray;
    }

    public void setOperationsList(List<DMOperation> operationsList) {
        this.operationsList = operationsList;
    }

}
