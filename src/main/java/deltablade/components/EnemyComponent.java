package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.WaveManager;

import java.util.Random;

public class EnemyComponent extends Component {

    public enum EnemyType {
        BASIC(1, 100, 55, 0.015),
        FAST(1, 150, 80, 0.02),
        TOUGH(3, 300, 45, 0.025),
        BOSS(10, 1000, 35, 0.04),
        UFO(5, 500, 110, 0.045),
        MONSTER(2, 160, 48, 0.028);

        public final int health;
        public final int scoreValue;
        public final double speed;
        public final double fireRate;

        EnemyType(int health, int scoreValue, double speed, double fireRate) {
            this.health = health;
            this.scoreValue = scoreValue;
            this.speed = speed;
            this.fireRate = fireRate;
        }
    }

    public enum State {
        ENTERING,
        FORMATION,
        DIVING,
        RETURNING,
        BOSS_HOVER,
        BOSS_DIVE,
        UFO_SWEEP
    }

    private EnemyType type;
    private int health;
    private State state = State.ENTERING;

    private double targetX;
    private double targetY;
    private WaveManager.EntryPath entryPath;
    private int squadId = -1;
    private boolean beadMarching;
    private double beadLaneX;

    private double entryProgress = 0;
    private double entrySpeed = 200;
    private double entryCurvePhase = 0;
    private int flightIndex = 0;

    private double baseY;
    private double hoverPhase;
    private double coverPeekWait;
    private double coverPeekT;
    private boolean coverPeeking;
    private int coverPeekDir = 1;
    private double coverPeekDist;
    private double coverPeekEase;

    private double diveSpeed = 150;
    private double diveTargetX;
    private double diveTime;
    private int diveSide = 1;
    private double pendingDive = -1;
    private int pendingSide = 0;
    private double heading;

    private double fireTimer = 0;
    private double formationHoldTime = 0;
    private double minFormationTime = 2.0;

    private boolean isBoss = false;
    private int bossNo = 1;
    private int maxHealth = 1;
    private double bossShotTimer = 1.5;
    private int bossPhase = 1;
    private double bossHelperTimer = 3.0;
    private double bossSpecialTimer = 2.5;
    private boolean bossRingNext = true;
    private double beamWarmup = 0;
    private double beamLeft = 0;
    private double beamTick = 0;
    private double bossSwayPhase = 0;
    private double bossDiveTimer = 0;
    private double bossReturnY = 0;
    private boolean bossDiving = false;
    private boolean isKamikaze = false;
    private boolean isBonusRunner = false;
    private boolean scooped = false;
    private boolean docked = false;
    private int dockSlot = -1;
    private double scoopTime = 0;
    private static final double SCOOP_DURATION = 0.85;
    private static final double DOCK_SCALE = 0.72;
    private boolean isUfo = false;
    private boolean isMonster = false;
    private boolean canSplit = false;
    private boolean hasSplit = false;
    private boolean diveOnAdd = false;
    private int spriteIndex = 0;
    private double ufoDir = 1;
    private int waveLevel = 1;

    private static final Random RANDOM = new Random();

    public EnemyComponent(EnemyType type, int level) {
        this.type = type;
        this.waveLevel = Math.max(1, level);
        this.health = type.health;
        this.entrySpeed = 60 + level * 5;
        this.hoverPhase = RANDOM.nextDouble() * Math.PI * 2;
        this.coverPeekWait = 0.7 + RANDOM.nextDouble() * 2.8;
        this.diveSpeed = 90 + level * 10;
        this.minFormationTime = 3.0 + RANDOM.nextDouble() * 2.0;

        if (type == EnemyType.BOSS) {
            this.isBoss = true;
            this.bossNo = deltablade.GameVars.bossNumber(level);
            this.health = deltablade.GameVars.BOSS_HP_BASE + deltablade.GameVars.BOSS_HP_PER_BOSS * bossNo;
            this.maxHealth = this.health;
        }
        if (type == EnemyType.UFO) {
            this.isUfo = true;
            this.ufoDir = RANDOM.nextBoolean() ? 1 : -1;
        }
        if (type == EnemyType.MONSTER) {
            this.isMonster = true;
            this.minFormationTime = 1.4 + RANDOM.nextDouble() * 1.2;
            this.diveSpeed *= 0.92;
        }
    }

    public void setSpriteIndex(int spriteIndex) {
        this.spriteIndex = spriteIndex;
    }

    public int getSpriteIndex() {
        return spriteIndex;
    }

    public void setCanSplit(boolean canSplit) {
        this.canSplit = canSplit;
    }

    public void beginDiveNow(double targetX) {
        this.canSplit = false;
        this.hasSplit = true;
        this.diveOnAdd = true;
        this.diveTargetX = targetX;
        this.state = State.DIVING;
    }

    public boolean isMonster() {
        return isMonster;
    }

    public void setKamikaze(boolean kamikaze) {
        this.isKamikaze = kamikaze;
        if (kamikaze) {
            this.diveSpeed *= 1.3;
        }
    }

    public void setBonusRunner(boolean bonusRunner) {
        this.isBonusRunner = bonusRunner;
        if (bonusRunner) {
            this.minFormationTime = 0.15;
        }
    }

    public boolean isBonusRunner() {
        return isBonusRunner;
    }

    public boolean isUfo() {
        return isUfo;
    }

    public boolean isBoss() {
        return isBoss;
    }

    /** Jefe alienígena de una oleada de monstruos: -1 = jefe normal; si no, índice de la casilla en la hoja alienz2. */
    private int alienBoss = -1;
    private double alienAttackTimer = 2.0;
    private double alienSpecialTimer = 5.0;
    private double alienHelperTimer = 4.0;

    public void setAlienBoss(int kind, int cycle) {
        this.alienBoss = kind;
        this.health = 40 + 20 * Math.max(1, cycle);
        this.maxHealth = this.health;
    }

    public boolean isAlienBoss() {
        return alienBoss >= 0;
    }

    public String bossLabel() {
        if (alienBoss < 0) {
            return deltablade.I18n.tr("boss.numbered", bossNo);
        }
        return switch (alienBoss) {
            case deltablade.SpriteSheets.ALIEN_CRAB -> deltablade.I18n.tr("boss.crab");
            case deltablade.SpriteSheets.ALIEN_BRAIN -> deltablade.I18n.tr("boss.brain");
            default -> deltablade.I18n.tr("boss.eye");
        };
    }

    /**
     * Ataques de los jefes alienígenas.
     * Cangrejo: abanicos de disparos lentos y anchos ("telaraña"). Cerebro: llama a monstruos pequeños. Ojo: disparos apuntados + rayo.
     * Por debajo de un cuarto de la vida, todos se vuelven más rápidos y añaden un anillo de balas.
     */
    private void updateAlienBossAttack(double tpf) {
        if (beamWarmup > 0 || beamLeft > 0) {
            updateBeam(tpf);
            return;
        }
        boolean angry = bossPhase == 3;
        var app = FXGL.<deltablade.DeltaBladeApp>getAppCast();
        alienAttackTimer -= tpf;
        alienSpecialTimer -= tpf;
        if (alienAttackTimer <= 0) {
            switch (alienBoss) {
                case deltablade.SpriteSheets.ALIEN_CRAB -> {
                    bossFan(7, 110, 130);
                    alienAttackTimer = angry ? 1.7 : 2.4;
                }
                case deltablade.SpriteSheets.ALIEN_BRAIN -> {
                    bossFan(3, 40, 170);
                    alienAttackTimer = angry ? 1.9 : 2.6;
                }
                default -> {
                    double cx = entity.getX() + entity.getWidth() / 2.0;
                    double by = entity.getY() + entity.getHeight() * 0.8;
                    for (int i = -1; i <= 1; i++) {
                        app.spawnAimedBossBullet(cx + i * 16, by, 210);
                    }
                    deltablade.SoundHelper.play("enemy_shot.wav");
                    alienAttackTimer = angry ? 1.3 : 1.8;
                }
            }
        }
        if (alienBoss == deltablade.SpriteSheets.ALIEN_BRAIN) {
            alienHelperTimer -= tpf;
            if (alienHelperTimer <= 0) {
                alienHelperTimer = angry ? 4.5 : 6.0;
                app.spawnAlienBossHelpers(entity, angry ? 3 : 2);
            }
        }
        if (alienSpecialTimer <= 0) {
            alienSpecialTimer = 5.0;
            if (alienBoss == deltablade.SpriteSheets.ALIEN_EYE) {
                beamWarmup = 1.4;
            } else if (angry) {
                bossRing(10, 150);
            }
        }
    }

    public void setEntrySpeed(double speed) {
        this.entrySpeed = speed;
    }

    public void setEntryData(double targetX, double targetY, WaveManager.EntryPath path, int squadId) {
        this.targetX = targetX;
        this.targetY = formationHoldY(targetY);
        this.entryPath = path;
        this.squadId = squadId;
        this.state = State.ENTERING;
        this.entryProgress = 0;
        this.beadMarching = path != null && path.beadString;
    }

    @Override
    public void onAdded() {
        entity.getTransformComponent().setRotationOrigin(
                new javafx.geometry.Point2D(entity.getWidth() / 2.0, entity.getHeight() / 2.0));
        if (diveOnAdd) {
            state = State.DIVING;
            baseY = entity.getY();
            return;
        }
        if (isUfo && entryPath == null) {
            state = State.UFO_SWEEP;
            baseY = entity.getY();
            return;
        }
        if (entryPath == null) {
            state = State.FORMATION;
            baseY = entity.getY();
            targetX = entity.getX();
            return;
        }
        if (beadMarching) {
            beadLaneX = entity.getX();
        }
    }

    private static final double MAX_TPF = 1.0 / 45.0;
    private int frameCount = 0;
    private static final int WARMUP_FRAMES = 3;

    @Override
    public void onUpdate(double tpf) {
        try {
            if (FXGL.<deltablade.DeltaBladeApp>getAppCast().isCombatStopped()) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }
        frameCount++;
        if (frameCount <= WARMUP_FRAMES) {
            tpf = Math.min(tpf, 0.008);
        } else {
            tpf = Math.min(tpf, MAX_TPF);
        }

        if (scooped) {
            updateScooped(tpf);
            return;
        }

        switch (state) {
            case ENTERING -> updateEntering(tpf);
            case FORMATION -> updateFormation(tpf);
            case DIVING -> updateDiving(tpf);
            case RETURNING -> updateReturning(tpf);
            case BOSS_HOVER -> updateBossHover(tpf);
            case BOSS_DIVE -> updateBossDive(tpf);
            case UFO_SWEEP -> updateUfoSweep(tpf);
        }

        if (isBoss) {
            if (state == State.BOSS_HOVER || state == State.BOSS_DIVE) {
                updateBossAttack(tpf);
            }
        } else if (!isBonusRunner && (state != State.ENTERING || canFireWhileClimbing())) {
            updateFiring(tpf);
        }
    }

    private void updateBeadMarch(double tpf) {
        boolean fromBelow = entryPath != null && entryPath.type == WaveManager.EntryPath.Type.FROM_BELOW;
        entity.setX(beadLaneX);
        double speed = entrySpeed * tpf;
        if (fromBelow) {
            entity.translateY(-speed);
            if (entity.getY() <= 78) {
                beadMarching = false;
            }
        } else {
            entity.translateY(speed);
            if (entity.getY() >= 78) {
                beadMarching = false;
            }
        }
    }

    private void updateEntering(double tpf) {
        if (beadMarching) {
            updateBeadMarch(tpf);
            return;
        }
        if (entryPath != null && entryPath.flight != null) {
            updateFlight(tpf);
            return;
        }

        entryProgress += tpf * entrySpeed / 300.0;
        entryCurvePhase += tpf * 4;

        double currentX = entity.getX();
        double currentY = entity.getY();

        double dx = targetX - currentX;
        double dy = targetY - currentY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 5 || (entryProgress >= 1.0 && dist < 30)) {
            entity.setPosition(targetX, targetY);
            settleAfterEntry();
            return;
        }
        moveEnteringStep(tpf, dx, dy, dist);
    }

    /** Paso 3: la nave recorre una trayectoria fija y después va a su sitio. */
    private void updateFlight(double tpf) {
        double speed = Math.min(300, 200 + waveLevel * 4);
        double step = speed * tpf;
        java.util.List<javafx.geometry.Point2D> pts = entryPath.flight;
        double cx = entity.getX() + entity.getWidth() / 2.0;
        double cy = entity.getY() + entity.getHeight() / 2.0;
        while (step > 0) {
            double tx;
            double ty;
            boolean last = flightIndex >= pts.size();
            if (last) {
                if (baseY == 0) {
                    baseY = targetY;
                }
                tx = slotX() + entity.getWidth() / 2.0;
                ty = slotY() + entity.getHeight() / 2.0;
            } else {
                tx = pts.get(flightIndex).getX();
                ty = pts.get(flightIndex).getY();
            }
            double dx = tx - cx;
            double dy = ty - cy;
            double d = Math.hypot(dx, dy);
            if (d <= step) {
                cx = tx;
                cy = ty;
                step -= d;
                if (last) {
                    entity.setPosition(cx - entity.getWidth() / 2.0, cy - entity.getHeight() / 2.0);
                    double sx = entity.getX();
                    settleAfterEntry();
                    entity.setX(sx);
                    return;
                }
                flightIndex++;
            } else {
                double vx = dx / d;
                double vy = dy / d;
                cx += vx * step;
                cy += vy * step;
                faceToward(vx * speed, vy * speed, tpf);
                step = 0;
            }
        }
        entity.setPosition(cx - entity.getWidth() / 2.0, cy - entity.getHeight() / 2.0);
    }

    private void settleAfterEntry() {
        {
            if (isBoss) {
                state = State.BOSS_HOVER;
                baseY = targetY;
                bossReturnY = targetY;
            } else if (isUfo) {
                state = State.UFO_SWEEP;
            } else if (isKamikaze) {
                startDive();
            } else if (isBonusRunner) {
                state = State.DIVING;
                diveTargetX = targetX;
            } else {
                state = State.FORMATION;
            }
            baseY = formationHoldY(targetY);
            formationHoldTime = 0;

            notifySettled();
        }
    }

    private void moveEnteringStep(double tpf, double dx, double dy, double dist) {
        double moveSpeed = entrySpeed * tpf;

        double curveOffset = 0;
        if (entryPath != null && !entryPath.beadString) {
            double curveFade = Math.max(0, 1 - entryProgress);
            curveOffset = switch (entryPath.type) {
                case FROM_LEFT_CURVE -> Math.sin(entryCurvePhase) * 30 * curveFade;
                case FROM_RIGHT_CURVE -> -Math.sin(entryCurvePhase) * 30 * curveFade;
                case FROM_TOP_SPLIT -> Math.sin(entryCurvePhase * 0.5) * 50 * curveFade;
                case FROM_SIDE_SWOOP -> Math.sin(entryCurvePhase * 1.5) * 40 * curveFade;
                case FROM_CENTER -> {
                    double side = targetX < FXGL.getAppWidth() / 2.0 ? -1 : 1;
                    yield side * Math.sin(entryCurvePhase * 0.85) * 90 * curveFade;
                }
                case FROM_BELOW -> Math.sin(entryCurvePhase * 0.65) * 36 * curveFade;
            };
        }

        double normalizedDx = dx / dist;
        double normalizedDy = dy / dist;

        double perpX = -normalizedDy;

        entity.translate(
            normalizedDx * moveSpeed + perpX * curveOffset * tpf * 3,
            normalizedDy * moveSpeed
        );
    }

    private void updateFormation(double tpf) {
        formationHoldTime += tpf;

        hoverPhase += tpf * 2;
        boolean cover = meteorCover();
        if (cover) {
            updateMeteorPeek(tpf);
        } else {
            baseY = Math.min(baseY, formationCeiling());
            // Toda la formación se balancea a la vez y "respira" (paso 2).
            double hoverY = Math.min(slotY() + Math.sin(hoverPhase) * 3, formationCeiling() + 6);
            double k = Math.min(1, tpf * 6);
            entity.setX(entity.getX() + (slotX() - entity.getX()) * k);
            entity.setY(entity.getY() + (hoverY - entity.getY()) * k);
            relaxRotation(tpf);
        }

        if (isBonusRunner && formationHoldTime > minFormationTime) {
            startDive();
            diveTargetX = entity.getX();
            return;
        }

        if (pendingDive >= 0) {
            pendingDive -= tpf;
            if (pendingDive < 0) {
                startDive();
                if (pendingSide != 0) {
                    diveSide = pendingSide;
                }
            }
            return;
        }

        // Las naves que vuelven a su sitio se lanzan en picado en grupo (lo controla WaveManager).
        if (formationHoldTime > minFormationTime && !meteorCover() && !canWrapDive()) {
            double diveChance = 0.001 * (1 + FXGL.geti("level") * 0.2);
            if (RANDOM.nextDouble() < diveChance) {
                startDive();
            }
        }
    }

    private void updateMeteorPeek(double tpf) {
        if (!coverPeeking) {
            coverPeekWait -= tpf;
            if (coverPeekWait <= 0) {
                coverPeeking = true;
                coverPeekT = 0;
                coverPeekDir = RANDOM.nextBoolean() ? 1 : -1;
                coverPeekDist = 22 + RANDOM.nextDouble() * 20;
            }
        } else {
            coverPeekT += tpf;
            if (coverPeekT >= 1.25) {
                coverPeeking = false;
                coverPeekWait = 1.4 + RANDOM.nextDouble() * 3.2;
            }
        }

        double ease = 0;
        if (coverPeeking) {
            if (coverPeekT < 0.32) {
                ease = smoothstep(coverPeekT / 0.32);
            } else if (coverPeekT < 0.78) {
                ease = 1;
            } else {
                ease = 1 - smoothstep((coverPeekT - 0.78) / 0.47);
            }
        }

        double minX = deltablade.GameVars.RAIL_WIDTH + 4;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH - 4;
        double peekX = targetX + coverPeekDir * coverPeekDist * ease;
        entity.setX(Math.max(minX, Math.min(maxX, peekX)));
        entity.setY(baseY + Math.sin(hoverPhase) * 4 + ease * 10);
        coverPeekEase = ease;
    }

    /** True mientras está escondida detrás de un meteorito y no asoma lo suficiente para recibir disparos. */
    public boolean isTuckedBehindMeteor() {
        if (!meteorCover() || isUfo || scooped) {
            return false;
        }
        return state == State.FORMATION && coverPeekEase < 0.42;
    }

    private static double smoothstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private void startDive() {
        state = State.DIVING;
        diveTime = 0;
        double cx = entity.getX() + entity.getWidth() / 2.0;
        diveSide = cx < FXGL.getAppWidth() / 2.0 ? -1 : 1;

        try {
            var player = FXGL.getGameWorld().getSingleton(deltablade.EntityType.PLAYER);
            diveTargetX = player.getX() + player.getWidth() / 2;
        } catch (Exception e) {
            diveTargetX = entity.getX();
        }
    }

    private void updateDiving(double tpf) {
        diveTime += tpf;
        double vx;
        double vy;
        if (canWrapDive() && diveTime < 0.6) {
            // Arco al estilo Galaga: primero un poco hacia arriba/afuera y luego hacia abajo.
            double t = diveTime / 0.6;
            vx = diveSide * 150 * (1 - t);
            vy = -110 * (1 - t) + diveSpeed * t;
        } else {
            double dx = diveTargetX - entity.getX();
            vx = Math.signum(dx) * Math.min(Math.abs(dx), 100);
            vy = diveSpeed;
        }
        entity.translateX(vx * tpf);
        entity.translateY(vy * tpf);
        if (canWrapDive()) {
            faceToward(vx, vy, tpf);
        }

        if (canSplit && !hasSplit && isMonster && waveLevel >= deltablade.GameVars.MONSTER_SPLIT_MIN_LEVEL
                && entity.getY() > FXGL.getAppHeight() * 0.40) {
            hasSplit = true;
            FXGL.<deltablade.DeltaBladeApp>getAppCast().splitMonster(entity, this);
            return;
        }

        if (entity.getY() > FXGL.getAppHeight()) {
            if (canWrapDive()) {
                // Sale por abajo, vuelve a entrar por arriba y regresa a su sitio.
                entity.setY(-entity.getHeight() - 10);
                state = State.RETURNING;
                return;
            }
            FXGL.<deltablade.DeltaBladeApp>getAppCast().onEnemyLeftScreen(squadId);
            entity.removeFromWorld();
        }
    }

    /** ¿Puede esta nave unirse ahora a un picado en grupo? */
    public boolean canGroupDive() {
        return state == State.FORMATION && !scooped && pendingDive < 0
                && formationHoldTime > minFormationTime && canWrapDive();
    }

    public void scheduleGroupDive(double delay, int side) {
        pendingDive = Math.max(0, delay);
        pendingSide = side;
    }

    /** ¿Quién puede salir por abajo y volver a entrar por arriba? Kamikaze, bonus, ovni y jefe no. */
    private boolean canWrapDive() {
        return !isKamikaze && !isBonusRunner && !isUfo && !isBoss && !diveOnAdd
                && entryPath != null && !meteorCover();
    }

    private void updateReturning(double tpf) {
        double tx = slotX();
        double ty = slotY();
        double dx = tx - entity.getX();
        double dy = ty - entity.getY();
        double dist = Math.hypot(dx, dy);
        double speed = Math.max(150, entrySpeed * 1.6);
        if (dist < Math.max(6, speed * tpf)) {
            entity.setPosition(tx, ty);
            entity.setRotation(0);
            heading = 0;
            state = State.FORMATION;
            formationHoldTime = 0;
            return;
        }
        double vx = dx / dist * speed;
        double vy = dy / dist * speed;
        entity.translate(vx * tpf, vy * tpf);
        faceToward(vx, vy, tpf);
    }

    /** Posición en la formación (X). */
    private double slotX() {
        double w = entity.getWidth();
        double cx = FXGL.getAppWidth() / 2.0;
        double x = cx + (targetX + w / 2.0 - cx) * WaveManager.formationBreath() - w / 2.0
                + WaveManager.formationSwayX();
        double minX = deltablade.GameVars.RAIL_WIDTH + 2;
        double maxX = FXGL.getAppWidth() - w - deltablade.GameVars.RAIL_WIDTH - 2;
        return Math.max(minX, Math.min(maxX, x));
    }

    /** Posición en la formación (Y). */
    private double slotY() {
        double top = 40;
        return top + (baseY - top) * (1 + (WaveManager.formationBreath() - 1) * 0.6);
    }

    /** La nave gira suavemente hacia la dirección de vuelo (con el morro por delante). */
    private void faceToward(double vx, double vy, double tpf) {
        if (Math.abs(vx) + Math.abs(vy) < 1) {
            return;
        }
        // Las imágenes de los enemigos apuntan hacia arriba -> +90 grados.
        double want = Math.toDegrees(Math.atan2(vy, vx)) + 90;
        double diff = ((want - heading) % 360 + 540) % 360 - 180;
        heading += diff * Math.min(1, tpf * 10);
        entity.setRotation(heading);
    }

    private void relaxRotation(double tpf) {
        if (heading != 0) {
            double diff = ((-heading) % 360 + 540) % 360 - 180;
            heading += diff * Math.min(1, tpf * 8);
            if (Math.abs(heading) < 0.5) {
                heading = 0;
            }
            entity.setRotation(heading);
        }
    }

    private void updateFiring(double tpf) {
        fireTimer += tpf;
        boolean angled = type == EnemyType.FAST || type == EnemyType.TOUGH;
        boolean cover = meteorCover();
        if (angled) {
            double gap = waveLevel <= 20 ? 2.4 - (waveLevel - 1) * 0.05 : 1.3;
            if (cover && !isUfo) {
                gap *= 2.4;
            }
            if (fireTimer >= gap) {
                fireTimer = 0;
                fireNow();
            }
            return;
        }
        double effectiveFireRate = isBoss ? type.fireRate * 1.5 : type.fireRate;
        if (cover && !isUfo) {
            effectiveFireRate *= deltablade.GameVars.METEOR_FIRE_SCALE;
        }
        if (fireTimer > 0.5 && RANDOM.nextDouble() < effectiveFireRate * tpf * 60) {
            fireTimer = 0;
            fireNow();
        }
    }

    private void fireNow() {
        FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnEnemyShot(
            type,
            entity.getX() + entity.getWidth() / 2,
            entity.getBottomY()
        );
    }

    private void updateUfoSweep(double tpf) {
        hoverPhase += tpf * 3;
        baseY = Math.min(baseY, formationCeiling());
        entity.setY(Math.min(baseY + Math.sin(hoverPhase) * 10, formationCeiling()));
        entity.translateX(type.speed * ufoDir * tpf);

        double minX = deltablade.GameVars.RAIL_WIDTH + 8;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH - 8;
        if (entity.getX() <= minX) {
            entity.setX(minX);
            ufoDir = 1;
        } else if (entity.getX() >= maxX) {
            entity.setX(maxX);
            ufoDir = -1;
        }
    }

    private void updateBossHover(double tpf) {
        if (isBeaming()) {
            return;
        }
        bossSwayPhase += tpf * (bossPhase == 3 ? 2.7 : 1.5);
        bossDiveTimer += tpf;

        double swayX = Math.sin(bossSwayPhase) * 80;
        double swayY = Math.sin(bossSwayPhase * 0.7) * 20;

        double centerX = (FXGL.getAppWidth() - entity.getWidth()) / 2;
        double targetX = centerX + swayX;

        double dx = targetX - entity.getX();
        entity.translateX(dx * tpf * 2);

        entity.setY(bossReturnY + swayY);

        double minX = deltablade.GameVars.RAIL_WIDTH;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH;
        if (entity.getX() < minX) entity.setX(minX);
        if (entity.getX() > maxX) entity.setX(maxX);

        if (bossDiveTimer > (bossPhase == 1 ? 4.0 : 2.5) && RANDOM.nextDouble() < 0.02) {
            bossDiveTimer = 0;
            state = State.BOSS_DIVE;
            bossDiving = true;
        }
    }

    /**
     * Ataques del jefe.
     * Fase 1 (vida completa): abanico de 3-5 disparos.
     * Fase 2 (desde la mitad de la vida): además, ayudantes que se lanzan en picado.
     * Fase 3 (por debajo de un cuarto): más rápido, en rojo, con anillo de balas y rayo.
     */
    private void updateBossAttack(double tpf) {
        double ratio = health / (double) Math.max(1, maxHealth);
        int phase = ratio > 0.5 ? 1 : ratio > 0.25 ? 2 : 3;
        if (phase != bossPhase) {
            bossPhase = phase;
            onBossPhaseChange(phase);
        }
        if (alienBoss >= 0) {
            updateAlienBossAttack(tpf);
            return;
        }

        // ¿Hay un rayo en marcha? Entonces solo ese.
        if (beamWarmup > 0 || beamLeft > 0) {
            updateBeam(tpf);
            return;
        }

        bossShotTimer -= tpf;
        if (bossShotTimer <= 0) {
            double gap = Math.max(1.1, 2.5 - bossNo * 0.1);
            if (bossPhase == 3) {
                gap *= 0.7;
            }
            bossShotTimer = gap;
            int count = 3 + Math.min(2, (bossNo - 1) / 2) + (bossPhase == 3 && bossNo > 1 ? 1 : 0);
            double speed = Math.min(250, 180 + bossNo * 8) * (bossPhase == 3 ? 1.12 : 1.0);
            bossFan(count, 60, speed);
        }

        if (bossPhase >= 2) {
            bossHelperTimer -= tpf;
            if (bossHelperTimer <= 0) {
                bossHelperTimer = bossPhase == 3 ? 9.0 : 7.5;
                int helpers = 1 + Math.min(3, (bossNo - 1) / 2 + (bossNo > 1 ? 1 : 0));
                FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnBossHelpers(entity, helpers);
            }
        }

        if (bossPhase == 3) {
            bossSpecialTimer -= tpf;
            if (bossSpecialTimer <= 0) {
                bossSpecialTimer = 4.0;
                if (bossRingNext) {
                    bossRing(8 + Math.min(10, bossNo * 2), Math.min(200, 145 + bossNo * 8));
                } else {
                    beamWarmup = 1.2;
                }
                bossRingNext = !bossRingNext;
            }
        }
    }

    private void onBossPhaseChange(int phase) {
        var app = FXGL.<deltablade.DeltaBladeApp>getAppCast();
        if (phase == 2 && alienBoss >= 0) {
            app.bossAnnounce(deltablade.I18n.tr("boss.hurt", bossLabel()));
        } else if (phase == 2) {
            app.bossAnnounce(deltablade.I18n.tr("boss.callsForHelp"));
        } else if (phase == 3) {
            app.bossAnnounce(deltablade.I18n.tr("boss.enraged"));
            javafx.scene.effect.Lighting red = new javafx.scene.effect.Lighting(
                    new javafx.scene.effect.Light.Distant(45, 50, javafx.scene.paint.Color.rgb(255, 90, 90)));
            entity.getViewComponent().getParent().setEffect(red);
            diveSpeed *= 1.3;
        }
    }

    /** Anillo de balas en todas las direcciones. */
    private void bossRing(int count, double speed) {
        double cx = entity.getX() + entity.getWidth() / 2.0;
        double cy = entity.getY() + entity.getHeight() / 2.0;
        double off = RANDOM.nextDouble() * Math.PI;
        for (int i = 0; i < count; i++) {
            double a = off + i * Math.PI * 2 / count;
            FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnBossBullet(cx, cy, Math.cos(a) * speed, Math.sin(a) * speed);
        }
        deltablade.SoundHelper.play("enemy_shot.wav");
    }

    /** Rayo: tiembla un momento (aviso) y después lanza un chorro denso de disparos hacia abajo. */
    private void updateBeam(double tpf) {
        if (beamWarmup > 0) {
            beamWarmup -= tpf;
            // El temblor de aviso es solo de la imagen (bamboleo suave); el jefe en sí no se mueve.
            double shake = Math.sin(beamWarmup * 60) * 3;
            shakeView(shake);
            if (beamWarmup <= 0) {
                shakeView(0);
                beamLeft = 1.0;
                beamTick = 0;
            }
            return;
        }
        beamLeft -= tpf;
        beamTick -= tpf;
        if (beamTick <= 0) {
            beamTick = 0.08;
            double cx = entity.getX() + entity.getWidth() / 2.0;
            double by = entity.getY() + entity.getHeight() * 0.85;
            FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnBossBullet(cx, by, 0, 340);
        }
    }

    private void shakeView(double dx) {
        for (javafx.scene.Node n : entity.getViewComponent().getChildren()) {
            n.setTranslateX(dx);
        }
    }

    public boolean isBeaming() {
        return beamWarmup > 0 || beamLeft > 0;
    }

    private void bossFan(int count, double spreadDeg, double speed) {
        double cx = entity.getX() + entity.getWidth() / 2.0;
        double by = entity.getY() + entity.getHeight() * 0.8;
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0 : i / (double) (count - 1) - 0.5;
            double a = Math.toRadians(t * spreadDeg);
            FXGL.<deltablade.DeltaBladeApp>getAppCast().spawnBossBullet(cx, by, Math.sin(a) * speed, Math.cos(a) * speed);
        }
        deltablade.SoundHelper.play("enemy_shot.wav");
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getBossNumber() {
        return bossNo;
    }

    private void updateBossDive(double tpf) {
        double maxDiveY = FXGL.getAppHeight() * 0.62 - entity.getHeight();

        if (bossDiving) {
            entity.translateY(diveSpeed * tpf);
            if (entity.getY() >= maxDiveY) {
                bossDiving = false;
            }
        } else {
            entity.translateY(-diveSpeed * 0.7 * tpf);
            if (entity.getY() <= bossReturnY) {
                entity.setY(bossReturnY);
                state = State.BOSS_HOVER;
            }
        }

        double minX = deltablade.GameVars.RAIL_WIDTH;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - deltablade.GameVars.RAIL_WIDTH;
        if (entity.getX() < minX) entity.setX(minX);
        if (entity.getX() > maxX) entity.setX(maxX);
    }

    private void notifySettled() {
        if (squadId >= 0) {
            FXGL.<deltablade.DeltaBladeApp>getAppCast().onSquadMemberSettled(squadId);
        }
    }

    public boolean isScooped() {
        return scooped;
    }

    public boolean isDocked() {
        return docked;
    }

    public int getDockSlot() {
        return dockSlot;
    }

    public boolean beginScoop(int slot) {
        if (scooped || docked || isBoss || slot < 0) {
            return false;
        }
        scooped = true;
        dockSlot = slot;
        scoopTime = 0;
        entity.setZIndex(105);
        return true;
    }

    public void dock() {
        docked = true;
        scooped = true;
        entity.setScaleX(DOCK_SCALE);
        entity.setScaleY(DOCK_SCALE);
        entity.setRotation(0);
        entity.getViewComponent().setOpacity(1);
        entity.setZIndex(105);
        snapToDock();
    }

    private void updateScooped(double tpf) {
        if (docked) {
            snapToDock();
            return;
        }
        scoopTime += tpf;
        double t = Math.min(1.0, scoopTime / SCOOP_DURATION);
        double ease = t * t * (3.0 - 2.0 * t);
        pullTowardDock(0.08 + ease * 0.22);
        entity.setRotation(ease * 360);
        double scale = 1.0 + (DOCK_SCALE - 1.0) * ease;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        entity.getViewComponent().setOpacity(1);
        if (scoopTime >= SCOOP_DURATION && entity.isActive()) {
            FXGL.<deltablade.DeltaBladeApp>getAppCast().completeScoop(entity, this);
        }
    }

    private void snapToDock() {
        applyDockPose(1);
    }

    private void pullTowardDock(double pull) {
        applyDockPose(Math.min(1.0, pull));
    }

    private void applyDockPose(double pull) {
        try {
            var player = FXGL.getGameWorld().getSingleton(deltablade.EntityType.PLAYER);
            double offsetX = dockSlot == 0 ? -44 : 44;
            double tx = player.getX() + player.getWidth() / 2.0 + offsetX - entity.getWidth() / 2.0;
            double ty = player.getY() + 4;
            entity.setX(entity.getX() + (tx - entity.getX()) * pull);
            entity.setY(entity.getY() + (ty - entity.getY()) * pull);
            if (pull >= 1) {
                entity.setX(tx);
                entity.setY(ty);
                entity.setRotation(0);
                entity.setScaleX(DOCK_SCALE);
                entity.setScaleY(DOCK_SCALE);
            }
        } catch (Exception ignored) {
        }
    }

    public void hit() {
        hit(1);
    }

    public void hit(int amount) {
        health -= Math.max(1, amount);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public int getScoreValue() {
        if (isBoss) {
            return deltablade.GameVars.BOSS_KILL_SCORE * bossNo;
        }
        return type.scoreValue;
    }

    public EnemyType getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public boolean isEntering() {
        return state == State.ENTERING;
    }

    public boolean isHarmlessOnContact() {
        if (scooped) {
            return true;
        }
        return isEntering()
                && entryPath != null
                && (entryPath.beadString || entryPath.type == WaveManager.EntryPath.Type.FROM_BELOW);
    }

    private static double formationCeiling() {
        return FXGL.getAppHeight() * deltablade.GameVars.FORMATION_MAX_Y_RATIO;
    }

    private double formationHoldY(double y) {
        return meteorCover() ? y : Math.min(y, formationCeiling());
    }

    private boolean canFireWhileClimbing() {
        if (state != State.ENTERING || entryPath == null
                || entryPath.type != WaveManager.EntryPath.Type.FROM_BELOW) {
            return false;
        }
        return entity.getBottomY() < FXGL.getAppHeight() - 130;
    }

    private boolean meteorCover() {
        try {
            return FXGL.<deltablade.DeltaBladeApp>getAppCast().isMeteorChallenge();
        } catch (Exception ignored) {
            return false;
        }
    }

    public int getSquadId() {
        return squadId;
    }
}
