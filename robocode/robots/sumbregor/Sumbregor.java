package sumbregor;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.Point2D;

/*
 * Filho Panzer VI, temido por todos os tanques, forjado na zona franca de Manaus. 
 * Sumbregor representa o último de sua espécie, preso num loop interminável de dor e sofrimento.
 * Ele luta para conseguir o desejo de Sheilong e recuperar seu planeta, que foi explodido pelos temíveis e horrorosos pavimentadores galáticos.
 */

public class Sumbregor extends AdvancedRobot {

    public void run() {
        configurarCores();

        // Desacopla as partes do robô para movimento fluido
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            // Escaneamento contínuo assíncrono
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY); 
            
            // OBRIGATÓRIO em AdvancedRobots para executar os comandos "set"
            execute(); 
        }
    }

    private void configurarCores() {
        setBodyColor(Color.black);
        setGunColor(Color.black);
        setRadarColor(Color.yellow);
        setScanColor(Color.yellow);
        setBulletColor(Color.yellow);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // 1. Calcular a posição absoluta do inimigo na arena
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
        
        // 2. Dados de movimentação do inimigo
        double enemyHeading = e.getHeadingRadians();
        double enemyVelocity = e.getVelocity();
        
        // 3. Definir poder e velocidade do nosso projétil
        double bulletPower = Math.min(3.0, getEnergy()); // Atira forte, mas respeita a energia interna
        double bulletVelocity = 20 - 3 * bulletPower;
        
        // 4. Previsão iterativa do tempo e posição de impacto (Mira Preditiva)
        double predictedX = enemyX;
        double predictedY = enemyY;
        double deltaTime = 0;
        
        for (int i = 0; i < 5; i++) {
            double distance = Point2D.distance(getX(), getY(), predictedX, predictedY);
            deltaTime = distance / bulletVelocity;
            
            predictedX = enemyX + Math.sin(enemyHeading) * enemyVelocity * deltaTime;
            predictedY = enemyY + Math.cos(enemyHeading) * enemyVelocity * deltaTime;
        }
        
        // 5. Ajustar coordenadas para não prever tiros fora das paredes do campo
        predictedX = Math.max(18.0, Math.min(getBattleFieldWidth() - 18.0, predictedX));
        predictedY = Math.max(18.0, Math.min(getBattleFieldHeight() - 18.0, predictedY));
        
        // 6. Calcular o ângulo do canhão em direção ao ponto futuro projetado
        double aimAngle = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
        
        // 7. Rotacionar e mete bala
        setTurnGunRightRadians(Utils.normalRelativeAngle(aimAngle - getGunHeadingRadians()));
        
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 1) {
            setFire(bulletPower);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // Fica de lado para o tiro (90 graus)
        setTurnRight(e.getBearing() + 90); 
        
        // Randomiza uma distância de 50 a 200 para confundir o inimigo
        double distanciaAleatoria = 50 + (Math.random() * 150);
        
        // Foge para trás 
        setBack(distanciaAleatoria); 
    }

    public void onHitWall(HitWallEvent e) {
        setBack(20);
    }
}