package kst4contest.locatorUtils;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Latitude.
 *
 * @author Paul Picazo <ppicazo@gmail.com>
 *
 */
public class Latitude extends Angle {

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj.getClass() == this.getClass())) {
      return false;
    }
    return ((Latitude) obj).hashCode() == this.hashCode();
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    if (radians == null) {
      return prime;
    }
    return prime + radians.hashCode();
  }

  /**
   * @param degrees input
   * @return latitude object
   */
  public static Latitude fromDegrees(final double degrees) {
    Latitude latitude = new Latitude();
    latitude.radians = Angle.degreesToRadians(new BigDecimal(degrees),
        MathContext.DECIMAL128);
    return latitude;
  }

}