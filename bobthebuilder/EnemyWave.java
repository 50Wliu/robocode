package bobthebuilder;

import java.awt.geom.*;

public class EnemyWave
{
	private Point2D.Double fireLocation;
	private long fireTime;
	private double bulletVelocity;
	private double directAngle;
	private double distanceTraveled;
	private int direction;

	public EnemyWave()
	{
		// Noop
	}

	public void setFireLocation(Point2D.Double location)
	{
		fireLocation = location;
	}

	public void setFireTime(long time)
	{
		fireTime = time;
	}

	public void setBulletVelocity(double velocity)
	{
		bulletVelocity = velocity;
	}

	public void setDirectAngle(double angle)
	{
		directAngle = angle;
	}

	public void setDistanceTraveled(double distance)
	{
		distanceTraveled = distance;
	}

	public void setDirection(int newDirection)
	{
		direction = newDirection;
	}
}
