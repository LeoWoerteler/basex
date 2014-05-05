package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.query.value.type.SeqType.Occ;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Arithmetic expression.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
public final class Arith extends Arr {
  /** Calculation operator. */
  private Calc calc;

  /**
   * Constructor.
   * @param ii input info
   * @param e1 first expression
   * @param e2 second expression
   * @param c calculation operator
   */
  public Arith(final InputInfo ii, final Expr e1, final Expr e2, final Calc c) {
    super(ii, e1, e2);
    calc = c;
    type = SeqType.ITEM_ZO;
  }

  @Override
  public Expr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    super.compile(ctx, scp);
    return optimize(ctx, scp);
  }

  @Override
  public Expr optimize(final QueryContext ctx, final VarScope scp) throws QueryException {
    if(oneIsEmpty()) return optPre(null, ctx);
    if(allAreValues()) return optPre(item(ctx, info), ctx);

    final SeqType s0 = expr[0].type();
    final SeqType s1 = expr[1].type();
    final Type t0 = s0.type, t1 = s1.type;
    final boolean noneEmpty = s0.occ.min > 0 && s1.occ.min > 0;
    if(t0.isNumberOrUntyped() && t1.isNumberOrUntyped()) {
      final Type outType = Calc.type(t0, t1);
      calc = calc.specialize(outType);
      type = SeqType.get(outType, noneEmpty ? Occ.ONE : Occ.ZERO_ONE);
    } else if(noneEmpty) {
      type = SeqType.AAT;
    }
    return this;
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    final Item a = expr[0].item(ctx, info);
    if(a == null) return null;
    final Item b = expr[1].item(ctx, info);
    if(b == null) return null;
    return calc.ev(info, a, b);
  }

  @Override
  public Arith copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    final Expr a = expr[0].copy(ctx, scp, vs), b = expr[1].copy(ctx, scp, vs);
    return copyType(new Arith(info, a, b, calc));
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(OP, calc.name), expr);
  }

  @Override
  public String description() {
    return '\'' + calc.name + "' operator";
  }

  @Override
  public String toString() {
    return toString(' ' + calc.name + ' ');
  }
}
