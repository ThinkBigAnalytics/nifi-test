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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.*;

public class XmlUtils {

    public static File editXml(File input, File output, DocumentChangeCallback editCallback) {

      try(FileInputStream inputStream = new FileInputStream(input)) {

          DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
          Document document = documentBuilder.parse(new InputSource(inputStream));

          document = editCallback.edit(document);

          // save the result
          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          Transformer transformer = transformerFactory.newTransformer();
          transformer.transform(new DOMSource(document), new StreamResult(output));

          return output;


      } catch (Exception e) {
          throw new RuntimeException("Failed to change XML document: " + e.getMessage(), e);
      }


  }



//  public static void main(String[] args) throws Exception {
//    Document doc = DocumentBuilderFactory.newInstance()
//        .newDocumentBuilder().parse(new InputSource(inputFile));
//
//    // locate the node(s)
//    XPath xpath = XPathFactory.newInstance().newXPath();
//    NodeList nodes = (NodeList)xpath.evaluate
//        ("//employee/name[text()='John']", doc, XPathConstants.NODESET);
//
//    // make the change
//    for (int idx = 0; idx < nodes.getLength(); idx++) {
//      Node item = nodes.item(idx);
//      item.setTextContent("John Paul");
//    }
//
//    // save the result
//    Transformer xformer = TransformerFactory.newInstance().newTransformer();
//    xformer.transform
//        (new DOMSource(doc), new StreamResult(new File(outputFile)));
//  }
}