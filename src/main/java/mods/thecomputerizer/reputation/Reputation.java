package mods.thecomputerizer.reputation;

import mods.thecomputerizer.reputation.api.capability.IReputation;
import mods.thecomputerizer.reputation.common.ModDefinitions;
import mods.thecomputerizer.reputation.common.ai.ReputationMemoryModule;
import mods.thecomputerizer.reputation.common.ai.ReputationSenorType;
import mods.thecomputerizer.reputation.common.network.PacketHandler;
import mods.thecomputerizer.reputation.config.ClientConfigHandler;
import mods.thecomputerizer.reputation.config.FactionParser;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(value = ModDefinitions.MODID)
@Mod.EventBusSubscriber(modid = ModDefinitions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Reputation {

	private static final Logger logger = LogManager.getLogger(ModDefinitions.NAME);

	public static final Tag.Named<?> fleeTag = EntityTypeTags.bind(ModDefinitions.MODID+":flee");
	public static final Tag.Named<?> passiveNeutralTag = EntityTypeTags.bind(ModDefinitions.MODID+":passive_neutral");
	public static final Tag.Named<?> passiveGoodTag = EntityTypeTags.bind(ModDefinitions.MODID+":passive_good");
	public static final Tag.Named<?> hostileTag = EntityTypeTags.bind(ModDefinitions.MODID+":hostile");

	public Reputation() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonsetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCapabilities);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfigHandler.CONFIG, "reputation/client.toml");
		MinecraftForge.EVENT_BUS.register(this);
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ReputationMemoryModule.MEMORY_MODULES.register(eventBus);
		ReputationSenorType.SENSOR_TYPES.register(eventBus);
	}

	//These should use the mod bus
	public void clientSetup(FMLClientSetupEvent event){

	}

	public void commonsetup(FMLCommonSetupEvent event){
		FactionParser.readFactionsFromConfig();
		PacketHandler.initPackets();
	}

	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.register(IReputation.class);
	}

	public static void logInfo(Object message) {
		logger.info(message);
	}

	public static void logError(Object message, Exception e) {
		logger.error(message);
		e.printStackTrace();
	}
}
