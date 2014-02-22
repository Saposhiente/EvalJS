package com.github.Saposhiente.EvalJS;

import org.bukkit.conversations.Prompt;
import org.mozilla.javascript.ContinuationPending;

/**
 *
 * @author Saposhiente
 */
public class EvaluationPending extends Resumable {
    final CodePrompt callback;

    public EvaluationPending(ContinuationPending cause, final CodePrompt callback) {
        super(cause);
        this.callback = callback;
    }

    @Override
    public Prompt resume(String newInput) throws EvaluationPending {
        return callback.handleOutput(callback.bridge.resume(getCause(), newInput));
    }

    @Override
    public synchronized ContinuationPending getCause() {
        return (ContinuationPending) super.getCause();
    }
    
}
