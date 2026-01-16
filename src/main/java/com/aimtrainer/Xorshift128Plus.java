package com.aimtrainer;

/**
 * Xorshift128+ random number generator.
 * Very fast, but NOT cryptographically secure.
 */
public final class Xorshift128Plus {

  private long s0;
  private long s1;

  public Xorshift128Plus(long seed1, long seed2) {
    if (seed1 == 0 && seed2 == 0) {
      seed2 = 1; // avoid all-zero state
    }
    this.s0 = seed1;
    this.s1 = seed2;
  }

  // Core algorithm: returns next 64-bit random number
  public long nextLong() {
    long x = s0;
    long y = s1;
    s0 = y;
    x ^= x << 23;
    x ^= x >>> 17;
    x ^= y ^ (y >>> 26);
    s1 = x;
    return s0 + s1;
  }

  // Convenience: random int
  public int nextInt() {
    return (int) nextLong();
  }

  // Convenience: bounded int [0, bound)
  public int nextInt(int bound) {
    if (bound <= 0) throw new IllegalArgumentException(
      "bound must be positive"
    );
    long r = nextLong() >>> 1; // make non-negative
    return (int) (r % bound);
  }

  // Convenience: random double in [0,1)
  public double nextDouble() {
    return (nextLong() >>> 11) * (1.0 / (1L << 53));
  }

  /**
   * Generate a random radial position around a center point.
   *
   * @param centerX   X coordinate of the center
   * @param centerY   Y coordinate of the center
   * @param maxRadius Maximum radius distance from center
   * @return double[] {x, y} coordinates
   */
  public double[] nextRadialPosition(
    double centerX,
    double centerY,
    double maxRadius
  ) {
    // Random angle between 0 and 2π
    double angle = nextDouble() * 2.0 * Math.PI;
    // Random distance between 0 and maxRadius
    double distance = nextDouble() * maxRadius;

    double x = centerX + Math.cos(angle) * distance;
    double y = centerY + Math.sin(angle) * distance;

    return new double[] { x, y };
  }

  // Demo
  public static void main(String[] args) {
    Xorshift128Plus rng = new Xorshift128Plus(12345L, 67890L);

    // Generate 5 radial positions around (400, 300) with radius 200
    for (int i = 0; i < 5; i++) {
      double[] pos = rng.nextRadialPosition(400, 300, 200);
      System.out.printf("Target at (%.2f, %.2f)%n", pos[0], pos[1]);
    }
  }
}
