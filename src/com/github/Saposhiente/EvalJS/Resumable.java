package com.github.Saposhiente.EvalJS;

import org.bukkit.conversations.Prompt;

/**
 * Thrown when evaluation has been paused to ask for additional input from the user.
 * @see InputPrompt
 * @author Saposhiente
 */
public abstract class Resumable extends Exception {

    public Resumable(Throwable cause) {
        super(cause);
    }

    public abstract Prompt resume(String newInput) throws Resumable;
    
}
