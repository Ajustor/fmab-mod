package com.ajustor.fmab.alchemy.drawing;

/** Point du plan. Dans le carnet comme dans les glyphes, l'axe y descend. */
public record Vec2(double x, double y) {
	public static final Vec2 ZERO = new Vec2(0, 0);

	public Vec2 add(Vec2 o) {
		return new Vec2(x + o.x, y + o.y);
	}

	public Vec2 sub(Vec2 o) {
		return new Vec2(x - o.x, y - o.y);
	}

	public Vec2 scale(double k) {
		return new Vec2(x * k, y * k);
	}

	public Vec2 rotate(double radians) {
		double c = Math.cos(radians), s = Math.sin(radians);
		return new Vec2(x * c - y * s, x * s + y * c);
	}

	public double length() {
		return Math.hypot(x, y);
	}

	public double distance(Vec2 o) {
		return Math.hypot(x - o.x, y - o.y);
	}

	/** Angle polaire en radians, mesuré avec y vers le bas (sens horaire à l'écran). */
	public double angle() {
		return Math.atan2(y, x);
	}

	public static Vec2 polar(double radius, double radians) {
		return new Vec2(radius * Math.cos(radians), radius * Math.sin(radians));
	}
}
