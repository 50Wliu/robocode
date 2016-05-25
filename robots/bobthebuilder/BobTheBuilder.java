package bobthebuilder;

import robocode.*;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;

public class BobTheBuilder extends AdvancedRobot
{
	private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	private int moveDirection = 1;
	private int wallMargin = 50;
	private boolean tooCloseToWall = false;
	private boolean wallMovementHandled = false;
	private boolean hitRobot = false;

	private final String VERSION = "0.0.6";

	private enum RobotModes
	{
		// MODE_ENCIRCLE,
		MODE_STRAFE,
		MODE_TRACK,
		MODE_RAM
	}

	private RobotModes mode = RobotModes.MODE_STRAFE;

	public void run()
	{
		setColors(Color.blue, Color.blue, Color.yellow);
		setBulletColor(Color.yellow);
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		enemy.reset();

		// if(getOthers() >= 10)
		// {
		// 	mode = RobotModes.MODE_ENCIRCLE;
		// }
		/* else */if(getOthers() > 1)
		{
			mode = RobotModes.MODE_STRAFE;
		}
		else
		{
			mode = RobotModes.MODE_TRACK;
		}

		addCustomEvent(new Condition("there's_an_obstacle_ahead")
		{
			public boolean test()
			{
				return !tooCloseToWall && (
					getX() <= wallMargin ||
					getX() >= getBattleFieldWidth() - wallMargin ||
					getY() <= wallMargin ||
					getY() >= getBattleFieldHeight() - wallMargin
				);
			}
		});

		addCustomEvent(new Condition("safe")
		{
			public boolean test()
			{
				return (
					getX() > wallMargin * 1.5 &&
					getX() < getBattleFieldWidth() - (wallMargin * 1.5) &&
					getY() > wallMargin * 1.5 &&
					getY() < getBattleFieldHeight() - (wallMargin * 1.5)
				);
			}
		});

		while(true)
		{
			setDebugProperty("version", VERSION);
			setDebugProperty("mode", mode.toString());
			doScanner();
			doMovement();
			doGun();
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		if(enemy.none() // No enemy
		|| e.getEnergy() <= 0 // Enemy is disabled
		|| (e.getEnergy() < enemy.getEnergy() && e.getDistance() < enemy.getDistance()) // New robot has less life than the current enemy and is closer
		|| e.getDistance() < enemy.getDistance() - 70 // New robot is a lot closer than current enemy
		|| e.getName().equals(enemy.getName())) // New robot is the current enemy
		{
			setDebugProperty("enemy", e.getName());
			enemy.update(e, this);
			if(mode == RobotModes.MODE_TRACK || mode == RobotModes.MODE_RAM)
			{
				setTurnRight(enemy.getBearing());
			}
		}
	}

	public void onHitByBullet(HitByBulletEvent e)
	{
		//Stop wherever we're going and BACK UP
		// moveDirection *= -1;
		// setAhead(10000 * moveDirection);
	}

	public void onHitWall(HitWallEvent e)
	{
		//Go the other direction
		System.out.println("Wall hit at (" + getX() + ", " + getY() + "); bearing was " + e.getBearing() + "degrees");
		tooCloseToWall = true;
		wallMovementHandled = false;
	}

	public void onHitRobot(HitRobotEvent e)
	{
		hitRobot = true;
		if(mode != RobotModes.MODE_RAM)
		{
			// Move "backwards" a bit so that we don't get stuck
			// FIXME: There are situations where we still get stuck and aren't able to move back, which usually leads to death
			System.out.println("Hit robot (at fault: " + e.isMyFault() + "): bearing is " + e.getBearing());
			if(e.getBearing() > -90 && e.getBearing() <= 90)
			{
				setBack(100);
			}
			else
			{
				setAhead(100);
			}
		}
		else // Ram them!
		{
			setTurnRight(e.getBearing());
			setAhead(40);
		}
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		if(e.getName().equals(enemy.getName()))
		{
			enemy.reset();
		}

		// if(getOthers() >= 10)
		// {
		// 	mode = RobotModes.MODE_ENCIRCLE;
		// }
		/* else */if(getOthers() > 1)
		{
			mode = RobotModes.MODE_STRAFE;
		}
		else if(getOthers() == 1)
		{
			mode = RobotModes.MODE_TRACK;
		}
		else // Victory!
		{
			setMaxVelocity(0);
			setTurnGunRight(360 * 5);
		}
	}

	public void onCustomEvent(CustomEvent e)
	{
		if(e.getCondition().getName().equals("there's_an_obstacle_ahead"))
		{
			setDebugProperty("wall", "true");
			tooCloseToWall = true;
		}
		else if(e.getCondition().getName().equals("safe"))
		{
			setDebugProperty("wall", "false");
			tooCloseToWall = false;
			wallMovementHandled = false;
		}
	}

	public void doScanner()
	{
		setTurnRadarRight(360);
	}

	public void doMovement()
	{
		switch(mode)
		{
			/*case MODE_ENCIRCLE:
			{
				if(tooCloseToWall)
				{
					if(!wallMovementHandled)
					{
						moveDirection *= -1;
						wallMovementHandled = true;
					}
					setAhead(wallMargin * moveDirection);
				}

				if(hitRobot)
				{
					if(getDistanceRemaining() == 0)
					{
						hitRobot = false;
					}
					else
					{
						return;
					}
				}

				setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));

				if(!tooCloseToWall)
				{
					if(ThreadLocalRandom.current().nextInt(0, 51) == 50)
					{
						moveDirection *= -1;
					}
					setAhead(1000 * moveDirection);
				}
				break;
			}*/
			case MODE_STRAFE:
			{
				if(tooCloseToWall)
				{
					if(!wallMovementHandled)
					{
						moveDirection *= -1;
						wallMovementHandled = true;
					}
					setAhead(wallMargin * moveDirection);
				}

				if(hitRobot)
				{
					if(getDistanceRemaining() == 0)
					{
						hitRobot = false;
					}
					else
					{
						return;
					}
				}

				setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));

				if(!tooCloseToWall)
				{
					// Strafe rather randomly
					if(ThreadLocalRandom.current().nextInt(0, 51) == 50)
					{
						moveDirection *= -1;
					}
					setAhead(1000 * moveDirection);
				}
				break;
			}
			case MODE_TRACK:
			{
				if(enemy.getEnergy() < getEnergy())
				{
					// We have the advantage; ram them for extra points!
					mode = RobotModes.MODE_RAM;
					setAhead(enemy.getDistance() + 5);
				}
				else
				{
					setAhead(enemy.getDistance() - 50);
				}
				break;
			}
			case MODE_RAM:
			{
				if(enemy.getEnergy() < getEnergy())
				{
					setAhead(enemy.getDistance() + 5);
				}
				else // Ruh roh
				{
					mode = RobotModes.MODE_TRACK;
					setAhead(enemy.getDistance() - 50);
				}
				break;
			}
		}
	}

	// FIXME: This occasionally points the gun straight up in MODE_TRACK or MODE_RAM instead of at the enemy
	public void doGun()
	{
		if(enemy.none())
		{
			return;
		}
		
		if(mode == RobotModes.MODE_RAM && hitRobot)
		{
			setTurnGunRight(normalizeBearing(getHeading() - getGunHeading() + enemy.getBearing()));
			if(getGunHeat() == 0 && getGunTurnRemaining() < 10)
			{
				// We get extra points if we kill them by ramming
				if(e.getEnergy() > 16)
				{
					setFire(3);
				}
				else if(e.getEnergy() > 10)
				{
					setFire(2);
				}
				else if(e.getEnergy() > 4)
				{
					setFire(1);
				}
				else if(e.getEnergy() > 0.5)
				{
					setFire(0.5);
				}
				else if(e.getEnergy() > 0.4)
				{
					setFire(0.1);
				}
			}
			return;
		}

		double firePower = Math.min(500 / enemy.getDistance(), 3);
		double bulletSpeed = 20 - firePower * 3;
		int time = (int)(enemy.getDistance() / bulletSpeed);

		double absoluteDegree = absoluteBearing(getX(), getY(), enemy.getFutureX(time), enemy.getFutureY(time));

		setTurnGunRight(normalizeBearing(absoluteDegree - getGunHeading()));

		if(getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10)
		{
			setFire(firePower);
		}
	}

	public double absoluteBearing(double x1, double y1, double x2, double y2)
	{
		double distanceX = x2 - x1;
		double distanceY = y2 - y1;
		double hypotenuse = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
		double arcSin = Math.toDegrees(Math.asin(distanceX / hypotenuse));
		double bearing = 0;

		if(distanceX > 0 && distanceY > 0) // both pos: lower-Left
		{
			bearing = arcSin;
		}
		else if(distanceX < 0 && distanceY > 0) // x neg, y pos: lower-right
		{
			bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
		}
		else if(distanceX > 0 && distanceY < 0) // x pos, y neg: upper-left
		{
			bearing = 180 - arcSin;
		}
		else if(distanceX < 0 && distanceY < 0) // both neg: upper-right
		{
			bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
		}
		return bearing;
	}

	public double normalizeBearing(double angle)
	{
		while(angle > 180)
		{
			angle -= 360;
		}

		while(angle < -180)
		{
			angle += 360;
		}
		return angle;
	}
}
