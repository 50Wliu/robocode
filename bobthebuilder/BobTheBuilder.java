package bobthebuilder;

import robocode.*;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyEvent.*;

public class BobTheBuilder extends AdvancedRobot
{
	private HashMap<String, AdvancedEnemyBot> enemies;
	private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	private int id = 0; // Unimplemented
	private int moveDirection = 1;
	private int wallMargin = 50;
	private boolean tooCloseToWall = false;
	private boolean hitRobot = false;
	private boolean lockMode = false;

	private final String VERSION = "0.0.12";

	private enum RobotModes
	{
		// MODE_ENCIRCLE,
		MODE_STRAFE,
		MODE_TRACK,
		MODE_RAM,
		MODE_MANUAL // Unimplemented
	}

	private RobotModes mode = RobotModes.MODE_STRAFE;

	public void run()
	{
		enemies = new HashMap<String, AdvancedEnemyBot>(this.getOthers());

		this.setColors(Color.blue, Color.blue, Color.yellow);
		this.setBulletColor(Color.yellow);
		this.setAdjustRadarForGunTurn(true);
		this.setAdjustGunForRobotTurn(true);

		this.addCustomEvent(new Condition("there's_an_obstacle_ahead")
		{
			public boolean test()
			{
				return !tooCloseToWall && (
					BobTheBuilder.this.getX() <= wallMargin ||
					BobTheBuilder.this.getX() >= getBattleFieldWidth() - wallMargin ||
					BobTheBuilder.this.getY() <= wallMargin ||
					BobTheBuilder.this.getY() >= getBattleFieldHeight() - wallMargin
				);
			}
		});

		while(true)
		{
			this.setDebugProperty("version", VERSION);
			this.setDebugProperty("mode", mode.toString());
			scan();
			move();
			fire();
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		if(!enemies.containsKey(e.getName()))
		{
			enemies.put(e.getName(), new AdvancedEnemyBot(e, this, id));
			id++;
		}
		enemies.get(e.getName()).update(e, this);

		if(enemy.none() // No enemy
		|| e.getEnergy() <= 0 // Enemy is disabled
		|| (e.getEnergy() < enemy.getEnergy() && e.getDistance() < enemy.getDistance()) // New robot has less life than the current enemy and is closer
		|| e.getDistance() < enemy.getDistance() - 70 // New robot is a lot closer than current enemy
		|| e.getName().equals(enemy.getName())) // New robot is the current enemy
		{
			this.setDebugProperty("enemy", e.getName());
			enemy = enemies.get(e.getName());

			if(mode == RobotModes.MODE_TRACK || mode == RobotModes.MODE_RAM)
			{
				this.setTurnRight(enemy.getBearing());
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
		System.out.println("Wall hit at (" + getX() + ", " + getY() + "); bearing was " + e.getBearing() + " degrees");
		// Move immediately so that we don't generate more HitWallEvents while turning
		if(e.getBearing() > - 90 && e.getBearing() <= 90)
		{
			this.back(10);
		}
		else
		{
			this.ahead(10);
		}
		tooCloseToWall = true;
	}

	public void onHitRobot(HitRobotEvent e)
	{
		hitRobot = true;
		if(mode != RobotModes.MODE_RAM)
		{
			// Move "backwards" a bit so that we don't get stuck
			// TODO: Calculate the bearing to the wall even if we haven't hit a wall
			if(tooCloseToWall && !this.getHitWallEvents().isEmpty())
			{
				this.turnRight((this.getHitWallEvents().lastElement().getBearing() + e.getBearing()) / 2);
			}
			this.setAhead(e.getBearing() > - 90 && e.getBearing() <= 90 ? -100 : 100);
		}
		else // Ram them!
		{
			this.setTurnRight(e.getBearing());
			this.setAhead(40);
		}
	}

	public void onKeyPressed(KeyEvent e)
	{
		switch(e.getKeyCode())
		{
			// case KeyEvent.VK_BACK_QUOTE:
			// {
			// 	lockMode = true;
			// 	mode = RobotModes.MODE_MANUAL;
			// 	break;
			// }
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
			enemies.remove(enemy.getName());
			enemy.reset();
		}

		// if(getOthers() >= 10)
		// {
		// 	mode = RobotModes.MODE_ENCIRCLE;
		// }
		/* else */if(this.getOthers() > 1)
		{
			if(!lockMode)
			{
				mode = RobotModes.MODE_STRAFE;
			}
		}
		else if(this.getOthers() == 1)
		{
			if(!lockMode)
			{
				mode = RobotModes.MODE_TRACK;
			}
		}
		else // Victory!
		{
			this.setMaxVelocity(0);
			for(int i = 0; i < 10; i++)
			{
				this.setTurnGunRight(360 * 5);
			}
		}
	}

	public void onCustomEvent(CustomEvent e)
	{
		if(e.getCondition().getName().equals("there's_an_obstacle_ahead"))
		{
			tooCloseToWall = true;
		}
	}

	public void scan()
	{
		this.setTurnRadarRight(360);
	}

	public void move()
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
					if(this.getDistanceRemaining() == 0)
					{
						hitRobot = false;
					}
					else
					{
						return;
					}
				}

				if(tooCloseToWall) // Move towards the center of the battlefield
				{
					if(this.getDistanceRemaining() == 0)
					{
						tooCloseToWall = false;
					}
					else
					{
						double absoluteBearingToCenter = absoluteBearing(getX(), getY(), getBattleFieldWidth() / 2, getBattleFieldHeight() / 2);
						double turn = absoluteBearingToCenter - getHeading();
						this.setTurnRight(normalizeBearing(turn));
						this.setAhead(100);
						return;
					}
				}

				this.setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));

				// Strafe rather randomly
				if(ThreadLocalRandom.current().nextInt(0, 51) == 50)
				{
					moveDirection *= -1;
				}
				this.setAhead(1000 * moveDirection);
				break;
			}
			case MODE_TRACK:
			{
				if(enemy.getEnergy() < getEnergy() && !lockMode)
				{
					// We have the advantage; ram them for extra points!
					mode = RobotModes.MODE_RAM;
					this.setAhead(enemy.getDistance() + 5);
				}
				else if(getEnergy() < 15 && enemy.getEnergy() > 10 && !lockMode)
				{
					// Not looking too good for us - go back to random movement
					mode = RobotModes.MODE_STRAFE;
				}
				else
				{
					this.setAhead(enemy.getDistance() - 50);
				}
				break;
			}
			case MODE_RAM:
			{
				if(enemy.getEnergy() < getEnergy())
				{
					this.setAhead(enemy.getDistance() + 5);
				}
				else if(!lockMode) // Ruh roh
				{
					mode = RobotModes.MODE_TRACK;
					this.setAhead(enemy.getDistance() - 50);
				}
				break;
			}
		}
	}

	// FIXME: Predictive targeting isn't accurate at long distances or for spinning enemies
	public void fire()
	{
		if(enemy.none())
		{
			return;
		}

		if(mode == RobotModes.MODE_RAM && hitRobot)
		{
			this.setTurnGunRight(normalizeBearing(getHeading() - getGunHeading() + enemy.getBearing()));
			if(this.getGunHeat() == 0 && this.getGunTurnRemaining() < 10)
			{
				// We get extra points if we kill them by ramming
				// TODO: Make this more fluid and not a bunch of if/else statements
				if(enemy.getEnergy() > 16)
				{
					this.setFire(3);
				}
				else if(enemy.getEnergy() > 10)
				{
					this.setFire(2);
				}
				else if(enemy.getEnergy() > 4)
				{
					this.setFire(1);
				}
				else if(enemy.getEnergy() > 0.5)
				{
					this.setFire(0.5);
				}
				else if(enemy.getEnergy() > 0.4)
				{
					this.setFire(0.1);
				}
			}
		}
		else
		{
			double firePower = Math.min(500 / enemy.getDistance(), 3);
			double bulletSpeed = 20 - firePower * 3;
			int time = (int) Math.ceil((enemy.getDistance() / bulletSpeed));

			double absoluteDegree = absoluteBearing(this.getX(), this.getY(), enemy.getFutureX(time), enemy.getFutureY(time));

			this.setTurnGunRight(normalizeBearing(absoluteDegree - getGunHeading()));

			if(this.getGunHeat() == 0 && Math.abs(this.getGunTurnRemaining()) < 10)
			{
				this.setFire(firePower);
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
