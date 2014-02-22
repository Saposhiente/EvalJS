/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.Saposhiente.EvalJS;

import org.bukkit.conversations.Prompt;
import org.mozilla.javascript.ContinuationPending;

/**
 * Like EvaluationPending, except for when evaluation is within a substitution for a built-in command.
 * @see Resumable
 * @author Saposhiente
 */
public class SubstitutionPending extends Resumable {
    final String input;
    final StringBuilder substitutedInput;
    final int to;
    private final CodePrompt callback;

    public SubstitutionPending(String input, StringBuilder substitutedInput, int to, ContinuationPending cause, final CodePrompt callback) {
        super(cause);
        this.callback = callback;
        this.input = input;
        this.substitutedInput = substitutedInput;
        this.to = to;
    }

    @Override
    public Prompt resume(String newInput) throws SubstitutionPending {
        substitutedInput.append(callback.bridge.resume(getCause(), newInput));
        return callback.doCommand(callback.substitueInput(input, substitutedInput, input.indexOf("$(", to) + 2, to), callback);
    }

    @Override
    public synchronized ContinuationPending getCause() {
        return (ContinuationPending) super.getCause();
    }
    
}
