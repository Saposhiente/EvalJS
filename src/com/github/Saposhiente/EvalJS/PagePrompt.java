package com.github.Saposhiente.EvalJS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;

/**
 *
 * @author Saposhiente
 */
public class PagePrompt extends ValidatingPrompt {

    private final List<String> pages;
    private int pos = 0;
    
    private final CodePrompt callback;
    
    private boolean noDisplay = false;

    public PagePrompt(List<String> pages, CodePrompt callback) {
        this.pages = pages;
        this.callback = callback;
    }

    @Override
    public String getPromptText(ConversationContext cc) {
        //BUG: When Pager first becomes the active prompt, it doesn't display anything (works only once you provide 1 input). This method isn't called at all. I have no idea why.
        if (noDisplay) {
            return "";
        }
        return pages.get(pos) + "Page " + (pos + 1) + "/" + pages.size() + ". n:next p:prev §o#§r:page §o#§r q:quit";//§o = italics §r = end of italics
    }

    private static final Set<String> validInputs = new HashSet<>();
    static {
        validInputs.add("n");
        validInputs.add("p");
        validInputs.add("e");
        validInputs.add("q");
    }
    @Override
    protected boolean isInputValid(ConversationContext cc, String string) {
        
        if (string.startsWith("/") || validInputs.contains(string.toLowerCase())) {
            return true;
        }
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext cc, String string) {
        switch (string.toLowerCase()) {
            case "n":
                if (pos + 1 >= pages.size()) {
                    cc.getForWhom().sendRawMessage("Invalid page!");
                    noDisplay = true;
                } else {
                    pos++;
                    noDisplay = false;
                }
                return this;
            case "p":
                if (pos - 1 <= 0) {
                    cc.getForWhom().sendRawMessage("Invalid page!");
                    noDisplay = true;
                } else {
                    pos--;
                    noDisplay = false;
                }
                return this;
            case "e":
            case "q":
                return callback;
            default:
                if (string.startsWith("/")) {
                    noDisplay = true;
                    return callback.doCommand(string, this);
                } else {
                    int next = Integer.parseInt(string);
                    if (pos < 1 || pos > pages.size()) {
                        cc.getForWhom().sendRawMessage("Invalid page!");
                        noDisplay = true;
                    } else {
                        pos = next - 1;
                        noDisplay = false;
                    }
                    return this;
                }
        }
    }
    public static final int LINECHARS = 44;//low estimate; font is not monospaced
    public static final int TEXTLINES = 20;

    public static List<String> getPages(String result) {
        int lineNum = 0;
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        String[] lines = result.split("\n");
        for (String line : lines) {
            int size = (line.length() - 1) / LINECHARS + 1;
            lineNum += size;
            if (lineNum >= TEXTLINES - 1) {
                pages.add(page.toString());
                page = new StringBuilder();
                lineNum = size;
            }
            page.append(line).append("\n");
        }
        pages.add(page.toString());
        return pages;
    }
}
