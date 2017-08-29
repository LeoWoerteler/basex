package org.basex.query.value.map;

import java.util.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Evaluates the {@code map:for-each($map, $action)} function iteratively.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Leo Woerteler
 */
public final class ForEachIter extends Iter {
  /** Query context. */
  private final QueryContext qc;
  /** Input info. */
  private final InputInfo ii;
  /** User-defined function converting a key-value pair into a sequence of values. */
  private final FItem action;

  /** Stack of branch nodes. */
  private final List<TrieBranch> stack = new ArrayList<>(4);
  /** Stack of positions inside branch nodes. */
  private final IntList poss = new IntList(4);

  /** Iterator for entries inside the current leaf or list node. */
  private Iter leafIter = Empty.ITER;

  /**
   * Creates an iterator with the given parameters.
   *
   * @param qc query context
   * @param ii input info
   * @param root root node of the map
   * @param action user-defined function
   * @throws QueryException if the user-defined function throws an error
   */
  ForEachIter(final QueryContext qc, final InputInfo ii, final TrieNode root,
      final FItem action) throws QueryException {
    this.action = action;
    this.qc = qc;
    this.ii = ii;
    first(root);
  }

  /**
   * Navigates down to the leftmost non-branch node of the given sub-map.
   *
   * @param root root of the sub-map
   * @throws QueryException if the user-defined function throws an error
   */
  private void first(final TrieNode root) throws QueryException {
    TrieNode curr = root;
    while (curr instanceof TrieBranch) {
      final TrieBranch branch = (TrieBranch) curr;
      stack.add(branch);
      final int pos = Integer.numberOfTrailingZeros(branch.used);
      poss.add(pos);
      curr = branch.kids[pos];
    }
    leafIter = curr.forEach(action, qc, ii);
  }

  @Override
  public Item next() throws QueryException {
    Item next;
    while((next = leafIter.next()) == null) {
      for(;;) {
        final int sp = stack.size();
        if(sp == 0) return null;
        final TrieBranch branch = stack.get(sp - 1);
        final int nextPos = poss.peek() + 1;
        final int pos = nextPos + Integer.numberOfTrailingZeros(branch.used >>> nextPos);
        if (pos < branch.kids.length) {
          poss.set(sp - 1, pos);
          first(branch.kids[pos]);
          break;
        }
        stack.remove(sp - 1);
        poss.pop();
      }
    }
    return next;
  }
}
