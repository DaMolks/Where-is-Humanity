package com.whereishumanity.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Zombie plus fort capable de briser des blocs plus solides
 */
public class BruteZombieEntity extends SmartZombieEntity {
    
    public BruteZombieEntity(EntityType<? extends BruteZombieEntity> type, Level level) {
        super(type, level);
    }
    
    /**
     * Définit les attributs pour ce type de zombie
     */
    public static AttributeSupplier.Builder createAttributes() {
        return SmartZombieEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.18D)   // Plus lent
                .add(Attributes.ATTACK_DAMAGE, 5.0D)     // Beaucoup plus fort
                .add(Attributes.ARMOR, 4.0D)             // Plus résistant
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D) // Résistant aux reculs
                .add(Attributes.MAX_HEALTH, 30.0D);      // Plus de santé
    }
}