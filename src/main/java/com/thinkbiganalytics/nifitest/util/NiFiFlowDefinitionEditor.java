/*
 * Copyright (c) 2018 ThinkBig Analytics, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 
package com.thinkbiganalytics.nifitest.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.LinkedList;

public class NiFiFlowDefinitionEditor implements DocumentChangeCallback {


    private final LinkedList<DocumentChangeCallback> delegateActions;

    private NiFiFlowDefinitionEditor(LinkedList<DocumentChangeCallback> delegateActions) {
        this.delegateActions = delegateActions;
    }

    @Override
    public Document edit(Document document) throws Exception {

        for (DocumentChangeCallback change : delegateActions) {
            document = change.edit(document);
        }

        return document;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Builder() {
            // no external instance
        }

        XPath xpath = XPathFactory.newInstance().newXPath();
        private final LinkedList<DocumentChangeCallback> actions = new LinkedList<>();

        public Builder rawXmlChange(DocumentChangeCallback documentChangeCallback) {
            actions.addLast(documentChangeCallback);
            return this;
        }

        public Builder setSingleProcessorProperty(String processorName, String propertyName, String newValue) {

            return rawXmlChange(document -> {
                String xpathString = "//processor[name/text() = '" + processorName
                        + "']/property[name/text() = '" + propertyName + "']/value";

                Node propertyValueNode = (Node) xpath.evaluate(xpathString, document, XPathConstants.NODE);

                if (propertyValueNode == null) {
                    throw new IllegalArgumentException("Reference to processor '"+ processorName +"' with property '"
                            + propertyName + "' not found: " + xpathString);
                }

                propertyValueNode.setTextContent(newValue);

                return document;
            });


        }

        public Builder setClassOfSingleProcessor(String processorName, String newFullyQualifiedClassName) {

            return rawXmlChange(document -> {
                String xpathString = "//processor[name/text() = '" + processorName + "']/class";

                Node classNameNode = (Node) xpath.evaluate(xpathString, document, XPathConstants.NODE);

                if (classNameNode == null) {
                    throw new IllegalArgumentException("Reference to processor '"+ processorName +" not found: " +
                            xpathString);
                }

                classNameNode.setTextContent(newFullyQualifiedClassName);

                return document;
            });


        }

        public Builder setClassOfSingleProcessor(String processorName, Class<?> mockProcessor) {

            return setClassOfSingleProcessor(processorName, mockProcessor.getName());
        }


        public NiFiFlowDefinitionEditor build() {
            return new NiFiFlowDefinitionEditor(actions);
        }

    }
}
