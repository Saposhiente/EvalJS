package com.github.Saposhiente.EvalJS;

import static com.github.Saposhiente.EvalJS.EvalJS.debug;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.mozilla.javascript.Scriptable;

/**
 * Monitors game events to determine when to tab complete, and provides the completion in that case.
 * @author Saposhiente
 */
public class TabCompletionManager implements Listener, ConversationAbandonedListener {
    
    private final Map<UUID, JavascriptRunner> allConvos = new HashMap<>(); //used only if tabComplete
    
    public void reset() {
        allConvos.clear();
    }
    
    public void addConversation(UUID player, JavascriptRunner target) {
        allConvos.put(player, target);
    }
    
    @Override
    public void conversationAbandoned(ConversationAbandonedEvent e) {
        if (e.getContext().getForWhom() instanceof Player) {
            allConvos.remove(((Player) e.getContext().getForWhom()).getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChatTabComplete(PlayerChatTabCompleteEvent e) {
        if (e.getPlayer().isConversing() && allConvos.containsKey(e.getPlayer().getUniqueId())) {
            if (debug) {
                EvalJS.instance.getLogger().info("Detected tab completion");
            }
            int lastSeparator = -1;
            boolean string = false;
            loop:
            for (int i = e.getLastToken().length() - 1; i >= 0; i--) {
                char c = e.getLastToken().charAt(i);
                if (!Character.isJavaIdentifierPart(c)) {
                    switch (c) {
                        case '.':
                            continue;
                        case ')':
                            e.getTabCompletions().clear(); //no way to match
                            return;
                        case '"': //todo: doesn't recognize escaped strings
                        case '\'':
                            if (e.getLastToken().split(new String(new char[]{c})).length % 2 == 0) { //number of occurances is odd: inside a string
                                return; //match usernames TODO: not working, match manually?
                            }
                            string = true;
                        //fall thru to default
                        default:
                            lastSeparator = i;
                            break loop;
                    }
                }
            }
            e.getTabCompletions().clear(); //not matching usernames; if no matches found, don't complete.
            String word = e.getLastToken().substring(lastSeparator + 1, e.getLastToken().length());
            String[] names = word.split("\\.", -1);
            final int lastPos = names.length - 1;
            JavascriptRunner bridge = allConvos.get(e.getPlayer().getUniqueId());
            bridge.debug("Tab completing " + word);
            Object parent = string ? bridge.getObject(bridge.getScope(), "String") : bridge.getScope();
            int i = 0;
            while (i < lastPos) {
                if (!(parent instanceof Scriptable) || parent == Scriptable.NOT_FOUND) {
                    bridge.debug("Tab completion: " + names[i - 1] + "." + names[i] + " not found");
                    return; //no matches
                }
                parent = bridge.getObject((Scriptable) parent, names[i]);
                i++;
            }
            if (parent instanceof Scriptable && parent != Scriptable.NOT_FOUND) {
                String unfinished = names[lastPos];
                String initial = e.getLastToken().substring(0, e.getLastToken().length() - unfinished.length());
                for (Object id : ((Scriptable) parent).getIds()) {
                    String s = id.toString();
                    if (s.startsWith(unfinished)) {
                        e.getTabCompletions().add(initial + s);
                    }
                }
            }
        }
    }
}
