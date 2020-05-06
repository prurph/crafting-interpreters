package tt.presco.lox;

import static tt.presco.lox.TokenType.BANG;
import static tt.presco.lox.TokenType.BANG_EQUAL;
import static tt.presco.lox.TokenType.COMMA;
import static tt.presco.lox.TokenType.DOT;
import static tt.presco.lox.TokenType.EOF;
import static tt.presco.lox.TokenType.EQUAL;
import static tt.presco.lox.TokenType.EQUAL_EQUAL;
import static tt.presco.lox.TokenType.GREATER;
import static tt.presco.lox.TokenType.GREATER_EQUAL;
import static tt.presco.lox.TokenType.LEFT_BRACE;
import static tt.presco.lox.TokenType.LEFT_PAREN;
import static tt.presco.lox.TokenType.LESS;
import static tt.presco.lox.TokenType.LESS_EQUAL;
import static tt.presco.lox.TokenType.MINUS;
import static tt.presco.lox.TokenType.PLUS;
import static tt.presco.lox.TokenType.RIGHT_BRACE;
import static tt.presco.lox.TokenType.RIGHT_PAREN;
import static tt.presco.lox.TokenType.SEMICOLON;
import static tt.presco.lox.TokenType.SLASH;
import static tt.presco.lox.TokenType.STAR;
import static tt.presco.lox.TokenType.STRING;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // At the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if (match('/')) {
                    // Comments are meaningless (to the compiler) lexemes, so it adds no token for them
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\t':
            case '\r':
                // Ignore whitespace
                // By restarting the loop we start the next lexeme _after_ the whitespace character
                break;
            case '\n':
                // Used peek to find newline instead of match so that we preserve it, end up here, and increment line
                line++;
                break;
            case '"': string(); break;

            default:
                Lox.error(line, "Unexpected character.");
                break;
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        // Unterminated string
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // Closing " found.
        advance();

        // Trim surrounding quotes to capture the value of the string.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // Effectively a conditional advance: only consumes current character if it's what we're looking for.
    // Combines the fundamental operators of advance and peek.
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}
