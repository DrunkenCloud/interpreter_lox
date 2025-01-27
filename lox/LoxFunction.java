package lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final String name;
    private final List<Token> parameters;
    private final List<Stmt> body;
    private final Environment closure;

    LoxFunction(String name, List<Token> parameters, List<Stmt> body, Environment closure) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.closure = closure;
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
            return returnValue.value;
        }
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
