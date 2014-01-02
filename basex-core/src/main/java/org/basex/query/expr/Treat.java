package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import static org.basex.query.util.Err.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.value.type.SeqType.Occ;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Treat as expression.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class Treat extends Single {
  /** Sequence type to check. */
  public final SeqType as;

  /**
   * Constructor.
   * @param ii input info
   * @param e expression
   * @param s sequence type
   */
  public Treat(final InputInfo ii, final Expr e, final SeqType s) {
    super(ii, e);
    as = s;
    type = ExtSeqType.get(s);
  }

  @Override
  public Expr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    super.compile(ctx, scp);
    return optimize(ctx, scp);
  }

  @Override
  public Expr optimize(final QueryContext ctx, final VarScope scp) throws QueryException {
    return expr.isValue() ? optPre(value(ctx), ctx) : this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final Iter iter = ctx.iter(expr);
    final Item it = iter.next();
    // input is empty
    if(it == null) {
      if(as.mayBeZero()) return Empty.ITER;
      throw NOTREAT.get(info, description(), Empty.SEQ, as);
    }
    // treat as empty sequence
    if(as.occ == Occ.ZERO) throw NOTREAT.get(info, description(), it.type, as);

    if(as.zeroOrOne()) {
      if(iter.next() != null) throw NOTREATS.get(info, description(), as);
      if(!it.type.instanceOf(as.type)) throw NOTREAT.get(info, description(), it.type, as);
      return it.iter();
    }

    return new Iter() {
      Item i = it;

      @Override
      public Item next() throws QueryException {
        if(i == null) return null;
        if(!i.type.instanceOf(as.type)) throw NOTREAT.get(info, description(), i.type, as);
        final Item ii = i;
        i = iter.next();
        return ii;
      }
    };
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    final Value val = ctx.value(expr);

    final long len = val.size();
    // input is empty
    if(len == 0) {
      if(as.mayBeZero()) return val;
      throw NOTREAT.get(info, description(), Empty.SEQ, as);
    }
    // treat as empty sequence
    if(as.occ == Occ.ZERO) throw NOTREAT.get(info, description(), val.type, as);

    if(as.zeroOrOne()) {
      if(len > 1) throw NOTREATS.get(info, description(), as);
      final Item it = val.itemAt(0);
      if(!it.type.instanceOf(as.type)) throw NOTREAT.get(info, description(), it.type, as);
      return it;
    }

    for(long i = 0; i < len; i++) {
      final Item it = val.itemAt(i);
      if(!it.type.instanceOf(as.type))
        throw NOTREAT.get(info, description(), it.type, as);
    }
    return val;
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new Treat(info, expr.copy(ctx, scp, vs), as);
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(TYP, as), expr);
  }

  @Override
  public String toString() {
    return '(' + expr.toString() + ") " + TREAT + ' ' + AS + ' ' + as;
  }
}
