package com.github.Saposhiente.EvalJS;

import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.mozilla.javascript.ContinuationPending;

/**
 *
 * @author Saposhiente
 */
public class CodePrompt extends StringPrompt {

    public final JavascriptRunner bridge;
    private boolean cmd = false;
    private String result = "";
    private String prevInput = "";
    protected final CommandSender commandSender;
    public final Conversable coder;
    protected Conversation conversation;
    
    public Conversation getConversation() {
        return conversation;
    }
    
    public CodePrompt(final JavascriptRunner bridge, final Conversable coder) {
        this.bridge = bridge;
        this.coder = coder;
        if (coder instanceof CommandSender) {
            commandSender = (CommandSender) coder;
        } else {
            bridge.verbose("Making substitute command sender for you");
            commandSender = new ConsoleCommandSender() {
                // <editor-fold defaultstate="collapsed" desc="Redirect methods to coder, Bukkit.getConsoleSender()">
                @Override
                public void sendMessage(String string) {
                    coder.sendRawMessage(string);
                }

                @Override
                public void sendMessage(String[] strings) {
                    for (String s : strings) {
                        coder.sendRawMessage(s);
                    }
                }

                @Override
                public Server getServer() {
                    return Bukkit.getServer();
                }

                @Override
                public String getName() {
                    return "EvalJS" + System.identityHashCode(coder);
                }

                @Override
                public boolean isPermissionSet(String string) {
                    return Bukkit.getConsoleSender().isPermissionSet(string);
                }

                @Override
                public boolean isPermissionSet(Permission prmsn) {
                    return Bukkit.getConsoleSender().isPermissionSet(prmsn);
                }

                @Override
                public boolean hasPermission(String string) {
                    return true;
                }

                @Override
                public boolean hasPermission(Permission prmsn) {
                    return true;
                }

                @Override
                public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln) {
                    return Bukkit.getConsoleSender().addAttachment(plugin, string, bln);
                }

                @Override
                public PermissionAttachment addAttachment(Plugin plugin) {
                    return Bukkit.getConsoleSender().addAttachment(plugin);
                }

                @Override
                public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i) {
                    return Bukkit.getConsoleSender().addAttachment(plugin, string, bln, i);
                }

                @Override
                public PermissionAttachment addAttachment(Plugin plugin, int i) {
                    return Bukkit.getConsoleSender().addAttachment(plugin, i);
                }

                @Override
                public void removeAttachment(PermissionAttachment pa) {
                    Bukkit.getConsoleSender().removeAttachment(pa);
                }

                @Override
                public void recalculatePermissions() {
                    Bukkit.getConsoleSender().recalculatePermissions();
                }

                @Override
                public Set<PermissionAttachmentInfo> getEffectivePermissions() {
                    return Bukkit.getConsoleSender().getEffectivePermissions();
                }

                @Override
                public boolean isOp() {
                    return true;
                }

                @Override
                public void setOp(boolean bln) {
                }

                @Override
                public boolean isConversing() {
                    return coder.isConversing();
                }

                @Override
                public void acceptConversationInput(String string) {
                    coder.acceptConversationInput(string);
                }

                @Override
                public boolean beginConversation(Conversation c) {
                    return coder.beginConversation(c);
                }

                @Override
                public void abandonConversation(Conversation c) {
                    coder.abandonConversation(c);
                }

                @Override
                public void abandonConversation(Conversation c, ConversationAbandonedEvent cae) {
                    coder.abandonConversation(c, cae);
                }

                @Override
                public void sendRawMessage(String string) {
                    coder.sendRawMessage(string);
                }
                // </editor-fold>
            };
        }
        conversation = EvalJS.instance.convFactory.withFirstPrompt(this).buildConversation(coder);
    }
    
    @Override
    public String getPromptText(ConversationContext cc) {
        return cmd ? "" : result;
    }

    @Override
    public Prompt acceptInput(ConversationContext convContext, String input) {
        try {
            return processInput(input);
        } catch (Resumable resumable) {
            return new InputPrompt(resumable);
        }
    }
    private Prompt processInput(String input) throws Resumable {
        bridge.input(input); //on-before-evaluation function, by default just prints and logs the input, can be arbitrarily redefined
        if (input.length() == 0) {
            return this;
        }
        if (input.endsWith("\\")) {
            prevInput += input.substring(0, input.length() - 1);
            cmd = true;
            return this;
        }
        if (input.length() > 1 && input.endsWith("\\n")) {
            prevInput += input.substring(0, input.length() - 2) + "\n";
            cmd = true;
            return this;
        }
        input = prevInput + input;
        prevInput = "";
        if (input.startsWith("/")) {
            cmd = true;
            return doCommand(substitueInput(input), this);
        } else {
            cmd = false;
            try {
                return handleOutput(bridge.eval(input));
            } catch (ContinuationPending cc) {
                throw new EvaluationPending(cc, this);
            }
        }
    }
    Prompt handleOutput(String output) {
        List<String> pages = PagePrompt.getPages(output);
        if (pages.size() == 1) {
            result = output;
            return this;
        } else {
            result = "";
            return new PagePrompt(pages, this);
        }
    }
    
    Prompt doCommand(String input, Prompt callback) {
        if (input.equalsIgnoreCase("/exit")) {
            bridge.output("Exiting Eval.js console");
            return Prompt.END_OF_CONVERSATION;
        } else {
            coder.abandonConversation(conversation); // abandon a conversation while determing its fate? nothing could possibly go wrong
            boolean chat = input.toLowerCase().startsWith("/chat");
            boolean send = chat && input.length() > 5;
            if (send && input.charAt(6) == ' ') {
                if (coder instanceof Player) {
                    ((Player) coder).chat(input.substring(6));
                } else {
                    Bukkit.broadcastMessage(input.substring(6));
                }
            } else if (!chat || send) {//check for eg /chatAsdf, different commands
                try {
                    Bukkit.dispatchCommand(commandSender, input.substring(1));
                } catch (Exception e) {
                    commandSender.sendMessage("Command resulted in internal error " + e.getLocalizedMessage());
                }
            }//if given /chat with no args, destroy and rebuild the conversation, (receiving any missed chat messages?)
            conversation = EvalJS.instance.convFactory.withFirstPrompt(callback).buildConversation(coder);
            coder.beginConversation(conversation);
            return Prompt.END_OF_CONVERSATION; //it's a bit of a hack, but otherwise the command is intercepted by this CodePrompt and treated as an input
        }
    }
    String substitueInput(String input) throws SubstitutionPending {
        return substitueInput(input, new StringBuilder(), input.indexOf("$(") + 2, 0);
    }
    String substitueInput(String input, StringBuilder substitutedInput, int from, int to) throws SubstitutionPending {
        if (from > 1) {
            substitutedInput.append(input.substring(to, from - 2));
            to = input.indexOf(")$", from) + 2;
            if (to < 2) {
                substitutedInput.append(input.substring(to, input.length()));
                return substitutedInput.toString();
            }
            try {
                substitutedInput.append(bridge.eval(input.substring(from, to - 2)));
            } catch (ContinuationPending cc) {
                throw new SubstitutionPending(input, substitutedInput, to, cc, this);
            }
            return substitueInput(input, substitutedInput, input.indexOf("$(", to) + 2, to);
        } else {
            substitutedInput.append(input.substring(to, input.length()));
            return substitutedInput.toString();
        }
    }
}