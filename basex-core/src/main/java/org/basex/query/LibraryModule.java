package org.basex.query;

import org.basex.query.func.*;
import org.basex.query.value.item.*;
import org.basex.query.var.*;
import org.basex.util.hash.*;

/**
 * An XQuery library module.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public final class LibraryModule extends Module {
  /** Library name. */
  public final QNm name;

  /**
   * Constructor.
   * @param name of library
   * @param xqdoc documentation
   * @param funcs user-defined functions
   * @param vars static variables
   * @param modules imported modules
   * @param sctx static context
   */
  public LibraryModule(final QNm name, final byte[] xqdoc, final TokenObjMap<StaticFunc> funcs,
      final TokenObjMap<StaticVar> vars, final TokenSet modules, final StaticContext sctx) {
    super(xqdoc, funcs, vars, modules, sctx);
    this.name = name;
  }

  @Override
  public byte[] nsURI() {
    return name.uri();
  }
}
