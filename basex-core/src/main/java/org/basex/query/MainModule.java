package org.basex.query;

import org.basex.query.func.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * An XQuery main module.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public final class MainModule extends Module {
  /** Main Expression. */
  public final MainExpr expr;

  /**
   * Constructor.
   * @param expr root expression
   * @param xqdoc documentation
   * @param funcs user-defined functions
   * @param vars static variables
   * @param imports namespace URIs of imported modules
   * @param sctx static context
   */
  public MainModule(final MainExpr expr, final byte[] xqdoc, final TokenObjMap<StaticFunc> funcs,
      final TokenObjMap<StaticVar> vars, final TokenSet imports, final StaticContext sctx) {
    super(xqdoc, funcs, vars, imports, sctx);
    this.expr = expr;
  }

  @Override
  public byte[] nsURI() {
    return Token.EMPTY;
  }
}
