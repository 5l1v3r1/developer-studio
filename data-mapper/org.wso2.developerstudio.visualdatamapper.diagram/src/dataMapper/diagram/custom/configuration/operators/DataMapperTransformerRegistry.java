package dataMapper.diagram.custom.configuration.operators;

import java.util.HashMap;
import java.util.Map;

import dataMapper.Concat;
import dataMapper.Constant;
import dataMapper.Contains;
import dataMapper.Equal;
import dataMapper.LowerCase;
import dataMapper.Operator;
import dataMapper.Split;
import dataMapper.UpperCase;

public class DataMapperTransformerRegistry {

	private static DataMapperTransformerRegistry singleton;

	/**
	 * Visual model type to transformers map.
	 */
	private Map<Class<?>, OperatorsTransformer> transformersMap;

	/**
	 * @return singleton instance.
	 */
	public static DataMapperTransformerRegistry getInstance() {
		if (null == singleton) {
			singleton = new DataMapperTransformerRegistry();
		}
		return singleton;
	}
	/**
	 * All config generation logics implemented class must map to relevant operator classes.
	 * 
	 */
	private DataMapperTransformerRegistry() {
		transformersMap = new HashMap<Class<?>, OperatorsTransformer>();
		addTransformer(Concat.class, new ConcatTransform());
		addTransformer(Split.class, new SplitTransform());
		addTransformer(LowerCase.class, new ToLowerCaseTransform());
		addTransformer(UpperCase.class, new ToUpperCaseTransformer());
		addTransformer(Contains.class, new ContainsTransformer());
		addTransformer(Constant.class, new ConstantTransformer());

	}

	public <K extends Operator> void addTransformer(Class<K> visualModelClass,
			OperatorsTransformer transformer) {
		transformersMap.put(visualModelClass, transformer);
	}

	/**
	 * 
	 * @param operator operator object which needs to find relevant config generation object
	 * @return relevant config generation object
	 */
	public <K extends Operator> OperatorsTransformer getTransformer(K operator) {
		return transformersMap.get(operator.eClass().getInstanceClass());
	}
}
