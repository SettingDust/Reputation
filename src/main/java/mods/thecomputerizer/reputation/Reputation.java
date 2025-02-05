package mods.thecomputerizer.reputation;

import mods.thecomputerizer.reputation.api.capability.IPlacedContainer;
import mods.thecomputerizer.reputation.api.capability.IPlayerFaction;
import mods.thecomputerizer.reputation.api.capability.IReputation;
import mods.thecomputerizer.reputation.client.ClientTrackers;
import mods.thecomputerizer.reputation.client.event.RenderEvents;
import mods.thecomputerizer.reputation.client.event.WorldEvents;
import mods.thecomputerizer.reputation.client.render.RenderIcon;
import mods.thecomputerizer.reputation.common.ModDefinitions;
import mods.thecomputerizer.reputation.common.command.ReputationFactionArgument;
import mods.thecomputerizer.reputation.common.network.PacketHandler;
import mods.thecomputerizer.reputation.common.registration.RegistryHandler;
import mods.thecomputerizer.reputation.config.ClientConfigHandler;
import mods.thecomputerizer.reputation.util.FactionListener;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod(value = ModDefinitions.MODID)
@Mod.EventBusSubscriber(modid = ModDefinitions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Reputation {

	private static final Logger LOGGER = LogManager.getLogger(ModDefinitions.NAME);

	public Reputation() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCapabilities);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfigHandler.CONFIG, "reputation/client.toml");
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::reloadData);
		RegistryHandler.registerCommonObjects(FMLJavaModLoadingContext.get().getModEventBus());
	}

	public void commonSetup(FMLCommonSetupEvent event) {
		PacketHandler.initPackets();
		ArgumentTypes.register("reputation:faction_argument", ReputationFactionArgument.class, new EmptyArgumentSerializer<>(ReputationFactionArgument::id));
	}

	public void clientSetup(FMLClientSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(RenderIcon.class);
		MinecraftForge.EVENT_BUS.register(ClientTrackers.class);
		MinecraftForge.EVENT_BUS.register(RenderEvents.class);
		MinecraftForge.EVENT_BUS.register(WorldEvents.class);
	}

	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.register(IReputation.class);
		event.register(IPlacedContainer.class);
		event.register(IPlayerFaction.class);
	}

	@SubscribeEvent
	public void reloadData(AddReloadListenerEvent event) {
		event.addListener(new FactionListener());
	}

	public static void logInfo(String message, Object... vars) {
		LOGGER.info(message, vars);
	}

	public static void logError(String message, Object... vars) {
		LOGGER.error(message,vars);
	}

	public static void logStringCollection(Level logLevel, String initialMessage, Collection<String> collection, int numPerLine) {
		StringBuilder builder = new StringBuilder();
		builder.append(initialMessage).append(": ");
		int i = 0;
		for(String element : collection) {
			builder.append("[").append(i).append("] ").append(element);
			if(i<collection.size()-1) {
				if((i+1)%numPerLine==0) builder.append("\n");
				else builder.append(" ");
				i++;
			}
		} LOGGER.log(logLevel,builder.toString());
	}
}
