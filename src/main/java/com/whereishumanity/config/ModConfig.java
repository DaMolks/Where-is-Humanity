package com.whereishumanity.config;

import com.whereishumanity.WhereIsHumanity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration principale du mod "Where is Humanity"
 * Cette classe gère les paramètres configurables du mod
 */
@Mod.EventBusSubscriber(modid = WhereIsHumanity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {

    public static class Common {
        // Configuration des biomes
        public final ForgeConfigSpec.DoubleValue metropolisSpawnWeight;
        public final ForgeConfigSpec.DoubleValue citySpawnWeight;
        public final ForgeConfigSpec.DoubleValue villageSpawnWeight;
        
        // Configuration des zombies
        public final ForgeConfigSpec.BooleanValue zombiesCanBreakGlass;
        public final ForgeConfigSpec.BooleanValue zombiesCanBreakWoodenDoors;
        public final ForgeConfigSpec.BooleanValue brutesCanBreakIronDoors;
        public final ForgeConfigSpec.IntValue zombieAlertRadius;
        
        // Configuration du système sonore
        public final ForgeConfigSpec.IntValue lowSoundDetectionRadius;
        public final ForgeConfigSpec.IntValue mediumSoundDetectionRadius;
        public final ForgeConfigSpec.IntValue loudSoundDetectionRadius;
        
        // Configuration de la difficulté
        public final ForgeConfigSpec.IntValue difficultyProgressionRate; // en jours de jeu

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuration de Where is Humanity")
                   .push("general");
            
            builder.comment("Configuration des biomes")
                   .push("biomes");
            
            metropolisSpawnWeight = builder
                    .comment("Poids d'apparition des métropoles")
                    .defineInRange("metropolisSpawnWeight", 5.0, 0.0, 100.0);
            
            citySpawnWeight = builder
                    .comment("Poids d'apparition des villes moyennes")
                    .defineInRange("citySpawnWeight", 10.0, 0.0, 100.0);
            
            villageSpawnWeight = builder
                    .comment("Poids d'apparition des villages")
                    .defineInRange("villageSpawnWeight", 15.0, 0.0, 100.0);
            
            builder.pop();
            
            builder.comment("Configuration des zombies")
                   .push("zombies");
            
            zombiesCanBreakGlass = builder
                    .comment("Les zombies peuvent-ils briser les vitres?")
                    .define("zombiesCanBreakGlass", true);
            
            zombiesCanBreakWoodenDoors = builder
                    .comment("Les zombies peuvent-ils briser les portes en bois?")
                    .define("zombiesCanBreakWoodenDoors", true);
            
            brutesCanBreakIronDoors = builder
                    .comment("Les zombies brutes peuvent-ils briser les portes en fer?")
                    .define("brutesCanBreakIronDoors", true);
            
            zombieAlertRadius = builder
                    .comment("Rayon (en blocs) dans lequel un zombie peut alerter d'autres zombies")
                    .defineInRange("zombieAlertRadius", 32, 8, 128);
            
            builder.pop();
            
            builder.comment("Configuration du système sonore")
                   .push("sound");
            
            lowSoundDetectionRadius = builder
                    .comment("Rayon de détection des sons faibles (en blocs)")
                    .defineInRange("lowSoundDetectionRadius", 8, 1, 64);
            
            mediumSoundDetectionRadius = builder
                    .comment("Rayon de détection des sons moyens (en blocs)")
                    .defineInRange("mediumSoundDetectionRadius", 16, 1, 128);
            
            loudSoundDetectionRadius = builder
                    .comment("Rayon de détection des sons forts (en blocs)")
                    .defineInRange("loudSoundDetectionRadius", 32, 1, 256);
            
            builder.pop();
            
            builder.comment("Configuration de la difficulté")
                   .push("difficulty");
            
            difficultyProgressionRate = builder
                    .comment("Nombre de jours de jeu avant que la difficulté n'augmente d'un niveau")
                    .defineInRange("difficultyProgressionRate", 3, 1, 30);
            
            builder.pop();
            builder.pop(); // general
        }
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        WhereIsHumanity.LOGGER.debug("Configuration chargée");
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        WhereIsHumanity.LOGGER.debug("Configuration rechargée");
    }
}