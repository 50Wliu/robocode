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
		bearing = e.getBearing();
		distance = e.getDistance();
		energy = e.getEnergy();
		heading = e.getHeading();
		name = e.getName();
		velocity = e.getVelocity();
	}

	public boolean shouldTrack(ScannedRobotEvent e, long closer)
	{
		return none() || e.getDistance() < getDistance() - 70 || e.getName().equals(getName());
	}

	public boolean none()
	{
		return name.equals("");
	}

	public double getBearing()
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

	public double getHeading()
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
