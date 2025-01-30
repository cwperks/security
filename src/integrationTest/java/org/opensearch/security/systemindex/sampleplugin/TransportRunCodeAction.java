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
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
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
        super(RunCodeAction.NAME, transportService, actionFilters, RunCodeRequest::new);
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

            // Debug logging
            logger.debug("Compiling source code:\n{}", sourceCode);

            // Set up the compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("No Java compiler available");
            }

            // Create our custom class loader
            final Map<String, byte[]> classBytes = new HashMap<>();
            ClassLoader loader = new ClassLoader(this.getClass().getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] bytes = classBytes.get(name);
                    if (bytes == null) {
                        throw new ClassNotFoundException(name);
                    }
                    return defineClass(name, bytes, 0, bytes.length);
                }
            };

            // Set up the file manager and compilation units
            JavaFileObject sourceFile = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                JavaFileObject.Kind.SOURCE
            ) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return sourceCode;
                }
            };

            JavaFileObject classFile = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.CLASS.extension),
                JavaFileObject.Kind.CLASS
            ) {
                @Override
                public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() throws IOException {
                            classBytes.put(className, toByteArray());
                        }
                    };
                }
            };

            // Create file manager
            ForwardingJavaFileManager<StandardJavaFileManager> fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(
                compiler.getStandardFileManager(null, null, null)
            ) {
                @Override
                public JavaFileObject getJavaFileForOutput(
                    Location location,
                    String className,
                    JavaFileObject.Kind kind,
                    FileObject sibling
                ) {
                    return classFile;
                }
            };

            // Compile
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask javaTask = compiler.getTask(null, fileManager, diagnostics, null, null, Arrays.asList(sourceFile));

            boolean success = javaTask.call();

            if (!success) {
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    sb.append(diagnostic.toString()).append("\n");
                }
                logger.error("Compilation failed: {}", sb.toString());
                actionListener.onFailure(new IllegalStateException("Compilation failed: " + sb.toString()));
                return;
            }

            // Debug logging
            logger.debug(
                "Compilation successful. Class bytes size: {}",
                classBytes.get(className) != null ? classBytes.get(className).length : "null"
            );

            // Load and execute
            Class<?> loadedClass = loader.loadClass(className);
            Method executeMethod = loadedClass.getMethod("execute");
            Object result = executeMethod.invoke(null);

            logger.debug("Execution result: {}", result);

            actionListener.onResponse(new AcknowledgedResponse(true));

        } catch (Exception e) {
            logger.error("Error executing code", e);
            actionListener.onFailure(e);
        }
    }
}
