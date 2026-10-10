package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import deltablade.components.BulletComponent;
import deltablade.components.CoinComponent;
import deltablade.components.CoverMeteorComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExplosionComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.MeteorRockComponent;
import deltablade.components.MissileComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlasmaOrbComponent;
import deltablade.components.RankMarkerComponent;
import deltablade.components.UfoAnimComponent;
import deltablade.components.BackgroundScrollComponent;
import deltablade.components.PlayerAnimationComponent;
import deltablade.components.PlayerComponent;
import deltablade.components.RailPulseComponent;
import deltablade.components.StarComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DeltaBladeFactory implements EntityFactory {

    private static final int SHIP_SIZE = 48;
    private static final int BOSS_SIZE = 80;
    private static final int BULLET_W = 12;
    private static final int BULLET_H = 20;
    private static final int PICKUP_SIZE = 28;

    /**
     * Load a texture from embedded bytes first (guaranteed to work).
     * Falls back to a triangle polygon if name is missing from embed.
     * NEVER returns a Rectangle - squares are the visual bug indicator.
     */
    private static Node sheetTile(String sheet, int index, int size, Color fallback) {
        Image tile = SpriteSheets.tile(sheet, index);
        if (tile != null && !tile.isError()) {
            ImageView view = new ImageView(tile);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.setSmooth(false);
            return view;
        }
        return EmbeddedTextures.createFallbackShip(size, fallback);
    }

    private static Node safeTexture(String name, int w, int h, Color fallback) {
        Image img = EmbeddedTextures.getImage(name, w, h);
        if (img != null && !img.isError()) {
            ImageView view = new ImageView(img);
            view.setFitWidth(w);
            view.setFitHeight(h);
            view.setSmooth(false);
            return view;
        }
        return EmbeddedTextures.createFallbackShip(w, fallback);
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        Image thrusterSheet = EmbeddedTextures.getImage("player_blue_thruster.png", 384, 48);
        Image bankSheet = EmbeddedTextures.getImage("player_blue_bank.png", 384, 48);
        Image bowwaveSheet = EmbeddedTextures.getImage("player_blue_bowwave.png", 384, 48);
        Image beamSheet = EmbeddedTextures.getImage("player_blue_beam.png", 384, 48);
        
        if (thrusterSheet != null && !thrusterSheet.isError()) {
            PlayerAnimationComponent animComp = new PlayerAnimationComponent(thrusterSheet, bankSheet, bowwaveSheet, beamSheet);
            ImageView view = animComp.getView();
            
            return FXGL.entityBuilder(data)
                    .type(EntityType.PLAYER)
                    .viewWithBBox(view)
                    .zIndex(100)
                    .collidable()
                    .with(new PlayerComponent())
                    .with(animComp)
                    .build();
        }
        
        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(safeTexture("player.png", SHIP_SIZE, SHIP_SIZE, Color.DODGERBLUE))
                .zIndex(100)
                .collidable()
                .with(new PlayerComponent())
                .build();
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        EnemyComponent.EnemyType type = data.get("enemyType");
        int level = data.get("level");

        boolean isBoss = (type == EnemyComponent.EnemyType.BOSS);
        boolean isUfo = (type == EnemyComponent.EnemyType.UFO);
        int size = isBoss ? GameVars.BOSS_DRAW_SIZE : SHIP_SIZE;

        EnemyComponent enemyComponent = new EnemyComponent(type, level);

        if (data.hasKey("kamikaze") && data.<Boolean>get("kamikaze")) {
            enemyComponent.setKamikaze(true);
        }
        if (data.hasKey("bonus") && data.<Boolean>get("bonus")) {
            enemyComponent.setBonusRunner(true);
        }

        if (data.hasKey("entrySpeed")) {
            enemyComponent.setEntrySpeed(data.get("entrySpeed"));
        }

        if (data.hasKey("entering") && data.<Boolean>get("entering")) {
            double targetX = data.get("targetX");
            double targetY = data.get("targetY");
            WaveManager.EntryPath entryPath = data.get("entryPath");
            int squadId = data.get("squadId");
            enemyComponent.setEntryData(targetX, targetY, entryPath, squadId);
        }

        if (data.hasKey("spriteIndex")) {
            enemyComponent.setSpriteIndex(data.get("spriteIndex"));
        }
        if (data.hasKey("canSplit") && data.<Boolean>get("canSplit")) {
            enemyComponent.setCanSplit(true);
        }
        if (data.hasKey("diveNow") && data.<Boolean>get("diveNow")) {
            double diveX = data.hasKey("diveTargetX") ? data.get("diveTargetX") : data.getX();
            enemyComponent.beginDiveNow(diveX);
        }

        if (isUfo) {
            return buildUfo(data, enemyComponent);
        }

        Node view;
        if (type == EnemyComponent.EnemyType.MONSTER) {
            int tile = data.hasKey("spriteIndex")
                    ? data.get("spriteIndex")
                    : SpriteSheets.monsterTileFor((int) (Math.random() * SpriteSheets.MONSTER_TILES));
            enemyComponent.setSpriteIndex(tile);
            view = sheetTile(SpriteSheets.MONSTERS, tile, size, Color.MEDIUMPURPLE);
        } else if (type != EnemyComponent.EnemyType.BOSS) {
            boolean useClassic = Math.random() < 0.45;
            if (useClassic) {
                String textureName = switch (type) {
                    case FAST -> "enemy_fast.png";
                    case TOUGH -> "enemy_tough.png";
                    default -> "enemy_basic.png";
                };
                Color fallback = switch (type) {
                    case FAST -> Color.LIME;
                    case TOUGH -> Color.MEDIUMPURPLE;
                    default -> Color.CRIMSON;
                };
                view = safeTexture(textureName, size, size, fallback);
            } else {
                int tile = SpriteSheets.shipTileFor(type, data.hasKey("spriteIndex")
                        ? data.get("spriteIndex")
                        : (int) (Math.random() * 32));
                int drawSize = switch (type) {
                    case FAST -> Math.max(40, size - 6);
                    case TOUGH -> size + 8;
                    default -> size;
                };
                view = sheetTile(SpriteSheets.SHIPS, tile, drawSize, Color.CRIMSON);
            }
        } else {
            // Boss: vorhandenes Gegner-Bild, nur viel groesser. Jeder Boss sieht anders aus.
            int bossNo = GameVars.bossNumber(level);
            if (bossNo % 2 == 0) {
                view = sheetTile(SpriteSheets.MONSTERS, (bossNo / 2) % SpriteSheets.MONSTER_TILES, size, Color.DARKVIOLET);
            } else {
                view = sheetTile(SpriteSheets.SHIPS, (bossNo * 5) % SpriteSheets.SHIP_TILES, size, Color.DARKVIOLET);
            }
        }

        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY)
                .viewWithBBox(view)
                .zIndex(50)
                .collidable()
                .with(enemyComponent)
                .build();
    }

    private Entity buildUfo(SpawnData data, EnemyComponent enemyComponent) {
        int size = 96;
        Image[] frames = new Image[8];
        int frameCount = 0;
        for (int i = 1; i <= 8; i++) {
            Image frame = EmbeddedTextures.getImage("enemy_ufo_0" + i + ".png", 256, 256);
            if (frame != null && !frame.isError()) {
                frames[frameCount++] = frame;
            }
        }
        Image first = frameCount > 0 ? frames[0] : EmbeddedTextures.getImage("enemy_ufo.png", 256, 256);
        Node view;
        ImageView imageView = null;
        if (first != null && !first.isError()) {
            imageView = new ImageView(first);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            view = imageView;
        } else {
            Ellipse body = new Ellipse(size / 2.0, size * 0.62, size * 0.36, size * 0.14);
            body.setFill(Color.rgb(180, 220, 255));
            body.setStroke(Color.CYAN);
            Rectangle space = new Rectangle(size, size);
            space.setFill(Color.TRANSPARENT);
            view = new Group(space, body);
        }

        var builder = FXGL.entityBuilder(data)
                .type(EntityType.ENEMY)
                .view(view)
                .bbox(new HitBox("BODY", new Point2D(14, 46), BoundingShape.box(68, 28)))
                .zIndex(55)
                .collidable()
                .with(enemyComponent);
        if (imageView != null && frameCount > 1) {
            Image[] used = new Image[frameCount];
            System.arraycopy(frames, 0, used, 0, frameCount);
            builder.with(new UfoAnimComponent(imageView, used));
        }
        return builder.build();
    }

    @Spawns("playerBullet")
    public Entity newPlayerBullet(SpawnData data) {
        double speedX = data.hasKey("speedX") ? data.<Double>get("speedX") : 0.0;
        double speedY = data.hasKey("speedY") ? data.<Double>get("speedY") : -500.0;

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_BULLET)
                .viewWithBBox(safeTexture("bullet_player.png", BULLET_W, BULLET_H, Color.YELLOW))
                .zIndex(75)
                .collidable()
                .with(new BulletComponent(speedX, speedY, true))
                .build();
    }

    @Spawns("enemyBullet")
    public Entity newEnemyBullet(SpawnData data) {
        double speedX = data.hasKey("speedX") ? data.<Double>get("speedX") : 0.0;
        double speedY = data.hasKey("speedY") ? data.<Double>get("speedY") : 250.0;
        boolean homing = data.hasKey("homing") && data.<Boolean>get("homing");
        String kind = data.hasKey("kind") ? data.get("kind") : "bolt";

        Node view;
        if (homing) {
            view = missileView();
        } else if (!"bolt".equals(kind)) {
            view = gooView(kind);
        } else {
            view = safeTexture("bullet_enemy.png", 10, 16, Color.ORANGERED);
        }

        BulletComponent bullet = new BulletComponent(speedX, speedY, false, homing);
        bullet.setShotKind(kind);
        return FXGL.entityBuilder(data)
                .type(EntityType.ENEMY_BULLET)
                .viewWithBBox(view)
                .zIndex(75)
                .collidable()
                .with(bullet)
                .build();
    }

    private static Node gooView(String kind) {
        return switch (kind) {
            case "drip" -> {
                Ellipse drop = new Ellipse(4, 7);
                drop.setFill(Color.rgb(80, 220, 140, 0.95));
                drop.setStroke(Color.rgb(30, 90, 60));
                drop.setStrokeWidth(1);
                yield drop;
            }
            case "flame" -> {
                Ellipse flame = new Ellipse(5, 8);
                flame.setFill(new RadialGradient(0, 0, 0.5, 0.7, 0.7, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 240, 140)),
                        new Stop(0.45, Color.ORANGE),
                        new Stop(1, Color.rgb(180, 40, 0, 0.2))));
                yield flame;
            }
            default -> {
                Circle blob = new Circle(6, Color.rgb(170, 70, 220, 0.92));
                blob.setStroke(Color.rgb(80, 20, 120));
                blob.setStrokeWidth(1.2);
                yield blob;
            }
        };
    }

    private static Node missileView() {
        Image img = EmbeddedTextures.getImage("bullet_missile.png", 256, 256);
        Node sprite;
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(14);
            iv.setFitHeight(22);
            iv.setSmooth(true);
            sprite = iv;
        } else {
            Rectangle body = new Rectangle(6, 16);
            body.setFill(Color.ORANGE);
            body.setArcWidth(3);
            body.setArcHeight(3);
            sprite = body;
        }
        Rectangle trail = new Rectangle(3, 12);
        trail.setFill(Color.rgb(255, 140, 40, 0.75));
        trail.setTranslateX(5.5);
        trail.setTranslateY(18);
        return new Group(trail, sprite);
    }

    @Spawns("weaponPickup")
    public Entity newWeaponPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_weapon.png", PICKUP_SIZE, PICKUP_SIZE, Color.GOLD))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.WEAPON_UPGRADE))
                .build();
    }

    @Spawns("ammoPickup")
    public Entity newAmmoPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_ammo.png", PICKUP_SIZE, PICKUP_SIZE, Color.CYAN))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_AMMO))
                .build();
    }

    @Spawns("skullPickup")
    public Entity newSkullPickup(SpawnData data) {
        Node view = safeTexture("skull.png", PICKUP_SIZE, PICKUP_SIZE, Color.DARKRED);
        Circle glow = new Circle(PICKUP_SIZE / 2.0 + 4, Color.rgb(180, 20, 20, 0.28));
        glow.setTranslateX(PICKUP_SIZE / 2.0);
        glow.setTranslateY(PICKUP_SIZE / 2.0);
        Group group = new Group(glow, view);
        // ~35% der Skulls rasen runter — schwerer zu vermeiden
        double fallSpeed = Math.random() < 0.35 ? 260 : 80;
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(group)
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.SKULL, fallSpeed))
                .build();
    }

    @Spawns("meteorPickup")
    public Entity newMeteorPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(orbPickup(Color.ORANGERED, Color.ORANGE, "M"))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.METEOR))
                .build();
    }

    @Spawns("cognitivePickup")
    public Entity newCognitivePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(orbPickup(Color.CYAN, Color.AQUA, "C"))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.COGNITIVE))
                .build();
    }

    @Spawns("mazePickup")
    public Entity newMazePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(orbPickup(Color.MEDIUMPURPLE, Color.VIOLET, "Z"))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.MAZE))
                .build();
    }


    @Spawns("plasmaOrb")
    public Entity newPlasmaOrb(SpawnData data) {
        double speedX = data.hasKey("speedX") ? data.<Double>get("speedX") : 0.0;
        double speedY = data.hasKey("speedY") ? data.<Double>get("speedY") : -280.0;
        int pierce = data.hasKey("pierce") ? data.<Integer>get("pierce") : 1;
        int core = data.hasKey("coreDamage") ? data.<Integer>get("coreDamage") : 3;
        int splash = data.hasKey("splashDamage") ? data.<Integer>get("splashDamage") : 2;
        double radius = data.hasKey("splashRadius") ? data.<Double>get("splashRadius") : 64.0;

        Circle coreOrb = new Circle(14);
        coreOrb.setFill(new RadialGradient(
                0, 0, 0.35, 0.35, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.35, Color.MEDIUMPURPLE),
                new Stop(0.75, Color.DARKVIOLET),
                new Stop(1, Color.rgb(20, 8, 30))
        ));
        Circle halo = new Circle(20, Color.rgb(180, 80, 255, 0.22));
        Group view = new Group(halo, coreOrb);
        halo.setTranslateX(14);
        halo.setTranslateY(14);
        coreOrb.setTranslateX(14);
        coreOrb.setTranslateY(14);

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_PLASMA)
                .viewWithBBox(view)
                .zIndex(55)
                .collidable()
                .with(new PlasmaOrbComponent(speedX, speedY, pierce, core, splash, radius))
                .build();
    }

    @Spawns("meteorRock")
    public Entity newMeteorRock(SpawnData data) {
        double size = data.hasKey("size") ? data.<Double>get("size") : 36.0;
        double baseSpeed = data.hasKey("baseSpeed") ? data.<Double>get("baseSpeed") : 120.0;
        double driftX = data.hasKey("driftX") ? data.<Double>get("driftX") : 0.0;
        double spin = data.hasKey("spin") ? data.<Double>get("spin") : 40.0;
        int variant = data.hasKey("variant") ? data.<Integer>get("variant") : 0;
        variant = Math.floorMod(variant, 4);

        Image rock = EmbeddedTextures.getImage("meteor_" + variant + ".png", 128, 128);
        Node view;
        double displayW = size;
        double displayH = size;
        if (rock != null && !rock.isError()) {
            double nw = Math.max(1, rock.getWidth());
            double nh = Math.max(1, rock.getHeight());
            double scale = size / Math.max(nw, nh);
            displayW = Math.max(16, nw * scale);
            displayH = Math.max(16, nh * scale);
            ImageView imageView = new ImageView(rock);
            imageView.setFitWidth(displayW);
            imageView.setFitHeight(displayH);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(false);
            view = imageView;
        } else {
            javafx.scene.shape.Polygon poly = new javafx.scene.shape.Polygon(
                    size * 0.5, 0,
                    size * 0.95, size * 0.28,
                    size * 0.82, size * 0.9,
                    size * 0.2, size * 0.95,
                    0, size * 0.4
            );
            poly.setFill(Color.rgb(110, 95, 80));
            poly.setStroke(Color.rgb(50, 42, 36));
            view = poly;
        }

        Entity meteor = FXGL.entityBuilder(data)
                .type(EntityType.MINIGAME_HAZARD)
                .viewWithBBox(view)
                .zIndex(70)
                .with(new MeteorRockComponent(baseSpeed, driftX, spin))
                .build();
        meteor.getTransformComponent().setRotationOrigin(new Point2D(displayW / 2.0, displayH / 2.0));
        return meteor;
    }

    @Spawns("coverMeteor")
    public Entity newCoverMeteor(SpawnData data) {
        int variant = data.hasKey("variant") ? data.<Integer>get("variant") : 1;
        double scale = data.hasKey("scale") ? data.<Double>get("scale") : 1.0;
        double spin = data.hasKey("spin") ? data.<Double>get("spin") : 12.0;
        variant = Math.floorMod(variant, 4);

        String name = "meteor_" + variant + ".png";
        Image rock = EmbeddedTextures.getImage(name, 128, 128);
        Node view;
        double displayW;
        double displayH;
        if (rock != null && !rock.isError()) {
            displayW = Math.max(16, rock.getWidth() * scale);
            displayH = Math.max(16, rock.getHeight() * scale);
            ImageView imageView = new ImageView(rock);
            imageView.setFitWidth(displayW);
            imageView.setFitHeight(displayH);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(false);
            view = imageView;
        } else {
            displayW = 72 * scale;
            displayH = 72 * scale;
            view = safeTexture("asteroid1.png", (int) displayW, (int) displayH, Color.GRAY);
        }

        double[] box = meteorHitBox(rock, displayW, displayH);
        Entity meteor = FXGL.entityBuilder(data)
                .type(EntityType.METEOR)
                .view(view)
                .bbox(new HitBox("BODY", new Point2D(box[0], box[1]), BoundingShape.box(box[2], box[3])))
                .zIndex(68)
                .collidable()
                .with(new CoverMeteorComponent(spin,
                        (rock != null && !rock.isError()) ? rock : null, displayW, displayH))
                .build();
        meteor.getTransformComponent().setRotationOrigin(new Point2D(displayW / 2.0, displayH / 2.0));
        return meteor;
    }

    private static double[] meteorHitBox(Image rock, double displayW, double displayH) {
        double inset = 0.22;
        double x = displayW * inset;
        double y = displayH * inset;
        double w = displayW * (1 - 2 * inset);
        double h = displayH * (1 - 2 * inset);
        if (rock == null || rock.getPixelReader() == null) {
            return new double[] {x, y, Math.max(12, w), Math.max(12, h)};
        }
        PixelReader reader = rock.getPixelReader();
        int iw = (int) rock.getWidth();
        int ih = (int) rock.getHeight();
        int minX = iw;
        int minY = ih;
        int maxX = 0;
        int maxY = 0;
        boolean any = false;
        for (int py = 0; py < ih; py++) {
            for (int px = 0; px < iw; px++) {
                if (reader.getColor(px, py).getOpacity() < 0.35) {
                    continue;
                }
                any = true;
                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
            }
        }
        if (!any) {
            return new double[] {x, y, Math.max(12, w), Math.max(12, h)};
        }
        double sx = displayW / iw;
        double sy = displayH / ih;
        x = minX * sx;
        y = minY * sy;
        w = (maxX - minX + 1) * sx;
        h = (maxY - minY + 1) * sy;
        double padX = Math.max(4, w * 0.22);
        double padY = Math.max(4, h * 0.22);
        x += padX;
        y += padY;
        w = Math.max(10, w - padX * 2);
        h = Math.max(10, h - padY * 2);
        return new double[] {x, y, w, h};
    }

    private static Group orbPickup(Color outer, Color inner, String letter) {
        Circle glow = new Circle(14);
        glow.setFill(new RadialGradient(
                0, 0, 0.3, 0.3, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.35, inner),
                new Stop(0.8, outer),
                new Stop(1, Color.rgb(20, 10, 8))
        ));
        Text text = new Text(letter);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        text.setFill(Color.WHITE);
        text.setTranslateX(-5);
        text.setTranslateY(5);
        return new Group(glow, text);
    }

    @Spawns("timePickup")
    public Entity newTimePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_time.png", PICKUP_SIZE, PICKUP_SIZE, Color.LIMEGREEN))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.EXTRA_TIME))
                .build();
    }

    @Spawns("rankMarker")
    public Entity newRankMarker(SpawnData data) {
        int colorIndex = data.hasKey("colorIndex") ? data.<Integer>get("colorIndex") : 0;
        colorIndex = Math.max(0, Math.min(RankMarkerComponent.COLORS.length - 1, colorIndex));
        Color color = RankMarkerComponent.COLORS[colorIndex];
        Image img = EmbeddedTextures.getImage(RankMarkerComponent.TEXTURES[colorIndex], 256, 256);
        Node view;
        double size = 20;
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setSmooth(true);
            Circle ring = new Circle(size / 2, size / 2, size / 2 - 1.4);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(color);
            ring.setStrokeWidth(2.2);
            view = new Group(iv, ring);
        } else {
            Rectangle tile = new Rectangle(size, size);
            tile.setArcWidth(6);
            tile.setArcHeight(6);
            tile.setFill(color);
            tile.setStroke(Color.WHITE);
            tile.setStrokeWidth(2.4);
            view = tile;
        }
        return FXGL.entityBuilder(data)
                .type(EntityType.RANK_MARKER)
                .viewWithBBox(view)
                .zIndex(65)
                .collidable()
                .with(new RankMarkerComponent(colorIndex))
                .build();
    }

    @Spawns("scoopPickup")
    public Entity newScoopPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_scoop.png", PICKUP_SIZE, PICKUP_SIZE, Color.LIMEGREEN))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.SCOOP))
                .build();
    }

    @Spawns("missilePickup")
    public Entity newMissilePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(pulsingExtraPickup(
                        "pickup_missile.png", Color.ORANGERED, Color.rgb(255, 120, 40, 0.30)))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.MISSILE))
                .build();
    }

    @Spawns("playerMissile")
    public Entity newPlayerMissile(SpawnData data) {
        Image img = EmbeddedTextures.getImage("player_missile.png", 256, 256);
        Node sprite;
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setSmooth(false);
            iv.setRotate(MissileComponent.ART_OFFSET_DEG);
            sprite = iv;
        } else {
            sprite = safeTexture("pickup_missile.png", 32, 32, Color.ORANGERED);
            sprite.setRotate(MissileComponent.ART_OFFSET_DEG);
        }
        Ellipse flame = new Ellipse(3.2, 7);
        flame.setFill(Color.rgb(255, 170, 50, 0.9));
        flame.setTranslateX(16);
        flame.setTranslateY(28);
        Group view = new Group(flame, sprite);
        Entity missile = FXGL.entityBuilder(data)
                .type(EntityType.PLAYER_MISSILE)
                .viewWithBBox(view)
                .zIndex(82)
                .collidable()
                .with(new MissileComponent())
                .build();
        missile.getTransformComponent().setRotationOrigin(new Point2D(
                missile.getWidth() / 2.0, missile.getHeight() / 2.0));
        return missile;
    }

    @Spawns("shieldPickup")
    public Entity newShieldPickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(pulsingExtraPickup(
                        "pickup_shield.png", Color.AQUA, Color.rgb(80, 220, 255, 0.32)))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.SHIELD))
                .build();
    }

    private static Node pulsingExtraPickup(String texture, Color fallback, Color glowColor) {
        Node icon = safeTexture(texture, PICKUP_SIZE, PICKUP_SIZE, fallback);
        Circle glow = new Circle(PICKUP_SIZE / 2.0 + 5, glowColor);
        glow.setTranslateX(PICKUP_SIZE / 2.0);
        glow.setTranslateY(PICKUP_SIZE / 2.0);
        return new Group(glow, icon);
    }

    @Spawns("autofirePickup")
    public Entity newAutofirePickup(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.PICKUP)
                .viewWithBBox(safeTexture("pickup_autofire.png", PICKUP_SIZE, PICKUP_SIZE, Color.CYAN))
                .zIndex(60)
                .collidable()
                .with(new PickupComponent(PickupComponent.PickupType.AUTOFIRE))
                .build();
    }

    @Spawns("extraLetterOrb")
    public Entity newExtraLetterOrb(SpawnData data) {
        char letter = data.get("letter");
        int letterIndex = data.get("letterIndex");
        
        Color[] letterColors = {
            Color.rgb(255, 100, 100),
            Color.rgb(100, 255, 100),
            Color.rgb(100, 200, 255),
            Color.rgb(255, 200, 100),
            Color.rgb(200, 100, 255)
        };
        
        Color orbColor = letterColors[letterIndex % letterColors.length];
        Color innerColor = orbColor.brighter().brighter();
        
        RadialGradient gradient = new RadialGradient(
            0, 0, 0.25, 0.25, 0.9, true, CycleMethod.NO_CYCLE,
            new Stop(0, innerColor),
            new Stop(0.4, orbColor.brighter()),
            new Stop(0.7, orbColor),
            new Stop(1, orbColor.darker().darker())
        );
        
        Circle orb = new Circle(16);
        orb.setFill(gradient);
        
        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        letterText.setFill(Color.WHITE);
        letterText.setTranslateX(-6);
        letterText.setTranslateY(6);
        
        ExtraLetterPickupComponent component = new ExtraLetterPickupComponent(letter, letterIndex, letterText);
        Group flipWrapper = component.getFlipWrapper();
        
        Group group = new Group(orb, flipWrapper != null ? flipWrapper : letterText);
        
        return FXGL.entityBuilder(data)
                .type(EntityType.EXTRA_LETTER_PICKUP)
                .viewWithBBox(group)
                .zIndex(65)
                .collidable()
                .with(component)
                .build();
    }

    @Spawns("sideRail")
    public Entity newSideRail(SpawnData data) {
        int width = GameVars.RAIL_WIDTH;
        int height = data.get("height");
        boolean isLeft = data.get("isLeft");

        LinearGradient metalGradient = isLeft
                ? new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(18, 22, 32)),
                    new Stop(0.18, Color.rgb(46, 54, 70)),
                    new Stop(0.45, Color.rgb(78, 88, 108)),
                    new Stop(0.72, Color.rgb(42, 50, 64)),
                    new Stop(1, Color.rgb(12, 14, 22)))
                : new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(12, 14, 22)),
                    new Stop(0.28, Color.rgb(42, 50, 64)),
                    new Stop(0.55, Color.rgb(78, 88, 108)),
                    new Stop(0.82, Color.rgb(46, 54, 70)),
                    new Stop(1, Color.rgb(18, 22, 32)));

        Group railGroup = new Group();

        Rectangle rail = new Rectangle(width, height);
        rail.setFill(metalGradient);
        railGroup.getChildren().add(rail);

        for (int y = 18; y < height - 18; y += 36) {
            Rectangle seam = new Rectangle(width - 10, 1);
            seam.setFill(Color.rgb(10, 12, 18, 0.7));
            seam.setX(5);
            seam.setY(y);
            railGroup.getChildren().add(seam);

            Rectangle seamHi = new Rectangle(width - 10, 1);
            seamHi.setFill(Color.rgb(140, 160, 190, 0.18));
            seamHi.setX(5);
            seamHi.setY(y + 1);
            railGroup.getChildren().add(seamHi);
        }

        for (int y = 28; y < height - 20; y += 36) {
            double boltX = isLeft ? 11 : width - 15;
            Circle bolt = new Circle(boltX, y, 2.4);
            bolt.setFill(new RadialGradient(
                    0, 0, 0.35, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(190, 205, 220)),
                    new Stop(0.55, Color.rgb(110, 120, 140)),
                    new Stop(1, Color.rgb(40, 45, 55))));
            railGroup.getChildren().add(bolt);
        }

        Rectangle capTop = new Rectangle(width, 14);
        capTop.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(90, 110, 140)),
                new Stop(1, Color.rgb(28, 34, 46))));
        railGroup.getChildren().add(capTop);

        Rectangle capBottom = new Rectangle(width, 14);
        capBottom.setY(height - 14);
        capBottom.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(28, 34, 46)),
                new Stop(1, Color.rgb(90, 110, 140))));
        railGroup.getChildren().add(capBottom);

        Rectangle outerEdge = new Rectangle(2, height);
        outerEdge.setFill(Color.rgb(8, 10, 14));
        outerEdge.setX(isLeft ? 0 : width - 2);
        railGroup.getChildren().add(outerEdge);

        Rectangle energy = new Rectangle(3, height - 28);
        energy.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(40, 220, 255, 0.15)),
                new Stop(0.5, Color.CYAN),
                new Stop(1, Color.rgb(40, 220, 255, 0.15))));
        energy.setX(isLeft ? width - 5 : 2);
        energy.setY(14);
        energy.setEffect(new javafx.scene.effect.DropShadow(10, Color.CYAN));
        railGroup.getChildren().add(energy);

        Rectangle innerShade = new Rectangle(3, height);
        innerShade.setFill(Color.rgb(0, 0, 0, 0.45));
        innerShade.setX(isLeft ? width - 8 : 5);
        railGroup.getChildren().add(innerShade);

        return FXGL.entityBuilder(data)
                .view(railGroup)
                .zIndex(200)
                .with(new RailPulseComponent(energy))
                .build();
    }

    @Spawns("star")
    public Entity newStar(SpawnData data) {
        double size = data.get("size");
        double opacity = data.get("opacity");

        Circle star = new Circle(size / 2);
        star.setFill(Color.WHITE);
        star.setOpacity(opacity);

        return FXGL.entityBuilder(data)
                .view(star)
                .zIndex(-100)
                .build();
    }

    @Spawns("scrollingStar")
    public Entity newScrollingStar(SpawnData data) {
        double size = data.get("size");
        double opacity = data.get("opacity");
        double scrollSpeed = data.get("scrollSpeed");
        boolean isNear = data.hasKey("isNear") && data.<Boolean>get("isNear");
        
        Circle star = new Circle(size / 2);
        
        if (isNear && size > 2.5) {
            RadialGradient flare = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.3, Color.rgb(200, 220, 255, 0.9)),
                new Stop(0.7, Color.rgb(150, 180, 255, 0.5)),
                new Stop(1, Color.TRANSPARENT)
            );
            star.setFill(flare);
        } else {
            Color starColor = isNear ? Color.rgb(220, 230, 255) : Color.rgb(180, 180, 200);
            star.setFill(starColor);
        }
        star.setOpacity(opacity);
        
        double twinkleSpeed = 1.5 + Math.random() * 2.0;
        double twinkleAmount = 0.15 + Math.random() * 0.15;
        
        return FXGL.entityBuilder(data)
                .view(star)
                .zIndex(isNear ? -90 : -100)
                .with(new StarComponent(star, scrollSpeed, opacity, twinkleSpeed, twinkleAmount))
                .build();
    }

    @Spawns("nebula")
    public Entity newNebula(SpawnData data) {
        double radius = data.get("radius");
        Color color = data.get("color");
        double opacity = data.get("opacity");
        
        RadialGradient gradient = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.8)),
            new Stop(0.4, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.4)),
            new Stop(0.7, Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.15)),
            new Stop(1, Color.TRANSPARENT)
        );
        
        Circle blob = new Circle(radius);
        blob.setFill(gradient);
        
        return FXGL.entityBuilder(data)
                .view(blob)
                .zIndex(-500)
                .build();
    }

    @Spawns("distantPlanet")
    public Entity newDistantPlanet(SpawnData data) {
        double radius = data.get("radius");
        
        RadialGradient planetGradient = new RadialGradient(
            -45, 0.3, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(60, 50, 80)),
            new Stop(0.5, Color.rgb(40, 35, 55)),
            new Stop(0.8, Color.rgb(25, 20, 35)),
            new Stop(1, Color.rgb(15, 12, 22))
        );
        
        Circle planet = new Circle(radius);
        planet.setFill(planetGradient);
        planet.setOpacity(0.6);
        
        return FXGL.entityBuilder(data)
                .view(planet)
                .zIndex(-400)
                .build();
    }

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        int width = data.get("width");
        int height = data.get("height");

        Rectangle fallback = new Rectangle(width, height);
        fallback.setFill(Color.rgb(5, 5, 16));

        Image space = EmbeddedTextures.getImage("space_bg.png", width, width);
        if (space == null || space.isError()) {
            return FXGL.entityBuilder(data)
                    .at(0, 0)
                    .view(fallback)
                    .zIndex(-1000)
                    .build();
        }

        double tileH = width;
        ImageView tileA = new ImageView(space);
        tileA.setFitWidth(width);
        tileA.setFitHeight(tileH);
        tileA.setSmooth(true);
        tileA.setPreserveRatio(false);
        tileA.setOpacity(0.85);

        ImageView tileB = new ImageView(space);
        tileB.setFitWidth(width);
        tileB.setFitHeight(tileH);
        tileB.setSmooth(true);
        tileB.setPreserveRatio(false);
        tileB.setOpacity(0.85);
        tileB.setTranslateY(-tileH);

        Group view = new Group(fallback, tileA, tileB);

        return FXGL.entityBuilder(data)
                .at(0, 0)
                .view(view)
                .zIndex(-1000)
                .with(new BackgroundScrollComponent(tileA, tileB, tileH, 12))
                .build();
    }

    /**
     * Spawns an explosion effect at the given position.
     * Data keys:
     *   - "size": String - "hit" (small sparks), "ship" (medium), or "big" (boss, 2-row grid)
     * The explosion is centered on the spawn position (x,y is center, not top-left).
     */
    @Spawns("explosion")
    public Entity newExplosion(SpawnData data) {
        String size = data.hasKey("size") ? data.<String>get("size") : "ship";

        String textureName;
        int frameCount;
        int frameWidth;
        int frameHeight;
        int columns;
        int sheetWidth;
        int sheetHeight;
        double duration;
        int displayW;
        int displayH;

        int zIndex = 90;
        switch (size) {
            case "hit" -> {
                textureName = "explosion_hit.png";
                frameCount = 6;
                frameWidth = 32;
                frameHeight = 32;
                columns = 6;
                sheetWidth = 192;
                sheetHeight = 32;
                duration = 0.35;
                displayW = 80;
                displayH = 80;
            }
            case "boss_hit" -> {
                textureName = "explosion_hit.png";
                frameCount = 6;
                frameWidth = 32;
                frameHeight = 32;
                columns = 6;
                sheetWidth = 192;
                sheetHeight = 32;
                duration = 0.45;
                displayW = 150;
                displayH = 150;
                zIndex = 150;
            }
            case "big" -> {
                textureName = "explosion_big.png";
                frameCount = 10;
                frameWidth = 64;
                frameHeight = 64;
                columns = 8;
                sheetWidth = 512;
                sheetHeight = 128;
                duration = 0.9;
                displayW = 280;
                displayH = 280;
            }
            default -> {
                textureName = "explosion_ship.png";
                frameCount = 8;
                frameWidth = 64;
                frameHeight = 64;
                columns = 8;
                sheetWidth = 512;
                sheetHeight = 64;
                duration = 0.7;
                displayW = 192;
                displayH = 192;
            }
        }

        Image spriteSheet = EmbeddedTextures.getImage(textureName, sheetWidth, sheetHeight);
        if (spriteSheet == null || spriteSheet.isError()) {
            return createFallbackExplosion(data, size);
        }

        ExplosionComponent explosionComp = new ExplosionComponent(
                spriteSheet, frameCount, frameWidth, frameHeight, columns, duration);

        explosionComp.setDisplaySize(displayW, displayH);

        ImageView view = explosionComp.getView();
        view.setTranslateX(-displayW / 2.0);
        view.setTranslateY(-displayH / 2.0);

        return FXGL.entityBuilder(data)
                .view(view)
                .zIndex(zIndex)
                .with(explosionComp)
                .build();
    }

    private Entity createFallbackExplosion(SpawnData data, String size) {
        int displaySize;
        double durationSec;
        switch (size) {
            case "hit" -> { displaySize = 80; durationSec = 0.35; }
            case "big" -> { displaySize = 280; durationSec = 0.9; }
            default -> { displaySize = 192; durationSec = 0.7; }
        }
        
        Group circleGroup = new Group();
        double center = displaySize / 2.0;

        Circle c1 = new Circle(center * 0.6);
        c1.setFill(Color.ORANGE);
        c1.setCenterX(0);
        c1.setCenterY(0);

        Circle c2 = new Circle(center * 0.4);
        c2.setFill(Color.YELLOW);
        c2.setCenterX(-center * 0.2);
        c2.setCenterY(-center * 0.15);

        Circle c3 = new Circle(center * 0.25);
        c3.setFill(Color.WHITE);
        c3.setCenterX(center * 0.15);
        c3.setCenterY(center * 0.1);

        circleGroup.getChildren().addAll(c1, c2, c3);

        Entity entity = FXGL.entityBuilder(data)
                .view(circleGroup)
                .zIndex(90)
                .build();

        FXGL.getGameTimer().runOnceAfter(() -> {
            if (entity.isActive()) {
                entity.removeFromWorld();
            }
        }, javafx.util.Duration.seconds(durationSec));

        return entity;
    }

    @Spawns("coin")
    public Entity newCoin(SpawnData data) {
        String coinTypeStr = data.hasKey("coinType") ? data.<String>get("coinType") : "white";
        
        CoinComponent.CoinType coinType = switch (coinTypeStr) {
            case "green" -> CoinComponent.CoinType.GREEN;
            case "blue" -> CoinComponent.CoinType.BLUE;
            case "violet" -> CoinComponent.CoinType.VIOLET;
            default -> CoinComponent.CoinType.WHITE;
        };
        
        Color coinColor = switch (coinType) {
            case WHITE -> Color.rgb(240, 240, 240);
            case GREEN -> Color.rgb(100, 220, 100);
            case BLUE -> Color.rgb(100, 150, 255);
            case VIOLET -> Color.rgb(200, 100, 255);
        };
        
        Color highlightColor = coinColor.brighter();

        Circle outer = new Circle(8);
        outer.setFill(new RadialGradient(
            0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
            new Stop(0, highlightColor),
            new Stop(0.5, coinColor),
            new Stop(1, coinColor.darker())
        ));

        Circle highlight = new Circle(3);
        highlight.setFill(Color.rgb(255, 255, 255, 0.7));
        highlight.setCenterX(-2);
        highlight.setCenterY(-2);

        Group coinView = new Group(outer, highlight);

        return FXGL.entityBuilder(data)
                .type(EntityType.COIN)
                .viewWithBBox(coinView)
                .zIndex(65)
                .collidable()
                .with(new CoinComponent(coinType))
                .build();
    }
}
