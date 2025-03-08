package com.whereishumanity;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for "Where is Humanity"
 * This mod adds abandoned city biomes and intelligent zombies to Minecraft.
 */
@Mod(WhereIsHumanity.MOD_ID)
public class WhereIsHumanity {
    // Mod identifier
    public static final String MOD_ID = "whereishumanity";
    // Logger for this mod
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Constructor for the main mod class.
     * Registers event handlers and initializes the mod systems.
     */
    public WhereIsHumanity() {
        LOGGER.info("Initializing 'Where is Humanity' mod");
        
        // Get the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register the setup method for mod loading
        modEventBus.addListener(this::setup);
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Common setup method, called during mod initialization.
     * This is where we'll register biomes, structures, and zombie behaviors.
     *
     * @param event The event triggered during initialization
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Where is Humanity: Setting up mod systems");
        
        // We'll add system initialization here as we develop the mod
    }
}