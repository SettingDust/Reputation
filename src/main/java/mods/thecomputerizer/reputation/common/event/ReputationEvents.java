package mods.thecomputerizer.reputation.common.event;

import mods.thecomputerizer.reputation.Reputation;
import mods.thecomputerizer.reputation.api.Faction;
import mods.thecomputerizer.reputation.api.ReputationHandler;
import mods.thecomputerizer.reputation.common.ModDefinitions;
import mods.thecomputerizer.reputation.common.ai.ReputationMemoryModule;
import mods.thecomputerizer.reputation.common.ai.ReputationSenorType;
import mods.thecomputerizer.reputation.common.capability.ReputationProvider;
import mods.thecomputerizer.reputation.common.command.AddReputationCommand;
import mods.thecomputerizer.reputation.common.command.SetReputationCommand;
import mods.thecomputerizer.reputation.common.network.PacketHandler;
import mods.thecomputerizer.reputation.common.network.SyncFactionsMessage;
import mods.thecomputerizer.reputation.config.ClientConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkDirection;

import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = ModDefinitions.MODID)
public class ReputationEvents {

	//I was getting annoyed with this method not working in the base class
	@SubscribeEvent
	public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
		Entity entity = event.getObject();
		if (entity instanceof Player &!(entity instanceof FakePlayer)) {
			event.addCapability(ModDefinitions.getResource("reputation"), new ReputationProvider());
		}
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event){
		SetReputationCommand.register(event.getDispatcher());
		AddReputationCommand.register(event.getDispatcher());
	}

	//activate when a player joins a server
	@SubscribeEvent
	public static void onPlayerJoin(PlayerLoggedInEvent event) {
		Player player = event.getPlayer();
		if (!player.level.isClientSide) {
			PacketHandler.NETWORK_INSTANCE.sendTo(new SyncFactionsMessage(ReputationHandler.getFactions()),
					((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
			for(Faction faction : ReputationHandler.getFactions()) {
				ReputationHandler.changeReputation(player, faction, 0);
			}
		}

	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntityLiving();
		Level level = entity.level;
		if(!level.isClientSide) {
			DamageSource source = event.getSource();
			Player player = null;
			if (source.getEntity() instanceof Player) player = (Player) source.getEntity();
			else if(source.getDirectEntity() instanceof Player) player = (Player) source.getDirectEntity();
			if (player != null) {
				for(Faction faction : ReputationHandler.getEntityFactions(entity)) {
					ReputationHandler.changeReputation(player, faction, -1);
				}
			}
		}
	}

	//attach reputation base goals to ai upon spawning
	@SubscribeEvent
	public static void onJoin(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof Villager) {
			Villager villager = (Villager)event.getEntity();
			Brain<Villager> brain = villager.getBrain();
			Map<SensorType<? extends Sensor<? extends LivingEntity>>, Sensor<? extends LivingEntity>> sensors = ObfuscationReflectionHelper.getPrivateValue(Brain.class, brain, "f_21844_");
			sensors.put(ReputationSenorType.NEAREST_PLAYER_REPUTATION.get(), ReputationSenorType.NEAREST_PLAYER_REPUTATION.get().create());
			Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = ObfuscationReflectionHelper.getPrivateValue(Brain.class, brain, "f_21843_");
			memories.put(ReputationMemoryModule.NEAREST_PLAYER_BAD_REPUTATION.get(), Optional.empty());
			memories.put(ReputationMemoryModule.NEAREST_PLAYER_NEUTRAL_REPUTATION.get(), Optional.empty());
			memories.put(ReputationMemoryModule.NEAREST_PLAYER_GOOD_REPUTATION.get(), Optional.empty());
		}
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.Clone event) {
		Reputation.logInfo("respawn log");
		Player original = event.getOriginal();
		Player respawned = event.getPlayer();
		original.reviveCaps();
		for(Faction f : ReputationHandler.getFactions()) {
			respawned.getCapability(ReputationHandler.REPUTATION_CAPABILITY).orElseThrow(RuntimeException::new)
					.setReputation(respawned,f,ReputationHandler.getReputation(original,f));
		}
		original.invalidateCaps();
	}

	//prints the player's reputation for each faction to the screen
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void debugInfo(RenderGameOverlayEvent.Text e) {
		if(ClientConfigHandler.debug.get() && Minecraft.getInstance().player!=null) {
			for (Faction f : ReputationHandler.getFactions()) {
				e.getLeft().add("Reputation for the "+f.getName()+" faction: "+ ReputationHandler.getReputation(Minecraft.getInstance().player,f));
			}
		}
	}
}
