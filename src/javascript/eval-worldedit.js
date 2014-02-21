//Eval.js v0.6.1 Worldedit craftscript edition. No further updates are planned for this edition; for additional features use the standalone version at
//dev.bukkit.org/
importClass(Packages.java.io.File);
importClass(Packages.java.util.Scanner);
importPackage(Packages.org.bukkit);
var eval_macrosFolder = new File(Bukkit.getPluginManager().getPlugin("WorldEdit").getDataFolder(), "EvalJS");
var eval_macroBufferSize = 200;
function exec(script) {
    var target = new File(eval_macrosFolder, script); //don't test for target's existance, allow Java to throw exception
    Bukkit.getPluginManager().getPlugin("WorldEdit").getLogger().info("Running " + target);
    var scanner = new Scanner(target);
    var read = "";
    eval_macroBufferSize = 200; //default
    if (scanner.hasNextLine()) {
        eval(scanner.nextLine()); //to allow changing of eval_macroBufferSize on line 1
    }
    if (eval_macroBufferSize === null) {
        while (scanner.hasNextLine()) {
            read += scanner.nextLine();
        }
        if (read !== "") {
            eval(read);
        }
    } else {
        var num = 0;
        while (scanner.hasNextLine()) {
            read += scanner.nextLine();
            if (num < eval_macroBufferSize) {
                num++;
            } else {
                eval(read);
                num = 0;
            }
        }
        if (num !== 0) {
            eval(read);
        }
    }
}
//###Config
var admin, timeout, unauthorized_warn, unauthorized_log, unauthorized_notify, unauthorized_kick, eval_usePluginKick, unauthorized_ban, eval_usePluginBan;
exec("config.js");
//###Imports
importClass(Packages.java.lang.Class);
//importClass(Packages.java.lang.NoClassDefFoundError); used in eval_reflect (which is currently broken)
importPackage(Packages.java.lang.reflect);
importPackage(Packages.java.util.logging);
importPackage(Packages.org.bukkit.conversations);
var eval_upToDate = Bukkit.getVersion().contains("1.5.2-R1.0");
///###Security
var caller = Bukkit.getPlayerExact(player.getName());
var eval_player = Bukkit.getPlayerExact(admin);
function security_fail(isTimeout) {
    var message = "Unauthorized player §6" + player.getName() + "§c attempted to use eval.js" + isTimeout ? "§6 while it was disabled." : "!";
    if (unauthorized_warn) {
        player.printError(isTimeout? "eval.js is disabled.": "You are not allowed to use eval.js!");
    }
    if (unauthorized_log) {
        Bukkit.getPluginManager().getPlugin("WorldEdit").getLogger().warning(message); //don't use custom logger to prevent unauthed players from changing state
    }
    if (unauthorized_notify) {
        var pmessage = "\u00A7c" + message;
        var recipients = Bukkit.getOnlinePlayers();
        for (var i=0; i<recipients.length; i++) {
            var recipient = recipients[i];
            if (recipient.hasPermission("evaljs.notify")) {
                recipient.sendMessage(pmessage);
            }
        }
    }
}
function eval_kick(player, message) {
    if (eval_usePluginKick) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/kick " + player.getName() + " " + message);
    } else {
        player.kickPlayer(message);
    }
}
function eval_ban(player, message) {
    if (eval_usePluginBan) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/ban " + player.getName() + " " + message);
    } else {
        player.setBanned(true);
        eval_kick(player, message);
    }
}
function doSecurity() {
    if (caller === null) {
        Bukkit.getPluginManager().getPlugin("WorldEdit").getLogger().severe("§cNonexistant/Logged out player §6" + player.getName() + "§c attempted to use eval.js!");
        return false;
    }
    if (!eval_player.equals(caller)) {
        security_fail(false);
        if (unauthorized_ban) {
            eval_ban(player, "Automatically banned from server:\nYou are not allowed to use eval.js!");
        } else if (unauthorized_kick) {
            eval_kick(player, "Automatically kicked from server:\nYou are not allowed to use eval.js!");
        }
        return false;
    }
    if (timeout !== null) {
        var today = new Date();
        var max = new Date(timeout);
        if (max < today) {
            security_fail(true);
            return false;
        }
    }
    return true;
}
if (doSecurity()) {
    //###Prompt
    var eval_worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit"); //TODO: make self plugin
    var eval_logger = eval_worldEdit.getLogger();
    var eval_error = false;
    var eval_cmd = false;
    var eval_result = "Entering Eval.js console";
    var result = null;
    var eval_prevInput = "";
    var eval_convFactory;
    var eval_conversation;
    var eval_prompt;
    var eval_jsprompt = {
        getPromptText: function (convContext) { 
            if (eval_cmd) {
                return "";
            } else if (eval_error) {
                return "Error: " + eval_result;
            } else {
                return String(eval_result);
            }
        }, 
        acceptInput: function (convContext, input) { //don't use convContext to ensure data is saved across abandonment
            eval_input(input);
            if (input.length() === 0) {
                return eval_prompt;
            }
            if (input.endsWith("\\")) {
                eval_prevInput += input.substring(0, input.length() - 1);
                eval_cmd = true;
                return eval_prompt;
            }
            if (input.length() > 1 && input.endsWith("\\n")) {
                eval_prevInput += input.substring(0, input.length() - 2) + "\n";
                eval_cmd = true;
                return eval_prompt;
            }
            input = new Packages.java.lang.String(eval_prevInput + input);
            eval_prevInput = "";
            if (input.startsWith("/")) {
                eval_cmd = true;
                if (input.equalsIgnoreCase("/exit")) {
                    eval_output("Exiting Eval.js console");
                    return Prompt.END_OF_CONVERSATION;
                } else {
                    eval_player.abandonConversation(eval_conversation); // abandon a conversation while determing its fate? nothing could possibly go wrong
                    if (input.toLowerCase().startsWith("/chat ")) { 
                        eval_player.chat(input.substring(6));
                    } else {
                        Bukkit.dispatchCommand(eval_player, input.substring(1));
                    }
                    eval_conversation = eval_convFactory.buildConversation(eval_player);
                    eval_player.beginConversation(eval_conversation);
                    return Prompt.END_OF_CONVERSATION; //logic.
                } //sometimes I amuse myself with my programming
            } else {
                eval_cmd = false;
                try {
                    eval_result = eval(String(input));
                    eval_error = false;
                } catch(err) {
                    eval_result = err;
                    eval_error = true;
                }
                result = eval_result;
            }
            return eval_prompt; 
        }
    };
    eval_prompt = new StringPrompt(eval_jsprompt);
    eval_convFactory = new ConversationFactory(eval_worldEdit).withFirstPrompt(eval_prompt).withLocalEcho(false).withTimeout(300);
    eval_conversation = eval_convFactory.buildConversation(eval_player);
    eval_player.beginConversation(eval_conversation);
    //###Convenience
    var So = "§";//chat formatting symbol
    var me = eval_player;
    function getTargetBlock() {
        return eval_player.getTargetBlock(null, 7);
    }
    //###Misc
    function eval_toStr(o) {
        if (o === null) {
            return "<null>";
        } else {
            return o;
        }
    }
    function eval_getClass(javaClass) { //for Rhino object JavaClass
        var str = String(javaClass); //[JavaClass foo.bar]
        var end = str.length - 1;
        var start = str.lastIndexOf(" ", end) + 1;
        return Class.forName(str.substring(start, end));
    }
    //###Output
    function eval_input(s) {
        eval_logger.info("Console input for " + eval_player.getName() + ": " + s);
        eval_player.sendRawMessage("> " + s);
    }
    function eval_output(s) {
        eval_player.sendRawMessage(s);
    }
    function eval_verbose(s) { //additional information eg. fields skipped by inspect();
    }
    function eval_debug(s) {
    }
    //###Inspection internals
    function eval_getReturnTypeNameOf(method) {
        var clazz = method.getReturnType();
        if (clazz.getTypeParameters().length === 0) {
            return clazz.getSimpleName();
        } else {
            return method.getGenericReturnType().toString();
        }
    }
    function eval_getTypeNameOf(field) {
        var clazz = field.getType();
        if (clazz.getTypeParameters().length === 0) {
            return clazz.getSimpleName();
        } else {
            return field.getGenericType().toString();
        }
    }
    function eval_getParameterNameOf(genericParameters, parameters, pos) {
        var clazz = parameters[pos];
        if (clazz.getTypeParameters().length === 0) {
            return clazz.getSimpleName();
        } else {
            return genericParameters[pos].toString();
        }
    }
    /*function eval_nameOf(clazz) {
    var s = clazz.getSimpleName();
    
    var types = clazz.getTypeParameters();
    if (types.length != 0) {
        s += "<";
        for (var i=0; ;) {
            s += types[i].toString();
            i++;
            if (i<types.length) {
                s += ", ";
            } else {
                s += ">";
                break;
            }
        }
    }
    return s;
}*/
    function eval_printMethod(obj, method) {
        var parameters = method.getParameterTypes();
        var noparams = parameters.length === 0;
        if (noparams && !Modifier.isAbstract(method.getModifiers()) && ( 
            (method.getName().startsWith("get") && method.getName().length() > 3 && Packages.java.lang.Character.isUpperCase(method.getName().charAt(3)) 
                && !method.getGenericReturnType().toString().equals("void") && !method.getGenericReturnType().toString().equals("boolean")) 
            ||(method.getName().startsWith("is") && method.getName().length() > 2 && Packages.java.lang.Character.isUpperCase(method.getName().charAt(2)) 
                && method.getGenericReturnType().toString().equals("boolean")) ) ) {
            try {
                if (!Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }
                eval_output(Modifier.toString(method.getModifiers()) + " " + eval_getReturnTypeNameOf(method) + " " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "() --> " + eval_toStr(method.invoke(obj)));
            } catch (ex) {
                eval_output("..Could not execute protected method " + method.getName() + ". See console for details.");
                eval_logger.log(Level.SEVERE, "Could not execute protected method " + method.getName() + ": " + ex.message, ex.javaException);
            }
        } else {
            var s;
            if (!noparams) {
                var genericParameters = method.getGenericParameterTypes();
                s = "(";
                for (var i=0; ;) {
                    s += eval_getParameterNameOf(genericParameters, parameters, i);
                    i++;
                    if (i<parameters.length) {
                        s += ", ";
                    } else {
                        s += ")";
                        break;
                    }
                }
            } else {
                s = "()";
            }
            eval_output(Modifier.toString(method.getModifiers()) + " " + eval_getReturnTypeNameOf(method) + " " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + s);
        }
    }

    function eval_printField(obj, field) {
        try {
            if (!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            eval_output(Modifier.toString(field.getModifiers()) + " " + eval_getTypeNameOf(field) + " " + field.getDeclaringClass().getSimpleName() + "." + field.getName() + " --> " + eval_toStr(field.get(obj)));
        } catch (ex) {
            eval_output("..Could not view protected field " + field.getName() + ". See console for details.");
            eval_logger.log(Level.SEVERE, "Could not view protected field " + field.getName() + ": ", ex.javaException);
        }
    }
    function eval_printConstructor(clazz, constructor) {
        var parameters = constructor.getParameterTypes();
        var noparams = parameters.length === 0;
        var s;
        if (!noparams) {
            var genericParameters = constructor.getGenericParameterTypes();
            s = "(";
            for (var i=0; ;) {
                s += eval_getParameterNameOf(genericParameters, parameters, i);
                i++;
                if (i<parameters.length) {
                    s += ", ";
                } else {
                    s += ")";
                    break;
                }
            }
        } else {
            s = "()";
        }
        eval_output(Modifier.toString(constructor.getModifiers()) + " " + clazz.getSimpleName() + s);
    }
    /*function eval_reflect(clazz, type, accessibleObject) {//doesn't work yet; needs ASM
        var get = "get" + type + accessibleObject;//accessibleObject = method/field/constructor
        try {
            var result = clazz[get + "s"](); //getDeclaredMethods()
            return result;
        } catch (e if e.javaException instanceof NoClassDefFoundError) { //Thrown by Rhino methods, can't be caught! :(
            //} catch (e) {
            result = new Array();
            var pos = 0;
            for (var property in clazz) {
                try {
                    if (accessibleObject === "Method") { //have to get the parameter types for overloaded functions
                        var methodSources = clazz[property].toSource().split("\n");
                        var max = methodSources.length - 1; //skip first and last elements of methodSources;
                        var paramLists = new Array();
                        for (var source = 1; source < max; source++) {
                            var paramNamesCSV = methodSources[source].split("(", 2)[1];
                            var paramNames = paramNamesCSV.substring(0, paramNamesCSV.length - 1).split(",");
                            var params = new Array();
                            paramNames.map(function (name, pos) {
                                params[pos] = Class.forName(name);
                            });
                            paramLists[source-1] = params;
                        }
                        for (var i = 0; i<paramLists.length; i++) {
                            var accessible = clazz[get](property, paramLists[i]);
                            if (accessible !== null) {
                                result[pos] = accessible;
                                pos++;
                            }
                        }
                    } else {
                        accessible = clazz[get](property);
                        if (accessible !== null) {
                            result[pos] = accessible;
                            pos++;
                        }
                    }
                } catch (e if e.javaException instanceof NoClassDefFoundError) {
                    //} catch (e) {
                    eval_verbose("Skipping " + accessibleObject + " " + property + ": NoClassDefFound (probably from an unloaded plugin)");
                }
            }
            return result;
        }
    }*/
    function eval_inspectAccessibles(accessibleObject, obj, type, name, anyName, isStatic, clazz) {//accessibleObject = method/field/constructor
        //var methods = eval_reflect(clazz, type, "Method");
        var accessibles = clazz["get" + type + accessibleObject + "s"]();//eg. getDeclaredMethods()
        for (var i=0; i<accessibles.length; i++) {
            if ((!isStatic || Modifier.isStatic(accessibles[i].getModifiers())) && (anyName || accessibles[i].getName().startsWith(name))){
                this["eval_print" + accessibleObject](obj, accessibles[i]); //eval_printField, etc
            //if static, pass JavaClass object to print, but ok because it's ignored for static methods
            }
        }
    }
    function eval_inspect(obj, type, clazz, accessibleObject, name) { //type = getMethods vs getDeclaredMethods
        var allAccessibles = accessibleObject === undefined;
        var anyName = name === undefined;
        if (typeof obj !== "object") {
            eval_output("Can't inspect primitives!");
            return;
        }
        var isStatic = !obj instanceof Packages.java.lang.Object;
        if (clazz === undefined) {
            clazz = isStatic ? eval_getClass(obj) : obj.getClass();
        }
        if (allAccessibles) {
            eval_debug("Inspecting all accessibles");
            eval_inspectAccessibles("Method", obj, type, name, anyName, isStatic, clazz);
            eval_inspectAccessibles("Field", obj, type, name, anyName, isStatic, clazz);
            if (isStatic) {
                eval_inspectAccessibles("Constructor", obj, type, name, anyName, isStatic, clazz);
            }
        } else {
            eval_inspectAccessibles(accessibleObject, obj, type, name, anyName, isStatic, clazz);
        }
    }
    function eval_userInspect(obj, type, clazz, accessibleObject, name) { //type = getMethods vs getDeclaredMethods
        if (obj === undefined) {
            obj = eval_result;
        }
        eval_inspect(obj, type, clazz, accessibleObject, name);
        return obj;
    }
    //###Inspect() function family
    function inspectJS(obj) {
        if (obj === undefined) {
            obj = eval_result;
        }
        for (var x in obj) {
            eval_output(x);
        }
        return obj;
    }
    function inspectJSName(name, obj) {
        if (obj === undefined) {
            obj = eval_result;
        }
        for (var x in obj) {
            if (x.lastIndexOf(name, 0) === 0) {//startsWith
                eval_output(x);
            }
        }
        return obj;
    }
    function inspect(obj) {
        return eval_userInspect(obj, "");
    }
    function inspectFields(obj) {
        return eval_userInspect(obj, "", undefined, "Field");
    }
    function inspectMethods(obj) {
        return eval_userInspect(obj, "", undefined, "Method");
    }
    function inspectConstructors(obj) {
        return eval_userInspect(obj, "", undefined, "Constructor");
    }
    function inspectName(name, obj) {
        return eval_userInspect(obj, "", undefined, undefined, name);
    }
    function inspectNameFields(name, obj) {
        return eval_userInspect(obj, "", undefined, "Field", name);
    }
    function inspectNameMethods(name, obj) {
        return eval_userInspect(obj, "", undefined, "Method", name);
    }
    function inspectNameConstructors(name, obj) {
        return eval_userInspect(obj, "", undefined, "Constructor", name);
    }
    function inspectAs(clazz, obj) {
        return eval_userInspect(obj, "", clazz, undefined);
    }
    function inspectFieldsAs(clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Field");
    }
    function inspectMethodsAs(clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Method");
    }
    function inspectConstructorsAs(clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Constructor");
    }
    function inspectNameAs(name, clazz, obj) {
        return eval_userInspect(obj, "", clazz, undefined, name);
    }
    function inspectNameFieldsAs(name, clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Field", name);
    }
    function inspectNameMethodsAs(name, clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Method", name);
    }
    function inspectNameConstructorsAs(name, clazz, obj) {
        return eval_userInspect(obj, "", clazz, "Constructor", name);
    }
    function inspectPrivate(obj) {
        return eval_userInspect(obj, "Declared");
    }
    function inspectPrivateFields(obj) {
        return eval_userInspect(obj, "Declared", undefined, "Field");
    }
    function inspectPrivateMethods(obj) {
        return eval_userInspect(obj, "Declared", undefined, "Method");
    }
    function inspectPrivateConstructors(obj) {
        return eval_userInspect(obj, "Declared", undefined, "Constructor");
    }
    function inspectPrivateName(name, obj) {
        return eval_userInspect(obj, "Declared", undefined, undefined, name);
    }
    function inspectPrivateNameFields(name, obj) {
        return eval_userInspect(obj, "Declared", undefined, "Field", name);
    }
    function inspectPrivateNameMethods(name, obj) {
        return eval_userInspect(obj, "Declared", undefined, "Method", name);
    }
    function inspectPrivateNameConstructors(name, obj) {
        return eval_userInspect(obj, "Declared", undefined, "Constructor", name);
    }
    function inspectPrivateAs(clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, undefined);
    }
    function inspectPrivateFieldsAs(clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Field");
    }
    function inspectPrivateMethodsAs(clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Method");
    }
    function inspectPrivateConstructorsAs(clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Constructor");
    }
    function inspectPrivateNameAs(name, clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, undefined, name);
    }
    function inspectPrivateNameFieldsAs(name, clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Field", name);
    }
    function inspectPrivateNameMethodsAs(name, clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Method", name);
    }
    function inspectPrivateNameConstructorsAs(name, clazz, obj) {
        return eval_userInspect(obj, "Declared", clazz, "Constructor", name);
    }
    //###Other
    function getTileEntity(block) {
        return block.chunk.getHandle().world.getTileEntitity(block.getX(), block.getY(), block.getZ());
    }
}