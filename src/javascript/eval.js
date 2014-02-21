
importClass(Packages.org.bukkit.Bukkit);
importPackage(Packages.com.github.Saposhiente.EvalJS);
var _logger = EvalJS.instance.getLogger();
function _input(s) {
    _logger.info('Console input for ' + _name + ': ' + s);
    _coder.sendRawMessage('> ' + s);
}
function _output(s) {
    _coder.sendRawMessage(s);
}
function _verbose(s) {
}
function _debug(s) {
}
var So = "§";
var me = _coder;
/*var tools = {};
tools.__proto__ = _tools;*/
//tools.getTileEntity = function(block) {
//    return block.getWorld().getTileEntitityAt(block.getX(), block.getY(), block.getZ());
//};
function _userInspect(obj, view, asClass, searchName) {
    if ((view & 0x3) === 0) { //ALL_ACCESS
        view += 0x1; //PUBLIC
    }
    if ((view & 0x1c) === 0) { //ALL_ACCESSIBLES
        view += 0x1c;
    }
    if (obj === undefined) {
        obj = _result;
    }
    if (typeof obj !== "object") {
        _output("Can't inspect primitives!");
        return;
    }
    if (obj instanceof Packages.java.lang.Object) {
        if (asClass === undefined) {
            asClass = obj.getClass();
        }
        view += _inspector.INSTANCE;
    } else if (asClass === undefined) {
        asClass = _inspector.rhinoClassToJava(String(obj));
    }
    if (searchName === undefined) {
        searchName = "";
    }
    _inspector.inspect(obj, asClass, view, searchName);
    return obj;
};
function inspectJS(obj) {
    if (obj === undefined) {
        obj = _result;
    }
    for (var x in obj) {
        _output(x);
    }
    return obj;
}
function inspectJSName(name, obj) {
    if (obj === undefined) {
        obj = _result;
    }
    for (var x in obj) {
        if (x.lastIndexOf(name, 0) === 0) {//startsWith
            _output(x);
        }
    }
    return obj;
}
/*var ALL_ACCESS = 0x3;
 var PUBLIC = 0x1;
 var DECLARED = 0x2;
 var ALL_ACCESSIBLES = 0x1c;
 var FIELDS = 0x4;
 var METHODS = 0x8;
 var CONSTRUCTORS = 0x10;*/

function inspect(obj) {
    return _userInspect(obj, 0);
}
function inspectFields(obj) {
    return _userInspect(obj, 0x4);
}
// <editor-fold defaultstate="collapsed" desc="etc">
function inspectMethods(obj) {
    return _userInspect(obj, 0x8);
}
function inspectConstructors(obj) {
    return _userInspect(obj, 0x10);
}
function inspectName(name, obj) {
    return _userInspect(obj, 0, undefined, name);
}
function inspectNameFields(name, obj) {
    return _userInspect(obj, 0x4, undefined, name);
}
function inspectNameMethods(name, obj) {
    return _userInspect(obj, 0x8, undefined, name);
}
function inspectNameConstructors(name, obj) {
    return _userInspect(obj, 0x10, undefined, name);
}
function inspectAs(clazz, obj) {
    return _userInspect(obj, 0, clazz);
}
function inspectFieldsAs(clazz, obj) {
    return _userInspect(obj, 0x4, clazz);
}
function inspectMethodsAs(clazz, obj) {
    return _userInspect(obj, 0x8, clazz);
}
function inspectConstructorsAs(clazz, obj) {
    return _userInspect(obj, 0x10, clazz);
}
function inspectNameAs(name, clazz, obj) {
    return _userInspect(obj, 0, clazz, name);
}
function inspectNameFieldsAs(name, clazz, obj) {
    return _userInspect(obj, 0x4, clazz, name);
}
function inspectNameMethodsAs(name, clazz, obj) {
    return _userInspect(obj, 0x8, clazz, name);
}
function inspectNameConstructorsAs(name, clazz, obj) {
    return _userInspect(obj, 0x10, clazz, name);
}
function inspectPrivate(obj) {
    return _userInspect(obj, 0x2);
}
function inspectPrivateFields(obj) {
    return _userInspect(obj, 0x2 + 0x4);
}
function inspectPrivateMethods(obj) {
    return _userInspect(obj, 0x2 + 0x8);
}
function inspectPrivateConstructors(obj) {
    return _userInspect(obj, 0x2 + 0x10);
}
function inspectPrivateName(name, obj) {
    return _userInspect(obj, 0x2 + 0, undefined, name);
}
function inspectPrivateNameFields(name, obj) {
    return _userInspect(obj, 0x2 + 0x4, undefined, name);
}
function inspectPrivateNameMethods(name, obj) {
    return _userInspect(obj, 0x2 + 0x8, undefined, name);
}
function inspectPrivateNameConstructors(name, obj) {
    return _userInspect(obj, 0x2 + 0x10, undefined, name);
}
function inspectPrivateAs(clazz, obj) {
    return _userInspect(obj, 0x2 + 0, clazz);
}
function inspectPrivateFieldsAs(clazz, obj) {
    return _userInspect(obj, 0x2 + 0x4, clazz);
}
function inspectPrivateMethodsAs(clazz, obj) {
    return _userInspect(obj, 0x2 + 0x8, clazz);
}
function inspectPrivateConstructorsAs(clazz, obj) {
    return _userInspect(obj, 0x2 + 0x10, clazz);
}
function inspectPrivateNameAs(name, clazz, obj) {
    return _userInspect(obj, 0x2 + 0, clazz, name);
}
function inspectPrivateNameFieldsAs(name, clazz, obj) {
    return _userInspect(obj, 0x2 + 0x4, clazz, name);
}
function inspectPrivateNameMethodsAs(name, clazz, obj) {
    return _userInspect(obj, 0x2 + 0x8, clazz, name);
}
function inspectPrivateNameConstructorsAs(name, clazz, obj) {
    return _userInspect(obj, 0x2 + 0x10, clazz, name);
}
//</editor-fold>

var _reprompt = tools.reprompt;
function find(obj, item, pos) {
    var clazz;
    var viewInstance;
    if (obj instanceof Packages.java.lang.Object) {
        viewInstance = true;
        clazz = obj.getClass();
    } else {
        viewInstance = false;
        clazz = _inspector.rhinoClassToJava(String(obj));
    }
    var items = _inspector.find(clazz, item, viewInstance);
    if (items.size() === 1) {
        return items.get(0);
    } else if (items.size() === 0) {
        _verbose("Nothing found for " + obj + "." + item);
        return null;
    } else {
        if (pos === undefined) {
            for (var i = 0; i < items.size(); i++) {
                _output("[" + i + "] " + _inspector.getDescription(obj, items.get(i), clazz))
            }
            pos = parseInt(_reprompt());
        }
        if (pos === Number.NaN) {
            _verbose("Promting for selection failed.");
            return null;
        }
        return items.get(pos);
    }
}
_debug("Ran eval.js!");