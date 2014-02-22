package com.github.Saposhiente.EvalJS;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

/**
 *
 * @author Saposhiente
 */

    public class InputPrompt extends StringPrompt {

        protected final Resumable resumable;
        public InputPrompt(Resumable resumable) {
            this.resumable = resumable;
        }

        @Override
        public String getPromptText(ConversationContext cc) {
            return "";
        }

        @Override
        public Prompt acceptInput(ConversationContext cc, String input) {
            try {
                return resumable.resume(input);
            } catch (Resumable subResumable) {
                return new InputPrompt(subResumable);
            }
        }
        
    }
