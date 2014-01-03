package org.basex.query.value.type;

import org.basex.query.value.type.SeqType.Occ;

/**
 * A sequence type extended by explicit minimum and maximum sizes.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Leo Woerteler
 */
@SuppressWarnings({ "javadoc" })
public final class ExtSeqType {
  /** Extended sequence type of an arbitrary item sequence. */
  public static final ExtSeqType ANY = new ExtSeqType(SeqType.ITEM_ZM);
  /** Extended sequence type of the empty sequence. */
  public static final ExtSeqType EMP = new ExtSeqType(SeqType.EMP);

  /** The sequence type. */
  private final SeqType seqType;
  /** The minimum size. */
  private final long minSize;
  /** The maximum size, {@code -1} for unknown maximum. */
  private final long maxSize;

  /**
   * Constructor.
   *
   * @param type sequence type
   * @param min minimum size
   * @param max maximum size
   */
  private ExtSeqType(final SeqType type, final long min, final long max) {
    seqType = type.withOcc(occ(min, max));
    minSize = min;
    maxSize = max;
  }

  /**
   * Constructor inferring the size from the sequence type.
   *
   * @param type the sequence type
   */
  private ExtSeqType(final SeqType type) {
    this(type, type.occ.min, type.occ.max == Integer.MAX_VALUE ? -1 : type.occ.max);
  }

  public static ExtSeqType get(final SeqType type) {
    return get(type, type.occ.min, type.occ.max == Integer.MAX_VALUE ? -1 : type.occ.max);
  }

  public static ExtSeqType get(final SeqType type, final long min, final long max) {
    return min == 0 && max == 0 ? EMP :
      min == 0 && max == -1 && type.eq(ANY.seqType) ? ANY :
        new ExtSeqType(type, min, max);
  }

  /**
   * Returns the extended sequence type of the result of concatenating a sequence having
   * this extended sequence type and a sequence having the given one.
   *
   * @param other extended sequence type of the other sequence
   * @return the resulting sequence type
   */
  public ExtSeqType plus(final ExtSeqType other) {
    if(this == EMP) return other;
    if(other == EMP) return this;
    final SeqType type = seqType.union(other.seqType);
    final long min = minSize + other.minSize;
    final long max = maxSize < 0 || other.maxSize < 0 ? -1 : maxSize + other.maxSize;
    return get(type, min, max);
  }

  /**
   * Returns the extended sequence type of the result of choosing either a sequence having
   * this extended sequence type or a sequence having the given one.
   *
   * @param other extended sequence type of the other sequence
   * @return the resulting sequence type
   */
  public ExtSeqType or(final ExtSeqType other) {
    final SeqType type = this == EMP ? other.seqType : other == EMP ? seqType :
      seqType.union(other.seqType);
    final long min = Math.min(minSize, other.minSize);
    final long max = maxSize < 0  || other.maxSize < 0 ? -1 : Math.max(maxSize, other.maxSize);
    return get(type, min, max);
  }

  /**
   * Returns the number of elements of the sequence that has this extended sequence type.
   * @return number of elements if known, {@code -1} otherwise
   */
  public long size() {
    return minSize == maxSize ? minSize : -1;
  }

  @Override
  public boolean equals(final Object obj) {
    if(!(obj instanceof ExtSeqType)) return false;
    final ExtSeqType other = (ExtSeqType) obj;
    return seqType.eq(other.seqType) && minSize == other.minSize && maxSize == other.maxSize;
  }

  @Override
  public int hashCode() {
    return (int) ((31 * seqType.type.id().hashCode() + minSize) * 31 + maxSize);
  }

  public SeqType seqType() {
    return seqType;
  }

  public boolean zeroOrOne() {
    return maxSize == 0 || maxSize == 1;
  }

  public Occ occ() {
    return occ(minSize, maxSize);
  }

  private static Occ occ(final long min, final long max) {
    return min > 0
        ? (max == 1 ? Occ.ONE : Occ.ONE_MORE)
        : max == 0 ? Occ.ZERO : max == 1 ? Occ.ZERO_ONE : Occ.ZERO_MORE;
  }

  public ExtSeqType withSize(final long size) {
    if(size >= 0) {
      if(minSize == size && maxSize == size) return this;
      final Occ occ = size == 0 ? Occ.ZERO : size == 1 ? Occ.ONE : Occ.ONE_MORE;
      return new ExtSeqType(seqType.withOcc(occ), size, size);
    }
    return minSize == 0 && maxSize == -1 ? this :
      new ExtSeqType(seqType.withOcc(Occ.ZERO_MORE));
  }

  public ExtSeqType withMinSize(final long l) {
    if(l == minSize) return this;
    final long max = maxSize >= 0 ? Math.max(maxSize, l) : -1;
    return get(seqType.withOcc(occ(l, maxSize)), l, max);
  }

  public ExtSeqType withMaxSize(final long max) {
    if(max == maxSize) return this;
    final long min = max < 0 ? minSize : Math.min(minSize, max);
    return get(seqType.withOcc(occ(min, max)), min, max);
  }

  public boolean nonEmpty() {
    return minSize > 0;
  }

  public boolean isBounded() {
    return maxSize >= 0;
  }

  public long minSize() {
    return minSize;
  }

  public long maxSize() {
    return isBounded() ? maxSize : Long.MAX_VALUE;
  }

  public ExtSeqType withSize(final long min, final long max) {
    return min == minSize && max == maxSize ? this :
      get(seqType.withOcc(occ(min, max)), min, max);
  }

  @Override
  public String toString() {
    return seqType.toString();
  }

  public boolean instanceOf(final ExtSeqType other) {
    return seqType.instanceOf(other.seqType) && minSize >= other.minSize
        && (other.maxSize < 0 || maxSize >= 0 && maxSize <= other.maxSize);
  }

  public ExtSeqType union(final ExtSeqType other) {
    final long min = Math.min(minSize, other.minSize),
        max = maxSize >= 0 && other.maxSize >= 0 ? Math.max(maxSize, other.maxSize) : -1;
    return get(seqType.union(other.seqType), min, max);
  }

  public boolean eq(final ExtSeqType t) {
    return seqType.eq(t.seqType) && minSize == t.minSize && maxSize == t.maxSize;
  }

  public ExtSeqType intersect(final ExtSeqType t) {
    final SeqType st = seqType.intersect(t.seqType);
    if(st == null) return null;
    final long min = Math.max(minSize, t.minSize),
        max = maxSize < 0 ? t.maxSize : t.maxSize < 0 ? maxSize : Math.min(maxSize, t.maxSize);
    return max >= 0 && min > max ? null : get(st, min, max);
  }

  public ExtSeqType subSeq(final long start) {
    if(this == EMP || start < 2 || minSize == 0 && maxSize == -1) return this;
    if(maxSize >= 0 && start > maxSize) return EMP;
    final long min = Math.max(minSize - start + 1, 0);
    final long max = maxSize == -1 ? -1 : Math.max(maxSize - start + 1, 0);
    return get(seqType, min, max);
  }

  public ExtSeqType subSeq(final long start, final long length) {
    if(length <= 0 || maxSize >= 0 && start > maxSize) return EMP;
    final long min = Math.min(Math.max(minSize - start + 1, 0), length);
    final long max = maxSize == -1 ? length : Math.min(Math.max(maxSize - start + 1, 0), length);
    return min == minSize && max == maxSize ? this : get(seqType, min, max);
  }
}
