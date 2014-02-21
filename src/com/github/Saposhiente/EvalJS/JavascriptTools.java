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
 * Toolset for scripts.
 * @author Saposhiente
 */
public class JavascriptTools {

    /*public static final boolean isUpToDate;
    static {
        boolean b;
        try {
            b = Bukkit.getServer() instanceof org.bukkit.craftbukkit.v1_5_R3.CraftServer;
        } catch (Throwable t) {
            b = false;
        }
        isUpToDate = b;
    }*/
    public final JavascriptRunner bridge;

    /*public Object getTileEntity(CraftBlock block) { Instead made as javascript tool because it is unlikely to be affected by versions
    return ((CraftWorld)(block.getWorld())).getTileEntityAt(block.getX(), block.getY(),  block.getZ();
    }*/
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
    /** Do not call from outside JS! */
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
