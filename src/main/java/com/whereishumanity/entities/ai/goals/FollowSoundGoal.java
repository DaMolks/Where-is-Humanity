package com.whereishumanity.entities.ai.goals;

import com.whereishumanity.entities.SmartZombieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import java.util.EnumSet;

/**
 * Objectif d'IA permettant aux zombies de suivre les sons détectés
 */
public class FollowSoundGoal extends Goal {
    
    private final SmartZombieEntity zombie;
    private final double speedModifier;
    private BlockPos soundSource;
    private int timeToRecalcPath;
    private final int recalcDelay = 5; // Recalculer le chemin toutes les 5 ticks (0.25s)
    
    public FollowSoundGoal(SmartZombieEntity zombie, double speedModifier) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    
    @Override
    public boolean canUse() {
        // Utiliser cette IA seulement si on a une source sonore et pas de cible visuelle
        return zombie.getTargetSound() != null && 
               (zombie.getTarget() == null || !zombie.getTarget().isAlive());
    }
    
    @Override
    public void start() {
        this.soundSource = zombie.getTargetSound();
        this.timeToRecalcPath = 0;
        
        // Commencer à se déplacer vers la source du son
        PathNavigation navigation = zombie.getNavigation();
        navigation.moveTo(
            soundSource.getX() + 0.5, 
            soundSource.getY() + 0.5, 
            soundSource.getZ() + 0.5, 
            speedModifier
        );
    }
    
    @Override
    public void stop() {
        // Si la source sonore disparaît ou si on trouve une cible visuelle, arrêter de suivre le son
        this.soundSource = null;
        zombie.getNavigation().stop();
    }
    
    @Override
    public boolean canContinueToUse() {
        // Continuer tant qu'on a une source sonore et pas de cible visuelle
        return zombie.getTargetSound() != null && 
               (zombie.getTarget() == null || !zombie.getTarget().isAlive()) &&
               !zombie.getNavigation().isDone();
    }
    
    @Override
    public void tick() {
        BlockPos targetSound = zombie.getTargetSound();
        if (targetSound == null) return;
        
        // Si la source du son a changé, mettre à jour la destination
        if (!targetSound.equals(this.soundSource)) {
            this.soundSource = targetSound;
            this.timeToRecalcPath = 0;
        }
        
        // Recalculer le chemin périodiquement
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = recalcDelay;
            
            // Vérifier si on est proche de la source du son
            double distanceSq = zombie.blockPosition().distSqr(soundSource);
            if (distanceSq <= 4.0) { // À environ 2 blocs de distance
                // Arrivée à la source du son, arrêter la navigation
                zombie.getNavigation().stop();
                
                // Regarder autour brièvement (comportement de recherche)
                zombie.getLookControl().setLookAt(
                    soundSource.getX() + 0.5, 
                    soundSource.getY() + 0.5, 
                    soundSource.getZ() + 0.5
                );
            } else {
                // Continuer à se déplacer vers la source du son
                zombie.getNavigation().moveTo(
                    soundSource.getX() + 0.5, 
                    soundSource.getY() + 0.5, 
                    soundSource.getZ() + 0.5, 
                    speedModifier
                );
            }
        }
        
        // Regarder vers la destination de temps en temps
        if (zombie.getRandom().nextInt(20) == 0) {
            zombie.getLookControl().setLookAt(
                soundSource.getX() + 0.5, 
                soundSource.getY() + 0.5, 
                soundSource.getZ() + 0.5
            );
        }
    }
}