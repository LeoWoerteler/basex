package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Expression list.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class List extends Arr {
  /** Limit for the size of sequences that are materialized at compile time. */
  private static final int MAX_MAT_SIZE = 1 << 20;

  /**
   * Constructor.
   * @param ii input info
   * @param l expression list
   */
  public List(final InputInfo ii, final Expr... l) {
    super(ii, l);
  }

  @Override
  public void checkUp() throws QueryException {
    checkAllUp(expr);
  }

  @Override
  public Expr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    final int es = expr.length;
    for(int e = 0; e < es; e++) expr[e] = expr[e].compile(ctx, scp);
    return optimize(ctx, scp);
  }

  @Override
  public Expr optimize(final QueryContext ctx, final VarScope scp) throws QueryException {
    // compute number of results
    ExtSeqType extType = null;
    int j = 0;
    for(int i = 0; i < expr.length; i++) {
      final Expr e = expr[i];
      // size == 0 && !has(Flag.NDT) && !has(Flag.UPD)
      if(e.isEmpty()) continue;
      if(i != j) expr[j] = e;
      final ExtSeqType est = e.type();
      extType = extType == null ? est : extType.plus(est);
      j++;
    }

    // no concatenation needed, return contents
    if(j < 2) return optPre(j == 0 ? null : expr[0], ctx);

    if(j < expr.length) {
      // remove empty sequences
      final Expr[] copy = new Expr[j];
      System.arraycopy(expr, 0, copy, 0, j);
      expr = copy;
    }

    type = extType;
    final long size = extType.size();
    if(size >= 0) {
      if(allAreValues() && size <= MAX_MAT_SIZE) {
        Type all = null;
        final Value[] vs = new Value[expr.length];
        int c = 0;
        for(final Expr e : expr) {
          final Value v = e.value(ctx);
          if(c == 0) all = v.type;
          else if(all != v.type) all = null;
          vs[c++] = v;
        }

        final Value val;
        final int s = (int) size;
        if(all == AtomType.STR)      val = StrSeq.get(vs, s);
        else if(all == AtomType.BLN) val = BlnSeq.get(vs, s);
        else if(all == AtomType.FLT) val = FltSeq.get(vs, s);
        else if(all == AtomType.DBL) val = DblSeq.get(vs, s);
        else if(all == AtomType.DEC) val = DecSeq.get(vs, s);
        else if(all == AtomType.BYT) val = BytSeq.get(vs, s);
        else if(all != null && all.instanceOf(AtomType.ITR)) {
          val = IntSeq.get(vs, s, all);
        } else {
          final ValueBuilder vb = new ValueBuilder(s);
          for(int i = 0; i < c; i++) vb.add(vs[i]);
          val = vb.value();
        }
        return optPre(val, ctx);
      }
    }

    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) {
    return new Iter() {
      Iter ir;
      int e;

      @Override
      public Item next() throws QueryException {
        while(true) {
          if(ir == null) {
            if(e == expr.length) return null;
            ir = ctx.iter(expr[e++]);
          }
          final Item it = ir.next();
          if(it != null) return it;
          ir = null;
        }
      }
    };
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    final ValueBuilder vb = new ValueBuilder();
    for(final Expr e : expr) vb.add(ctx.value(e));
    return vb.value();
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return copyType(new List(info, copyAll(ctx, scp, vs, expr)));
  }

  @Override
  public boolean isVacuous() {
    for(final Expr e : expr) if(!e.isVacuous()) return false;
    return true;
  }

  @Override
  public String toString() {
    return toString(SEP);
  }
}
