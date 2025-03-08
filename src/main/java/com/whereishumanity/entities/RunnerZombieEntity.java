package com.whereishumanity.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Zombie plus rapide mais moins résistant
 */
public class RunnerZombieEntity extends SmartZombieEntity {
    
    public RunnerZombieEntity(EntityType<? extends RunnerZombieEntity> type, Level level) {
        super(type, level);
    }
    
    /**
     * Définit les attributs pour ce type de zombie
     */
    public static AttributeSupplier.Builder createAttributes() {
        return SmartZombieEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D) // 30% plus rapide
                .add(Attributes.ATTACK_DAMAGE, 3.0D)  // Légèrement plus faible
                .add(Attributes.ARMOR, 1.0D)         // Moins résistant
                .add(Attributes.MAX_HEALTH, 16.0D);  // Moins de santé
    }
}