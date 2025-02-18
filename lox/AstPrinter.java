package lox;

import lox.Expr.*;

public class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitLogicalExpr(Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Call expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(call ").append(expr.callee.accept(this));
        for (Expr argument : expr.arguments) {
            builder.append(" ").append(argument.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitLambdaExpr(Lambda expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(lambda (");
        for (Token param : expr.params) {
            builder.append(param.lexeme).append(" ");
        }
        if (!expr.params.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(") ");
        for (Stmt statement : expr.body) {
            builder.append(statement.toString()).append(" ");
        }
        if (!expr.body.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitTernaryExpr(Ternary expr) {
        return parenthesize("ternary", expr.Condition, expr.left, expr.right);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);

        for (Expr expr : exprs) {
            builder.append(" ");
            if (expr == null) builder.append("nil");
            else builder.append(expr.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Literal(45.67)));
    
        System.out.println(new AstPrinter().print(expression));
    }

    @Override
    public String visitGetExpr(Get expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGetExpr'");
    }

    @Override
    public String visitSetExpr(Set expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSetExpr'");
    }

    @Override
    public String visitThisExpr(This expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisExpr'");
    }
}
