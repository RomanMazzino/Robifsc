package sumbregor;
import robocode.*;
import java.awt.Color;

/*
 * Filho Panzer VI, temido por todos os tanques, forjado na zona franca de Manus. 
 * Sumbregor representa o último de sua espécie, preso num loop interminável de dor e sofrimento.
 * Ele luta para conseguir o desejo de Sheilong e recuperar seu planeta, que foi explodido pelos temíveis
 * e horrosos pavimentadores galáticos.
 */

public class Sumbregor extends Robot
{
	public void run() {
		setBodyColor(Color.black)
		setGunColor(Color.black)
		setRadarColor(Color.yellow)
		setScanColor(Color.yellow)
		setBulletColor(Color.yellow)

		// Robot main loop
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			ahead(100);
			turnGunRight(360);
			back(100);
			turnGunRight(360);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// Replace the next line with any behavior you would like
		fire(1);
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		back(10);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// Replace the next line with any behavior you would like
		back(20);
	}	
}
