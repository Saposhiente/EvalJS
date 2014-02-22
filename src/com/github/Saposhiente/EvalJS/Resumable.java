package com.github.Saposhiente.EvalJS;

import org.bukkit.conversations.Prompt;

/**
 *
 * @author Saposhiente
 */
public abstract class Resumable extends Exception {

    public Resumable(Throwable cause) {
        super(cause);
    }

    public abstract Prompt resume(String newInput) throws Resumable;
    
}
