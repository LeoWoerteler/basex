package org.basex.query.func;

import static org.basex.query.util.Err.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * Functions on functions.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Leo Woerteler
 */
public final class FNFunc extends StandardFunc {
  /** Minimum size of a loop that should not be unrolled. */
  static final int UNROLL_LIMIT = 10;

  /**
   * Constructor.
   * @param sctx static context
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  public FNFunc(final StaticContext sctx, final InputInfo ii, final Function f, final Expr... e) {
    super(sctx, ii, f, e);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    switch(sig) {
      case FOR_EACH:      return forEach(ctx);
      case FILTER:        return filter(ctx);
      case FOR_EACH_PAIR: return forEachPair(ctx);
      case FOLD_LEFT:     return foldLeft(ctx);
      case FOLD_RIGHT:    return foldRight(ctx);
      default:            return super.iter(ctx);
    }
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    switch(sig) {
      case FUNCTION_ARITY:  return Int.get(checkFunc(expr[0], ctx).arity());
      case FUNCTION_NAME:   return checkFunc(expr[0], ctx).funcName();
      case FUNCTION_LOOKUP: return lookup(ctx, ii);
      default:              return super.item(ctx, ii);
    }
  }

  @Override
  Expr opt(final QueryContext ctx, final VarScope scp) throws QueryException {
    if(oneOf(sig, Function.FOLD_LEFT, Function.FOLD_RIGHT, Function.FOR_EACH)
        && allAreValues() && expr[0].size() < UNROLL_LIMIT) {
      // unroll the loop
      ctx.compInfo(QueryText.OPTUNROLL, this);
      final Value seq = (Value) expr[0];
      final int len = (int) seq.size();

      // fn:for-each(...)
      if (sig == Function.FOR_EACH) {
        final Expr[] results = new Expr[len];
        for(int i = 0; i < len; i++) {
          results[i] = new DynFuncCall(info, expr[1], seq.itemAt(i)).optimize(ctx, scp);
        }
        return new List(info, results).optimize(ctx, scp);
      }

      // folds
      Expr e = expr[1];
      if (sig == Function.FOLD_LEFT) {
        for (final Item it : seq)
          e = new DynFuncCall(info, expr[2], e, it).optimize(ctx, scp);
      } else {
        for (int i = len; --i >= 0;)
          e = new DynFuncCall(info, expr[2], seq.itemAt(i), e).optimize(ctx, scp);
      }
      return e;
    }

    // iteration over the empty sequence
    if(oneOf(sig, Function.FOR_EACH, Function.FOR_EACH_PAIR, Function.FILTER)
        && expr[0].isEmpty() && !(has(Flag.NDT) || has(Flag.UPD))) {
      return Empty.SEQ;
    }

    final ExtSeqType st = expr.length < 1 ? null : expr[0].type();
    switch(sig) {
      case FOR_EACH:
        final ExtSeqType ret = retType(expr[1]);
        type = ret.withSize(st.minSize() * ret.minSize(),
            st.isBounded() && ret.isBounded() ? st.maxSize() * ret.maxSize() : -1);
        break;
      case FILTER:
        type = st.withMinSize(0);
        break;
      case FOR_EACH_PAIR:
        if(!expr[1].isEmpty()) {
          final ExtSeqType st2 = expr[1].type();
          final ExtSeqType rt = retType(expr[2]);
          final long maxIter = st.isBounded()
              ? (st2.isBounded() ? Math.min(st.maxSize(), st2.maxSize()) : st.maxSize())
              : (st2.isBounded() ? st2.maxSize() : -1);
          type = rt.withSize(rt.minSize() * Math.min(st.minSize(), st2.minSize()),
              rt.isBounded() && maxIter >= 0 ? maxIter * rt.maxSize() : -1);
        } else if(!(has(Flag.NDT) || has(Flag.UPD))) {
          return Empty.SEQ;
        } else {
          type = ExtSeqType.EMP;
        }
        break;
      case FOLD_LEFT:
      case FOLD_RIGHT:
        if(expr[0].size() == 0) {
          if(!(has(Flag.NDT) || has(Flag.UPD))) return expr[1];
          type = expr[1].type();
        } else {
          final ExtSeqType rt = retType(expr[2]);
          type = st.minSize() > 0 ? rt : expr[1].type().union(rt);
        }
        break;
      default:
        break;
    }

    return this;
  }

  /**
   * Gets the return type of the function item produced by the given expression.
   * @param e expression producing the function item
   * @return the return type
   */
  private static ExtSeqType retType(final Expr e) {
    if(e instanceof XQFunction) return ((XQFunction) e).returnType();
    final SeqType st = e.seqType();
    if(st.type instanceof FuncType) {
      final SeqType ret = ((FuncType) st.type).ret;
      if(ret != null) return ExtSeqType.get(ret);
    }
    return ExtSeqType.ANY;
  }

  /**
   * Looks up the specified function item.
   * @param ctx query context
   * @param ii input info
   * @return function item
   * @throws QueryException query exception
   */
  private Item lookup(final QueryContext ctx, final InputInfo ii) throws QueryException {
    final QNm name = checkQNm(expr[0], ctx, sc);
    final long arity = checkItr(expr[1], ctx);
    if(arity < 0 || arity > Integer.MAX_VALUE) throw FUNCUNKNOWN.get(ii, name);

    try {
      final Expr lit = Functions.getLiteral(name, (int) arity, ctx, sc, ii);
      return lit == null ? null : lit.item(ctx, ii);
    } catch(final QueryException e) {
      // function not found (in most cases: XPST0017)
      return null;
    }
  }

  /**
   * Maps a function onto a sequence of items.
   * @param ctx query context
   * @return sequence of results
   * @throws QueryException exception
   */
  private Iter forEach(final QueryContext ctx) throws QueryException {
    final FItem f = withArity(1, 1, ctx);
    final Iter xs = expr[0].iter(ctx);
    return new Iter() {
      /** Results. */
      Iter ys = Empty.ITER;

      @Override
      public Item next() throws QueryException {
        do {
          final Item it = ys.next();
          if(it != null) return it;
          final Item x = xs.next();
          if(x == null) return null;
          ys = f.invokeValue(ctx, info, x).iter();
        } while(true);
      }
    };
  }

  /**
   * Filters the given sequence with the given predicate.
   * @param ctx query context
   * @return filtered sequence
   * @throws QueryException query exception
   */
  private Iter filter(final QueryContext ctx) throws QueryException {
    final FItem f = withArity(1, 1, ctx);
    final Iter xs = expr[0].iter(ctx);
    return new Iter() {
      @Override
      public Item next() throws QueryException {
        do {
          final Item it = xs.next();
          if(it == null) return null;
          if(checkBln(checkNoEmpty(f.invokeItem(ctx, info, it)), ctx)) return it;
        } while(true);
      }
    };
  }

  /**
   * Zips two sequences with the given zipper function.
   * @param ctx query context
   * @return sequence of results
   * @throws QueryException query exception
   */
  private Iter forEachPair(final QueryContext ctx) throws QueryException {
    final FItem zipper = withArity(2, 2, ctx);
    final Iter xs = expr[0].iter(ctx);
    final Iter ys = expr[1].iter(ctx);
    return new Iter() {
      /** Results. */
      Iter zs = Empty.ITER;

      @Override
      public Item next() throws QueryException {
        do {
          final Item it = zs.next();
          if(it != null) return it;
          final Item x = xs.next(), y = ys.next();
          if(x == null || y == null) return null;
          zs = zipper.invokeValue(ctx, info, x, y).iter();
        } while(true);
      }
    };
  }

  /**
   * Folds a sequence into a return value, starting from the left.
   * @param ctx query context
   * @return resulting sequence
   * @throws QueryException query exception
   */
  private Iter foldLeft(final QueryContext ctx) throws QueryException {
    final FItem f = withArity(2, 2, ctx);
    final Iter xs = expr[0].iter(ctx);
    Item x = xs.next();

    // don't convert to a value if not necessary
    if(x == null) return expr[1].iter(ctx);

    Value sum = ctx.value(expr[1]);
    do sum = f.invokeValue(ctx, info, sum, x);
    while((x = xs.next()) != null);
    return sum.iter();
  }

  /**
   * Folds a sequence into a return value, starting from the left.
   * @param ctx query context
   * @return resulting sequence
   * @throws QueryException query exception
   */
  private Iter foldRight(final QueryContext ctx) throws QueryException {
    final FItem f = withArity(2, 2, ctx);
    final Value xs = ctx.value(expr[0]);
    // evaluate start value lazily if it's passed straight through
    if(xs.isEmpty()) return expr[1].iter(ctx);

    Value res = ctx.value(expr[1]);
    for(long i = xs.size(); --i >= 0;) res = f.invokeValue(ctx, info, xs.itemAt(i), res);
    return res.iter();
  }

  /**
   * Casts and checks the function item for its arity.
   * @param p position of the function
   * @param a arity
   * @param ctx query context
   * @return function item
   * @throws QueryException query exception
   */
  private FItem withArity(final int p, final int a, final QueryContext ctx) throws QueryException {
    final Item it = checkItem(expr[p], ctx);
    if(it instanceof FItem) {
      final FItem fi = (FItem) it;
      if(fi.arity() == a) return fi;
    }
    throw Err.typeError(this, FuncType.arity(a), it);
  }
}
