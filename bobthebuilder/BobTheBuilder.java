/*
Created by Winston Liu and Eric Roberts for AP Computer Science

Credits:
PEZ - GuessFactor, BulletWave
Voidious - WaveSurfing, EnemyWave
Mark Whitley - AdvancedEnemyBot, EnemyBot
Voidious (and probably others) - Helpers
*/

package bobthebuilder;

import robocode.*;
import robocode.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.awt.Color;
import java.awt.geom.*;

public class BobTheBuilder extends AdvancedRobot
{
	private static final int WALL_MARGIN = 150;
	private static final String VERSION = "0.2.3";

	private HashMap<String, AdvancedEnemyBot> enemies;
	private AdvancedEnemyBot enemy;
	private Rectangle2D.Double safetyRectangle;
	private Point2D.Double position;
	private Point2D.Double enemyPosition; // TODO: Move this into AdvancedEnemyBot
	private ArrayList<EnemyWave> surfWaves;
	private ArrayList<Integer> surfDirections;
	private ArrayList<Double> surfAbsoluteBearings;
	private static double surfStats[] = new double[Helpers.BINS];
	private int moveDirection = 1;
	private int enemyDirection = 1;
	private int wallMargin = 50;
	private boolean tooCloseToWall = false;
	private boolean hitRobot = false;
	private static Random rand = new Random();

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
			if(this.getTime() - enemy.getLastUpdateTime() > 5)
			{
				// We have outdated data that really isn't going to help us
				enemy.reset();
			}

			if(enemy.none() || getOthers() != 1)
			{
				this.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}

			position = new Point2D.Double(this.getX(), this.getY());

			updateSurfWaves();
			move();
			fire();
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		if(!enemies.containsKey(e.getName()))
		{
			enemies.put(e.getName(), new AdvancedEnemyBot(e, this));
		}

		enemies.get(e.getName()).update(e, this);

		double lateralVelocity = this.getVelocity() * Math.sin(e.getBearingRadians());
		double absoluteBearing = e.getBearingRadians() + this.getHeadingRadians();

		surfDirections.add(0, new Integer(((lateralVelocity >= 0) ? 1 : -1)));
		surfAbsoluteBearings.add(0, new Double(absoluteBearing + Math.PI));

		double power = enemies.get(e.getName()).getCachedEnergy() - e.getEnergy();
		if(power > Rules.MIN_BULLET_POWER - 0.01 && power < Rules.MAX_BULLET_POWER + 0.01 && surfDirections.size() > 2) // Doubles are imprecise so don't compare equal to
		{
			EnemyWave wave = new EnemyWave();
			wave.setFireTime(this.getTime() - 1);
			wave.setBulletVelocity(Rules.getBulletSpeed(power));
			wave.setDistanceTraveled(Rules.getBulletSpeed(power));
			wave.setDirection(surfDirections.get(2).intValue());
			wave.setDirectAngle(surfAbsoluteBearings.get(2).doubleValue());
			wave.setFireLocation((Point2D.Double) enemyPosition.clone());
			surfWaves.add(wave);
		}

		enemyPosition = Helpers.project(position, absoluteBearing, e.getDistance());

		enemies.get(e.getName()).setCachedEnergy(e.getEnergy());

		if(enemy.none() // No enemy
		|| e.getEnergy() <= 0 // Enemy is disabled
		|| (e.getEnergy() < enemy.getEnergy() && e.getDistance() < enemy.getDistance()) // New robot has less life than the current enemy and is closer
		|| e.getDistance() < enemy.getDistance() - 70 // New robot is a lot closer than current enemy
		|| e.getName().equals(enemy.getName())) // New robot is the current enemy
		{
			enemy = enemies.get(e.getName());

			enemy.setCachedVelocity(enemy.getVelocity());

			this.setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - this.getRadarHeadingRadians()) * 2);
		}
	}

	public void onBulletHit(BulletHitEvent e)
	{
		if(enemies.containsKey(e.getName()))
		{
			double cachedEnergy = enemies.get(e.getName()).getCachedEnergy();
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy - Rules.getBulletDamage(e.getBullet().getPower()));
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
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy + Rules.getBulletHitBonus(e.getBullet().getPower()));
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
		System.out.println("Wall hit at (" + getX() + ", " + getY() + "); bearing was " + e.getBearingRadians() + " rads");
		// Move immediately so that we don't generate more HitWallEvents while turning
		if(Math.abs(e.getBearingRadians()) <= Math.PI / 2)
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
		// Move "backwards" a bit so that we don't get stuck
		// TODO: Calculate the bearing to the wall even if we haven't hit a wall
		if(tooCloseToWall && !this.getHitWallEvents().isEmpty())
		{
			this.turnRightRadians((this.getHitWallEvents().lastElement().getBearingRadians() + e.getBearingRadians()) / 2);
		}
		this.setAhead(e.getBearingRadians() > -Math.PI / 2 && e.getBearingRadians() <= Math.PI / 2 ? -100 : 100);

		if(enemies.containsKey(e.getName()))
		{
			double cachedEnergy = enemies.get(e.getName()).getCachedEnergy();
			enemies.get(e.getName()).setCachedEnergy(cachedEnergy - Rules.ROBOT_HIT_DAMAGE);
			// TODO: Robots also gain Rules.ROBOT_HIT_BONUS on ramming?
		}
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		if(e.getName().equals(enemy.getName()))
		{
			enemies.remove(enemy.getName());
			enemy.reset();
		}

		if(this.getOthers() == 0) // Victory!
		{
			this.setMaxVelocity(0);
			for(int i = 0; i < 10; i++)
			{
				this.setTurnLeftRadians(2 * Math.PI * 5);
				this.setTurnGunRightRadians(2 * Math.PI * 5);
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
		if(surfWave != null && this.getOthers() == 1) // Wave surfing
		{
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
		else // Random movement
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

			// Strafe rather randomly
			if(rand.nextInt(21) == 0)
			{
				moveDirection *= -1;
			}

			double heading = this.getHeadingRadians() + (moveDirection == 1 ? 0 : Math.PI);
			double goAngle = enemy.getBearingRadians() + Math.PI / 2 - (Math.PI / 12 * moveDirection);
			goAngle = Helpers.limit(-Rules.getTurnRateRadians(this.getVelocity()), goAngle, Rules.getTurnRateRadians(this.getVelocity()));
			double wallSmoothingAngle = wallSmoothing(position, heading + goAngle, moveDirection);
			this.setTurnRightRadians(Utils.normalRelativeAngle(wallSmoothingAngle - heading));

			this.setAhead(100 * moveDirection);
		}
	}

	private void fire()
	{
		if(enemy.none())
		{
			return;
		}

		if(this.getOthers() > 1) // Normal predictive firing
		{
			double firePower = Math.min(500 / enemy.getDistance(), 3);
			double bulletSpeed = 20 - firePower * 3;
			int time = (int) Math.ceil((enemy.getDistance() / bulletSpeed));

			double absoluteDegree = Helpers.absoluteBearing(position, Helpers.project(position, enemy.getBearingRadians() + this.getHeadingRadians(), enemy.getDistance()));

			this.setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteDegree - this.getGunHeadingRadians()));

			if(this.getGunHeat() == 0 && Math.abs(this.getGunTurnRemaining()) < 10)
			{
				this.setFire(firePower);
			}
		}
		else // Guess factor targeting
		{
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

			if(enemy.getEnergy() <= 0) // Enemy is disabled, just shoot at their current location
			{
				this.setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - this.getGunHeadingRadians()));
				this.setFire(0.1);
				return;
			}

			double power = Math.min(500 / enemy.getDistance(), Rules.MAX_BULLET_POWER);
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
		double factor = Utils.normalRelativeAngle(offsetAngle) / Math.asin(Rules.MAX_VELOCITY / wave.getBulletVelocity()) * wave.getDirection();

		return (int) Helpers.limit(0, (factor * ((Helpers.BINS - 1) / 2)) + ((Helpers.BINS - 1) / 2), Helpers.BINS - 1);
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
			predictedHeading = Utils.normalRelativeAngle(predictedHeading + Helpers.limit(-Rules.getTurnRateRadians(this.getVelocity()), moveAngle, Rules.getTurnRateRadians(this.getVelocity())));

			// If predictedVelocity and moveDir have different signs slow down, otherwise accelerate
			predictedVelocity += (predictedVelocity * moveDir < 0) ? Rules.DECELERATION * moveDir : Rules.ACCELERATION * moveDir;
			predictedVelocity = Helpers.limit(-Rules.MAX_VELOCITY, predictedVelocity, Rules.MAX_VELOCITY);

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
		if(Math.abs(angle) > Math.PI / 2)
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

	/**
	 * Returns the angle we should travel at in order to avoid hitting a wall
	 *
	 * @param position The robot's position
	 * @param angle The angle that the robot would have traveled in without any wall smoothing
	 * @param direction 1 for clockwise rotation, -1 for counterclockwise rotation
	 *
	 * @return The new angle based on how much wall smoothing is needed
	 *
	 * @author Voidious
	 */
	private double wallSmoothing(Point2D.Double position, double angle, int direction)
	{
		double x = position.getX();
		double y = position.getY();

		angle += 4 * Math.PI; // Make sure our angle is positive

		double testX = x + (Math.sin(angle) * WALL_MARGIN);
		double testY = y + (Math.cos(angle) * WALL_MARGIN);
		double wallDistanceX = Math.min(x - Helpers.ROBOT_SIZE, this.getBattleFieldWidth() - x - Helpers.ROBOT_SIZE);
		double wallDistanceY = Math.min(y - Helpers.ROBOT_SIZE, this.getBattleFieldHeight() - y - Helpers.ROBOT_SIZE);
		double testDistanceX = Math.min(testX - Helpers.ROBOT_SIZE, this.getBattleFieldWidth() - testX - Helpers.ROBOT_SIZE);
		double testDistanceY = Math.min(testY - Helpers.ROBOT_SIZE, this.getBattleFieldHeight() - testY - Helpers.ROBOT_SIZE);

		double distance = 0;
		double iterations = 0; // To prevent an infinite loop...just in case

		while(!safetyRectangle.contains(testX, testY) && iterations < 25)
		{
			if(testDistanceY < 0 && testDistanceY < testDistanceX) // We're too close to the north or south wall
			{
				angle = ((int) ((angle + Math.PI / 2) / Math.PI)) * Math.PI;
				distance = Math.abs(wallDistanceY);
			}
			else if(testDistanceX < 0 && testDistanceX <= testDistanceY) // Too close to the east/west wall
			{
				angle = (((int) (angle / Math.PI)) * Math.PI) + Math.PI / 2;
				distance = Math.abs(wallDistanceX);
			}

			angle += direction * (Math.abs(Math.acos(distance / WALL_MARGIN)) + 0.005); // 0.005 determines how close to the wall we get

			testX = x + (Math.sin(angle) * WALL_MARGIN);
			testY = y + (Math.cos(angle) * WALL_MARGIN);
			testDistanceX = Math.min(testX - Helpers.ROBOT_SIZE, this.getBattleFieldWidth() - testX - Helpers.ROBOT_SIZE);
			testDistanceY = Math.min(testY - Helpers.ROBOT_SIZE, this.getBattleFieldHeight() - testY - Helpers.ROBOT_SIZE);

			iterations++;
		}
		return angle;
	}
}
