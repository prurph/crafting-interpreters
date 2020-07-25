package tt.presco.lox;

import static tt.presco.lox.TokenType.AND;
import static tt.presco.lox.TokenType.BANG;
import static tt.presco.lox.TokenType.BANG_EQUAL;
import static tt.presco.lox.TokenType.CLASS;
import static tt.presco.lox.TokenType.COMMA;
import static tt.presco.lox.TokenType.DOT;
import static tt.presco.lox.TokenType.ELSE;
import static tt.presco.lox.TokenType.EOF;
import static tt.presco.lox.TokenType.EQUAL;
import static tt.presco.lox.TokenType.EQUAL_EQUAL;
import static tt.presco.lox.TokenType.FALSE;
import static tt.presco.lox.TokenType.FOR;
import static tt.presco.lox.TokenType.FUN;
import static tt.presco.lox.TokenType.GREATER;
import static tt.presco.lox.TokenType.GREATER_EQUAL;
import static tt.presco.lox.TokenType.IDENTIFIER;
import static tt.presco.lox.TokenType.IF;
import static tt.presco.lox.TokenType.LEFT_BRACE;
import static tt.presco.lox.TokenType.LEFT_PAREN;
import static tt.presco.lox.TokenType.LESS;
import static tt.presco.lox.TokenType.LESS_EQUAL;
import static tt.presco.lox.TokenType.MINUS;
import static tt.presco.lox.TokenType.NIL;
import static tt.presco.lox.TokenType.NUMBER;
import static tt.presco.lox.TokenType.OR;
import static tt.presco.lox.TokenType.PLUS;
import static tt.presco.lox.TokenType.PRINT;
import static tt.presco.lox.TokenType.RETURN;
import static tt.presco.lox.TokenType.RIGHT_BRACE;
import static tt.presco.lox.TokenType.RIGHT_PAREN;
import static tt.presco.lox.TokenType.SEMICOLON;
import static tt.presco.lox.TokenType.SLASH;
import static tt.presco.lox.TokenType.STAR;
import static tt.presco.lox.TokenType.STRING;
import static tt.presco.lox.TokenType.SUPER;
import static tt.presco.lox.TokenType.THIS;
import static tt.presco.lox.TokenType.TRUE;
import static tt.presco.lox.TokenType.VAR;
import static tt.presco.lox.TokenType.WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
  private static final Map<String, TokenType> keywords;

  static {
    keywords =
      new HashMap<>() {

        {
          put("and", AND);
          put("class", CLASS);
          put("else", ELSE);
          put("false", FALSE);
          put("for", FOR);
          put("fun", FUN);
          put("if", IF);
          put("nil", NIL);
          put("or", OR);
          put("print", PRINT);
          put("return", RETURN);
          put("super", SUPER);
          put("this", THIS);
          put("true", TRUE);
          put("var", VAR);
          put("while", WHILE);
        }
      };
  }

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
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '*':
        addToken(STAR);
        break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        if (match('/')) {
          // Comments are meaningless (to the compiler) lexemes, so it adds no token for them
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }
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
      case '"':
        string();
        break;
      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          // Assume any lexeme starting with a letter or underscore is an identifier
          identifier();
        } else {
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }

    // Get identifier; see if it's a reserved word or a regular user-defined identifier
    TokenType type = keywords.get(source.substring(start, current));
    addToken(type == null ? IDENTIFIER : type);
  }

  private void number() {
    while (isDigit(peek())) {
      advance();
    }

    // Handle fractional part if it exists
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) {
        advance();
      }
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') {
        line++;
      }
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
    if (isAtEnd()) {
      return false;
    }
    if (source.charAt(current) != expected) {
      return false;
    }

    current++;
    return true;
  }

  private char advance() {
    return source.charAt(current++);
  }

  private char peek() {
    return isAtEnd() ? '\0' : source.charAt(current);
  }

  private char peekNext() {
    return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
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
