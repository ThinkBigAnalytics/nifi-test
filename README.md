# NiFi Test Harness Proof-of-Concept

A Test Harness to boot a clear NiFi instance with a flow definition installed and perform 
asserts regarding the behaviour of the flow just like a Java unit test.

## Status

This whole project is in its infancy, with a number of limitations, but proves that 
a NiFi flow can programmatically be tested. Anyone is welcome to contribute / join. :) 

## Introduction

We have a small NiFi flow in `src/test/resources/flow.xml`, which contains two nodes: 
a `GetHTTP`, which reads `http://feeds.bbci.co.uk/news/technology/rss.xml?edition=uk#`
and then stores the result in the user's home directory in `NiFiTest/NiFiReadTest`

A simple behaviour, which still could only be tested manually, by installing the flow
to NiFi and, checking the results manually.

The integration test in `src/test/java/com/thinkbiganalytics/nifi/test/NiFiFlowTest.java` 
expects a NiFi 1.6 binary ZIP installer package to be placed to 
`nifi-1.6.0-bin.zip` inside the project directory.

```
  _____ __  __ _____   ____  _____ _______       _   _ _______  
 |_   _|  \/  |  __ \ / __ \|  __ \__   __|/\   | \ | |__   __| 
   | | | \  / | |__) | |  | | |__) | | |  /  \  |  \| |  | | (_)
   | | | |\/| |  ___/| |  | |  _  /  | | / /\ \ | . ` |  | |    
  _| |_| |  | | |    | |__| | | \ \  | |/ ____ \| |\  |  | |  _ 
 |_____|_|  |_|_|     \____/|_|  \_\ |_/_/    \_\_| \_|  |_| (_)

```
when running, the work directory of `NiFiFlowTest` has to be set to `nifi_test_nifi_home`,
otherwise, it does not work. The Maven build does this by default, but you will have to adjust
the settings manually if you run the test cases in an IDE.

## Overview

Once started, `NiFiFlowTest` unpacks the contents of the NiFi ZIP, and installs the 
`flow.xml` to the appropriate location. The `DocumentChangeCallback` implemented 
inside `NiFiFlowTest` is executed before the flow is actually installed: this allows the service URL 
to be rewritten to a local Jetty instance started by the integration test. 
The flow will hence read data from the Jetty instance used to mock source data. 

Once the instance is online,`NiFiFlowTest#testFlowCreatesFilesInCorrectLocation()` executes
 standard Java-style asserts on the expected behaviour of the flow. (Again, a lot to be improved here,
 but the _concept itself_ seems to work)

NOTE:
 * The code was written and tested against the official NiFi 1.6.0 build
 * The binary ZIP of NiFi 1.6 has to be placed to `nifi-1.6.0-bin.zip` inside 
    the project directory for it to work
    
    
## Running the test cases

### Pre-requisites

Download the official NiFi 1.6 build to the project directory, to a file `nifi-1.6.0-bin.zip`.

### using Maven

To run the test case from Maven, simply go to the project directory and issue a the following command:
`mvn clean test` 

### from any IDE

Create a run configuration for the test cases `NiFiFlowTest` or `NiFiMockFlowTest` and
make sure the test case is started within the directory `nifi_test_nifi_home`. You can
both simply _run_ or _debug_.
    
## Installing the flow to a NiFi instance

The NiFi flow `src/test/resources/flow.xml` was extracted from NiFi. To install it to a standalone 
NiFi instance to view it on the GUI or to modify it, follow the these instructions:

1. Open a shell, and switch to `src/test/resources/` directory
2. Create a GZip file called `flow.xml.gz` from `flow.xml` by issuing 'gzip < flow.xml > flow.xml.gz' 
    NOTE: step 1. is important: the GZip file must NOT contain relative paths, 
    it should contain a a single file called `flow.xml`, WITHOUT any directories
3. Unzip a standard NiFi 1.6.0 distribution zip to a location of your choice
4. Put the `flow.xml.gz` file to the `conf` directory of the clean NiFi installation created in the previous step.  
5. Start the NiFi instance using the standard scripts. 
    
      
  
