package com.whereishumanity.entities;

import com.whereishumanity.WhereIsHumanity;
import com.whereishumanity.config.ModConfig;
import com.whereishumanity.entities.ai.ZombieInvestigateSoundGoal;
import com.whereishumanity.sound.SoundDetectionSystem;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
    private int alertCooldown = 0;
    private int forgetTargetTimer = 0;
    
    /**
     * Constructeur principal
     * @param type Type d'entité
     * @param level Niveau dans lequel l'entité existe
     */
    public SmartZombieEntity(EntityType<? extends SmartZombieEntity> type, Level level) {
        super(type, level);
        
        // Les zombies intelligents sont plus conscients de leur environnement
        this.xpReward = 8; // Plus d'XP pour les encourager à être stealthy
    }
    
    /**
     * Définit les attributs de base pour ce type d'entité
     * @return Le builder d'attributs configuré
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D) // Portée de détection augmentée
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // Légèrement plus rapide
                .add(Attributes.ATTACK_DAMAGE, 3.5D) // Légèrement plus fort
                .add(Attributes.ARMOR, 2.0D) // Légèrement plus résistant
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.18D); // Plus de chance d'appeler des renforts
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
        this.goalSelector.addGoal(2, new ZombieInvestigateSoundGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new MoveThroughVillageGoal(this, 1.0D, false, 4, () -> false));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        
        // Objectifs de ciblage - aucun ciblage visuel automatique
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        
        // Suppression du ciblage visuel automatique NearestAttackableTargetGoal
        // Les zombies ne peuvent plus voir les joueurs automatiquement
        // Ils doivent les entendre ou les sentir
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
        
        // Gérer le cooldown d'alerte
        if (alertCooldown > 0) {
            alertCooldown--;
        }
        
        // Gestion de l'oubli progressif des cibles
        if (getTarget() != null) {
            // Si la cible est un joueur accroupi et qu'on n'a pas fait de son récemment
            if (getTarget() instanceof Player player && player.isShiftKeyDown() && soundTrackingCooldown <= 0) {
                // Vérifier si on peut le sentir (distance proche)
                if (!SoundDetectionSystem.canSmellPlayer(this, player)) {
                    // Incrémenter le compteur d'oubli
                    forgetTargetTimer++;
                    
                    // Si ça fait assez longtemps qu'on n'a pas vu/entendu/senti la cible, on l'oublie
                    if (forgetTargetTimer > 60) { // 3 secondes
                        setTarget(null);
                        forgetTargetTimer = 0;
                    }
                } else {
                    // Si on peut le sentir, réinitialiser le compteur
                    forgetTargetTimer = 0;
                }
            } else {
                // Si la cible n'est pas accroupie ou qu'on a entendu un son récemment, réinitialiser le compteur
                forgetTargetTimer = 0;
            }
        }
        
        // Si on est alerté, chercher des cibles dans les environs
        if (isAlerted() && alertCooldown <= 0 && getTarget() == null) {
            // Chercher des joueurs proches
            double detectionRange = 10.0;
            
            AABB searchArea = this.getBoundingBox().inflate(detectionRange);
            List<Player> nearbyPlayers = level().getEntitiesOfClass(
                Player.class, 
                searchArea
            );
            
            // Vérifier chaque joueur
            for (Player player : nearbyPlayers) {
                if (!player.isShiftKeyDown() || SoundDetectionSystem.canSmellPlayer(this, player)) {
                    // Si le joueur est visible ou qu'on peut le sentir
                    setTarget(player);
                    alertOthers(player); // Alerter les autres zombies
                    break;
                }
            }
            
            // Définir un cooldown pour les vérifications d'alerte
            alertCooldown = 20; // 1 seconde entre les vérifications
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
            case 1 -> 60;   // 3 secondes
            case 2 -> 140;  // 7 secondes
            case 3 -> 300;  // 15 secondes
            default -> 60;
        };
        
        // Définir la position du son comme cible
        this.targetSoundPos = soundPos;
        this.soundTrackingCooldown = trackingDuration;
        
        // En cas de son fort, on passe en état d'alerte
        if (soundLevel >= 2) {
            setAlerted(true);
            
            // Si le son est très fort, on alerte les zombies proches
            if (soundLevel >= 3) {
                alertNearbyZombies();
            }
        }
        
        // Si la source du son est une entité et qu'on est proche, on la cible directement
        Level level = level();
        if (level != null && !level.isClientSide) {
            // Vérifier si une entité a fait ce son
            Vec3 soundVec = new Vec3(soundPos.getX() + 0.5, soundPos.getY() + 0.5, soundPos.getZ() + 0.5);
            Vec3 zombiePos = position();
            double distanceToSound = zombiePos.distanceTo(soundVec);
            
            // Si on est près du son, chercher qui l'a fait
            if (distanceToSound < 10) {
                AABB searchBox = new AABB(
                    soundPos.getX() - 1, soundPos.getY() - 1, soundPos.getZ() - 1,
                    soundPos.getX() + 1, soundPos.getY() + 1, soundPos.getZ() + 1
                );
                
                List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox);
                if (!nearbyPlayers.isEmpty()) {
                    Player source = nearbyPlayers.get(0);
                    
                    // Si c'est un joueur qui a fait le son, on le cible
                    setTarget(source);
                    
                    // Si le son est fort, on alerte d'autres zombies
                    if (soundLevel >= 2) {
                        alertOthers(source);
                    }
                }
            }
        }
    }
    
    /**
     * Alerte les zombies proches d'un son de haute intensité
     */
    private void alertNearbyZombies() {
        double alertRadius = ModConfig.COMMON.zombieAlertRadius.get();
        AABB alertBox = this.getBoundingBox().inflate(alertRadius);
        
        // Récupérer tous les zombies à proximité
        List<SmartZombieEntity> nearbyZombies = level().getEntitiesOfClass(
            SmartZombieEntity.class,
            alertBox,
            entity -> entity != this
        );
        
        // Alerter les zombies proches
        for (SmartZombieEntity nearbyZombie : nearbyZombies) {
            nearbyZombie.setAlerted(true);
            
            // Leur indiquer la position du son si ils n'ont pas déjà une cible
            if (nearbyZombie.getTarget() == null && this.targetSoundPos != null) {
                nearbyZombie.targetSoundPos = this.targetSoundPos;
                nearbyZombie.soundTrackingCooldown = 80; // 4 secondes
            }
        }
    }
    
    /**
     * Notifie d'autres zombies proches de la présence d'une cible
     * @param target La cible à signaler
     */
    public void alertOthers(LivingEntity target) {
        if (target == null) return;
        
        double alertRadius = ModConfig.COMMON.zombieAlertRadius.get();
        AABB alertBox = this.getBoundingBox().inflate(alertRadius);
        
        // Récupérer tous les zombies à proximité
        List<SmartZombieEntity> nearbyZombies = level().getEntitiesOfClass(
            SmartZombieEntity.class,
            alertBox,
            entity -> entity != this
        );
        
        // Alerter les zombies proches
        for (SmartZombieEntity nearbyZombie : nearbyZombies) {
            // Selon la distance, il y a une chance que le zombie ne remarque pas
            double distance = nearbyZombie.distanceTo(this);
            double chanceToNotice = 1.0 - (distance / (alertRadius * 1.5));
            
            if (random.nextDouble() < chanceToNotice) {
                nearbyZombie.setTarget(target);
                nearbyZombie.setAlerted(true);
            }
        }
    }
    
    /**
     * Définit l'état d'alerte du zombie
     * @param alerted true si le zombie est en alerte, false sinon
     */
    public void setAlerted(boolean alerted) {
        this.entityData.set(IS_ALERTED, alerted);
        
        // Si on passe en état d'alerte, augmenter temporairement la vitesse
        if (alerted) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28D);
        } else {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        }
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
    
    /**
     * Récupère le cooldown de suivi du son
     * @return Le cooldown actuel
     */
    public int getSoundTrackingCooldown() {
        return soundTrackingCooldown;
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
