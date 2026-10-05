package sumbregor;
import robocode.*;
import java.awt.Color;

/*
 * Filho Panzer VI, temido por todos os tanques, forjado na zona franca de Manaus. 
 * Sumbregor representa o último de sua espécie, preso num loop interminável de dor e sofrimento.
 * Ele luta para conseguir o desejo de Sheilong e recuperar seu planeta, que foi explodido pelos temíveis e horrosos pavimentadores galáticos.
 */

public class Sumbregor extends Robot
{
	public void run() {
        configurarCores();

		// Robot main loop
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			ahead(100);
			turnGunRight(360);
			back(100);
			turnGunRight(360);
		}
	}

    private void configurarCores() {
		setBodyColor(Color.black) // Base
		setGunColor(Color.black) // Arma
		setRadarColor(Color.yellow) // Radar
		setScanColor(Color.yellow) // Scan
		setBulletColor(Color.yellow) // Bala
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
