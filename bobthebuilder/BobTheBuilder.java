package bobthebuilder;

import robocode.*;
import robocode.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyEvent.*;
import java.awt.geom.*;

public class BobTheBuilder extends AdvancedRobot
{
	private static final int WALL_MARGIN = 150;
	private static final String VERSION = "0.2.0";

	private HashMap<String, AdvancedEnemyBot> enemies;
	private AdvancedEnemyBot enemy;
	private Rectangle2D.Double safetyRectangle;
	private Point2D.Double position;
	private Point2D.Double enemyPosition; // TODO: Move this into AdvancedEnemyBot
	private ArrayList<EnemyWave> surfWaves;
	private ArrayList<Integer> surfDirections;
	private ArrayList<Double> surfAbsoluteBearings;
	private static double surfStats[] = new double[Helpers.BINS];
	private int id = 0; // Unimplemented
	private int moveDirection = 1;
	private int enemyDirection = 1;
	private int wallMargin = 50;
	private boolean tooCloseToWall = false;
	private boolean hitRobot = false;
	private boolean lockMode = false;

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
		enemy = new AdvancedEnemyBot();
		safetyRectangle = new Rectangle2D.Double(Helpers.ROBOT_SIZE, Helpers.ROBOT_SIZE, this.getBattleFieldWidth() - Helpers.ROBOT_SIZE * 2, this.getBattleFieldHeight() - Helpers.ROBOT_SIZE * 2);
		surfWaves = new ArrayList<EnemyWave>();
		surfDirections = new ArrayList<Integer>();
		surfAbsoluteBearings = new ArrayList<Double>();

		this.setColors(Color.blue, Color.blue, Color.yellow);
		this.setBulletColor(Color.yellow);
		this.setAdjustRadarForRobotTurn(true);
		this.setAdjustRadarForGunTurn(true);
		this.setAdjustGunForRobotTurn(true);

		this.addCustomEvent(new Condition("there's_an_obstacle_ahead")
		{
			public boolean test()
			{
				return !tooCloseToWall && (
					BobTheBuilder.this.getX() <= wallMargin ||
					BobTheBuilder.this.getX() >= BobTheBuilder.this.getBattleFieldWidth() - wallMargin ||
					BobTheBuilder.this.getY() <= wallMargin ||
					BobTheBuilder.this.getY() >= BobTheBuilder.this.getBattleFieldHeight() - wallMargin
				);
			}
		});

		while(true)
		{
			this.setDebugProperty("version", VERSION);
			this.setDebugProperty("mode", mode.toString());
			if(this.getTime() - enemy.getLastUpdateTime() > 5)
			{
				// We have outdated data that really isn't going to help us
				enemy.reset();
			}

			if(enemy.none() || getOthers() != 1)
			{
				this.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			move();
			// fire();
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

		position = new Point2D.Double(this.getX(), this.getY());

		double lateralVelocity = this.getVelocity() * Math.sin(e.getBearingRadians());
		double absoluteBearing = e.getBearingRadians() + this.getHeadingRadians();

		surfDirections.add(0, new Integer((lateralVelocity >= 0 ? 1 : -1)));
		surfAbsoluteBearings.add(0, new Double(absoluteBearing + Math.PI));

		double power = enemies.get(e.getName()).getCachedEnergy() - e.getEnergy();
		if(power > 0.09 && power < 3.01 && surfDirections.size() > 2) // Doubles are imprecise so don't compare equal to
		{
			EnemyWave wave = new EnemyWave();
			wave.setFireTime(this.getTime() - 1);
			wave.setBulletVelocity(Helpers.bulletVelocity(power));
			wave.setDistanceTraveled(Helpers.bulletVelocity(power));
			wave.setDirection(surfDirections.get(2).intValue());
			wave.setDirectAngle(surfAbsoluteBearings.get(2).doubleValue());
			wave.setFireLocation((Point2D.Double) enemyPosition.clone());
			surfWaves.add(wave);
		}

		enemyPosition = Helpers.project(position, absoluteBearing, e.getDistance());

		updateSurfWaves();
		surf();

		enemies.get(e.getName()).setCachedEnergy(e.getEnergy());

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
				this.setTurnRightRadians(enemy.getBearingRadians());
			}
			fire();
			enemy.setCachedVelocity(enemy.getVelocity());

			this.setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - this.getRadarHeadingRadians()) * 2);
		}
	}

	public void onBulletHit(BulletHitEvent e)
	{
		if(enemies.containsKey(e.getName()))
		{
			double cachedEnergy = enemies.get(e.getName()).getCachedEnergy();
			double power = e.getBullet().getPower();
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy - (4 * power + Math.max(0, 2 * (power - 1))));
		}
	}

	public void onHitByBullet(HitByBulletEvent e)
	{
		// If the surfWaves collection is empty, we must have missed the
		// detection of this wave somehow.
		if(!surfWaves.isEmpty())
		{
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for(int i = 0; i < surfWaves.size(); i++)
			{
				EnemyWave wave = surfWaves.get(i);

				if(Math.abs(wave.getDistanceTraveled() - position.distance(wave.getFireLocation())) < 50
				&& Math.abs(e.getBullet().getVelocity() - wave.getBulletVelocity()) < 0.001)
				{
					hitWave = wave;
					break;
				}
			}

			if(hitWave != null)
			{
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				surfWaves.remove(surfWaves.lastIndexOf(hitWave));
			}
		}

		if(enemies.containsKey(e.getName()))
		{
			double cachedEnergy = enemies.get(e.getName()).getCachedEnergy();
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy + 3 * e.getBullet().getPower());
		}
	}

	public void onBulletHitBullet(BulletHitBulletEvent e)
	{
		// If the surfWaves collection is empty, we must have missed the
		// detection of this wave somehow.
		if(!surfWaves.isEmpty())
		{
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getHitBullet().getX(), e.getHitBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for(int i = 0; i < surfWaves.size(); i++)
			{
				EnemyWave wave = surfWaves.get(i);

				if(Math.abs(wave.getDistanceTraveled() - hitBulletLocation.distance(wave.getFireLocation())) < 5
				&& Math.abs(e.getHitBullet().getVelocity() - wave.getBulletVelocity()) < 0.001)
				{
					hitWave = wave;
					break;
				}
			}

			if(hitWave != null)
			{
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				surfWaves.remove(surfWaves.lastIndexOf(hitWave));
			}
		}
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

		if(enemies.containsKey(e.getName()))
		{
			double cachedEnergy = enemies.get(e.getName()).getCachedEnergy();
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy - 0.6); // Robots lose 0.6 energy / collision
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

	private void move()
	{
		EnemyWave surfWave = getClosestSurfableWave();

		if(surfWave != null)
		{
			return;
		}

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

				this.setTurnRight(normalizeBearing(Math.toDegrees(enemy.getBearingRadians()) + 90 - (15 * moveDirection)));

				// Strafe rather randomly
				if(ThreadLocalRandom.current().nextInt(0, 11) == 0)
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

	private void fire()
	{
		if(enemy.none())
		{
			return;
		}

		if(mode == RobotModes.MODE_RAM && hitRobot)
		{
			this.setTurnGunRight(normalizeBearing(Math.toDegrees(getHeadingRadians() - getGunHeadingRadians() + enemy.getBearingRadians())));
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
			// double firePower = Math.min(500 / enemy.getDistance(), 3);
			// double bulletSpeed = 20 - firePower * 3;
			// int time = (int) Math.ceil((enemy.getDistance() / bulletSpeed));
			//
			// double absoluteDegree = absoluteBearing(this.getX(), this.getY(), enemy.getFutureX(time), enemy.getFutureY(time));
			//
			// this.setTurnGunRight(normalizeBearing(absoluteDegree - getGunHeading()));
			//
			// if(this.getGunHeat() == 0 && Math.abs(this.getGunTurnRemaining()) < 10)
			// {
			// 	this.setFire(firePower);
			// }

			double absoluteBearing = enemy.getBearingRadians() + this.getHeadingRadians();
			if(enemy.getVelocity() != 0)
			{
				if(Math.sin(enemy.getHeadingRadians() - absoluteBearing) * enemy.getVelocity() < 0)
				{
					enemyDirection = -1;
				}
				else
				{
					enemyDirection = 1;
				}
			}

			double power = Math.min(500 / enemy.getDistance(), 3);
			Point2D.Double position = new Point2D.Double(this.getX(), this.getY());
			BulletWave wave = new BulletWave(this, enemy, position, absoluteBearing, power, enemyDirection);
			BulletWave.enemyPosition = Helpers.project(position, absoluteBearing, enemy.getDistance());

			double radians = Utils.normalRelativeAngle(absoluteBearing - this.getGunHeadingRadians() + wave.mostVisitedBearingOffset());
			this.setTurnGunRightRadians(radians);
			this.setFire(power);

			if(this.getEnergy() >= power)
			{
				this.addCustomEvent(wave);
			}
		}
	}

	private double checkDanger(EnemyWave surfWave, int direction)
	{
		int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));
		return surfStats[index];
	}

	private void surf()
	{
		EnemyWave surfWave = getClosestSurfableWave();

		if(surfWave == null)
		{
			return;
		}

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = Helpers.absoluteBearing(surfWave.getFireLocation(), position);
		if(dangerLeft < dangerRight)
		{
			goAngle = wallSmoothing(position, goAngle - (Math.PI / 2), -1);
		}
		else
		{
			goAngle = wallSmoothing(position, goAngle + (Math.PI / 2), 1);
		}

		setBackAsFront(goAngle);
	}

	private void updateSurfWaves()
	{
		for(int i = 0; i < surfWaves.size(); i++)
		{
			EnemyWave wave = surfWaves.get(i);
			double distance = (this.getTime() - wave.getFireTime()) * wave.getBulletVelocity();

			wave.setDistanceTraveled(distance);
			// 'tis past us now
			if(distance > position.distance(wave.getFireLocation()) + Helpers.ROBOT_SIZE)
			{
				surfWaves.remove(i);
				i--;
			}
		}
	}

	private EnemyWave getClosestSurfableWave()
	{
		double closestDistance = Double.POSITIVE_INFINITY;
		EnemyWave surfWave = null;

		for(int i = 0; i < surfWaves.size(); i++)
		{
			EnemyWave wave = surfWaves.get(i);
			double distance = position.distance(wave.getFireLocation()) - wave.getDistanceTraveled();
			if(distance > wave.getBulletVelocity() && distance < closestDistance)
			{
				surfWave = wave;
				closestDistance = distance;
			}
		}
		return surfWave;
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	private int getFactorIndex(EnemyWave wave, Point2D.Double targetLocation)
	{
		double offsetAngle = Helpers.absoluteBearing(wave.getFireLocation(), targetLocation) - wave.getDirectAngle();
		double factor = Utils.normalRelativeAngle(offsetAngle) / Math.asin(8.0 / wave.getBulletVelocity()) * wave.getDirection();

		return (int) Helpers.limit(0, factor * ((Helpers.BINS - 1) / 2) * ((Helpers.BINS - 1) / 2), Helpers.BINS - 1);
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	private void logHit(EnemyWave wave, Point2D.Double targetLocation)
	{
		int index = getFactorIndex(wave, targetLocation);

		for(int i = 0; i < Helpers.BINS; i++)
		{
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			surfStats[i] += 1.0 / (Math.pow(index - i, 2) + 1);
		}
	}

	// Where will we be when the given wave hits us?
	private Point2D.Double predictPosition(EnemyWave surfWave, int direction)
	{
		Point2D.Double predictedPosition = (Point2D.Double) position.clone();
		double predictedVelocity = this.getVelocity();
		double predictedHeading = this.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do
		{
			moveAngle = wallSmoothing(predictedPosition,
									  Helpers.absoluteBearing(surfWave.getFireLocation(),
									  predictedPosition) + (direction * (Math.PI / 2)), direction) - predictedHeading;
			moveDir = 1;

			if(Math.cos(moveAngle) < 0)
			{
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Math.PI / 720.0 * (40.0 - 3.0 * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading + Helpers.limit(-maxTurning, moveAngle, maxTurning));

			// If predictedVelocity and moveDir have different signs slow down, otherwise accelerate
			predictedVelocity += predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir;
			predictedVelocity = Helpers.limit(-8, predictedVelocity, 8);

			// Calculate the new predicted position
			predictedPosition = Helpers.project(predictedPosition, predictedHeading, predictedVelocity);

			counter++;

			if(predictedPosition.distance(surfWave.getFireLocation()) < surfWave.getDistanceTraveled() + (counter * surfWave.getBulletVelocity()) + surfWave.getBulletVelocity())
			{
				intercepted = true;
			}
		}
		while(!intercepted && counter < 500);

		return predictedPosition;
	}

	private void setBackAsFront(double goAngle)
	{
		// this.setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));
		double angle = Utils.normalRelativeAngle(goAngle - this.getHeadingRadians());
		if(Math.abs(angle) > (Math.PI / 2))
		{
			if(angle < 0)
			{
				this.setTurnRightRadians(Math.PI + angle);
			}
			else
			{
				this.setTurnLeftRadians(Math.PI - angle);
			}
			moveDirection = -1;
		}
		else
		{
			if(angle < 0)
			{
				this.setTurnLeftRadians(-1 * angle);
			}
			else
			{
				this.setTurnRightRadians(angle);
			}
			moveDirection = 1;
		}
		setAhead(100 * moveDirection);
	}

	// Returns how much we should turn in order to avoid hitting a wall
	private double wallSmoothing(Point2D.Double position, double angle, int orientation)
	{
		while(!safetyRectangle.contains(Helpers.project(position, angle, WALL_MARGIN)))
		{
			angle += orientation * 0.05;
		}
		return angle;
	}

	// TODO: Remove in favor of Helpers.absoluteBearing
	private double absoluteBearing(double x1, double y1, double x2, double y2)
	{
		double distanceX = x2 - x1;
		double distanceY = y2 - y1;
		double hypotenuse = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
		double arcSin = Math.toDegrees(Math.asin(distanceX / hypotenuse)); // Yes, we actually do want sin here
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

	// TODO: Remove this in favor of Utils.NormalRelativeAngle, and if we can't do that, convert it to radians instead
	private double normalizeBearing(double angle)
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
