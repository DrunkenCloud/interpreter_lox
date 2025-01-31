package lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass clas;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass clas) {
        this.clas = clas;
    }

    @Override
    public String toString() {
        return "<instance of " + clas.name + " >";
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = clas.findMethod(name.lexeme);
        if (method != null) return method;

        throw new RuntimeError(name, "Unefined Property '" + name.lexeme + "'.");
    }

    void set(Token name, Object Value) {
        fields.put(name.lexeme, Value);
    }
}
