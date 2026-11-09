package pl.pearltrail;

import net.minecraft.block.ShapeContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

import java.util.*;

/** Śledzi lecące perły, rysuje smugi i wykrywa miejsce uderzenia. */
public class TrailManager {

    /** Odstęp między punktami smugi w blokach (mniej = gęściej). */
    private static final double SPACING = 0.2;
    private static final int MAX_POINTS = 2000;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();
    public static int clientTicks = 0;

    private static class Trail {
        final List<Vec3d> points = new ArrayList<>();
        Vec3d last;
        Vec3d velocity = Vec3d.ZERO;
        int deadTicks = -1; // -1 = perła wciąż leci
    }

    public static void clear() {
        TRAILS.clear();
        HighlightRenderer.clear();
    }

    public static void tick(ClientWorld world) {
        clientTicks++;
        TrailConfig cfg = TrailConfig.INSTANCE;
        Random random = world.getRandom();

        HighlightRenderer.tick(world, random);

        if (!cfg.enabled) {
            TRAILS.clear();
            return;
        }

        Set<Integer> alive = new HashSet<>();

        // 1. Zapisywanie trasy lecących pereł
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) continue;

            int id = pearl.getId();
            alive.add(id);
            Trail trail = TRAILS.computeIfAbsent(id, k -> new Trail());
            Vec3d pos = pearl.getPos();
            addSegment(trail, pos);
            trail.velocity = pearl.getVelocity();
        }

        int fadeTicks = Math.round(cfg.fadeSeconds * 20);

        // 2. Rysowanie smug, wykrywanie uderzeń, usuwanie starych smug
        Iterator<Map.Entry<Integer, Trail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Trail> entry = it.next();
            Trail trail = entry.getValue();

            if (!alive.contains(entry.getKey())) {
                if (trail.deadTicks == -1) {
                    trail.deadTicks = 0;
                    onImpact(world, random, trail, cfg);
                } else {
                    trail.deadTicks++;
                }
                if (trail.deadTicks >= fadeTicks) {
                    it.remove();
                    continue;
                }
            }

            float chance = cfg.density;
            if (trail.deadTicks >= 0 && fadeTicks > 0) {
                chance *= 1.0f - (float) trail.deadTicks / fadeTicks;
            }

            for (Vec3d p : trail.points) {
                if (random.nextFloat() < chance) {
                    spawnPortal(world, random, p, 0.1);
                }
            }
        }
    }

    private static void addSegment(Trail trail, Vec3d pos) {
        if (trail.last == null) {
            trail.points.add(pos);
        } else {
            double dist = trail.last.distanceTo(pos);
            if (dist > 0.01) {
                int steps = Math.max(1, (int) Math.ceil(dist / SPACING));
                for (int i = 1; i <= steps; i++) {
                    trail.points.add(trail.last.lerp(pos, (double) i / steps));
                }
            }
        }
        trail.last = pos;
        while (trail.points.size() > MAX_POINTS) {
            trail.points.remove(0);
        }
    }

    /** Perła zniknęła - szukamy bloku, w który uderzyła. */
    private static void onImpact(ClientWorld world, Random random, Trail trail, TrailConfig cfg) {
        if (trail.last == null) return;

        Vec3d dir = trail.velocity.lengthSquared() < 1.0E-4 ? new Vec3d(0, -1, 0) : trail.velocity;
        Vec3d end = trail.last.add(dir.multiply(2.0));
        BlockHitResult hit = world.raycast(new RaycastContext(
                trail.last, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, ShapeContext.absent()));

        Vec3d impact = trail.last;
        if (hit.getType() == HitResult.Type.BLOCK) {
            impact = hit.getPos();
            addSegment(trail, impact); // smuga dochodzi dokładnie do miejsca uderzenia
            if (cfg.highlight) {
                HighlightRenderer.add(hit.getBlockPos());
            }
        }

        // Wybuch cząsteczek jak przy teleporcie endermana
        for (int i = 0; i < cfg.burstParticles; i++) {
            spawnPortal(world, random, impact, 0.8);
        }
    }

    /** Ta sama cząsteczka i ten sam ruch, co przy teleportacji endermana. */
    public static void spawnPortal(ClientWorld world, Random random, Vec3d p, double spread) {
        world.addParticle(
                ParticleTypes.PORTAL,
                p.x + (random.nextDouble() - 0.5) * spread,
                p.y + (random.nextDouble() - 0.5) * spread,
                p.z + (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * 2.0,
                -random.nextDouble(),
                (random.nextDouble() - 0.5) * 2.0
        );
    }
}
