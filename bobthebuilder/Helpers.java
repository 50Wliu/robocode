/* Helper functions */

package exam2016;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

public class Helpers
{
	public static final int ROBOT_SIZE = 18;
	public static final int BINS = 47;

	public static double absoluteBearing(Point2D.Double source, Point2D.Double target)
	{
		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
	}

	public static Point2D.Double project(Point2D.Double position, double angle, double length)
	{
		return new Point2D.Double(position.getX() + Math.sin(angle) * length, position.getY() + Math.cos(angle) * length);
	}

	public static double limit(double min, double value, double max)
	{
		return Math.max(min, Math.min(value, max));
	}
}
