package pl.pearltrail;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Pulsująca ramka wokół bloku, w który uderzyła perła. */
public class HighlightRenderer {

    private record Highlight(BlockPos pos, int startTick) {}

    private static final List<Highlight> HIGHLIGHTS = new ArrayList<>();

    public static void add(BlockPos pos) {
        HIGHLIGHTS.removeIf(h -> h.pos.equals(pos));
        HIGHLIGHTS.add(new Highlight(pos.toImmutable(), TrailManager.clientTicks));
    }

    public static void clear() {
        HIGHLIGHTS.clear();
    }

    private static int durationTicks() {
        return Math.max(1, Math.round(TrailConfig.INSTANCE.highlightSeconds * 20));
    }

    /** Usuwa wygasłe podświetlenia i dodaje cząsteczki unoszące się z bloku. */
    public static void tick(ClientWorld world, Random random) {
        int duration = durationTicks();
        Iterator<Highlight> it = HIGHLIGHTS.iterator();
        while (it.hasNext()) {
            Highlight h = it.next();
            if (TrailManager.clientTicks - h.startTick > duration || !TrailConfig.INSTANCE.highlight) {
                it.remove();
                continue;
            }
            if (random.nextInt(3) == 0) {
                double x = h.pos.getX() + random.nextDouble();
                double y = h.pos.getY() + 1.05;
                double z = h.pos.getZ() + random.nextDouble();
                world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.03, 0);
            }
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (HIGHLIGHTS.isEmpty()) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        ClientWorld world = ctx.world();
        if (matrices == null || consumers == null || world == null) return;

        Vec3d cam = ctx.camera().getPos();
        float tickDelta = ctx.tickCounter().getTickDelta(false);
        int duration = durationTicks();

        int rgb = TrailConfig.INSTANCE.highlightColor.rgb;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        for (Highlight h : HIGHLIGHTS) {
            float age = TrailManager.clientTicks - h.startTick + tickDelta;
            float pulse = 0.5f + 0.5f * MathHelper.sin(age * 0.3f);
            // w ostatniej sekundzie ramka gaśnie
            float fadeOut = MathHelper.clamp((duration - age) / 20f, 0f, 1f);
            float alpha = (0.55f + 0.45f * pulse) * fadeOut;

            VoxelShape shape = world.getBlockState(h.pos).getOutlineShape(world, h.pos);
            Box box = shape.isEmpty() ? new Box(h.pos) : shape.getBoundingBox().offset(h.pos);

            // dwie ramki: stała i "oddychająca" na zewnątrz
            drawBox(matrices.peek(), lines, box.expand(0.004), r, g, b, alpha);
            drawBox(matrices.peek(), lines, box.expand(0.02 + 0.06 * pulse), r, g, b, alpha * 0.5f);
        }

        matrices.pop();

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void drawBox(MatrixStack.Entry entry, VertexConsumer vc, Box b,
                                float r, float g, float bl, float a) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        // dół
        line(entry, vc, x1, y1, z1, x2, y1, z1, r, g, bl, a);
        line(entry, vc, x2, y1, z1, x2, y1, z2, r, g, bl, a);
        line(entry, vc, x2, y1, z2, x1, y1, z2, r, g, bl, a);
        line(entry, vc, x1, y1, z2, x1, y1, z1, r, g, bl, a);
        // góra
        line(entry, vc, x1, y2, z1, x2, y2, z1, r, g, bl, a);
        line(entry, vc, x2, y2, z1, x2, y2, z2, r, g, bl, a);
        line(entry, vc, x2, y2, z2, x1, y2, z2, r, g, bl, a);
        line(entry, vc, x1, y2, z2, x1, y2, z1, r, g, bl, a);
        // pionowe krawędzie
        line(entry, vc, x1, y1, z1, x1, y2, z1, r, g, bl, a);
        line(entry, vc, x2, y1, z1, x2, y2, z1, r, g, bl, a);
        line(entry, vc, x2, y1, z2, x2, y2, z2, r, g, bl, a);
        line(entry, vc, x1, y1, z2, x1, y2, z2, r, g, bl, a);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer vc,
                             float xa, float ya, float za, float xb, float yb, float zb,
                             float r, float g, float b, float a) {
        float nx = xb - xa, ny = yb - ya, nz = zb - za;
        float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;
        Matrix4f m = entry.getPositionMatrix();
        vc.vertex(m, xa, ya, za).color(r, g, b, a).normal(entry, nx, ny, nz);
        vc.vertex(m, xb, yb, zb).color(r, g, b, a).normal(entry, nx, ny, nz);
    }
}
