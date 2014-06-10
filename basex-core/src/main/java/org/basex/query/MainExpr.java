package org.basex.query;

import java.util.*;

import org.basex.core.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * A main method's expression.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public class MainExpr extends StaticScope {
  /**
   * Constructor.
   * @param expr root expression
   * @param scp variable scope
   * @param xqdoc documentation
   * @param sctx static context
   * @param info input info
   */
  public MainExpr(final Expr expr, final VarScope scp, final byte[] xqdoc, final StaticContext sctx,
      final InputInfo info) {
    super(scp, xqdoc, sctx, info);
    this.expr = expr;
  }

  /**
   * Creates a new main module for the specified function.
   * @param uf user-defined function
   * @param args arguments
   * @return main module
   * @throws QueryException query exception
   */
  public static MainExpr get(final StaticFunc uf, final Expr[] args) throws QueryException {
    final StaticFuncCall sfc = new StaticFuncCall(uf.name, args, uf.sc, uf.info).init(uf);
    return new MainExpr(sfc, new VarScope(uf.sc), Token.EMPTY, uf.sc, null);
  }

  @Override
  public void compile(final QueryContext ctx) throws QueryException {
    if(compiled) return;
    try {
      compiled = true;
      expr = expr.compile(ctx, scope);
    } finally {
      scope.cleanUp(this);
    }
  }

  /**
   * Evaluates this module and returns the result as a cached value iterator.
   * @param ctx query context
   * @return result
   * @throws QueryException evaluation exception
   */
  public ValueBuilder cache(final QueryContext ctx) throws QueryException {
    final int fp = scope.enter(ctx);
    try {
      final Iter iter = expr.iter(ctx);
      if(iter instanceof ValueBuilder) return (ValueBuilder) iter;
      final ValueBuilder cache = new ValueBuilder();
      for(Item it; (it = iter.next()) != null;) cache.add(it);
      return cache;
    } finally {
      scope.exit(ctx, fp);
    }
  }

  /**
   * Creates a result iterator which lazily evaluates this module.
   * @param ctx query context
   * @return result iterator
   * @throws QueryException query exception
   */
  public Iter iter(final QueryContext ctx) throws QueryException {
    final int fp = scope.enter(ctx);
    final Iter iter = expr.iter(ctx);
    return new Iter() {
      @Override
      public Item next() throws QueryException {
        final Item it = iter.next();
        if(it == null) scope.exit(ctx, fp);
        return it;
      }

      @Override
      public long size() {
        return iter.size();
      }

      @Override
      public Item get(final long i) throws QueryException {
        return iter.get(i);
      }

      @Override
      public boolean reset() {
        return iter.reset();
      }
    };
  }

  @Override
  public String toString() {
    return expr.toString();
  }

  @Override
  public void plan(final FElem e) {
    expr.plan(e);
  }

  @Override
  public boolean visit(final ASTVisitor visitor) {
    return expr.accept(visitor);
  }

  /**
   * Lock visitor.
   * @author Leo Woerteler
   */
  static class LockVisitor extends ASTVisitor {
    /** Already visited scopes. */
    private final IdentityHashMap<Scope, Object> funcs = new IdentityHashMap<>();
    /** List of databases to be locked. */
    private final StringList sl;
    /** Focus level. */
    private int level;

    /**
     * Constructor.
     * @param lr lock result
     * @param ctx query context
     */
    LockVisitor(final LockResult lr, final QueryContext ctx) {
      sl = ctx.updating ? lr.write : lr.read;
      level = ctx.ctxItem == null ? 0 : 1;
    }

    @Override
    public boolean lock(final String db) {
      if(db == null) return false;
      if(level == 0 || db != DBLocking.CTX) sl.add(db);
      return true;
    }

    @Override
    public void enterFocus() {
      level++;
    }

    @Override
    public void exitFocus() {
      level--;
    }

    @Override
    public boolean staticVar(final StaticVar var) {
      if(funcs.containsKey(var)) return true;
      funcs.put(var, null);
      return var.visit(this);
    }

    @Override
    public boolean staticFuncCall(final StaticFuncCall call) {
      return func(call.func());
    }

    @Override
    public boolean inlineFunc(final Scope sub) {
      enterFocus();
      final boolean ac = sub.visit(this);
      exitFocus();
      return ac;
    }

    @Override
    public boolean funcItem(final FuncItem func) {
      return func(func);
    }

    /**
     * Visits a scope.
     * @param scp scope
     * @return if more expressions should be visited
     */
    private boolean func(final Scope scp) {
      if(funcs.containsKey(scp)) return true;
      funcs.put(scp, null);
      enterFocus();
      final boolean ac = scp.visit(this);
      exitFocus();
      return ac;
    }
  }
}
