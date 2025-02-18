package lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> staticMethods;

    LoxClass(String name, Map<String, LoxFunction> methods, Map<String, LoxFunction> staticMethods) {
        this.name = name;
        this.methods = methods;
        this.staticMethods = staticMethods;
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


    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    public LoxFunction findStaticMethod(String name) {
        if (staticMethods.containsKey(name)) {
            return staticMethods.get(name);
        }
        return null;
    }
}
