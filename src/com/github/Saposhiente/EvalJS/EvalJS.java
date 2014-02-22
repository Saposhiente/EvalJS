package com.github.Saposhiente.EvalJS;

import com.github.Saposhiente.ClassDefSubstitutor.ClassDefSubstitutor;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.ContinuationPending;

/**
 * Main plugin class. Manages configuration and opens code prompts.
 * @author Saposhiente
 */
public class EvalJS extends JavaPlugin {

    static EvalJS instance;
    public static EvalJS getInstance() {
        return instance;
    }
    public static final boolean debug = false;
    
    ClassDefSubstitutor substitutor;
    
    public File macroFolder;
    
    public boolean alwaysAllowConsole;
    public List<String> admins;
    private int timeout;
    public boolean enabled;
    public boolean showWelcome;
    private boolean hideChat;
    public boolean reprompt;
    
    private TabCompletionManager tabCompletionManager;
    
    protected ConversationFactory convFactory;

    public int getTimeout() {
        return timeout;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        convFactory = convFactory.withTimeout(timeout);
    }
    public boolean isHideChat() {
        return hideChat;
    }
    public void setHideChat(boolean hideChat) {
        this.hideChat = hideChat;
        convFactory = convFactory.withModality(hideChat);
    }

    @Override
    public void onEnable() {
        getConfig().options().copyHeader(true);
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        instance = this;
        
        File linuxSystem = new File("etc");
        if (debug && linuxSystem.exists()) getLogger().info("Detected Linux OS");
        if (linuxSystem.exists() && linuxSystem.canWrite()) {
            getLogger().log(Level.SEVERE, "The server has access to system files! (Linux system folder /etc/)\nThis is a serious security risk, and an unacceptable one if you are running EvalJS. (One minecraft.net error, or one admin leaving their Minecraft window open, ore one admin getting the computer hacked, and BAM! This whole computer could be easily taken over and all of your data stolen.)\nCreate a separate user, who is not an administrator, to run the Minecraft server.\nShutting down server...");
            Bukkit.shutdown();
            return;
        }
        File windowsSystem = new File("Windows");
        if (debug && windowsSystem.exists()) getLogger().info("Detected Windows OS");
        if (windowsSystem.exists() && windowsSystem.canWrite()) {
            getLogger().log(Level.SEVERE, "The server has access to system files! (Windows system folder C:\\Windows\\)\nThis is a serious security risk, and an unacceptable one if you are running EvalJS. (One minecraft.net error, or one admin leaving their Minecraft window open, ore one admin getting the computer hacked, and BAM! This whole computer could be easily taken over and all of your data stolen.)\nCreate a separate user, who is not an administrator, to run the Minecraft server.\nShutting down server...");
            Bukkit.shutdown();
            return;
        }
        
        substitutor = new ClassDefSubstitutor(getLogger());
        
        macroFolder = new File(getDataFolder(), "macros");
        
        alwaysAllowConsole = getConfig().getBoolean("alwaysAllowConsole");
        admins = getConfig().getStringList("admins");
        timeout = getConfig().getInt("timeout");
        showWelcome = getConfig().getBoolean("showWelcome");
        hideChat = getConfig().getBoolean("hideChat");
        final boolean tabComplete = getConfig().getBoolean("tabComplete");
        reprompt = getConfig().getBoolean("reprompt");
        
        convFactory = new ConversationFactory(this).withTimeout(timeout).withLocalEcho(false).withModality(hideChat);
        if (tabComplete) {
            if (tabCompletionManager == null) tabCompletionManager = new TabCompletionManager();
            convFactory = convFactory.addConversationAbandonedListener(tabCompletionManager);
            Bukkit.getPluginManager().registerEvents(tabCompletionManager, this);
        } else {
            tabCompletionManager = null;
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

    @Override
    public void onDisable() {
        if (tabCompletionManager != null) tabCompletionManager.reset();
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
                        sender.sendMessage(Color.RED + "Could not retrieve javascript code! See console for details.");
                    }
                }
            } else {
                sender.sendMessage(Color.RED + "Cannot open javascript prompt: You are not Conversable. (Perhaps you are using an IRC plugin that doesn't support it? All players, and the console, are Conversable.)");
            }
        } else {
            String message = "Player " + sender.getName() + " tried to use EvalJS!";
            Bukkit.broadcast(message, "evaljs.code");
            getLogger().warning(message);
        }
        return true;
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

    public void openPrompt(Conversable coder) {
        try {
            CodePrompt prompt = new CodePrompt(new JavascriptRunner(coder), coder);
            //Conversation conv = convFactory.withFirstPrompt(prompt).buildConversation(coder);
            //prompt.conversation = conv;
            prompt.bridge.debug(prompt.conversation.toString());
            coder.beginConversation(prompt.conversation);
            prompt.bridge.debug("Started conversation!");
            if (tabCompletionManager != null && coder instanceof Player) {
                tabCompletionManager.addConversation(((Player) coder).getUniqueId(), prompt.bridge);
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
            coder.sendRawMessage(Color.RED + "Could not retrieve javascript code! See console for details.");
        }
    }

}
