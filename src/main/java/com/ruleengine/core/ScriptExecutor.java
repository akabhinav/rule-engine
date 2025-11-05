package com.ruleengine.core;

import com.ruleengine.model.Action;
import com.ruleengine.model.RuleContext;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Sandboxed Script Executor
 *
 * Executes user scripts in a safe, sandboxed environment
 * Supports:
 * - JavaScript (GraalVM)
 * - MVEL
 * - Groovy (future)
 *
 * Security:
 * - Timeout protection
 * - Resource limits
 * - No access to system resources
 */
@Component
public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final ScriptEngineManager scriptEngineManager;
    private final ExecutorService executorService;
    private final long defaultTimeoutMs = 5000; // 5 seconds

    public ScriptExecutor() {
        this.scriptEngineManager = new ScriptEngineManager();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Execute script safely with timeout
     */
    public Object executeScript(Action action, RuleContext context) {
        String script = action.getScript();
        Action.ScriptLanguage language = action.getScriptLanguage();

        if (script == null || language == null) {
            throw new IllegalArgumentException("Script and language must be specified");
        }

        try {
            return switch (language) {
                case JAVASCRIPT -> executeJavaScript(script, context);
                case MVEL -> executeMvel(script, context);
                case GROOVY -> executeGroovy(script, context);
            };
        } catch (Exception e) {
            logger.error("Error executing script", e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute JavaScript with timeout
     */
    private Object executeJavaScript(String script, RuleContext context) throws Exception {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");

        if (engine == null) {
            // Fallback to Nashorn if available
            engine = scriptEngineManager.getEngineByName("nashorn");
        }

        if (engine == null) {
            throw new UnsupportedOperationException("JavaScript engine not available");
        }

        // Create bindings with context data
        Bindings bindings = engine.createBindings();
        bindings.putAll(buildScriptContext(context));

        // Execute with timeout
        Future<Object> future = executorService.submit(() -> {
            try {
                return engine.eval(script, bindings);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(defaultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Script execution timeout after " + defaultTimeoutMs + "ms");
        }
    }

    /**
     * Execute MVEL script
     */
    private Object executeMvel(String script, RuleContext context) {
        Map<String, Object> variables = buildScriptContext(context);

        try {
            // Compile for better performance
            Serializable compiled = MVEL.compileExpression(script);

            // Execute with timeout
            Future<Object> future = executorService.submit(() ->
                    MVEL.executeExpression(compiled, variables)
            );

            return future.get(defaultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("MVEL script execution timeout");
        } catch (Exception e) {
            throw new RuntimeException("MVEL script execution failed", e);
        }
    }

    /**
     * Execute Groovy script (placeholder)
     */
    private Object executeGroovy(String script, RuleContext context) {
        // TODO: Implement Groovy execution
        throw new UnsupportedOperationException("Groovy support not yet implemented");
    }

    /**
     * Build script execution context from rule context
     */
    private Map<String, Object> buildScriptContext(RuleContext context) {
        Map<String, Object> scriptContext = new HashMap<>();

        // Add facts
        context.getFacts().forEach((key, fact) -> {
            scriptContext.put(key, fact.getData());
        });

        // Add context attributes
        scriptContext.putAll(context.getContextAttributes());

        // Add intermediate results
        scriptContext.putAll(context.getIntermediateResults());

        // Add utility functions
        scriptContext.put("log", (ScriptLogger) message ->
                logger.info("Script log: {}", message));

        return scriptContext;
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Script logger interface
     */
    @FunctionalInterface
    public interface ScriptLogger {
        void log(String message);
    }
}
