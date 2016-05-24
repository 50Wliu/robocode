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

	private enum RobotModes
	{
		MODE_ENCIRCLE,
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

		if(getOthers() >= 10)
		{
			mode = RobotModes.MODE_ENCIRCLE;
		}
		else if(getOthers() > 1)
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
			setDebugProperty("mode", mode.toString());
			doScanner();
			doMovement();
			doGun();
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		if(enemy.none() ||
		   e.getDistance() < enemy.getDistance() - 70 ||
		   e.getName().equals(enemy.getName()))
		{
			enemy.update(e, this);
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

	public void OnHitRobot(HitRobotEvent e)
	{
		if(mode == RobotModes.MODE_RAM)
		{
			setTurnRight(e.getBearing());
			setTurnGunRight(e.getBearing());

			if(getGunHeat() == 0 && getGunTurnRemaining() < 10)
			{
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
			setAhead(40);
		}
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		if(e.getName().equals(enemy.getName()))
		{
			enemy.reset();
		}

		if(getOthers() >= 10)
		{
			mode = RobotModes.MODE_ENCIRCLE;
		}
		else if(getOthers() > 1)
		{
			mode = RobotModes.MODE_STRAFE;
		}
		else
		{
			mode = RobotModes.MODE_TRACK;
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
			case MODE_ENCIRCLE:
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
			}
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
				setTurnRight(enemy.getBearing());

				if(Math.abs(getTurnRemaining()) < 10)
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
				}
				break;
			}
			case MODE_RAM:
			{
				if(enemy.getEnergy() < getEnergy())
				{
					setTurnRight(enemy.getBearing());
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

	public void doGun()
	{
		if(enemy.none())
		{
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
