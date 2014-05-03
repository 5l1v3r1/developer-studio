package org.wso2.developerstudio.datamapper.diagram.custom.configuration.operators;

import java.util.ArrayList;

import org.eclipse.emf.common.util.EList;
import org.wso2.developerstudio.datamapper.Element;
import org.wso2.developerstudio.datamapper.Operator;
import org.wso2.developerstudio.datamapper.OperatorRightConnector;
import org.wso2.developerstudio.datamapper.SchemaDataType;
import org.wso2.developerstudio.datamapper.TreeNode;
import org.wso2.developerstudio.datamapper.diagram.custom.configuration.function.AssignmentStatement;

public class OneToManyTransformer implements OperatorsTransformer{

	@Override
	public AssignmentStatement transform(Operator operator) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public TreeNode getOutputElementParent(Operator operator) {
		ArrayList<Element> elements = getOutputElements(operator);
		TreeNode highestParent = null;
		for (Element element : elements){
			if (element != null) {
				if (highestParent != null) {
					if (highestParent.getLevel() >= element.getFieldParent().getLevel()) {
						highestParent = element.getFieldParent();
					}
				} else {
					highestParent = element.getFieldParent();
				}
			}
		}
		
		if(getInputElement(operator).getFieldParent().getSchemaDataType().equals(SchemaDataType.ARRAY) && !(highestParent.getSchemaDataType().equals(SchemaDataType.ARRAY))){
			while(highestParent.getFieldParent() != null && !(highestParent.getSchemaDataType().equals(SchemaDataType.ARRAY))){
				highestParent = highestParent.getFieldParent();
			}
		}
		
		return highestParent;
	}

	@Override
	public TreeNode getInputElementParent(Operator operator) {
		// TODO Auto-generated method stub
		return null;
	}



	
	/**
	 * mapped output elements needs for create map statements
	 * 
	 * @param operator
	 *            split operator
	 * @return output elements which split results mapped
	 */
	protected ArrayList<Element> getOutputElements(Operator operator) {
		EList<OperatorRightConnector> rightConnectors = operator.getBasicContainer().getRightContainer().getRightConnectors();
		ArrayList<Element> elementList = new ArrayList<Element>();
		for (OperatorRightConnector connector : rightConnectors) {
			if (connector.getOutNode().getOutgoingLink().size() != 0) {
				elementList.add(connector.getOutNode().getOutgoingLink().get(0).getInNode().getElementParent());
			} else {
				elementList.add(null);
			}
		}
		return elementList;
	}
	
	/**
	 * mapped input element needs for create map statements
	 * 
	 * @param operator
	 *            split operator
	 * @return input element for split
	 */
	protected Element getInputElement(Operator operator) {
		return operator.getBasicContainer().getLeftContainer().getLeftConnectors().get(0).getInNode().getIncomingLink().get(0).getOutNode().getElementParent();
	}


	@Override
	public String trasnform(String statement, Operator operator, Operator nextOperator) {
		// TODO Auto-generated method stub
		return null;
	}

}
