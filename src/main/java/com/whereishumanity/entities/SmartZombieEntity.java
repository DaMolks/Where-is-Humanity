package com.whereishumanity.entities;

import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Classe de base pour les zombies intelligents
 * Cette entité étend le zombie vanilla avec des comportements améliorés
 */
public class SmartZombieEntity extends Zombie {
    
    // Paramètres synchronisés
    private static final EntityDataAccessor<Boolean> IS_ALERTED = SynchedEntityData.defineId(
            SmartZombieEntity.class, EntityDataSerializers.BOOLEAN);
    
    // Position du dernier son détecté
    private BlockPos targetSoundPos = null;
    private int soundTrackingCooldown = 0;
    
    /**
     * Constructeur principal
     * @param type Type d'entité
     * @param level Niveau dans lequel l'entité existe
     */
    public SmartZombieEntity(EntityType<? extends SmartZombieEntity> type, Level level) {
        super(type, level);
    }
    
    /**
     * Définit les attributs de base pour ce type d'entité
     * @return Le builder d'attributs configuré
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D) // Portée de détection augmentée
                .add(Attributes.MOVEMENT_SPEED, 0.23D) // Légèrement plus rapide
                .add(Attributes.ATTACK_DAMAGE, 3.5D) // Légèrement plus fort
                .add(Attributes.ARMOR, 2.0D) // Légèrement plus résistant
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.15D); // Plus de chance d'appeler des renforts
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ALERTED, false);
    }
    
    @Override
    protected void registerGoals() {
        // Objectifs de base du zombie
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveThroughVillageGoal(this, 1.0D, false, 4, () -> false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        // Objectifs de ciblage
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Gère le cooldown de suivi du son
        if (soundTrackingCooldown > 0) {
            soundTrackingCooldown--;
            if (soundTrackingCooldown <= 0) {
                targetSoundPos = null;
            }
        }
    }
    
    /**
     * Réagit à un son détecté
     * @param soundPos Position du son
     * @param soundLevel Niveau du son (1-3)
     */
    public void onSoundDetected(BlockPos soundPos, int soundLevel) {
        // Si on a déjà une cible, on ignore les sons faibles
        if (this.getTarget() != null && soundLevel < 2) {
            return;
        }
        
        // Détermine la durée de poursuite en fonction du niveau du son
        int trackingDuration = switch (soundLevel) {
            case 1 -> 40;   // 2 secondes
            case 2 -> 100;  // 5 secondes
            case 3 -> 200;  // 10 secondes
            default -> 60;
        };
        
        this.targetSoundPos = soundPos;
        this.soundTrackingCooldown = trackingDuration;
        
        // En cas de son fort, on passe en état d'alerte
        if (soundLevel >= 2) {
            setAlerted(true);
        }
    }
    
    /**
     * Notifie d'autres zombies proches de la présence d'une cible
     * @param target La cible à signaler
     */
    public void alertOthers(LivingEntity target) {
        if (target == null) return;
        
        double alertRadius = ModConfig.COMMON.zombieAlertRadius.get();
        level().getNearbyEntities(
                SmartZombieEntity.class,
                null,
                this,
                this.getBoundingBox().inflate(alertRadius),
                entity -> entity != this
        ).forEach(zombie -> {
            zombie.setTarget(target);
            zombie.setAlerted(true);
        });
    }
    
    /**
     * Définit l'état d'alerte du zombie
     * @param alerted true si le zombie est en alerte, false sinon
     */
    public void setAlerted(boolean alerted) {
        this.entityData.set(IS_ALERTED, alerted);
    }
    
    /**
     * Vérifie si le zombie est en état d'alerte
     * @return true si le zombie est en alerte, false sinon
     */
    public boolean isAlerted() {
        return this.entityData.get(IS_ALERTED);
    }
    
    /**
     * Récupère la position du dernier son détecté
     * @return La position du son ou null si aucun son n'est suivi
     */
    public BlockPos getTargetSound() {
        return targetSoundPos;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsAlerted", isAlerted());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsAlerted")) {
            setAlerted(tag.getBoolean("IsAlerted"));
        }
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return isAlerted() ? SoundEvents.ZOMBIE_AMBIENT : super.getAmbientSound();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
    
    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }
}