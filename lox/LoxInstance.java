package lox;

import java.util.ArrayList;
import java.util.HashMap;
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
    
        LoxFunction method = clas.findMethod(name.lexeme);
        if (method != null) {
            if (method.arity() == 0) {
                return method.call(interpreter, new ArrayList<>());
            }
            return method;
        }
        
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }    

    void set(Token name, Object Value) {
        fields.put(name.lexeme, Value);
    }
}
