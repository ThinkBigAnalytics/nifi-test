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

 
package com.thinkbiganalytics.nifi.test;

import java.io.File;

public class Constants {

    private static final String USER_HOME = System.getProperty("user.home");
    static final File OUTPUT_DIR = new File(USER_HOME + "/NiFiTest/NiFiReadTest");
    static final File NIFI_ZIP_FILE = new File("../nifi-1.6.0-bin.zip");
    static File FLOW_XML_FILE = new File(NiFiMockFlowTest.class.getResource("/flow.xml").getFile());
}
