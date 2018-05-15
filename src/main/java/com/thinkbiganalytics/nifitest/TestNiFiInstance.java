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

 
package com.thinkbiganalytics.nifitest;

import com.thinkbiganalytics.nifitest.util.*;
import org.apache.nifi.EmbeddedNiFi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;

public class TestNiFiInstance {


    private EmbeddedNiFi testNiFi;

    private final File nifiHomeDir;
    private final File configDir;
    private final File libDir;
    private final File bootstrapLibDir;

    private File nifiProperties;

    private final File flowXmlGz;

    private final File placeholderNiFiHomeDir;


    private enum State {
        STOPPED,
        STOP_FAILED,
        START_FAILED(STOPPED),
        STARTED(STOPPED, STOP_FAILED),
        INSTALLATION_FAILED(),
        INSTALLED(STARTED, START_FAILED),
        CREATED(INSTALLED, INSTALLATION_FAILED);


        private final Set<State> allowedTransitions;

        State(State... allowedTransitions) {
            this.allowedTransitions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(allowedTransitions)));
        }

        private State checkCanTransition(State newState) {
            if (!this.allowedTransitions.contains(newState)) {
                throw new IllegalStateException("Cannot transition from " + this + " to " + newState);
            }

            return newState;
        }
    }

    private State currentState = State.CREATED;

    private final File nifiBinaryZip;
    private final File flowXml;
    private final DocumentChangeCallback editCallback;

    private TestNiFiInstance(File nifiBinaryZip, File flowXml, DocumentChangeCallback editCallback) {
        this.nifiBinaryZip = Objects.requireNonNull(nifiBinaryZip, "nifiBinaryZip");
        this.flowXml = Objects.requireNonNull(flowXml, "flowXml");
        this.editCallback = editCallback;

        nifiHomeDir = requireCurrentWorkingDirectoryIsCorrect();

        configDir = new File(nifiHomeDir, "conf");
        libDir = new File(nifiHomeDir, "lib");
        bootstrapLibDir = new File(libDir, "bootstrap");

        nifiProperties = new File(configDir, "nifi.properties");

        flowXmlGz = new File(configDir, "flow.xml.gz");

        placeholderNiFiHomeDir = requireCurrentWorkingDirectoryIsCorrect();
    }

    public void install() throws IOException {

        currentState.checkCanTransition(State.INSTALLED);

        File[] staleInstallations = placeholderNiFiHomeDir.listFiles((dir, name) -> name.startsWith("nifi-"));
        Arrays.stream(staleInstallations).forEach(TestNiFiInstance::deleteFileOrDirectoryRecursively);


        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("installable-flow");

            File installableFlowFile = createInstallableFlowFile(tempDirectory);

            System.out.printf("Uncompressing NiFi archive %s ...%n", nifiBinaryZip);

            Zip.unzipFile(nifiBinaryZip, placeholderNiFiHomeDir, new Zip.StatusListenerAdapter() {
                @Override
                public void onUncompressDone(ZipEntry ze) {
                    System.out.printf("Uncompressed %s %n", ze.getName());
                }
            });

            System.out.println("\tUncompressing DONE");

            File actualNiFiHomeDir = getActualNiFiHomeDir(placeholderNiFiHomeDir);

            createSymlinks(actualNiFiHomeDir);

            installFlowFile(installableFlowFile);
        } catch (Exception e) {

            currentState = State.INSTALLATION_FAILED;

            throw new RuntimeException("Installation failed: " + e.getMessage(), e);

        } finally {
            if (tempDirectory != null) {
                FileUtils.deleteDirectoryRecursive(tempDirectory);
            }
        }

        currentState = State.INSTALLED;
    }

    private void createSymlinks(File actualNiFiHomeDir) {
        Arrays.stream(actualNiFiHomeDir.list())
                .filter(fileName -> !".DS_Store".equals(fileName))
                .forEach(fileName -> {
                    Path newLink = Paths.get(placeholderNiFiHomeDir.getAbsolutePath(), fileName);
                    Path existingFile = Paths.get(actualNiFiHomeDir.getAbsolutePath(), fileName);

                    File symlinkFile = newLink.toFile();
                    if (symlinkFile.exists()) {
                        symlinkFile.delete();
                    }

                    FileUtils.createLink(newLink, existingFile);
                });
    }

    private File createInstallableFlowFile(Path tempDirectory) throws IOException {

        File flowXmlFile = new File(tempDirectory.toFile(), "flow.xml");

        if (editCallback == null) {
            Files.copy(flowXml.toPath(), flowXmlFile.toPath());
        } else {
            XmlUtils.editXml(flowXml, flowXmlFile, editCallback);
        }

        return flowXmlFile;
    }

    private void installFlowFile(File fileToIncludeInGz) throws IOException {
        Zip.gzipFile(fileToIncludeInGz, flowXmlGz);
    }

    public void start() {

        currentState.checkCanTransition(State.STARTED);

        try {
            if (!bootstrapLibDir.exists()) {
                throw new IllegalStateException("Not found: " + bootstrapLibDir);
            }



            System.setProperty("org.apache.jasper.compiler.disablejsr199", "true");
            System.setProperty("java.security.egd", "file:/dev/urandom");
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("java.protocol.handler.pkgs", "sun.net.www.protocol");

            System.setProperty("nifi.properties.file.path", nifiProperties.getAbsolutePath());
            System.setProperty("app", "NiFi");
            System.setProperty("org.apache.nifi.bootstrap.config.log.dir", "./logs");

            ClassLoader coreClassLoader = new NiFiCoreLibClassLoader(nifiHomeDir, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(coreClassLoader);



            this.testNiFi = new EmbeddedNiFi(new String[0], coreClassLoader);

            // Wait for NiFi to start processing
            // FIXME: we definitely need something slightly more than this...
            Thread.sleep(10 * 1000);

        } catch (Exception ex) {

            currentState = State.START_FAILED;

            throw new RuntimeException("Startup failed", ex);

        }

        currentState = State.STARTED;


    }


    public void stopAndCleanup() {
        currentState.checkCanTransition(State.STOPPED);

        try {
            testNiFi.shutdown();

            removeNiFiFilesCreatedForTemporaryInstallation(placeholderNiFiHomeDir);

        } catch (Exception e) {
            currentState = State.STOP_FAILED;

            throw new RuntimeException(e);
        }

        currentState = State.STOPPED;
    }

    private static File requireCurrentWorkingDirectoryIsCorrect() {
        String currentDir = System.getProperty("user.dir");
        if (!currentDir.endsWith("nifi_test_nifi_home")) {

            throw new IllegalStateException(
                    "The test's working directory has to be set to nifi_test_nifi_home, but was: " + currentDir);
        }
        return new File(currentDir);
    }

    private static File getActualNiFiHomeDir(File currentDir) {
        File[] files = currentDir.listFiles((dir, name) -> name.startsWith("nifi-"));

        if (files.length == 0) {
            throw new IllegalStateException(
                    "No \"nifi-*\" directory found in temporary NiFi home directory container: " + currentDir);
        }

        if (files.length != 1) {
            throw new IllegalStateException(
                    "Multiple \"nifi-*\" directories found in temporary NiFi home directory container: " + currentDir);
        }

        return files[0];
    }

    private static void removeNiFiFilesCreatedForTemporaryInstallation(File directoryToClear) {

        Arrays.stream(directoryToClear.listFiles())
                .filter(file -> !"NIFI_TEST_README.txt".equals(file.getName()))
                .forEach(file -> {
                    deleteFileOrDirectoryRecursively(file);


                });
    }

    private static void deleteFileOrDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            FileUtils.deleteDirectoryRecursive(file);
        }
        else {
            file.delete();
        }
    }

    @Override
    public String toString() {
        return "NiFi test instance(" + Integer.toHexString(hashCode())
                + ") state: " + currentState + ", home: " + nifiHomeDir;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {

        private boolean isDisposed = false;

        private File nifiBinaryZip;
        private File flowXml;
        private DocumentChangeCallback editCallback;

        public Builder setNiFiBinaryDistributionZip(File nifiBinaryZip) {
            if (!nifiBinaryZip.exists()) {
                throw new IllegalArgumentException("File not found: " + nifiBinaryZip);
            }

            this.nifiBinaryZip = nifiBinaryZip;
            return this;
        }

        public Builder setFlowXmlToInstallForTesting(File flowXml) {
            if (!flowXml.exists()) {
                throw new IllegalArgumentException("File not found: " + flowXml);
            }

            this.flowXml = flowXml;
            return this;
        }

        public Builder modifyFlowXmlBeforeInstalling(DocumentChangeCallback callback) {
            this.editCallback = callback;
            return this;
        }



        public TestNiFiInstance build() {
            if (isDisposed) {
                throw new IllegalStateException("builder can only be used once");
            }
            isDisposed = true;

            return new TestNiFiInstance(nifiBinaryZip, flowXml, editCallback);
        }


    }


}
