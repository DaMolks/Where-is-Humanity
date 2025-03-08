package com.whereishumanity.entities.ai.goals;

import com.whereishumanity.config.ModConfig;
import com.whereishumanity.entities.BruteZombieEntity;
import com.whereishumanity.entities.SmartZombieEntity;
import com.whereishumanity.sound.SoundDetectionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Objectif d'IA permettant aux zombies de briser des blocs spécifiques
 * pour atteindre leur cible (portes, fenêtres, etc.)
 */
public class BreakBlocksGoal extends Goal {
    
    private final SmartZombieEntity zombie;
    private final float breakChance;
    private BlockPos targetPos = null;
    private int breakingTime = 0;
    private int breakingTimeMax = 0;
    private int lastBreakProgress = -1;
    
    public BreakBlocksGoal(SmartZombieEntity zombie, float breakChance) {
        this.zombie = zombie;
        this.breakChance = breakChance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        if (!zombie.isAlerted() || zombie.getTarget() == null) {
            return false;
        }
        
        // Vérifier si un bloc bloque le chemin vers la cible
        if (!isBlockingPathToTarget()) {
            return false;
        }
        
        // Trouver le bloc à casser
        BlockPos blockingPos = findBlockingBlock();
        if (blockingPos == null) {
            return false;
        }
        
        // Vérifier si le bloc peut être cassé
        if (!canBreakBlock(zombie.level().getBlockState(blockingPos))) {
            return false;
        }
        
        this.targetPos = blockingPos;
        return true;
    }
    
    @Override
    public void start() {
        this.breakingTime = 0;
        
        // Déterminer le temps nécessaire pour casser le bloc
        BlockState blockState = zombie.level().getBlockState(targetPos);
        this.breakingTimeMax = calculateBreakTime(blockState);
    }
    
    @Override
    public void stop() {
        this.targetPos = null;
        this.breakingTime = 0;
        
        // Arrêter l'animation de cassage
        zombie.level().destroyBlockProgress(zombie.getId(), targetPos, -1);
    }
    
    @Override
    public boolean canContinueToUse() {
        return targetPos != null 
                && zombie.isAlerted() 
                && zombie.getTarget() != null 
                && breakingTime < breakingTimeMax 
                && canBreakBlock(zombie.level().getBlockState(targetPos));
    }
    
    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null || targetPos == null) return;
        
        // Regarder vers le bloc
        double d0 = targetPos.getX() + 0.5 - zombie.getX();
        double d1 = targetPos.getY() + 0.5 - zombie.getEyeY();
        double d2 = targetPos.getZ() + 0.5 - zombie.getZ();
        zombie.getLookControl().setLookAt(
                zombie.getX() + d0, 
                zombie.getEyeY() + d1, 
                zombie.getZ() + d2);
        
        // Incrémenter le temps de cassage
        breakingTime++;
        
        // Jouer des sons de cassage
        if (breakingTime % 10 == 0) {
            zombie.level().playSound(null, targetPos, 
                    zombie.level().getBlockState(targetPos).getSoundType().getHitSound(), 
                    SoundSource.HOSTILE, 0.5F, 0.8F + zombie.getRandom().nextFloat() * 0.4F);
        }
        
        // Mise à jour visuelle du progrès de cassage
        int progress = (int) ((float) breakingTime / (float) breakingTimeMax * 10.0F);
        if (progress != lastBreakProgress) {
            zombie.level().destroyBlockProgress(zombie.getId(), targetPos, progress);
            lastBreakProgress = progress;
        }
        
        // Si le bloc est cassé
        if (breakingTime >= breakingTimeMax) {
            // Casser le bloc
            Level level = zombie.level();
            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();
            
            level.destroyBlock(targetPos, false);
            
            // Jouer un son approprié
            if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
                level.playSound(null, targetPos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                
                // Le bris de verre émet un son fort qui peut attirer plus de zombies
                SoundDetectionSystem.emitSound(level, targetPos, 3, zombie);
            } else if (block instanceof DoorBlock) {
                level.playSound(null, targetPos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
                
                // Casser une porte émet un son moyen
                SoundDetectionSystem.emitSound(level, targetPos, 2, zombie);
            }
            
            stop();
        }
    }
    
    /**
     * Vérifie si le chemin vers la cible est bloqué
     * @return true si le chemin est bloqué, false sinon
     */
    private boolean isBlockingPathToTarget() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return false;
        
        PathNavigation nav = zombie.getNavigation();
        Path path = nav.getPath();
        
        // Si pas de chemin ou si le zombie est déjà proche
        if (path == null || zombie.distanceTo(target) < 2.0) {
            return false;
        }
        
        // Vérifier si le chemin est bloqué
        return !path.canReach();
    }
    
    /**
     * Cherche le bloc qui bloque le chemin vers la cible
     * @return La position du bloc ou null si aucun trouvé
     */
    private BlockPos findBlockingBlock() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return null;
        
        // Direction vers la cible
        Vec3 direction = target.position().subtract(zombie.position()).normalize();
        
        // Vérifier les blocs devant le zombie
        for (int i = 1; i <= 3; i++) {
            BlockPos checkPos = new BlockPos(
                    (int) (zombie.getX() + direction.x * i),
                    (int) (zombie.getY() + direction.y * i),
                    (int) (zombie.getZ() + direction.z * i));
            
            // Vérifier aussi les portes et fenêtres au-dessus
            for (int y = 0; y <= 1; y++) {
                BlockPos pos = checkPos.above(y);
                BlockState state = zombie.level().getBlockState(pos);
                
                if (canBreakBlock(state)) {
                    return pos;
                }
            }
        }
        
        // Vérifier spécifiquement les portes/fenêtres autour du zombie
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos checkPos = zombie.blockPosition().relative(dir);
            BlockState state = zombie.level().getBlockState(checkPos);
            
            if (canBreakBlock(state)) {
                return checkPos;
            }
            
            // Vérifier aussi au-dessus (pour les portes/fenêtres de 2 blocs de haut)
            BlockPos checkPosAbove = checkPos.above();
            BlockState stateAbove = zombie.level().getBlockState(checkPosAbove);
            
            if (canBreakBlock(stateAbove)) {
                return checkPosAbove;
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie si le zombie peut casser ce type de bloc
     * @param state L'état du bloc à vérifier
     * @return true si le bloc peut être cassé, false sinon
     */
    private boolean canBreakBlock(BlockState state) {
        Block block = state.getBlock();
        
        // Tous les zombies peuvent casser les vitres
        if ((block == Blocks.GLASS || block == Blocks.GLASS_PANE) && ModConfig.COMMON.zombiesCanBreakGlass.get()) {
            return true;
        }
        
        // Tous les zombies peuvent casser les portes en bois
        if (state.is(BlockTags.WOODEN_DOORS) && ModConfig.COMMON.zombiesCanBreakWoodenDoors.get()) {
            return true;
        }
        
        // Seules les brutes peuvent casser les portes en fer
        if (block == Blocks.IRON_DOOR && zombie instanceof BruteZombieEntity && ModConfig.COMMON.brutesCanBreakIronDoors.get()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Calcule le temps nécessaire pour casser un bloc
     * @param state L'état du bloc
     * @return Le nombre de ticks nécessaires
     */
    private int calculateBreakTime(BlockState state) {
        Block block = state.getBlock();
        
        // Temps de base en fonction du type de bloc
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
            return 60; // 3 secondes pour casser une vitre
        } else if (state.is(BlockTags.WOODEN_DOORS)) {
            return 100; // 5 secondes pour casser une porte en bois
        } else if (block == Blocks.IRON_DOOR) {
            return 200; // 10 secondes pour casser une porte en fer (uniquement pour les brutes)
        }
        
        // Si le zombie est une brute, il casse plus vite
        if (zombie instanceof BruteZombieEntity) {
            return 80; // Temps réduit pour les brutes
        }
        
        return 120; // Temps par défaut
    }
}