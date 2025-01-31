package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lox.Expr.*;
import lox.Stmt.*;
import lox.Stmt.Class;

public class Resolver implements Expr.Visitor<Expr>, Stmt.Visitor<Stmt> {
    private final Interpreter interpreter;
    private final Stack<Map<String, VariableState>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private LoopType currentLoop = LoopType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    private enum LoopType {
        NONE,
        LOOP
    }

    private enum VariableState {
        DECALRED,
        DEFINED,
        USED
    }

    @Override
    public Stmt visitBlockStmt(Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Stmt visitVarStmt(Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Expr visitAssignExpr(Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Stmt visitFunctionStmt(Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Stmt visitExpressionStmt(Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Stmt visitIfStmt(If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Stmt visitPrintStmt(Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Stmt visitWhileStmt(While stmt) {
        LoopType outerLoopType = currentLoop;
        currentLoop = LoopType.LOOP;
        
        resolve(stmt.condition);
        resolve(stmt.body);

        currentLoop = outerLoopType;
        return null;
    }

    @Override
    public Stmt visitBreakStmt(Break stmt) {
        if (currentLoop == LoopType.NONE) {
            Lox.error(stmt.keyword, "Cant break from top-level code.");
        }
        return null;
    }

    @Override
    public Stmt visitReturnStmt(lox.Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cant return from top-level code.");
        }
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    @Override
    public Stmt visitClassStmt(Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    @Override
    public Expr visitBinaryExpr(Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Expr visitGroupingExpr(Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Expr visitLiteralExpr(Literal expr) {
        return null;
    }

    @Override
    public Expr visitLogicalExpr(Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Expr visitCallExpr(Call expr) {
        resolve(expr.callee);

        for(Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Expr visitUnaryExpr(Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Expr visitTernaryExpr(Ternary expr) {
        resolve(expr.Condition);
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Expr visitVariableExpr(Variable expr) {
        if (!scopes.empty() && scopes.peek().get(expr.name.lexeme) == VariableState.DECALRED) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);

        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, VariableState> scope = scopes.get(i);
            if (scope.containsKey(expr.name.lexeme)) {
                scope.put(expr.name.lexeme, VariableState.USED);
                break;
            }
        }

        return null;
    }

    @Override
    public Expr visitLambdaExpr(Lambda expr) {
        resolveLambda(expr, FunctionType.FUNCTION);
        return expr;
    }


    private void beginScope() {
        scopes.push(new HashMap<String, VariableState>());
    }

    private void endScope() {
        Map<String, VariableState> scope = scopes.peek();
        for (Map.Entry<String, VariableState> entry : scope.entrySet()) {
            if (entry.getValue() != VariableState.USED) {
                System.out.println("Local variable '" + entry.getKey() + "' is never used.");
            }
        }
        scopes.pop();
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void declare(Token name) {
        if(scopes.empty()) return;

        Map<String, VariableState> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, VariableState.DECALRED);
    }

    private void define(Token name) {
        if (scopes.empty()) return;
        scopes.peek().put(name.lexeme, VariableState.DEFINED);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size()-1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size()-1-i);
                return;
            }
        }
    }
    
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        resolveFunctionBody(function.params, function.body, type);
    }
    
    private void resolveLambda(Expr.Lambda expr, FunctionType type) {
        resolveFunctionBody(expr.params, expr.body, type);
    }    

    private void resolveFunctionBody(List<Token> params, List<Stmt> body, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolve(body);
        endScope();
        currentFunction = enclosingFunction;
    }
}
