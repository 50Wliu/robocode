package bobthebuilder;

import robocode.*;

public class AdvancedEnemyBot extends EnemyBot
{
	private int id; // Unimplemented
	private double x;
	private double y;
	private double cachedEnergy;
	private double cachedVelocity;
	private long lastUpdateTime;

	public AdvancedEnemyBot()
	{
		reset();
	}

	public AdvancedEnemyBot(ScannedRobotEvent event, AdvancedRobot robot, int id)
	{
		reset();
		update(event, robot);
		this.id = id;
	}

	public void reset()
	{
		super.reset();

		x = 0.0;
		y = 0.0;
		cachedEnergy = 100.0;
		cachedVelocity = 0.0;
		lastUpdateTime = 0;
	}

	public void update(ScannedRobotEvent e, AdvancedRobot robot)
	{
		super.update(e);

		double absoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		if(absoluteBearing < 0)
		{
			absoluteBearing += 2 * Math.PI;
		}

		x = robot.getX() + Math.sin(absoluteBearing) * e.getDistance();
		y = robot.getY() + Math.cos(absoluteBearing) * e.getDistance();

		lastUpdateTime = robot.getTime();
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public double getFutureX(long when)
	{
		return x + Math.sin(getHeadingRadians()) * getVelocity() * when;
	}

	public double getFutureY(long when)
	{
		return y + Math.cos(getHeadingRadians()) * getVelocity() * when;
	}

	public double getCachedEnergy()
	{
		return cachedEnergy;
	}

	public void setCachedEnergy(double energy)
	{
		cachedEnergy = energy;
	}

	public double getCachedVelocity()
	{
		return cachedVelocity;
	}

	public void setCachedVelocity(double velocity)
	{
		cachedVelocity = velocity;
	}

	public long getLastUpdateTime()
	{
		return lastUpdateTime;
	}
}
