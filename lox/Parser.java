package lox;

import static lox.TokenType.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private int loop_depth = 0;

    private static class ParseError extends RuntimeException {}
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    Expr replParse() {
        Expr expr = expression();
        return expr;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(FOR)) return forStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(CLASS)) return classDeclaration();
        if (match(RETURN)) return returnStatement();
        if (match(FUN)) return function(null, "function");
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Expression(value);
    }

    private Stmt whileStatement() {
        loop_depth++;
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        loop_depth--;

        return new Stmt.While(condition, body);
    }

    private Stmt breakStatement() {
        if (loop_depth == 0) {
            throw error(peek(), "Cannot use 'break' outside of a loop.");
        }
        Token keyword = previous();
        consume(TokenType.SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break(keyword);
    }
    
    private Stmt forStatement() {
        loop_depth++;
        consume(LEFT_PAREN, "Expect '(' after 'for'.");
        
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }
        
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        loop_depth--;
        return body;
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected Variable name!");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration!");
        return new Stmt.Var(name, initializer);
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Class must have a name.");
        consume(LEFT_BRACE, "Expected '{' before class body.");
        
        List<Stmt.Function> methods = new ArrayList<>();
        List<Stmt.Function> staticMethods = new ArrayList<>();
    
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            Token methodName = consume(IDENTIFIER, "Expect method name.");
    
            if (peek().type == LEFT_PAREN) {
                if (match(STATIC)) {
                    staticMethods.add(function(methodName, "static method"));
                } else {
                    methods.add(function(methodName, "method"));
                }
            } else if (peek().type == LEFT_BRACE) {
                if (match(STATIC)) {
                    staticMethods.add(getterFunction(methodName, "static getter"));
                } else {
                    methods.add(getterFunction(methodName, "getter"));
                }
            } else {
                throw error(peek(), "Expect '(' for method or '{' for getter.");
            }
        }
    
        consume(RIGHT_BRACE, "Expected '}' after class body.");
        return new Stmt.Class(name, methods, staticMethods);
    }    

    private Stmt.Function getterFunction(Token name, String kind) {
        consume(LEFT_BRACE, "Expected '{' after " + kind + " name.");
        List<Stmt> body = block();
        return new Stmt.Function(name, new ArrayList<>(), body);
    }

    private Stmt.Function function(Token name, String kind) {
        if (name == null) {
            name = consume(IDENTIFIER, "Expect method name.");
        }
        consume(LEFT_PAREN, "Expected '(' after " + kind + " name.");
        
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        
        return new Stmt.Function(name, parameters, body);
    }
    

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    private List<Stmt> block() {
        List <Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Expr expression() {
        return comma();
    }
    
    private Expr comma() {
        Expr expr = assignment();
        while (match(COMMA)) {
            Token operator = previous();
            if (expr == null) {
                Lox.error(peek(), "Missing left-hand operand.");
            }
            Expr right = assignment();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    
    private Expr assignment() {
        Expr expr = lambda();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }
    
    private Expr lambda() {
        if (match(FUN)) {
            consume(LEFT_PAREN, "Expect '(' after 'fun'.");
            List<Token> parameters = new ArrayList<>();
            if (!check(RIGHT_PAREN)) {
                do {
                    if (parameters.size() >= 255) {
                        error(peek(), "Cannot have more than 255 parameters.");
                    }
                    parameters.add(consume(IDENTIFIER, "Expect parameter name."));
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.");
            consume(LEFT_BRACE, "Expect '{' before lambda body.");
            List<Stmt> body = block();
            return new Expr.Lambda(parameters, body);
        }
        return or();
    }
    
    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = ternary();
        while (match(AND)) {
            Token operator = previous();
            Expr right = ternary();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    
    private Expr ternary() {
        Expr expr = equality();
        if (match(QUESTION_MARK)) {
            Token operator = previous();
            Expr left = ternary();
            consume(COLON, "Expect ':' after true branch.");
            Expr right = ternary();
            return new Expr.Ternary(expr, left, right, operator);
        }
        return expr;
    }
    
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = bound();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = bound();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr bound() {
        Expr expr = factor();
        while (match(MODULO)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    
    private Expr factor() {
        Expr expr = binary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = binary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr binary() {
        Expr expr = shift();
        while (match(BITWISE_OR, BITWISE_AND, BITWISE_XOR)) {
            Token operator = previous();
            Expr right = shift();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    
    private Expr shift() {
        Expr expr = unary();
        while (match(LEFT_SHIFT, RIGHT_SHIFT)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }
    
    private Expr call() {
        Expr expr = primary();
        
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'");
                expr = new Expr.Get(expr, name);
            }
            else break;
        }

        return expr;
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if (match(THIS)) return new Expr.This(previous());
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        return null;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Arguments Count cannot exceed 255");
                }
                arguments.add(assignment());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }    

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }
        
            advance();
        }
    }
}
