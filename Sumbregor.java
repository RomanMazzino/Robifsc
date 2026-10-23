package sumbregor;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.Point2D;

// Sumbregor Mendonça Román de Meneses, temido por todos os tanques,
// forjado no calor e nas linhas de montagem da Zona Franca de Manaus.
// 
// Ele é o último de sua linhagem, condenado a reinar na arena em um ciclo
// sem fim de batalhas. Cada disparo é um passo em direção ao desejo de
// Shenlong para reconstruir seu planeta natal, devastado pelos tirânicos
// Pavimentadores Galáticos.

public class Sumbregor extends AdvancedRobot {

    // Distância de segurança das paredes ao se posicionar nos cantos
    private static final double DISTANCIA_CANTO = 60.0;
    private Point2D.Double cantoAtual;
    
    // Marcadores de tempo para gerenciar rotações e evitar que o robô fique estagnado
    private long ultimoAcertoTick = 0;
    private long tempoEntrouNoCanto = 0;
    private long esquivandoAteTick = 0; // Prioriza o impulso de esquiva sobre a rotina de canto
    
    // Limites de tolerância (em ticks de jogo) antes de trocar de estratégia
    private static final long MAX_TEMPO_SEM_ACERTO = 100;     // Ticks sem acertar nada
    private static final long MAX_TEMPO_NO_MESMO_CANTO = 150; // Ticks parado na mesma região
    
    private int direcaoMovimento = 1; // Alterna entre frente (1) e ré (-1) ao oscilar

    public void run() {
        configurarEstetica();

        // Independência total: corpo, canhão e radar giram livremente sem travar uns aos outros
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        mudarParaProximoCanto();

        while (true) {
            long tempoAtual = getTime();

            // Só navega para o canto se não estiver no meio de uma manobra evasiva urgente
            if (tempoAtual > esquivandoAteTick) {
                irParaCantoEOscilar(cantoAtual);
            }

            // Se estiver sem acertar tiros ou há tempo demais na mesma posição, busca outro canto
            if ((tempoAtual - ultimoAcertoTick > MAX_TEMPO_SEM_ACERTO) || 
                (tempoAtual - tempoEntrouNoCanto > MAX_TEMPO_NO_MESMO_CANTO)) {
                
                mudarParaProximoCanto();
                ultimoAcertoTick = tempoAtual;
            }

            // Se por algum motivo o radar travar ou perder o alvo, faz um giro completo para buscar alvos
            if (getRadarTurnRemaining() == 0.0) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

            execute();
        }
    }

    // Aplica a identidade visual sombria do Sumbregor: blindagem preta com feixes amarelos.
    private void configurarEstetica() {
        setBodyColor(Color.black);
        setGunColor(Color.black);
        setRadarColor(Color.yellow);
        setScanColor(Color.yellow);
        setBulletColor(Color.yellow);
    }

    // Seleciona aleatoriamente um dos quatro cantos da arena, garantindo que
    // o robô nunca repita a posição onde já está.
    private void mudarParaProximoCanto() {
        double largura = getBattleFieldWidth();
        double altura = getBattleFieldHeight();
        double d = DISTANCIA_CANTO;

        Point2D.Double[] cantos = new Point2D.Double[] {
            new Point2D.Double(d, d),                     // Cantinho inferior esquerdo
            new Point2D.Double(largura - d, d),           // Cantinho inferior direito
            new Point2D.Double(largura - d, altura - d),  // Cantinho superior direito
            new Point2D.Double(d, altura - d)             // Cantinho superior esquerdo
        };

        int novoIndice;
        do {
            novoIndice = (int) (Math.random() * 4);
        } while (cantoAtual != null && cantos[novoIndice].equals(cantoAtual));

        cantoAtual = cantos[novoIndice];
        tempoEntrouNoCanto = getTime();
    }

    // Conduz o robô até o canto escolhido pela rota mais curta (frente ou ré).
    // Quando atinge o destino, inicia uma oscilação curta para não virar um alvo fácil.
    private void irParaCantoEOscilar(Point2D.Double destino) {
        double distanciaAteDestino = Point2D.distance(getX(), getY(), destino.x, destino.y);
        
        if (distanciaAteDestino > 30) { 
            // Calcula o ângulo em direção ao ponto de destino
            double anguloAbsoluto = Math.atan2(destino.x - getX(), destino.y - getY());
            double anguloRelativo = Utils.normalRelativeAngle(anguloAbsoluto - getHeadingRadians());
            
            // Avalia se é mais rápido ir de ré do que girar o tanque inteiro
            if (Math.abs(anguloRelativo) > Math.PI / 2) {
                anguloRelativo = Utils.normalRelativeAngle(anguloRelativo + Math.PI);
                setTurnRightRadians(anguloRelativo);
                setBack(distanciaAteDestino);
            } else {
                setTurnRightRadians(anguloRelativo);
                setAhead(distanciaAteDestino);
            }
        } else {
            // Chegou ao canto: faz pequenos passos vaivém para dificultar a mira inimiga
            if (getDistanceRemaining() == 0) {
                direcaoMovimento = direcaoMovimento * -1;
                setAhead((30 + Math.random() * 40) * direcaoMovimento);
            }
        }
    }

    // Chamado sempre que o radar detecta um robô inimigo.
    // Cuida da trava de radar, cálculo de trajetória do tiro e tomada de decisão de disparo.
    public void onScannedRobot(ScannedRobotEvent e) {
        // --- 1. MANTÉM O RADAR CRAVADO NO INIMIGO ---
        double anguloAbsolutoInimigo = getHeadingRadians() + e.getBearingRadians();
        double giroRadar = Utils.normalRelativeAngle(anguloAbsolutoInimigo - getRadarHeadingRadians());
        
        // Pequeno ajuste extra de 3 graus para garantir que o feixe de scan continue cobrindo o alvo
        double margemExtra = Math.toRadians(3.0) * Math.signum(giroRadar == 0 ? 1 : giroRadar);
        setTurnRadarRightRadians(giroRadar + margemExtra);

        // --- 2. LOCALIZAÇÃO E TELEMETRIA DO ALVO ---
        double inimigoX = getX() + e.getDistance() * Math.sin(anguloAbsolutoInimigo);
        double inimigoY = getY() + e.getDistance() * Math.cos(anguloAbsolutoInimigo);
        double direcaoInimigo = e.getHeadingRadians();
        double velocidadeInimigo = e.getVelocity();

        // --- 3. GERENCIAMENTO INTELIGENTE DE ENERGIA ---
        // Quanto mais perto o inimigo, mais forte o tiro. Alvos distantes recebem tiros leves para economizar energia.
        double distancia = e.getDistance();
        double potenciaTiro;
        
        if (distancia < 150) {
            potenciaTiro = 3.0;
        } else if (distancia < 350) {
            potenciaTiro = 2.0;
        } else {
            potenciaTiro = 1.0;
        }
        
        // Proteção contra suicídio: nunca gasta mais energia do que possui
        potenciaTiro = Math.min(potenciaTiro, getEnergy() - 0.1);
        if (potenciaTiro < 0.1) potenciaTiro = 0.1;

        // Fórmula oficial do Robocode: velocidade do projetil depende da sua potência
        double velocidadeTiro = 20 - 3 * potenciaTiro;

        // --- 4. MIRA PREDITIVA LINEAR ---
        // Calcula onde o inimigo estará no momento em que a bala chegar até ele
        double XPrevisto = inimigoX;
        double YPrevisto = inimigoY;
        
        for (int i = 0; i < 7; i++) {
            double distanciaAtePontoPrevisto = Point2D.distance(getX(), getY(), XPrevisto, YPrevisto);
            double tempoAteImpacto = distanciaAtePontoPrevisto / velocidadeTiro;
            
            XPrevisto = inimigoX + Math.sin(direcaoInimigo) * velocidadeInimigo * tempoAteImpacto;
            YPrevisto = inimigoY + Math.cos(direcaoInimigo) * velocidadeInimigo * tempoAteImpacto;
        }

        // Impede de mirar fora da arena (considera a largura de 18px do robô inimigo)
        XPrevisto = Math.max(18.0, Math.min(getBattleFieldWidth() - 18.0, XPrevisto));
        YPrevisto = Math.max(18.0, Math.min(getBattleFieldHeight() - 18.0, YPrevisto));

        // --- 5. ALINHAMENTO DO CANHÃO E DISPARO ---
        double anguloParaAtirar = Utils.normalAbsoluteAngle(Math.atan2(XPrevisto - getX(), YPrevisto - getY()));
        double giroCanhao = Utils.normalRelativeAngle(anguloParaAtirar - getGunHeadingRadians());
        
        setTurnGunRightRadians(giroCanhao);

        // Só dispara se o canhão estiver frio e alinhado (tolerância de no máximo 1.5 graus de erro)
        boolean canhaoFrio = getGunHeat() == 0;
        boolean canhaoAlinhado = Math.abs(getGunTurnRemainingRadians()) < Math.toRadians(1.5);
        boolean energiaSuficiente = getEnergy() > 0.2;

        if (canhaoFrio && canhaoAlinhado && energiaSuficiente) {
            setFire(potenciaTiro);
        }
    }

    // Registra o momento em que um projetil atinge o inimigo para resetar o temporizador de inatividade.
    public void onBulletHit(BulletHitEvent e) {
        ultimoAcertoTick = getTime();
    }

    // Resposta tática ao levar um tiro: cancela temporariamente o movimento em direção ao canto
    // e executa uma manobra evasiva perpendicular ao ângulo do projetil.
    public void onHitByBullet(HitByBulletEvent e) {
        double anguloEsquiva = Utils.normalRelativeAngle(e.getBearingRadians() + (Math.PI / 2));
        setTurnRightRadians(anguloEsquiva);
        
        double arrancadaEvasiva = 80 + (Math.random() * 80);
        setBack(arrancadaEvasiva);

        // Garante 12 ticks de imunidade contra a rotina do canto para completar o impulso de esquiva
        esquivandoAteTick = getTime() + 12; 
    }

    // Se colidir com a parede (por exemplo, ao desviar de um tiro), força a troca imediata de canto.
    public void onHitWall(HitWallEvent e) {
        mudarParaProximoCanto();
    }
}