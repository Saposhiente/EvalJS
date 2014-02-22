package com.github.Saposhiente.EvalJS;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * Controls Rhino evaluation of JavaScript.
 * @author Saposhiente
 */
public class JavascriptRunner {

    public final Scriptable scope;
    public final Conversable owner;
    private final Scriptable importer;

    public JavascriptRunner(Conversable owner) throws IOException {
        this.owner = owner;
        final String name = owner instanceof CommandSender ? ((CommandSender) owner).getName() : owner.toString();
        Context cx = Context.enter();
        try {
            scope = cx.initStandardObjects();
            importer = new ImporterTopLevel(cx);
            scope.setPrototype(importer);
            set("_coder", owner);
            set("_name", name);
            set("tools", new JavascriptTools(this));
            set("_inspector", new ClassInspector(this));
            cx.evaluateReader(scope, new InputStreamReader(EvalJS.instance.getResource("javascript/eval.js")), "eval.js main for " + owner, 1, null);
            if (EvalJS.debug) cx.evaluateString(scope, "_debug = _verbose = _output", "eval.js debug setting for " + owner, 1, null);
            Object macros = new Object() { //can call output(String), therefore must be defined after the startup
                public Object exec(String filename) {
                    return JavascriptRunner.this.exec(filename);
                }
            };
            set("macros", macros);
        } finally {
            Context.exit();
        }
    }

    public Conversable getOwner() {
        return owner;
    }

    public Scriptable getScope() {
        return scope;
    }

    public Object exec(String filename) {
        File target = new File(EvalJS.instance.macroFolder, filename);
        if (!target.exists()) {
            output("Macro " + filename + " doesn't exist!");
            return null;
        }
        Context cx = Context.enter();
        try {
            return cx.evaluateReader(scope, new FileReader(target), "macro " + filename + " run for " + owner, 1, null);
        } catch (IOException ex) {
            output("Macro " + filename + " couldn't be read. See console for details.");
            EvalJS.instance.getLogger().log(Level.WARNING, "Couldn't run macro " + filename + " for " + owner + ": ", ex);
            return null;
        } finally {
            Context.exit();
        }
    }

    public void input(String input) {
        Object func = scope.get("_input", scope);
        if (func instanceof Function) {
            Object functionArgs[] = {input};
            Context cx = Context.enter();
            try {
                ((Function) func).call(cx, scope, scope, functionArgs);
            } finally {
                Context.exit();
            }
        }
    }

    public String eval(String input) throws ContinuationPending {
        byte errors = 0;
        final String buffered;
        if (buffer.length() > 0) {
            buffered = buffer.toString();
            //buffer = new StringBuilder(); done later (only if no continuation pending)
        } else {
            buffered = "";
        }
        while (errors >= 0) {
            try {
                final String result = doEval(input);
                //ContinuationPending maybe thrown here
                if (buffered.length() != 0) buffer = new StringBuilder();
                return buffered + result;
            } catch (NoClassDefFoundError err) {
                verbose("Substituting class def for " + err);
                EvalJS.instance.substitutor.substituteClassDef(err);
                errors++; //end loop if enough errors to byte overflow
            }
        }
        output("Could not run your statement: Too many class def substitutions. This is probably a bug.");
        return "";
    }

    //only in Context!
    private String handleResult(Object result) {
        //set("_outputCount", 0); //TODO: page for output function?
        set("_result", result);
        set("result", result);
        if (result == null) {
            return "null";
        } else if (result instanceof Scriptable) {
            return ((Scriptable)result).getDefaultValue(String.class).toString();
        } else if (result instanceof Undefined) {
            return "undefined";
        } else {
            return result.toString();
        }
    }
    
    private String doEval(String input) throws NoClassDefFoundError, ContinuationPending {
        Context cx = Context.enter();
        try {
            if (EvalJS.instance.reprompt) {
                cx.setOptimizationLevel(-1);
                return handleResult(cx.executeScriptWithContinuations(cx.compileString(input, "Input string " + input, 1, null), scope));
            } else {
                return handleResult(cx.evaluateString(scope, input, "Input string " + input, 1, null));
                
            }
        } catch (ContinuationPending cc) {
            throw cc;
        } catch (Exception e) {
            try {
                set("error", e);
            } finally {
                return "Error: " + e;
            }
        } finally {
            Context.exit();
        }
    }
    
    public String resume(ContinuationPending cause, Object newInput) throws ContinuationPending {
        Context cx = Context.enter();
        try {
            return handleResult(cx.resumeContinuation(cause.getContinuation(), scope, newInput));
        } catch (ContinuationPending cc) {
            throw cc;
        } catch (Exception e) {
            try {
                set("error", e);
            } finally {
                return "Error: " + e;
            }
        } finally {
            Context.exit();
        }
    }

    public void output(String output) {
        Object func = scope.get("_output", scope);
        if (func instanceof Function) {
            Object functionArgs[] = {output};
            Context cx = Context.enter();
            try {
                ((Function) func).call(cx, scope, scope, functionArgs);
            } finally {
                Context.exit();
            }
        }
    }
    
    private StringBuilder buffer = new StringBuilder();
    public void bufferedOutput(String output) {
        buffer.append(output).append("\n");
    }

    public void verbose(String output) {
        Object func = scope.get("_verbose", scope);
        if (func instanceof Function) {
            Object functionArgs[] = {output};
            Context cx = Context.enter();
            try {
                ((Function) func).call(cx, scope, scope, functionArgs);
            } finally {
                Context.exit();
            }
        }
    }

    public void debug(String output) {
        Object func = scope.get("_debug", scope);
        if (func instanceof Function) {
            Object functionArgs[] = {output};
            Context cx = Context.enter();
            try {
                ((Function) func).call(cx, scope, scope, functionArgs);
            } finally {
                Context.exit();
            }
        }
    }

    public void putObject(String name, Object value) {
        Context.enter();
        try {
            set(name, value);
        } finally {
            Context.exit();
        }
    }
    //only in Context!
    private void set(String name, Object value) {
        ScriptableObject.putProperty(scope, name, Context.javaToJS(value, scope));
    }
    
    public Object getObject(Scriptable parent, String name) {
        return scope.get(name, parent);
    }

}
