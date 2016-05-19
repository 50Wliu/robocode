package bobthebuilder;

import robocode.*;

public class BobTheBuilder extends AdvancedRobot
{

	private EnemyBot enemy = new EnemyBot();

	public void run()
	{
		setAdjustRadarForGunTurn(true);
		enemy.reset();
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
		if(enemy.none()
		|| e.getDistance() < enemy.getDistance() - 70
		|| e.getName().equals(enemy.getName()))
		{
			enemy.update(e);
			setTurnRight(e.getBearing());
		}
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		if(e.getName().equals(enemy.getName()))
		{
			enemy.reset();
		}
	}

	public void doScanner()
	{
		setTurnRadarRight(360);
	}

	public void doMovement()
	{
		// turning here causes a weird behavior, prolly because we're working
		// with outdated information
		//setTurnRight(enemy.getBearing());

		if(enemy.getDistance() > 200)
		{
			setAhead(enemy.getDistance() / 2);
		}

		if(enemy.getDistance() < 100)
		{
			setBack(enemy.getDistance());
		}
	}

	public void doGun()
	{
		if(enemy.none())
		{
			return;
		}

		double max = Math.max(getBattleFieldHeight(), getBattleFieldWidth());

		if(Math.abs(getTurnRemaining()) < 10)
		{
			if(enemy.getDistance() < max / 3)
			{
				// fire hard when close
				setFire(3);
			}
			else
			{
				// otherwise, just plink him
				setFire(1);
			}
		}
	}
}
