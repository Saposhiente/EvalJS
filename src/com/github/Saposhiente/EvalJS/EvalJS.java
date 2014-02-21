package com.github.Saposhiente.EvalJS;

import com.github.Saposhiente.ClassDefSubstitutor.ClassDefSubstitutor;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author Saposhiente
 */
public class EvalJS extends JavaPlugin implements Listener, ConversationAbandonedListener {

    protected ClassDefSubstitutor substitutor;
    public List<String> admins;
    public boolean enabled;
    public boolean alwaysAllowConsole;
    public int timeout;
    public static EvalJS instance;
    public File macroFolder;
    public boolean showWelcome;
    protected boolean tabComplete;
    public boolean reprompt;
    public static final boolean debug = true;
    public boolean hideChat;

    @Override
    public void onEnable() {
        getConfig().options().copyHeader(true);
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        instance = this;
        File linuxSystem = new File("etc");
        if (linuxSystem.exists() && linuxSystem.canWrite()) {
            getLogger().log(Level.SEVERE, "The server has access to system files! (Linux system folder /etc/)\nThis is a serious security risk, and an unacceptable one if you are running EvalJS. (One minecraft.net error, or one admin leaving their Minecraft window open, ore one admin getting the computer hacked, and BAM! This whole computer could be easily taken over and all of your data stolen.)\nCreate a separate user, who is not an administrator, to run the Minecraft server.\nShutting down server...");
            Bukkit.shutdown();
            return;
        }
        File windowsSystem = new File("Windows");
        if (windowsSystem.exists() && windowsSystem.canWrite()) {
            getLogger().log(Level.SEVERE, "The server has access to system files! (Windows system folder C:\\Windows\\)\nThis is a serious security risk, and an unacceptable one if you are running EvalJS. (One minecraft.net error, or one admin leaving their Minecraft window open, ore one admin getting the computer hacked, and BAM! This whole computer could be easily taken over and all of your data stolen.)\nCreate a separate user, who is not an administrator, to run the Minecraft server.\nShutting down server...");
            Bukkit.shutdown();
            return;
        }
        substitutor = new ClassDefSubstitutor(getLogger());
        macroFolder = new File(getDataFolder(), "macros");
        showWelcome = getConfig().getBoolean("showWelcome");
        hideChat = getConfig().getBoolean("hideChat");
        tabComplete = getConfig().getBoolean("tabComplete");
        if (tabComplete) {
            Bukkit.getPluginManager().registerEvents(this, this);
        }
        reprompt = getConfig().getBoolean("reprompt");
        alwaysAllowConsole = getConfig().getBoolean("alwaysAllowConsole");
        admins = getConfig().getStringList("admins");
        timeout = getConfig().getInt("timeout");
        if (tabComplete) {
            convFactory = new ConversationFactory(this).withTimeout(timeout).withLocalEcho(false).withModality(hideChat).addConversationAbandonedListener(this);
        } else {
            convFactory = new ConversationFactory(this).withTimeout(timeout).withLocalEcho(false).withModality(hideChat);
        }
        for (World w : Bukkit.getWorlds()) {
            File target = new File(w.getWorldFolder(), "players");
            if (target.exists() && target.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dat");
                }
            }).length > 0) {
                JavascriptTools.playerDataFolder = target;
                getLogger().log(Level.INFO, "Found player data folder inside world {0}", w.getName());
                break;
            }
        }
        if (JavascriptTools.playerDataFolder == null) {
            getLogger().log(Level.WARNING, "Could not find player data folder! Are there no players with saved data? (This will only affect NBT tools.)");
        }
        Date enabledUntil;
        String dateString = getConfig().getString("enabledUntil");
        try {
            long millis = Long.parseLong(dateString);
            if (millis < 0) {
                enabledUntil = new Date(Long.MAX_VALUE);
            } else {
                enabledUntil = new Date(millis);
            }
        } catch (NumberFormatException e) {
            final DateFormat format;
            switch (getConfig().getInt("enabledUntilDateMode")) {
                case 1:
                    format = DateFormat.getDateTimeInstance();
                    break;
                case 2:
                    format = DateFormat.getTimeInstance();
                    break;
                default: //case 0
                    format = DateFormat.getDateInstance();
            }
            try {
                enabledUntil = format.parse(dateString);
            } catch (ParseException ex) {
                throw new RuntimeException("Could not parse enabled until date in the configuration!", ex);
            }
        }
        if (enabledUntil.before(new Date())) {
            enabled = false;
            getLogger().info("enabledUntil is exceeded! Disabling player console...");
            return;
        }
        if (Bukkit.getOnlineMode()) {
            enabled = true;
        } else {
            enabled = false;
            getLogger().info("Server is in offline mode! Disabling player console...");
        }
    }
    
    
    /**
     * Makes changes to timeout and hideChat take effect until the next server restart.
     */
    public void reconfigure() {
        convFactory = convFactory.withTimeout(timeout).withModality(hideChat);
    }

    @Override
    public void onDisable() {
        allConvos.clear();
    }

    public static interface PermissionsTester {

        public boolean hasPermission(CommandSender sender);
    }
    public PermissionsTester permissionsTester = new PermissionsTester() { //allow overriding for additional security or for plugins that give custom login checking in offline mode
        @Override
        public boolean hasPermission(CommandSender sender) {
            if (sender instanceof ConsoleCommandSender) {
                return (alwaysAllowConsole || enabled) && sender.isOp();//check op JIC for IRC plugins
            } else {
                return enabled && sender.isOp() && admins.contains(sender.getName());
            }
        }
    };
    protected ConversationFactory convFactory;

    public void openPrompt(Conversable coder) {
        try {
            CodePrompt prompt = new CodePrompt(new JavascriptRunner(coder), coder);
            //Conversation conv = convFactory.withFirstPrompt(prompt).buildConversation(coder);
            //prompt.conversation = conv;
            prompt.bridge.debug(prompt.conversation.toString());
            coder.beginConversation(prompt.conversation);
            prompt.bridge.debug("Started conversation!");
            if (tabComplete && coder instanceof Player) {
                allConvos.put(((Player) coder).getUniqueId(), prompt.bridge);
            }
            if (showWelcome) {
                coder.sendRawMessage("Welcome to the EvalJS console! Use inspectJS(this) for a list of functions and variables.");
                coder.sendRawMessage("Names starting with an underscore are reserved; changing these variables will modify the behavior of EvalJS.");
                coder.sendRawMessage("Lines starting with / are interpreted as commands. Use /exit to exit.");
                coder.sendRawMessage("Lines ending with \\ are combined with the next line you input for multi-line statements.");
                coder.sendRawMessage("Limited tab completion is available.");
            } else {
                coder.sendRawMessage("Welcome to the EvalJS console! Use /exit to exit.");
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not retrieve javascript code for " + coder, ex);
            coder.sendRawMessage("§cCould not retrieve javascript code! See console for details.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permissionsTester.hasPermission(sender)) {
            if (sender instanceof Conversable) {
                if (args.length == 0) {
                    openPrompt((Conversable) sender);
                } else {
                    StringBuilder arg = new StringBuilder();
                    for (String s : args) {
                        arg.append(s);
                    }
                    String code = arg.toString();
                    try {
                        new JavascriptRunner(((Conversable) sender)).eval(code);
                    } catch (ContinuationPending cc) {
                        sender.sendMessage("This code requires reprompting! Open a console to run.");
                    } catch (IOException ex) {
                        getLogger().log(Level.SEVERE, "Could not retrieve javascript code for " + sender.getName(), ex);
                        sender.sendMessage("§cCould not retrieve javascript code! See console for details.");
                    }
                }
            } else {
                sender.sendMessage("§cCannot open javascript prompt: You are not Conversable. (Perhaps you are using an IRC plugin that doesn't support it? All players, and the console, are Conversable.)");
            }
        } else {
            String message = "Player " + sender.getName() + " tried to use EvalJS!";
            Bukkit.broadcast(message, "evaljs.code");
            getLogger().warning(message);
        }
        return true;
    }
    public final Map<UUID, JavascriptRunner> allConvos = new HashMap<>();

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
                getLogger().info("Detected tab completion");
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
    /*public static final int bukkitVersion;

     static {
     int i;
     Class c;
     try {
     c = org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer.class;
     i = 141;
     } catch (NoClassDefFoundError e) {
     try {
     c = org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer.class;
     i = 153;
     } catch (NoClassDefFoundError e2) {
     Bukkit.getLogger().log(Level.WARNING, "[EvalJS] EvalJS is not up to date! Tab completion is disabled.");
     i = 0;
     }
     }
     bukkitVersion = i;
     }
     public static final Field conversationTracker;

     static {
     Field f;
     try {
     switch(bukkitVersion) {
     case 141:
     f = org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer.class.getDeclaredField("conversationTracker");
     break;
     case 153:
     f = org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer.class.getDeclaredField("conversationTracker");
     break;
     default:
     f = null;
     }
     } catch (NoSuchFieldException | SecurityException | NoClassDefFoundError ex) { //NoClassDefFoundError in new versions
     Bukkit.getLogger().log(Level.SEVERE, "[EvalJS] Could not get conversation tracker field! Tab completion is disabled.", ex);
     f = null;
     }
     conversationTracker = f;
     }

     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerChatTabComplete(PlayerChatTabCompleteEvent e) {
     if (e.getPlayer().isConversing() && ((ConversationTracker)(conversationTracker.get(e.getPlayer()))).) {
     }
     }*/
}
