package lox;

import java.util.List;

import lox.Expr.*;
import lox.Stmt.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) execute(statement);
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
    public Object visitVariableExpr(Variable expr) {
        return environment.get(expr.name);
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
        environment.assign(expr.name, value);
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
}