package lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    protected LoxClass clas;
    protected final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.clas = klass;
    }

    @Override
    public String toString() {
        return "<instance of " + clas.name + ">";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = clas.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object Value) {
        fields.put(name.lexeme, Value);
    }
}
