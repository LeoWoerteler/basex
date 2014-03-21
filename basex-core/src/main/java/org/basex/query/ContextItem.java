package org.basex.query;

import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * A context item expression.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public class ContextItem extends StaticDecl {
  /**
   * Constructor.
   * @param e bound expression
   * @param declType declared type, {@code null} if undefined
   * @param scp variable scope
   * @param xqdoc documentation
   * @param sctx static context
   * @param ii input info
   */
  ContextItem(final Expr e, final SeqType declType, final VarScope scp, final byte[] xqdoc,
      final StaticContext sctx, final InputInfo ii) {
    super(sctx, declType, scp, xqdoc, ii);
    this.expr = declType == null ? e : new TypeCheck(sctx, ii, e, declType, false);
  }

  @Override
  public void compile(final QueryContext ctx) throws QueryException {
    if(compiled) return;
    final int fp = scope.enter(ctx);
    compiled = true;
    try {
      expr = expr.compile(ctx, scope);
      scope.cleanUp(this);
    } finally {
      scope.exit(ctx, fp);
    }
  }

  /**
   * Evaluates this context item expresion and returns the result as a value.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  public Value value(final QueryContext ctx) throws QueryException {
    final int fp = scope.enter(ctx);
    try {
      return ctx.value(expr);
    } finally {
      scope.exit(ctx, fp);
    }
  }

  @Override
  public boolean visit(final ASTVisitor visitor) {
    return expr.accept(visitor);
  }

  @Override
  public void plan(final FElem e) {
    addPlan(e, planElem(QueryText.TYP, declType == null ? SeqType.ITEM_ZM : declType), expr);
  }

  @Override
  public String toString() {
    final TokenBuilder tb = new TokenBuilder("declare context item");
    if(declType != null) tb.add(" as ").add(declType.toString());
    return tb.add(" := ").add(expr.toString()).add(';').toString();
  }
}
