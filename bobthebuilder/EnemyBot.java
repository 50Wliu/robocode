package bobthebuilder;

import robocode.*;

public class EnemyBot
{

	private double bearing;
	private double distance;
	private double energy;
	private double heading;
	private String name;
	private double velocity;

	public EnemyBot()
	{
		reset();
	}

	public void reset()
	{
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		name = "";
		velocity = 0.0;
	}

	final public void update(ScannedRobotEvent e)
	{
		bearing = e.getBearingRadians();
		distance = e.getDistance();
		energy = e.getEnergy();
		heading = e.getHeadingRadians();
		name = e.getName();
		velocity = e.getVelocity();
	}

	public boolean none()
	{
		return name.equals("");
	}

	public double getBearingRadians()
	{
		return bearing;
	}

	public double getDistance()
	{
		return distance;
	}

	public double getEnergy()
	{
		return energy;
	}

	public double getHeadingRadians()
	{
		return heading;
	}

	public String getName()
	{
		return name;
	}

	public double getVelocity()
	{
		return velocity;
	}
}
