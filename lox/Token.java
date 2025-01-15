package lox;

public class Token {
    final TokenType type;
    final String lexene;
    final Object literal;
    final int line;

    Token(TokenType type, String lexene, Object literal, int line) {
        this.type = type;
        this.lexene = lexene;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexene + " " + literal;
    }
}
