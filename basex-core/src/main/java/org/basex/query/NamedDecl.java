package org.basex.query;

import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * A named static declaration, e.g. functions and variables.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Leo Woerteler
 */
public abstract class NamedDecl extends StaticDecl {
  /** Annotations. */
  public final Ann ann;
  /** This declaration's name. */
  public final QNm name;

  /**
   * Constructor.
   *
   * @param sctx static context
   * @param a annotations
   * @param nm name
   * @param t declared type
   * @param scp variable scope
   * @param xqdoc xqDoc comment
   * @param ii input info
   */
  protected NamedDecl(final StaticContext sctx, final Ann a, final QNm nm, final SeqType t,
      final VarScope scp, final byte[] xqdoc, final InputInfo ii) {
    super(sctx, t, scp, xqdoc, ii);
    ann = a == null ? new Ann() : a;
    name = nm;
  }

  /**
   * Returns a unique identifier for this declaration.
   * @return a byte sequence that uniquely identifies this declaration
   */
  public abstract byte[] id();
}
