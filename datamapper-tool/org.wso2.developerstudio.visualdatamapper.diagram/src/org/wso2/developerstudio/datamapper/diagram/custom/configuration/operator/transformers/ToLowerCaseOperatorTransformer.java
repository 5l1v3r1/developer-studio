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
package org.wso2.developerstudio.datamapper.diagram.custom.configuration.operator.transformers;

import java.util.List;

import org.wso2.developerstudio.datamapper.diagram.custom.generator.SameLevelRecordMappingConfigGenerator;
import org.wso2.developerstudio.datamapper.diagram.custom.model.DMVariable;

public class ToLowerCaseOperatorTransformer implements DMOperatorTransformer {

    @Override
    public String generateScriptForOperation(Class<?> generatorClass, List<DMVariable> inputVariables) {
        StringBuilder operationBuilder = new StringBuilder();
        if (SameLevelRecordMappingConfigGenerator.class.equals(generatorClass)) {
            if (inputVariables.size() >= 1) {
                operationBuilder.append(inputVariables.get(0).getName() + ".toLowerCase();");
            } else {
                operationBuilder.append("'';");
            }
        } else {
            throw new IllegalArgumentException("Unknown MappingConfigGenerator type found : " + generatorClass);
        }
        return operationBuilder.toString();
    }
}
