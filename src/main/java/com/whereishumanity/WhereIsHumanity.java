package com.whereishumanity;

import com.whereishumanity.biomes.BiomeRegistry;
import com.whereishumanity.commands.GenerateCommandRegistry;
import com.whereishumanity.commands.StructureCommandRegistry;
import com.whereishumanity.config.ModConfig;
import com.whereishumanity.entities.EntityRegistry;
import com.whereishumanity.worldgen.features.FeatureRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe principale du mod "Where is Humanity"
 * Ce mod ajoute des biomes de villes abandonnées et des zombies intelligents à Minecraft.
 */
@Mod(WhereIsHumanity.MOD_ID)
public class WhereIsHumanity {
    // Identifiant du mod
    public static final String MOD_ID = "whereishumanity";
    // Logger pour ce mod
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Constructeur de la classe principale du mod.
     * Enregistre les gestionnaires d'événements et initialise les systèmes du mod.
     */
    public WhereIsHumanity() {
        LOGGER.info("Initialisation du mod 'Where is Humanity' v0.2.0");
        
        // Obtenir le bus d'événements du mod
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Enregistrer la méthode de configuration pour le chargement du mod
        modEventBus.addListener(this::setup);
        
        // Enregistrer les systèmes du mod
        registerSystems(modEventBus);
        
        // Enregistrer la configuration
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
        
        // S'enregistrer pour les événements serveur et autres événements de jeu
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Enregistre tous les systèmes du mod sur le bus d'événements
     * @param eventBus Le bus d'événements du mod
     */
    private void registerSystems(IEventBus eventBus) {
        // Enregistrer les entités
        EntityRegistry.register(eventBus);
        
        // Enregistrer les biomes
        BiomeRegistry.register(eventBus);
        
        // Enregistrer les features de génération du monde
        FeatureRegistry.register(eventBus);
        
        LOGGER.info("Systèmes du mod enregistrés");
    }

    /**
     * Méthode de configuration commune, appelée pendant l'initialisation du mod.
     * C'est ici que nous enregistrerons les biomes, les structures et les comportements des zombies.
     *
     * @param event L'événement déclenché pendant l'initialisation
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Where is Humanity: Configuration des systèmes du mod");
        
        // Enregistrer les processeurs d'événements spécifiques
        event.enqueueWork(() -> {
            // Configuration des biomes dans le système de génération de monde
            // Cette partie sera développée ultérieurement
            
            LOGGER.info("Configuration terminée");
        });
    }
    
    /**
     * Gestionnaire d'événement pour l'enregistrement des commandes
     * Appelé automatiquement par Forge lors de l'enregistrement des commandes
     * 
     * @param event L'événement d'enregistrement des commandes
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Enregistrement des commandes du mod");
        
        // Utiliser le registre central pour enregistrer toutes les commandes de structure
        StructureCommandRegistry.register(event.getDispatcher());
        
        // Utiliser le registre pour les commandes de génération
        GenerateCommandRegistry.register(event.getDispatcher());
    }
}
