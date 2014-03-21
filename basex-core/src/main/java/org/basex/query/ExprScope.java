package org.basex.query;

import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * An expression without free local variables, together with its variable scope.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public class ExprScope implements Scope {
  /** Expression. */
  private Expr expr;
  /** Variable scope. */
  private final VarScope scope;
  /** Declared type, {@code null} if not specified. */
  public final SeqType declType;
  /** Static context. */
  private final StaticContext sc;
  /** Input info. */
  private final InputInfo info;

  /** Compilation flag. */
  private boolean compiled;

  /**
   * Constructor.
   * @param expr expression
   * @param scope variable scope
   * @param declType declared type
   * @param sc static context
   * @param ii input info
   */
  public ExprScope(final Expr expr, final VarScope scope, final SeqType declType,
      final StaticContext sc, final InputInfo ii) {
    this.expr = declType != null ? new TypeCheck(sc, ii, expr, declType, false) : expr;
    this.scope = scope;
    this.declType = declType;
    this.sc = sc;
    this.info = ii;
  }

  @Override
  public void compile(final QueryContext ctx) throws QueryException {
    final int fp = scope.enter(ctx);
    try {
      compiled = true;
      expr = expr.compile(ctx, scope);
      scope.cleanUp(this);
    } finally {
      scope.exit(ctx, fp);
    }
  }

  /**
   * Fully evaluates this expression.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  public Value value(final QueryContext ctx) throws QueryException {
    final int fp = scope.enter(ctx);
    try {
      return expr.value(ctx);
    } finally {
      scope.exit(ctx, fp);
    }
  }

  @Override
  public boolean visit(final ASTVisitor visitor) {
    return expr.accept(visitor);
  }

  @Override
  public boolean compiled() {
    return compiled;
  }

  /**
   * Returns this scoped expression's static context.
   * @return static context
   */
  public StaticContext staticContext() {
    return sc;
  }

  /**
   * Returns this scoped expression's input info.
   * @return input info
   */
  public InputInfo info() {
    return info;
  }
}
