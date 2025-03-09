package com.whereishumanity.entities.ai;

import com.whereishumanity.entities.SmartZombieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal pour faire en sorte que les zombies aillent enquêter sur les sons qu'ils entendent
 */
public class ZombieInvestigateSoundGoal extends Goal {
    private final SmartZombieEntity zombie;
    private final double speedModifier;
    private Path path;
    private BlockPos targetPos;
    private int timeAtTarget;
    private int searchDelay;

    /**
     * Constructeur
     * @param zombie Le zombie qui utilise ce goal
     * @param speedModifier Modificateur de vitesse pour le déplacement
     */
    public ZombieInvestigateSoundGoal(SmartZombieEntity zombie, double speedModifier) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Détermine si le goal peut démarrer
     * @return true si le goal peut démarrer
     */
    @Override
    public boolean canUse() {
        // Si le zombie a une cible, il ne doit pas investiguer de sons
        if (this.zombie.getTarget() != null) {
            return false;
        }
        
        // Délai aléatoire pour éviter que tous les zombies ne réagissent en même temps
        if (this.searchDelay > 0) {
            --this.searchDelay;
            return false;
        }
        
        // Si le zombie a entendu un son, il doit l'investiguer
        BlockPos soundPos = this.zombie.getTargetSound();
        if (soundPos != null) {
            targetPos = soundPos;
            // Calculer un chemin vers la position du son
            this.path = this.zombie.getNavigation().createPath(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0);
            return this.path != null;
        }
        
        return false;
    }

    /**
     * Détermine si le goal doit continuer
     * @return true si le goal doit continuer
     */
    @Override
    public boolean canContinueToUse() {
        // Si le zombie a une cible, il arrête d'investiguer
        if (this.zombie.getTarget() != null) {
            return false;
        }
        
        // Si le chemin est invalide ou si il n'y a plus de son à investiguer
        if (this.path != null && !this.path.isDone() && this.zombie.getSoundTrackingCooldown() > 0 && this.timeAtTarget < 100) {
            return true;
        }
        
        return false;
    }

    /**
     * Démarre le goal
     */
    @Override
    public void start() {
        this.zombie.getNavigation().moveTo(this.path, this.speedModifier);
        this.timeAtTarget = 0;
    }

    /**
     * Arrête le goal
     */
    @Override
    public void stop() {
        this.targetPos = null;
        this.path = null;
        this.zombie.getNavigation().stop();
        this.searchDelay = 10 + this.zombie.getRandom().nextInt(20);
    }

    /**
     * Mise à jour du goal à chaque tick
     */
    @Override
    public void tick() {
        // Si on est proche de la position cible, on commence à chercher autour
        if (this.targetPos != null) {
            double distanceSquared = this.zombie.blockPosition().distSqr(this.targetPos);
            
            if (distanceSquared <= 3.0) {
                // On est arrivé à la position du son
                this.timeAtTarget++;
                
                // Regarder autour pour chercher des joueurs
                if (this.timeAtTarget % 20 == 0) { // Toutes les secondes
                    // Tourner un peu pour regarder autour
                    this.zombie.getLookControl().setLookAt(
                        this.targetPos.getX() + this.zombie.getRandom().nextInt(5) - 2, 
                        this.targetPos.getY(), 
                        this.targetPos.getZ() + this.zombie.getRandom().nextInt(5) - 2
                    );
                    
                    // Si on est resté assez longtemps à chercher, changer légèrement de position
                    if (this.timeAtTarget >= 60) { // 3 secondes
                        BlockPos newPos = this.targetPos.offset(
                            this.zombie.getRandom().nextInt(5) - 2,
                            0,
                            this.zombie.getRandom().nextInt(5) - 2
                        );
                        
                        this.path = this.zombie.getNavigation().createPath(newPos.getX(), newPos.getY(), newPos.getZ(), 0);
                        if (this.path != null) {
                            this.zombie.getNavigation().moveTo(this.path, this.speedModifier * 0.8);
                        }
                        
                        // Réinitialiser le compteur pour le prochain déplacement
                        this.timeAtTarget = 0;
                    }
                }
                
                // Vérifier si on peut sentir un joueur accroupi à proximité
                double smellRange = 3.5; // Distance de détection olfactive
                for (Player player : this.zombie.level().getEntitiesOfClass(Player.class, this.zombie.getBoundingBox().inflate(smellRange))) {
                    if (player.isShiftKeyDown()) {
                        // Si on trouve un joueur accroupi à proximité, l'attaquer
                        this.zombie.setTarget(player);
                        break;
                    }
                }
            } else if (distanceSquared <= 25.0 && this.zombie.getNavigation().isDone()) {
                // Si on est proche mais que le pathfinding est bloqué, on regarde dans la direction du son
                this.zombie.getLookControl().setLookAt(
                    this.targetPos.getX(), 
                    this.targetPos.getY(), 
                    this.targetPos.getZ()
                );
            }
        }
    }
}
