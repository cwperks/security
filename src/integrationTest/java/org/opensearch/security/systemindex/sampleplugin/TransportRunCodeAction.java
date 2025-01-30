/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.security.systemindex.sampleplugin;

// CS-SUPPRESS-SINGLE: RegexpSingleline It is not possible to use phrase "cluster manager" instead of master here
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
// CS-ENFORCE-SINGLE

public class TransportRunCodeAction extends HandledTransportAction<RunCodeRequest, AcknowledgedResponse> {

    private final Client client;

    @Inject
    public TransportRunCodeAction(final TransportService transportService, final ActionFilters actionFilters, final Client client) {
        super(RunClusterHealthAction.NAME, transportService, actionFilters, RunCodeRequest::new);
        this.client = client;
    }

    @Override
    protected void doExecute(Task task, RunCodeRequest request, ActionListener<AcknowledgedResponse> actionListener) {
        String code = request.getCode();
        // TODO Compile and execute the code here. Assume Java code
        try {
            String className = "DynamicCode";

            // Wrap the code in a class
            String sourceCode = String.format(
                "public class %s {\n"
                    + "    public static Object execute() {\n"
                    + "        try {\n"
                    + "            %s\n"
                    + "        } catch (Exception e) {\n"
                    + "            return e.getMessage();\n"
                    + "        }\n"
                    + "        return \"Code executed successfully\";\n"
                    + "    }\n"
                    + "}\n",
                className,
                code
            );
            // Create in-memory compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            InMemoryClassLoader classLoader = new InMemoryClassLoader();
            InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null), classLoader);

            // Prepare source code
            JavaFileObject sourceFile = new InMemorySourceFile(className, sourceCode);

            // Compile
            JavaCompiler.CompilationTask javaTask = compiler.getTask(null, fileManager, null, null, null, Arrays.asList(sourceFile));

            if (!javaTask.call()) {
                actionListener.onFailure(new IllegalStateException("Compilation failed"));
                return;
            }

            // Load and execute
            Class<?> loadedClass = classLoader.loadClass(className);
            Method executeMethod = loadedClass.getMethod("execute");
            Object result = executeMethod.invoke(null);

            actionListener.onResponse(new AcknowledgedResponse(true));

        } catch (Exception e) {
            actionListener.onFailure(e);
        }
    }

    // Custom ClassLoader
    private static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classData = new HashMap<>();

        public void addClass(String name, byte[] data) {
            classData.put(name, data);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] data = classData.get(name);
            if (data == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, data, 0, data.length);
        }
    }

    // Custom JavaFileObject for in-memory source code
    private static class InMemorySourceFile extends SimpleJavaFileObject {
        private final String code;

        public InMemorySourceFile(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    // Custom FileManager for in-memory compilation
    private static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final InMemoryClassLoader classLoader;

        public InMemoryFileManager(JavaFileManager fileManager, InMemoryClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
            JavaFileManager.Location location,
            String className,
            JavaFileObject.Kind kind,
            FileObject sibling
        ) {
            return new InMemoryClassFile(className, classLoader);
        }
    }

    // Custom JavaFileObject for in-memory bytecode
    private static class InMemoryClassFile extends SimpleJavaFileObject {
        private final String className;
        private final InMemoryClassLoader classLoader;
        private ByteArrayOutputStream outputStream;

        public InMemoryClassFile(String className, InMemoryClassLoader classLoader) {
            super(URI.create("byte:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.className = className;
            this.classLoader = classLoader;
        }

        @Override
        public OutputStream openOutputStream() {
            outputStream = new ByteArrayOutputStream();
            return outputStream;
        }
    }
}
