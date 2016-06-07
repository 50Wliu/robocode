package bobthebuilder;

import java.awt.geom.*;
import robocode.util.Utils;

public class BulletWave
{
	private double startX, startY, startBearing, power;
	private long fireTime;
	private int direction;
	private int[] returnSegment;

	public BulletWave(double x, double y, double bearing, double bulletPower, int bulletDirection, long time, int[] segment)
	{
		startX = x;
		startY = y;
		startBearing = bearing;
		power = bulletPower;
		direction = bulletDirection;
		fireTime = time;
		returnSegment = segment;
	}

	public boolean checkHit(double enemyX, double enemyY, long currentTime)
	{
		// If the distance from the wave origin to our enemy has passed the distance the bullet would have traveled...
		if(Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime) * getBulletSpeed())
		{
			double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
			double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);

			// Figure out the guess factor that the robot was at when this wave hit it and increment that index
			double guessFactor = Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
			int index = (int) Math.round((returnSegment.length - 1) / 2 * (guessFactor + 1));
			returnSegment[index]++;
			return true;
		}
		return false;
	}

	public double getBulletSpeed()
	{
		return 20 - power * 3;
	}

	public double maxEscapeAngle()
	{
		return Math.asin(8 / getBulletSpeed());
	}
}
