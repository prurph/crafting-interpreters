package tt.presco.lox;

public class RpnPrinter implements Expr.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return (
      print(expr.left) + " " + print(expr.right) + " " + expr.operator.lexeme
    );
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return print(expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    return expr.value == null ? "nil" : expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    // Must differentiate between unary and binary minus; otherwise we don't know whether to pop
    // one or two expressions off the stack to apply it to.
    String operator = expr.operator.lexeme;
    if (expr.operator.type.equals(TokenType.MINUS)) {
      operator = "~";
    }
    return print(expr.right) + " " + operator;
  }

  // Temporary test demo of RpnPrinter
  public static void main(String[] args) {
    // -123 * 45.67
    Expr expression = new Expr.Binary(
      new Expr.Unary(
        new Token(TokenType.MINUS, "-", null, 1),
        new Expr.Literal(123)
      ),
      new Token(TokenType.STAR, "*", null, 1),
      new Expr.Grouping(new Expr.Literal(45.67))
    );

    System.out.println(new RpnPrinter().print(expression));
  }
}
