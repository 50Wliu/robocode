package bobthebuilder;

import robocode.*;

public class AdvancedEnemyBot extends EnemyBot
{
	private int id; // Unimplemented
	private double x;
	private double y;
	private double cachedEnergy;
	private double cachedVelocity;

	public AdvancedEnemyBot()
	{
		reset();
	}

	public AdvancedEnemyBot(ScannedRobotEvent event, Robot robot, int id)
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
	}

	public void update(ScannedRobotEvent e, Robot robot)
	{
		super.update(e);

		// Normal robots don't have getHeadingRadians()
		double absoluteBearing = Math.toRadians(robot.getHeading()) + e.getBearingRadians();
		if(absoluteBearing < 0)
		{
			absoluteBearing += 2 * Math.PI;
		}

		x = robot.getX() + Math.sin(absoluteBearing) * e.getDistance();
		y = robot.getY() + Math.cos(absoluteBearing) * e.getDistance();
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
		return x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * when;
	}

	public double getFutureY(long when)
	{
		return y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * when;
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
}
