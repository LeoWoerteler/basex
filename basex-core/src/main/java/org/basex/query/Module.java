package org.basex.query;

import org.basex.query.func.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * An XQuery module.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public abstract class Module {
  /** User-defined functions. */
  final TokenObjMap<StaticFunc> funcs;
  /** Static variables. */
  final TokenObjMap<StaticVar> vars;
  /** Namespace URIs of imported modules. */
  private final TokenSet imports;

  /** This module's static context. */
  private final StaticContext sc;
  /** xqDoc comment (not {@code null}). */
  private final byte[] xqdoc;

  /**
   * Constructor.
   * @param xqdoc documentation
   * @param funcs user-defined functions
   * @param vars static variables
   * @param imports namespace URIs of imported modules
   * @param sctx static context
   */
  public Module(final byte[] xqdoc, final TokenObjMap<StaticFunc> funcs,
      final TokenObjMap<StaticVar> vars, final TokenSet imports, final StaticContext sctx) {
    this.funcs = funcs;
    this.vars = vars;
    this.imports = imports;
    this.sc = sctx;
    this.xqdoc = xqdoc;
  }

  /**
   * Return static variables.
   * @return static variables
   */
  public TokenObjMap<StaticVar> vars() {
    return vars;
  }

  /**
   * Return static functions.
   * @return static functions
   */
  public TokenObjMap<StaticFunc> funcs() {
    return funcs;
  }

  /**
   * Returns this module's namespace URI.
   * @return namespace URI; {@link Token#EMPTY} if this module is a main module
   */
  public abstract byte[] nsURI();

  /**
   * Returns this module's documentation string.
   * @return documentation
   */
  public final byte[] doc() {
    return xqdoc;
  }

  /**
   * Returns this module's static context.
   * @return static context
   */
  public StaticContext sc() {
    return sc;
  }
}
