package org.basex.query;

import org.basex.core.*;
import org.basex.data.*;
import org.basex.query.MainExpr.*;
import org.basex.query.expr.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * Superclass for static functions, variables and the main expression.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public abstract class StaticScope extends ExprInfo implements Scope {
  /** Static context. */
  public final StaticContext sc;
  /** Variable scope. */
  protected final VarScope scope;
  /** Input info. */
  public final InputInfo info;

  /** Root expression of this declaration. */
  public Expr expr;
  /** Compilation flag. */
  protected boolean compiled;
  /** Documentation. */
  private final byte[] doc;

  /**
   * Constructor.
   * @param scp variable scope
   * @param ii input info
   * @param xqdoc documentation
   * @param sctx static context
   */
  StaticScope(final VarScope scp, final byte[] xqdoc, final StaticContext sctx,
      final InputInfo ii) {
    sc = sctx;
    scope = scp;
    info = ii;
    doc = xqdoc;
  }

  @Override
  public final boolean compiled() {
    return compiled;
  }

  /**
   * Returns this scope's documentation if present, {@code null} otherwise.
   * @return documentation string or {@code null}
   */
  public final byte[] doc() {
    return doc;
  }

  /**
   * Adds the names of the databases that may be touched by the module.
   * @param lr lock result
   * @param ctx query context
   * @return result of check
   * @see Proc#databases(LockResult)
   */
  public final boolean databases(final LockResult lr, final QueryContext ctx) {
    return expr.accept(new LockVisitor(lr, ctx));
  }
}
