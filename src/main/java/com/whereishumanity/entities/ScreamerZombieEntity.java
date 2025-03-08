package com.whereishumanity.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Zombie spécialisé dans l'alerte d'autres zombies
 */
public class ScreamerZombieEntity extends SmartZombieEntity {
    
    public ScreamerZombieEntity(EntityType<? extends ScreamerZombieEntity> type, Level level) {
        super(type, level);
    }
    
    /**
     * Définit les attributs pour ce type de zombie
     */
    public static AttributeSupplier.Builder createAttributes() {
        return SmartZombieEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D)   // Assez rapide
                .add(Attributes.ATTACK_DAMAGE, 2.5D)     // Faible en attaque
                .add(Attributes.ARMOR, 1.5D)             // Peu résistant
                .add(Attributes.MAX_HEALTH, 15.0D)       // Peu de santé
                .add(Attributes.FOLLOW_RANGE, 48.0D);    // Très grande portée de détection
    }
}