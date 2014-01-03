package org.basex.query.value.seq;

import static org.basex.query.util.Err.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Sequence, containing at least two items.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class ItemSeq extends Seq {
  /** Item array. */
  private final Item[] item;
  /** Item Types. */
  private boolean homogeneous;
  /** Extended sequence type. */
  private ExtSeqType seqType;

  /**
   * Constructor.
   * @param it items
   * @param s size
   */
  private ItemSeq(final Item[] it, final int s) {
    super(s);
    item = it;
  }

  /**
   * Constructor.
   * @param it items
   * @param s size
   * @param t sequence type
   */
  ItemSeq(final Item[] it, final int s, final Type t) {
    this(it, s);
    seqType = t == null ? null : ExtSeqType.get(t.seqType(), s, s);
    homogeneous = t != null && t != AtomType.ITEM;
  }

  @Override
  public Item ebv(final QueryContext ctx, final InputInfo ii) throws QueryException {
    if(item[0] instanceof ANode) return item[0];
    throw CONDTYPE.get(ii, this);
  }

  @Override
  public ExtSeqType type() {
    if(seqType == null) {
      final Type fst = item[0].type;
      SeqType st = item[0].seqType();
      for(int s = 1; s < size; s++) {
        st = st.union(item[s].seqType());
        homogeneous &= item[s].type == fst;
      }
      type = st.type;
      seqType = ExtSeqType.get(st, size, size);
    }
    return seqType;
  }

  @Override
  public boolean iterable() {
    return false;
  }

  @Override
  public boolean sameAs(final Expr cmp) {
    if(!(cmp instanceof ItemSeq)) return false;
    final ItemSeq is = (ItemSeq) cmp;
    return item == is.item && size == is.size;
  }

  @Override
  public int writeTo(final Item[] arr, final int start) {
    System.arraycopy(item, 0, arr, start, (int) size);
    return (int) size;
  }

  @Override
  public Item itemAt(final long pos) {
    return item[(int) pos];
  }

  @Override
  public boolean homogeneous() {
    return homogeneous;
  }

  @Override
  public Value reverse() {
    final int s = item.length;
    final Item[] tmp = new Item[s];
    for(int l = 0, r = s - 1; l < s; l++, r--) tmp[l] = item[r];
    return get(tmp, s, type);
  }
}
