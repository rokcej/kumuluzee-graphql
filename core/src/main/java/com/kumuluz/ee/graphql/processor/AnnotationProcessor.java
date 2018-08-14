/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.kumuluz.ee.graphql.processor;

import com.kumuluz.ee.graphql.annotations.GraphQLApplicationClass;
import com.kumuluz.ee.graphql.annotations.GraphQLClass;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AnnotationProcessor extends AbstractProcessor{
    private Filer filer;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> annotatedClasses = roundEnvironment.getElementsAnnotatedWith(GraphQLClass.class);
        Set<String> serviceClassNames = new HashSet<>();
        for (Element element : annotatedClasses) {
            serviceClassNames.add(element.toString());
        }

        // write annotated class names to service file
        if (!serviceClassNames.isEmpty()) {
            try {
                writeServiceFile(serviceClassNames, "META-INF/kumuluzee/graphql/java.lang.Object");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Set<? extends Element> annotated = roundEnvironment.getElementsAnnotatedWith(GraphQLApplicationClass.class);
        Set<String> settingsClassNames = new HashSet<>();
        for (Element element : annotated) {
            settingsClassNames.add(element.toString());
        }

        // write annotated class names to service file
        if (!settingsClassNames.isEmpty()) {
            try {
                writeServiceFile(settingsClassNames, "META-INF/services/com.kumuluz.ee.graphql.GraphQLApplication");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }

    private void writeServiceFile(Set<String> serviceClassNames, String serviceFileName) throws IOException {

        FileObject file = readOldServiceFile(serviceClassNames, serviceFileName);

        if (file != null) {
            try {
                writeServiceFile(serviceClassNames, serviceFileName, file);
                return;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        writeServiceFile(serviceClassNames, serviceFileName, null);
    }

    private void writeServiceFile(Set<String> serviceClassNames, String serviceFileName, FileObject overrideFile) throws
            IOException {

        FileObject file = overrideFile;
        if (file == null) {
            file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", serviceFileName);
        }

        try (Writer writer = file.openWriter()) {
            for (String serviceClassName : serviceClassNames) {
                writer.write(serviceClassName);
                writer.write("\n");
            }
        }
    }

    private FileObject readOldServiceFile(Set<String> serviceClassNames, String serviceFileName) throws IOException {

        Reader reader = null;

        try {
            final FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", serviceFileName);
            reader = resource.openReader(true);
            readOldServiceFile(serviceClassNames, reader);
            return resource;
        } catch (FileNotFoundException e) {
            // close reader, return null
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return null;
    }

    private static void readOldServiceFile(Set<String> serviceClassNames, Reader reader) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                serviceClassNames.add(line);
                line = bufferedReader.readLine();
            }
        }
    }
}