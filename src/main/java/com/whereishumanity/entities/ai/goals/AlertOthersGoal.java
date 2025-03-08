package com.whereishumanity.entities.ai.goals;

import com.whereishumanity.entities.ScreamerZombieEntity;
import com.whereishumanity.entities.SmartZombieEntity;
import com.whereishumanity.sound.SoundDetectionSystem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Objectif d'IA permettant aux zombies d'alerter d'autres zombies
 * lorsqu'ils détectent une cible
 */
public class AlertOthersGoal extends Goal {
    
    private final SmartZombieEntity zombie;
    private final double alertRadius;
    private int alertCooldown = 0;
    private static final int BASE_COOLDOWN = 60; // 3 secondes entre les alertes
    
    public AlertOthersGoal(SmartZombieEntity zombie, double alertRadius) {
        this.zombie = zombie;
        this.alertRadius = alertRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Vérifier si le zombie a une cible, est en alerte, et si le cooldown d'alerte est écoulé
        return zombie.getTarget() != null && 
               zombie.isAlerted() && 
               alertCooldown <= 0 &&
               zombie.getRandom().nextInt(10) == 0; // Chance aléatoire pour éviter que tous les zombies hurlent en même temps
    }
    
    @Override
    public void start() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return;
        
        // Jouer un son d'alerte
        SoundEvent alertSound;
        float volume;
        
        // Les Screamer ont un cri plus fort et une portée plus grande
        if (zombie instanceof ScreamerZombieEntity) {
            alertSound = SoundEvents.GHAST_SCREAM;
            volume = 2.0F;
        } else {
            alertSound = SoundEvents.ZOMBIE_AMBIENT;
            volume = 1.0F;
        }
        
        // Jouer le son
        zombie.level().playSound(null, zombie.blockPosition(), 
                alertSound, SoundSource.HOSTILE, 
                volume, 0.8F + zombie.getRandom().nextFloat() * 0.4F);
        
        // Émettre un son fort qui peut être détecté par d'autres zombies
        SoundDetectionSystem.emitSound(zombie.level(), zombie.blockPosition(), 3, zombie);
        
        // Alerter les autres zombies directement
        zombie.alertOthers(target);
        
        // Définir le cooldown avant la prochaine alerte
        // Les Screamer ont un cooldown plus court
        if (zombie instanceof ScreamerZombieEntity) {
            alertCooldown = BASE_COOLDOWN / 2;
        } else {
            alertCooldown = BASE_COOLDOWN;
        }
    }
    
    @Override
    public void tick() {
        // Réduire le cooldown d'alerte
        if (alertCooldown > 0) {
            alertCooldown--;
        }
    }
    
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}