package exam2016;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

public class BulletWave extends Condition
{
	private static final double MAX_ESCAPE_ANGLE = 0.7;
	private static final int INDEXES = 5;
	private static final int MIDDLE_BIN = (Helpers.BINS - 1) / 2;
	private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;

	private double bearing, power, distanceTraveled;
	private Point2D.Double position;
	public static Point2D.Double enemyPosition;
	private long fireTime;
	private int direction;
	private static int[][][][] statBuffers = new int[INDEXES][INDEXES][INDEXES][Helpers.BINS];
	private int[] buffer;
	private AdvancedRobot robot;

	public BulletWave(AdvancedRobot robot, AdvancedEnemyBot enemy, Point2D.Double position, double bearing, double power, int direction)
	{
		this.robot = robot;
		this.position = position;
		this.bearing = bearing;
		this.power = power;
		this.direction = direction;

		// Update segmentations
		int distanceIndex = (int) (enemy.getDistance() / (Rules.RADAR_SCAN_RADIUS / INDEXES));
		int velocityIndex = (int) Math.abs(enemy.getVelocity() / 2);
		int cachedVelocityIndex = (int) Math.abs(enemy.getCachedVelocity() / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][cachedVelocityIndex];
	}

	public boolean test()
	{
		distanceTraveled += Rules.getBulletSpeed(power);
		if(distanceTraveled > position.distance(enemyPosition) - Helpers.ROBOT_SIZE)
		{
			buffer[currentBin()]++;
			robot.removeCustomEvent(this);
		}
		return false;
	}

	public double mostVisitedBearingOffset()
	{
		return (direction * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
	}

	private int currentBin()
	{
		int bin = (int) Math.round(((Utils.normalRelativeAngle(Helpers.absoluteBearing(position, enemyPosition) - bearing)) / (direction * BIN_WIDTH)) + MIDDLE_BIN);
		return (int) Helpers.limit(0, bin, Helpers.BINS - 1);
	}

	private int mostVisitedBin()
	{
		int mostVisited = MIDDLE_BIN;
		for(int i = 0; i < Helpers.BINS; i++)
		{
			if(buffer[i] > buffer[mostVisited])
			{
				mostVisited = i;
			}
		}
		return mostVisited;
	}
}
