package lox;

import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> staticMethods;
    private final Map<String, LoxFunction> getters;
    private final Map<String, LoxFunction> staticGetters;

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods, Map<String, LoxFunction> staticMethods, Map<String, LoxFunction> getters, Map<String, LoxFunction> staticGetters) {
        super(null);
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
        this.staticMethods = staticMethods;
        this.getters = getters;
        this.staticGetters = staticGetters;
        this.clas = this;

        for (LoxFunction function : staticMethods.values()) {
            function.bind(this);
        }
        for (LoxFunction function: staticGetters.values()) {
            function.bind(this);
        }
    }

    @Override
    public String toString() {
        return "<class " + name + ">";
    }

    @Override
    public int arity() {
        LoxFunction init = findMethod("init");
        if (init == null) return 0;
        return init.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    public LoxFunction findGetter(String name) {
        if (getters.containsKey(name)) {
            return getters.get(name);
        }
        if (superclass != null) {
            return superclass.findGetter(name);
        }
        return null;
    }
    
    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        if (superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }

    public LoxFunction findStaticMethod(String name) {
        if (staticMethods.containsKey(name)) {
            return staticMethods.get(name);
        }
        if (superclass != null) {
            return superclass.findStaticMethod(name);
        }
        return null;
    }

    public LoxFunction findStaticGetter(String name) {
        if (staticGetters.containsKey(name)) {
            return staticGetters.get(name);
        }
        if (superclass != null) {
            return superclass.findStaticGetter(name);
        }
        return null;
    }
}
