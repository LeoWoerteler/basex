package org.basex.query;

import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * Common superclass for static functions and variables.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public abstract class StaticDecl extends StaticScope {
  /** Declared type, {@code null} if not specified. */
  protected final SeqType declType;

  /** Flag that is set during compilation and execution and prevents infinite loops. */
  protected boolean dontEnter;

  /**
   * Constructor.
   * @param sctx static context
   * @param t declared return type
   * @param scp variable scope
   * @param xqdoc documentation
   * @param ii input info
   */
  protected StaticDecl(final StaticContext sctx, final SeqType t, final VarScope scp,
      final byte[] xqdoc, final InputInfo ii) {
    super(scp, xqdoc, sctx, ii);
    declType = t;
  }

  /**
   * Returns the type of this expression. If no type has been declare in the expression,
   * it is derived from the expression type.
   * @return return type
   */
  public SeqType type() {
    return declType != null ? declType : expr.type();
  }
}
