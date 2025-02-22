package lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final String name;
    private final List<Token> parameters;
    private final List<Stmt> body;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(String name, List<Token> parameters, List<Stmt> body, Environment closure, boolean isInitializer) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(name, parameters, body, environment, this.isInitializer);
    }

    LoxFunction bind(LoxClass klas) {
        Environment environment = new Environment(closure);
        environment.define("this", klas);
        return new LoxFunction(name, parameters, body, environment, this.isInitializer);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < parameters.size(); i++) {
            environment.define(parameters.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(body, environment);
        } catch (Return returnValue) {
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    @Override
    public int arity() {
        return parameters.size();
    }

    @Override
    public String toString() {
        return name == null ? "<lambda>" : "<fn " + name + ">";
    }
}
