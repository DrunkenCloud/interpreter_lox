package lox;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import lox.Expr.*;
import lox.Stmt.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            public String toString() {
                return "<native clock fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch(RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    Object replEval(Expr expr) {
        return expr.accept(this);
    }

    void replExecute(Stmt stmt) {
        stmt.accept(this);
    }

    String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
    
        return object.toString();
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        Object value = stmt.initializer != null ? evaluate(stmt.initializer) : Environment.UNINITIALIZED;
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitIfStmt(If stmt) {
        if (isTruthy(stmt.condition)) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch(BreakException e) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        LoxFunction function = new LoxFunction(stmt.name.lexeme, stmt.params, stmt.body, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(stmt.name.lexeme, method.params, method.body, environment);
            methods.put(method.name.lexeme, function);
        }

        LoxClass clas = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, clas);

        return null;
    }

    @Override
    public Object visitCallExpr(Call expr) {
        Object callee = evaluate(expr.callee);

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and calles");
        } 

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { 
            arguments.add(evaluate(argument));
        }

        LoxCallable function = (LoxCallable)callee;

        
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + ", got " + arguments.size() + " arguments.");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);

        return value;
    }

    @Override
    public Object visitLambdaExpr(Lambda expr) {
        return new LoxFunction(null, expr.params, expr.body, environment);
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return lookUpVariable(expr.name, expr);
    }
    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                return -(double)right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }

        return null;
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or one must be a string.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) throw new RuntimeError(expr.operator, "Division by Zero Error!!!");
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BITWISE_AND:
                checkIntegerOperands(expr.operator, left, right);
                return (long) left & (long) right;
            case BITWISE_OR:
                checkIntegerOperands(expr.operator, left, right);
                return (long) left | (long) right;
            case BITWISE_XOR:
                checkIntegerOperands(expr.operator, left, right);
                return (long) left ^ (long) right;
            case LEFT_SHIFT:
                checkIntegerOperands(expr.operator, left, right);
                return (long) left << (long) right;
            case RIGHT_SHIFT:
                checkIntegerOperands(expr.operator, left, right);
                return (long) left >> (long) right;
            default:
                break;
        }

        return null;
    }

    @Override
    public Object visitTernaryExpr(Ternary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        Object condition = evaluate(expr.Condition);
        Token operator = expr.operator;
        if (!isBoolable(condition)) {
            throw new RuntimeError(operator, "Condition is not logical operation");
        }
        return ((Boolean)condition) ? left : right;
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);
        
        Integer dist = locals.get(expr);
        if (dist != null) {
            environment.assignAt(dist, expr.name, value);
        } else {
            environment.assign(expr.name, value);
        }

        return value;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private void checkIntegerOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            if (((Double) left).longValue() == (double) left && ((Double) right).longValue() == (double) right) {
                return;
            }
        }
        throw new RuntimeError(operator, "Operands must be integers.");
    }

    private void checkNumberOperand(Token operator, Object object) {
        if (object instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be numbers.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private Boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;

        Boolean result = true;

        if (object instanceof Expr) {
            Object value = evaluate((Expr)object);
            if (isBoolable(value)) result = (Boolean)value;
        }
        
        return result;
    }

    private Boolean isBoolable(Object object) {
        return object instanceof Boolean;
    }
    
    private Boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer dist = locals.get(expr);
        if (dist != null) {
            return environment.getAt(dist, name.lexeme);
        }
        return globals.get(name);
    }
}