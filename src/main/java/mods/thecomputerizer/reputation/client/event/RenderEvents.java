package mods.thecomputerizer.reputation.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import mods.thecomputerizer.reputation.Reputation;
import mods.thecomputerizer.reputation.api.Faction;
import mods.thecomputerizer.reputation.api.ReputationHandler;
import mods.thecomputerizer.reputation.common.ModDefinitions;
import mods.thecomputerizer.reputation.common.ai.ReputationMemoryModule;
import mods.thecomputerizer.reputation.config.ClientConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModDefinitions.MODID)
@OnlyIn(value = Dist.CLIENT)
public class RenderEvents {

    public static final int RENDER_DISTANCE = 64;
    public static final ResourceLocation BAD_REPUTATION = new ResourceLocation(ModDefinitions.MODID,"textures/icons/minus.png");
    public static final ResourceLocation GOOD_REPUTATION = new ResourceLocation(ModDefinitions.MODID,"textures/icons/plus.png");
    public static final ResourceLocation FLEE = new ResourceLocation(ModDefinitions.MODID,"textures/icons/flee.png");
    public static HashMap<ResourceLocation, Faction> CLIENT_FACTIONS = new HashMap<>();

    public static ArrayList<UUID> fleeingMobs = new ArrayList<>();

    @SubscribeEvent
    public static void renderName(RenderNameplateEvent e) {
        Player player = Minecraft.getInstance().player;
        if(player!=null && e.getEntity() instanceof LivingEntity living) {
            if(living.distanceTo(player)<=RENDER_DISTANCE) {
                if (!fleeingMobs.contains(living.getUUID())) {
                    int offset = 0;
                    ResourceLocation icon = null;
                    for (Faction f : ReputationHandler.getEntityFactions(living)) {
                        if (ReputationHandler.isGoodReputation(player, f)) icon = GOOD_REPUTATION;
                        else if (ReputationHandler.isBadReputation(player, f)) icon = BAD_REPUTATION;
                        if (icon != null) {
                            double xTranslate = (living.getBbWidth() / 2f) - (living.getBbWidth() * 0.0125f * 1.1 * offset);
                            render(e,living,player,icon,xTranslate,0.25d,0.25f);
                        }
                        offset++;
                    }
                } else render(e,living,player,FLEE,0d,0.5d,1f);
            }
        }
    }

    private static void render(RenderNameplateEvent e, LivingEntity living, Player player, ResourceLocation icon, double xTranslate, double yTranslate, float scale) {
        float opacity = Mth.clamp(((float) RENDER_DISTANCE - living.distanceTo(player)) / (float) RENDER_DISTANCE, 0.0f, 1f);
        PoseStack poseStack = e.getPoseStack();
        EntityRenderer<?> renderer = e.getEntityRenderer();
        poseStack.pushPose();
        poseStack.translate(xTranslate, living.getBbHeight() + yTranslate, 0d);
        poseStack.mulPose(renderer.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025f*scale, -0.025f*scale, 0.025f*scale);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, opacity);
        poseStack.pushPose();
        poseStack.scale(1f, 1f, 1f);
        Matrix4f matrix = poseStack.last().pose();
        Minecraft.getInstance().getTextureManager().getTexture(icon).setFilter(false, false);
        RenderSystem.setShaderTexture(0, icon);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix, 0, 16, 0).uv(0, 1).endVertex();
        bufferbuilder.vertex(matrix, 16, 16, 0).uv(1, 1).endVertex();
        bufferbuilder.vertex(matrix, 16, 0, 0).uv(1, 0).endVertex();
        bufferbuilder.vertex(matrix, 0, 0, 0).uv(0, 0).endVertex();
        bufferbuilder.end();
        BufferUploader.end(bufferbuilder);
        poseStack.popPose();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        poseStack.popPose();
    }

    //prints the player's reputation for each faction to the screen
    @SubscribeEvent
    public static void debugInfo(RenderGameOverlayEvent.Text e) {
        if(ClientConfigHandler.debug.get() && Minecraft.getInstance().player!=null) {
            for (Faction f : CLIENT_FACTIONS.values()) {
                e.getLeft().add("Reputation for the "+f.getName()+" faction: "+ ReputationHandler.getReputation(Minecraft.getInstance().player,f));
            }
        }
    }
}
