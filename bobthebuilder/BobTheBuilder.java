package bobthebuilder;

import robocode.*;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyEvent.*;

public class BobTheBuilder extends AdvancedRobot
{
	private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	private int moveDirection = 1;
	private int wallMargin = 50;
	private boolean tooCloseToWall = false;
	private boolean wallMovementHandled = false;
	private boolean hitRobot = false;
	private boolean lockMode = false;

	private final String VERSION = "0.0.9";

	private enum RobotModes
	{
		// MODE_ENCIRCLE,
		MODE_STRAFE,
		MODE_TRACK,
		MODE_RAM,
		MODE_MANUAL
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

	public void onKeyPressed(KeyEvent e)
	{
		switch(e.getKeyCode())
		{
			case KeyEvent.VK_BACK_QUOTE:
			{
				lockMode = true;
				mode = RobotModes.MODE_MANUAL;
				break;
			}
			case KeyEvent.VK_1:
			{
				lockMode = true;
				mode = RobotModes.MODE_STRAFE;
				break;
			}
			case KeyEvent.VK_2:
			{
				lockMode = true;
				mode = RobotModes.MODE_TRACK;
				break;
			}
			case KeyEvent.VK_3:
			{
				lockMode = true;
				mode = RobotModes.MODE_RAM;
				break;
			}
			case KeyEvent.VK_ESCAPE:
			{
				lockMode = false;
				break;
			}
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
			if(!lockMode)
			{
				mode = RobotModes.MODE_STRAFE;
			}
		}
		else if(getOthers() == 1)
		{
			if(!lockMode)
			{
				mode = RobotModes.MODE_TRACK;
			}
		}
		else // Victory!
		{
			setMaxVelocity(0);
			for(int i = 0; i < 10; i++)
			{
				setTurnGunRight(360 * 5);
			}
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
				if(enemy.getEnergy() < getEnergy() && !lockMode)
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
				else if(!lockMode)// Ruh roh
				{
					mode = RobotModes.MODE_TRACK;
					setAhead(enemy.getDistance() - 50);
				}
				break;
			}
		}
	}

	// FIXME: Predictive targetting isn't accurate at long distances or for spinning enemies
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
				if(enemy.getEnergy() > 16)
				{
					setFire(3);
				}
				else if(enemy.getEnergy() > 10)
				{
					setFire(2);
				}
				else if(enemy.getEnergy() > 4)
				{
					setFire(1);
				}
				else if(enemy.getEnergy() > 0.5)
				{
					setFire(0.5);
				}
				else if(enemy.getEnergy() > 0.4)
				{
					setFire(0.1);
				}
			}
		}
		else
		{
			double firePower = Math.min(500 / enemy.getDistance(), 3);
			double bulletSpeed = 20 - firePower * 3;
			int time = (int) Math.ceil((enemy.getDistance() / bulletSpeed));

			double absoluteDegree = absoluteBearing(getX(), getY(), enemy.getFutureX(time), enemy.getFutureY(time));

			setTurnGunRight(normalizeBearing(absoluteDegree - getGunHeading()));

			if(getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10)
			{
				setFire(firePower);
			}
		}
	}

	public double absoluteBearing(double x1, double y1, double x2, double y2)
	{
		double distanceX = x2 - x1;
		double distanceY = y2 - y1;
		double hypotenuse = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
		double arcSin = Math.toDegrees(Math.asin(distanceX / hypotenuse));
		double bearing = 0;

		if(distanceX >= 0 && distanceY >= 0) // both pos: lower-Left
		{
			bearing = arcSin;
		}
		else if(distanceX <= 0 && distanceY >= 0) // x neg, y pos: lower-right
		{
			bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
		}
		else if(distanceX >= 0 && distanceY <= 0) // x pos, y neg: upper-left
		{
			bearing = 180 - arcSin;
		}
		else if(distanceX <= 0 && distanceY <= 0) // both neg: upper-right
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
