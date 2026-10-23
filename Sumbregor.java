package sumbregor;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.Point2D;

/*
 * Sumbregor Mendonça Román de Meneses, temido por todos os tanques, forjado na zona franca de Manaus. 
 * Sumbregor representa o último de sua espécie, preso num loop interminável de dor e sofrimento.
 * Ele luta para conseguir o desejo de Sheilong e recuperar seu planeta, que foi explodido pelos temíveis e horrorosos pavimentadores galáticos.
 */

public class Sumbregor extends AdvancedRobot {

    private static final double DISTANCIA_CANTO = 60.0;
    private Point2D.Double cantoAtual;
    
    private long ultimoAcertoTick = 0;
    private long tempoEntrouNoCanto = 0;
    
    private static final long MAX_TEMPO_SEM_ACERTO = 100; // Ticks sem acertar até mudar
    private static final long MAX_TEMPO_NO_MESMO_CANTO = 150; // Ticks máximos no mesmo canto em combate
    private int direcaoMovimento = 1; // Alterna entre frente e trás para não ficar parado

    public void run() {
        configurarCores();

        // Desacopla as partes do robô para movimento fluido
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        mudarParaProximoCanto();

        while (true) {
            // 1. Movimentação até o canto ou oscilação defensiva se já chegou lá
            irParaCantoEOscilar(cantoAtual);

            long tempoAtual = getTime();

            // 2. Mudar de canto por Inatividade (se não acertar nada) OU por tempo excessivo estático no mesmo canto
            if ((tempoAtual - ultimoAcertoTick > MAX_TEMPO_SEM_ACERTO) || 
                (tempoAtual - tempoEntrouNoCanto > MAX_TEMPO_NO_MESMO_CANTO)) {
                
                mudarParaProximoCanto();
                ultimoAcertoTick = tempoAtual;
            }

            // 3. Caso perca o radar por algum motivo, faz busca ampla 360
            if (getRadarTurnRemaining() == 0.0) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

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

    // --- LÓGICA DE MOVIMENTAÇÃO DE CANTOS E OSCLILAÇÃO ---

    private void mudarParaProximoCanto() {
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        double d = DISTANCIA_CANTO;

        Point2D.Double[] cantos = new Point2D.Double[] {
            new Point2D.Double(d, d),         // Inferior Esquerdo
            new Point2D.Double(w - d, d),     // Inferior Direito
            new Point2D.Double(w - d, h - d), // Superior Direito
            new Point2D.Double(d, h - d)      // Superior Esquerdo
        };

        int novoIndice;
        do {
            novoIndice = (int) (Math.random() * 4);
        } while (cantoAtual != null && cantos[novoIndice].equals(cantoAtual));

        cantoAtual = cantos[novoIndice];
        tempoEntrouNoCanto = getTime();
    }

    private void irParaCantoEOscilar(Point2D.Double destino) {
        double distance = Point2D.distance(getX(), getY(), destino.x, destino.y);
        
        if (distance > 30) { 
            // Ainda está a caminho do canto
            double angleToTarget = Math.atan2(destino.x - getX(), destino.y - getY());
            double targetHeading = Utils.normalRelativeAngle(angleToTarget - getHeadingRadians());
            
            if (Math.abs(targetHeading) > Math.PI / 2) {
                targetHeading = Utils.normalRelativeAngle(targetHeading + Math.PI);
                setTurnRightRadians(targetHeading);
                setBack(distance);
            } else {
                setTurnRightRadians(targetHeading);
                setAhead(distance);
            }
        } else {
            // Já chegou no canto: faz uma pequena oscilação/movimentação local para não ser alvo fácil
            if (getDistanceRemaining() == 0) {
                direcaoMovimento *= -1; // Inverte o sentido
                setAhead((30 + Math.random() * 40) * direcaoMovimento);
            }
        }
    }

    // --- EVENTOS DE MIRA, ESCANEMENTO E ESQUIVA ---

    public void onScannedRobot(ScannedRobotEvent e) {
        // 1. Radar Otimizado (Trava no inimigo sem escaneamento inútil de parede)
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
        double extraSweep = Math.toRadians(22) * Math.signum(radarTurn == 0 ? 1 : radarTurn);
        setTurnRadarRightRadians(radarTurn + extraSweep);

        // 2. Coordenadas do Inimigo
        double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
        double enemyHeading = e.getHeadingRadians();
        double enemyVelocity = e.getVelocity();

        // 3. Poder e Velocidade da Bala
        double bulletPower = Math.min(3.0, getEnergy());
        double bulletVelocity = 20 - 3 * bulletPower;

        // 4. Mira Preditiva
        double predictedX = enemyX;
        double predictedY = enemyY;
        
        for (int i = 0; i < 5; i++) {
            double distance = Point2D.distance(getX(), getY(), predictedX, predictedY);
            double deltaTime = distance / bulletVelocity;
            
            predictedX = enemyX + Math.sin(enemyHeading) * enemyVelocity * deltaTime;
            predictedY = enemyY + Math.cos(enemyHeading) * enemyVelocity * deltaTime;
        }

        // Limites da arena
        predictedX = Math.max(18.0, Math.min(getBattleFieldWidth() - 18.0, predictedX));
        predictedY = Math.max(18.0, Math.min(getBattleFieldHeight() - 18.0, predictedY));

        // 5. Ajuste do Canhão e Disparo
        double aimAngle = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
        setTurnGunRightRadians(Utils.normalRelativeAngle(aimAngle - getGunHeadingRadians()));

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 1) {
            setFire(bulletPower);
        }
    }

    public void onBulletHit(BulletHitEvent e) {
        // Atualiza a marca temporal de acerto
        ultimoAcertoTick = getTime();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // Restaurada a esquiva original com variação aleatória de recuo
        setTurnRight(e.getBearing() + 90);
        double distanciaAleatoria = 50 + (Math.random() * 150);
        setBack(distanciaAleatoria);
    }

    public void onHitWall(HitWallEvent e) {
        // Se bater na parede ao esquivar, força a ida para outro canto imediatamente
        mudarParaProximoCanto();
    }
}