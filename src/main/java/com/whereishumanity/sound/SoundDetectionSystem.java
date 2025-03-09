package com.whereishumanity.sound;

import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.config.ModConfig;
import com.whereishumanity.entities.SmartZombieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Système central de détection et de propagation des sons
 * Cette classe gère la création, la propagation et la réaction aux sons par les zombies
 */
@Mod.EventBusSubscriber(modid = WhereIsHumanity.MOD_ID)
public class SoundDetectionSystem {

    // Cache des sons actifs par dimension
    private static final Map<String, List<SoundEvent>> ACTIVE_SOUNDS = new HashMap<>();
    
    // Seuil de distance pour détecter un joueur accroupi (en blocs)
    private static final double SNEAK_DETECTION_DISTANCE = 3.5;
    
    // Modificateurs pour la course
    private static final double SPRINT_SOUND_MULTIPLIER = 2.0;
    
    /**
     * Classe interne représentant un événement sonore dans le monde
     */
    public static class SoundEvent {
        private final BlockPos position;
        private final int soundLevel; // 1 = faible, 2 = moyen, 3 = fort
        private final Entity source;
        private int age;
        private final int maxAge;
        
        public SoundEvent(BlockPos position, int soundLevel, Entity source) {
            this.position = position;
            this.soundLevel = soundLevel;
            this.source = source;
            this.age = 0;
            
            // Durée de vie du son basée sur son niveau
            this.maxAge = switch(soundLevel) {
                case 1 -> 10; // Sons faibles disparaissent rapidement (0.5s)
                case 2 -> 20; // Sons moyens durent un peu plus (1s)
                case 3 -> 60; // Sons forts persistent plus longtemps (3s)
                default -> 10;
            };
        }
        
        public void tick() {
            this.age++;
        }
        
        public boolean isExpired() {
            return this.age >= this.maxAge;
        }
        
        public BlockPos getPosition() {
            return position;
        }
        
        public int getSoundLevel() {
            return soundLevel;
        }
        
        public Entity getSource() {
            return source;
        }
    }
    
    /**
     * Émet un son dans le monde
     * @param level Le niveau où le son est émis
     * @param pos La position du son
     * @param soundLevel Le niveau du son (1-3)
     * @param source L'entité source du son (peut être null)
     */
    public static void emitSound(Level level, BlockPos pos, int soundLevel, Entity source) {
        if (level.isClientSide) return; // Sons traités seulement côté serveur
        
        // Si la source est un joueur accroupi et que le son est faible, on réduit davantage le son
        if (source instanceof Player player && player.isShiftKeyDown() && soundLevel == 1) {
            // Les joueurs accroupis ne font presque pas de bruit pour les actions faibles
            return;
        }
        
        // Si la source est un joueur accroupi et que le son est moyen, on le réduit à faible
        if (source instanceof Player player && player.isShiftKeyDown() && soundLevel == 2) {
            soundLevel = 1;
        }
        
        // Si la source est un joueur qui court, on augmente le niveau du son
        if (source instanceof Player player && player.isSprinting()) {
            // Les joueurs qui courent font plus de bruit
            soundLevel = Math.min(3, soundLevel + 1);
        }
        
        String dimensionKey = getDimensionKey(level);
        SoundEvent soundEvent = new SoundEvent(pos, soundLevel, source);
        
        // Ajouter à la liste des sons actifs
        ACTIVE_SOUNDS.computeIfAbsent(dimensionKey, k -> new ArrayList<>()).add(soundEvent);
        
        // Notifier les zombies à proximité immédiatement
        notifyNearbyZombies(level, soundEvent);
        
        if (soundLevel >= 2) {
            WhereIsHumanity.LOGGER.debug("Son de niveau {} émis à {}", soundLevel, pos);
        }
    }
    
    /**
     * Notifie les zombies à proximité d'un son
     * @param level Le niveau où se trouvent les zombies
     * @param soundEvent L'événement sonore
     */
    private static void notifyNearbyZombies(Level level, SoundEvent soundEvent) {
        int radius = switch(soundEvent.getSoundLevel()) {
            case 1 -> ModConfig.COMMON.lowSoundDetectionRadius.get();
            case 2 -> ModConfig.COMMON.mediumSoundDetectionRadius.get();
            case 3 -> ModConfig.COMMON.loudSoundDetectionRadius.get();
            default -> 8;
        };
        
        // Si la source est un joueur qui court, augmenter le rayon de détection
        if (soundEvent.getSource() instanceof Player player && player.isSprinting()) {
            radius = (int)(radius * SPRINT_SOUND_MULTIPLIER);
        }
        
        // Récupérer la boîte englobante autour du son
        AABB detectionBox = new AABB(
                soundEvent.getPosition().getX() - radius, soundEvent.getPosition().getY() - radius, soundEvent.getPosition().getZ() - radius,
                soundEvent.getPosition().getX() + radius, soundEvent.getPosition().getY() + radius, soundEvent.getPosition().getZ() + radius
        );
        
        // Récupérer tous les zombies intelligents dans la zone
        List<SmartZombieEntity> zombies = level.getEntitiesOfClass(
            SmartZombieEntity.class,
            detectionBox
        );
        
        // Notifier tous les zombies intelligents dans la zone
        for (SmartZombieEntity zombie : zombies) {
            // Vérifier la ligne de vue si nécessaire pour les sons faibles
            if (soundEvent.getSoundLevel() == 1) {
                if (hasLineOfSight(level, zombie.blockPosition(), soundEvent.getPosition())) {
                    zombie.onSoundDetected(soundEvent.getPosition(), soundEvent.getSoundLevel());
                }
            } else {
                zombie.onSoundDetected(soundEvent.getPosition(), soundEvent.getSoundLevel());
            }
        }
    }
    
    /**
     * Vérifie s'il y a une ligne de vue directe entre deux positions
     * @param level Le niveau à vérifier
     * @param from Position de départ
     * @param to Position d'arrivée
     * @return true s'il y a une ligne de vue, false sinon
     */
    private static boolean hasLineOfSight(Level level, BlockPos from, BlockPos to) {
        Vec3 start = new Vec3(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5);
        Vec3 end = new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
        
        ClipContext context = new ClipContext(
            start, end, 
            ClipContext.Block.COLLIDER, 
            ClipContext.Fluid.NONE, 
            null
        );
        
        return level.clip(context).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
    
    /**
     * Retourne la clé de dimension pour le stockage des sons
     * @param level Le niveau concerné
     * @return Une chaîne représentant la dimension
     */
    private static String getDimensionKey(Level level) {
        return level.dimension().location().toString();
    }
    
    /**
     * Met à jour tous les sons actifs et supprime ceux expirés
     * @param level Le niveau à mettre à jour
     */
    public static void tickSounds(Level level) {
        if (level.isClientSide) return;
        
        String dimensionKey = getDimensionKey(level);
        List<SoundEvent> sounds = ACTIVE_SOUNDS.get(dimensionKey);
        
        if (sounds == null) return;
        
        Iterator<SoundEvent> iterator = sounds.iterator();
        while (iterator.hasNext()) {
            SoundEvent sound = iterator.next();
            sound.tick();
            
            if (sound.isExpired()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Vérifie si un joueur est visible pour un zombie par l'odeur (quand accroupi)
     * @param zombie Le zombie qui détecte
     * @param player Le joueur à détecter
     * @return true si le joueur est détectable par l'odeur
     */
    public static boolean canSmellPlayer(SmartZombieEntity zombie, Player player) {
        // Vérifier si le joueur est accroupi
        if (!player.isShiftKeyDown()) {
            return true; // Si le joueur n'est pas accroupi, il est toujours visible
        }
        
        // Calculer la distance entre le zombie et le joueur
        double distance = zombie.distanceTo(player);
        
        // Le joueur est détectable par l'odeur uniquement si il est très proche
        return distance <= SNEAK_DETECTION_DISTANCE;
    }
    
    // Événements qui génèrent des sons
    
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player) {
            emitSound(event.getEntity().level(), event.getEntity().blockPosition(), 2, event.getEntity());
        }
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Les sons varient selon le matériau
        Block block = event.getState().getBlock();
        int soundLevel;
        
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE || 
            block.getDescriptionId().contains("glass")) {
            soundLevel = 3; // Bris de verre = son fort
        } else if (block == Blocks.OAK_DOOR || block == Blocks.SPRUCE_DOOR || 
                  block == Blocks.BIRCH_DOOR || block == Blocks.JUNGLE_DOOR ||
                  block == Blocks.ACACIA_DOOR || block == Blocks.DARK_OAK_DOOR ||
                  block == Blocks.CRIMSON_DOOR || block == Blocks.WARPED_DOOR ||
                  block.getDescriptionId().contains("door")) {
            soundLevel = 2; // Portes en bois = son moyen
        } else if (block == Blocks.STONE || block == Blocks.COBBLESTONE || 
                   block.getDescriptionId().contains("stone")) {
            soundLevel = 2; // Pierre = son moyen
        } else {
            soundLevel = 1; // Autres = son faible
        }
        
        // Cast LevelAccessor to Level
        if (event.getLevel() instanceof Level level) {
            emitSound(level, event.getPos(), soundLevel, event.getPlayer());
        }
    }
    
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        // Utiliser un item fait un son faible
        emitSound(event.getLevel(), event.getPos(), 1, event.getEntity());
    }
    
    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        // Interagir avec un bloc fait un son faible à moyen selon le bloc
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        int soundLevel = 1;
        
        // Portes et coffres font plus de bruit
        if (block.getDescriptionId().contains("door") || 
            block.getDescriptionId().contains("chest") || 
            block.getDescriptionId().contains("gate")) {
            soundLevel = 2;
        }
        
        emitSound(event.getLevel(), event.getPos(), soundLevel, event.getEntity());
    }
    
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        // Les explosions créent des sons forts
        Vec3 pos = event.getExplosion().getPosition();
        BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        emitSound(event.getLevel(), blockPos, 3, null);
    }
    
    @SubscribeEvent
    public static void onWorldTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level() instanceof ServerLevel && event.getEntity().tickCount % 10 == 0) {
            tickSounds(event.getEntity().level());
        }
        
        // Détecter les joueurs qui courent
        if (event.getEntity() instanceof Player player) {
            if (player.isSprinting() && player.tickCount % 3 == 0) {
                // Courir fait un son moyen fréquent
                emitSound(player.level(), player.blockPosition(), 2, player);
            } else if (player.tickCount % 20 == 0 && !player.isShiftKeyDown()) {
                // Se déplacer normalement fait un son faible occasionnel
                emitSound(player.level(), player.blockPosition(), 1, player);
            }
        }
        
        // Détecter les attaques
        if (event.getEntity() instanceof Player player && player.getAttackAnim(0) > 0.0F && player.getAttackAnim(0) < 0.2F) {
            // Frapper fait un son moyen
            emitSound(player.level(), player.blockPosition(), 2, player);
        }
    }
}
