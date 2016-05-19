package bobthebuilder;

import robocode.*;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;

public class BobTheBuilder extends AdvancedRobot
{
	private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	private int moveDirection = 1;
	private int wallMargin = 50;
	private int tooCloseToWall = 0;

	private enum RobotModes
	{
		MODE_ENCIRCLE,
		MODE_STRAFE,
		MODE_TRACK
	}

	private RobotModes mode = RobotModes.MODE_STRAFE;

	public void run()
	{
		setColors(Color.blue, Color.blue, Color.yellow);
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
				return (
					getX() <= wallMargin ||
					getX() >= getBattleFieldWidth() - wallMargin ||
					getY() <= wallMargin ||
					getY() >= getBattleFieldHeight() - wallMargin
				);
			}
		});

		while(true)
		{
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
		moveDirection *= -1;
		setAhead(10000 * moveDirection);
	}

	public void onHitWall(HitWallEvent e)
	{
		//Go the other direction
		System.out.println("Wall hit at (" + getX() + ", " + getY() + "); bearing was " + e.getBearing() + "degrees");
		tooCloseToWall += wallMargin;
		setMaxVelocity(0);
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
			if(tooCloseToWall <= 0)
			{
				tooCloseToWall += wallMargin;
				setMaxVelocity(0);
			}
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
				if(tooCloseToWall > 0)
				{
					tooCloseToWall--;
				}

				if(getVelocity() == 0)
				{
					setMaxVelocity(8);
					moveDirection *= -1;
				}

				setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));
				if(ThreadLocalRandom.current().nextInt(0, 101) % 20 == 0)
				{
					moveDirection *= -1;
				}

				setAhead(1000 * moveDirection);
				break;
			}
			case MODE_STRAFE:
			{
				if(tooCloseToWall > 0)
				{
					tooCloseToWall--;
				}

				if(getVelocity() == 0)
				{
					setMaxVelocity(8);
					moveDirection *= -1;
					setAhead(10000 * moveDirection);
				}

				setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));

				// Strafe rather randomly
				if(ThreadLocalRandom.current().nextInt(0, 1001) % 20 == 0)
				{
					moveDirection *= -1;
					setAhead(150 * moveDirection);
				}
				break;
			}
			case MODE_TRACK:
			{
				if(tooCloseToWall > 0)
				{
					tooCloseToWall--;
				}

				if(getVelocity() == 0)
				{
					setMaxVelocity(8);
					moveDirection *= -1;
					setAhead(10000 * moveDirection);
				}

				setTurnRight(enemy.getBearing());
				setTurnRadarRight(getHeading() - getRadarHeading() + enemy.getBearing());

				if(Math.abs(getTurnRemaining()) < 10)
				{
					if(enemy.getDistance() > 200)
					{
						setAhead(enemy.getDistance() / 2);
					}

					if(enemy.getDistance() < 100)
					{
						setBack(enemy.getDistance() * 2);
					}
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
