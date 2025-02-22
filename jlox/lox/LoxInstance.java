package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxInstance {
    protected LoxClass clas;
    protected final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass clas) {
        this.clas = clas;
    }

    @Override
    public String toString() {
        return "<instance of " + clas.name + ">";
    }

    Object get(Token name, Interpreter interpreter) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction getter = clas.findGetter(name.lexeme);
        if (getter != null) {
            getter = getter.bind(this);
            return getter.call(interpreter, List.of());
        }

        LoxFunction method = clas.findMethod(name.lexeme);
        if (method != null) {
            return method.bind(this);
        }

        LoxFunction staticGetter = clas.findStaticGetter(name.lexeme);
        if (staticGetter != null) {
            staticGetter = staticGetter.bind(clas);
            return staticGetter.call(interpreter, List.of());
        }

        LoxFunction staticMethod = clas.findStaticMethod(name.lexeme);
        if (staticMethod != null) {
            return staticMethod.bind(clas);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object Value) {
        fields.put(name.lexeme, Value);
    }
}
