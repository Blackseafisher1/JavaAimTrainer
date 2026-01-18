package com.aimtrainer;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Xorshift128+ Zufallszahlengenerator.
 * Sehr schnell, jedoch nicht kryptografisch sicher.
 */
public final class Xorshift128Plus {

  private static final Logger LOGGER = Logger.getLogger(Xorshift128Plus.class.getName());

  private long s0;
  private long s1;

  public Xorshift128Plus(long seed1, long seed2) {
    if (seed1 == 0 && seed2 == 0) {
      seed2 = 1; // avoid all-zero state
    }
    this.s0 = seed1;
    this.s1 = seed2;
  }

  // Kernalgorithmus: liefert die nächste 64-Bit-Zahl
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

  // Hilfsmethode: zufälliges int
  public int nextInt() {
    return (int) nextLong();
  }

  // Hilfsmethode: begrenztes int im Bereich [0, bound)
  public int nextInt(int bound) {
    if (bound <= 0) throw new IllegalArgumentException(
      "bound must be positive"
    );
    long r = nextLong() >>> 1; // make non-negative
    return (int) (r % bound);
  }

  // Hilfsmethode: zufälliges double im Bereich [0,1)
  public double nextDouble() {
    return (nextLong() >>> 11) * (1.0 / (1L << 53));
  }

  /**
   * Erzeugt eine zufällige radiale Position um einen Mittelpunkt.
   *
   * @param centerX   X-Koordinate des Mittelpunkts
   * @param centerY   Y-Koordinate des Mittelpunkts
   * @param maxRadius Maximale Distanz zum Mittelpunkt
   * @return double[] {x, y} Koordinaten
   */
  public double[] nextRadialPosition(
    double centerX,
    double centerY,
    double maxRadius
  ) {
    // Zufälliger Winkel zwischen 0 und 2π
    double angle = nextDouble() * 2.0 * Math.PI;
    // Zufällige Distanz zwischen 0 und maxRadius
    double distance = nextDouble() * maxRadius;

    double x = centerX + Math.cos(angle) * distance;
    double y = centerY + Math.sin(angle) * distance;

    return new double[] { x, y };
  }

  // Beispiel / Demo
  public static void main(String[] args) {
    Xorshift128Plus rng = new Xorshift128Plus(12345L, 67890L);

    // Erzeugt 5 radiale Positionen um (400,300) mit Radius 200
    for (int i = 0; i < 5; i++) {
      double[] pos = rng.nextRadialPosition(400, 300, 200);
      LOGGER.log(Level.INFO, String.format("Target at (%.2f, %.2f)", pos[0], pos[1]));
    }
  }
}
