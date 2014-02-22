package com.github.Saposhiente.EvalJS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;

/**
 * Useful Java methods provided to the user in the JavaScript console.
 * @author Saposhiente
 */
public class JavascriptTools {

    public final JavascriptRunner bridge;
    
    public JavascriptTools(JavascriptRunner bridge) {
        this.bridge = bridge;
    }
    
    public Block getTargetBlock() {
        return bridge.getOwner() instanceof Player ? ((Player)bridge.getOwner()).getTargetBlock(null, 7) : null;
    }
    
    public static File playerDataFolder = null; //set in onEnable
    public class JSNBTTools {
        public NBTOutputStream getOutputStream(String filename) throws IOException {
            File target = new File(Bukkit.getWorldContainer(), filename);
            if (!target.exists()) {
                target.createNewFile();
                bridge.verbose("Created new file " + filename + " for NBT");
            }
            return new NBTOutputStream(new FileOutputStream(target));
        }
        public NBTOutputStream getPlayerOutputStream(String filename) throws IOException {
            if (playerDataFolder == null) {
                bridge.output("Can't get player output stream: Player data folder not found at startup.");
                return null;
            }
            File target = new File(playerDataFolder, filename);
            if (!target.exists()) {
                target.createNewFile();
                bridge.verbose("Created new file " + filename + " for NBT");
            }
            return new NBTOutputStream(new FileOutputStream(target));
        }
        public NBTInputStream getInputStream(String filename) throws IOException {
            File target = new File(Bukkit.getWorldContainer(), filename);
            if (!target.exists()) {
                bridge.output("No such file.");
                return null;
            }
            return new NBTInputStream(new FileInputStream(target));
        }
        public NBTInputStream getPlayerInputStream(String filename) throws IOException {
            if (playerDataFolder == null) {
                bridge.output("Can't get player output stream: Player data folder not found at startup.");
                return null;
            }
            File target = new File(playerDataFolder, filename);
            if (!target.exists()) {
                bridge.output("No such file.");
                return null;
            }
            return new NBTInputStream(new FileInputStream(target));
        }
    }
    public final JSNBTTools nbt = new JSNBTTools();
    
    /**
     * Prompts the user for more input. Returns null if this is not possible.
     * Marked as Deprecated because this function is only to be called from within JavaScript code.
     * @return The user's new input.
     * @throws ContinuationPending Throws this as part of implementation. Do not catch this exception.
     * @deprecated Public and static only out of necessity. Do not call this function from outside of JavaScript.
     */
    @Deprecated
    public static String reprompt() throws ContinuationPending {
        if (!EvalJS.instance.reprompt) return null;
        Context cx = Context.enter();
        try {
            throw cx.captureContinuation();
        } finally {
            Context.exit();
        }
    }
}
