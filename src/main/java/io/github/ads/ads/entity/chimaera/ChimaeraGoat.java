package io.github.ads.ads.entity.chimaera;

import io.github.ads.ads.entity.Basilisk;
import io.github.ads.ads.registries.ModEntityTypes;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.function.Predicate;

import static net.minecraft.world.entity.Entity.RemovalReason.DISCARDED;

public class ChimaeraGoat extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(ChimaeraGoat.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FIRE_BREATH = SynchedEntityData.defineId(ChimaeraGoat.class, EntityDataSerializers.BOOLEAN);
    protected int breathCastingTickCount;
    private Goal landTargetGoal;
    private Goal angryTargetGoal;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChimaeraGoat(EntityType<? extends Animal> mob, Level level) {
        super(mob, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @javax.annotation.Nullable SpawnGroupData pSpawnData, @javax.annotation.Nullable CompoundTag pDataTag) {
        if (pLevel instanceof ServerLevel) {
            this.setTargetGoals();
        }

        if (this.getVehicle() == null) {
            ChimaeraBody body = ModEntityTypes.CHIMAERA_BODY.get().create(this.level);
            if (body != null) {
                body.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                body.finalizeSpawn(pLevel, pDifficulty, MobSpawnType.JOCKEY, (SpawnGroupData) null, (CompoundTag) null);
                this.startRiding(body);
                pLevel.addFreshEntity(body);
                if (pLevel instanceof ServerLevel) {
                    body.setTargetGoals();
                }
            }
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    protected void registerGoals() {
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, true, (Predicate<LivingEntity>)null));
        this.goalSelector.addGoal(6, new ChimaeraGoat.ChimaeraFireBreath(this));
        this.landTargetGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false, (target) -> {
            if (target.isAlive()) {
                return true;
            } else {
                return false;
            }
        });
        this.angryTargetGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> this.getLastHurtByMob() != null);
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FIRE_BREATH, false);
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.breathCastingTickCount = compound.getInt("SpellTicks");
    }

    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SpellTicks", this.breathCastingTickCount);
    }

    private <E extends GeoEntity> PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (this.isFireBreathing()) {
            event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("fire"));
        } else {
            event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "predicate", 10, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void aiStep() {

        if (this.level.isClientSide) {
            if (this.isFireBreathing()) {
                this.level.playLocalSound(this.getX() + 0.5D, this.getY() + 0.5D, this.getZ() + 0.5D, SoundEvents.BLAZE_BURN, this.getSoundSource(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
                this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
        }

        super.aiStep();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    public boolean isFireBreathing() {
        return this.entityData.get(FIRE_BREATH);
    }

    public void setFireBreathing(boolean courting) {
        this.entityData.set(FIRE_BREATH, courting);
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_SCREAMING_AMBIENT;
    }

    public boolean isAlliedTo(Entity entity) {
        Entity vehicle = this.getVehicle();
        if (entity == null) {
            return false;
        } else if (entity == this) {
            return true;
        } else if (super.isAlliedTo(entity)) {
            return true;
        } else if (entity == vehicle && vehicle instanceof ChimaeraBody) {
            return this.getTeam() == null && entity.getTeam() == null;
        } else {
            return false;
        }
    }

    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.breathCastingTickCount > 0) {
            --this.breathCastingTickCount;
        }

    }

    public void tick() {
        super.tick();

        if (this.getVehicle() instanceof ChimaeraBody) {
            if (!this.getVehicle().isAlive()) {
                this.remove(DISCARDED);
            }
        }
    }

    private boolean isCharged() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    void setCharged(boolean charged) {
        byte b0 = this.entityData.get(DATA_FLAGS_ID);
        if (charged) {
            b0 = (byte)(b0 | 1);
        } else {
            b0 = (byte)(b0 & -2);
        }

        this.entityData.set(DATA_FLAGS_ID, b0);
    }

    private void setTargetGoals() {
        this.targetSelector.addGoal(4, this.landTargetGoal);
        this.targetSelector.addGoal(4, this.angryTargetGoal);
    }

    static class ChimaeraFireBreath extends Goal {
        private final ChimaeraGoat goat;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        public ChimaeraFireBreath(ChimaeraGoat goat) {
            this.goat = goat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            LivingEntity livingentity = this.goat.getTarget();
            return livingentity != null && livingentity.isAlive() && this.goat.canAttack(livingentity);
        }

        public void start() {
            this.attackStep = 0;
        }

        public void stop() {
            this.goat.setCharged(false);
            this.lastSeen = 0;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            --this.attackTime;
            LivingEntity livingentity = this.goat.getTarget();
            if (livingentity != null) {
                boolean flag = this.goat.getSensing().hasLineOfSight(livingentity);
                if (flag) {
                    this.lastSeen = 0;
                } else {
                    ++this.lastSeen;
                }

                double d0 = this.goat.distanceToSqr(livingentity);
                if (d0 < 4.0D) {
                    if (!flag) {
                        return;
                    }

                    if (this.attackTime <= 0) {
                        this.attackTime = 20;
                        this.goat.doHurtTarget(livingentity);
                    }

                    this.goat.getMoveControl().setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), 1.0D);
                } else if (d0 < this.getFollowDistance() * this.getFollowDistance() && flag) {
                    double d1 = livingentity.getX() - this.goat.getX();
                    double d2 = livingentity.getY(0.5D) - this.goat.getY(0.5D);
                    double d3 = livingentity.getZ() - this.goat.getZ();
                    if (this.attackTime <= 0) {
                        ++this.attackStep;
                        if (this.attackStep == 1) {
                            this.attackTime = 45;
                            this.goat.setCharged(true);
                        } else if (this.attackStep <= 4) {
                            this.attackTime = 6;
                        } else {
                            this.attackTime = 50;
                            this.attackStep = 0;
                            this.goat.setCharged(false);
                        }

                        if (this.attackStep > 1) {
                            double d4 = Math.sqrt(Math.sqrt(d0)) * 0.5D;
                            if (!this.goat.isSilent()) {
                                this.goat.level.levelEvent((Player)null, 1018, this.goat.blockPosition(), 0);
                            }

                            for(int i = 0; i < 1; ++i) {
                                SmallFireball smallfireball = new SmallFireball(this.goat.level, this.goat, this.goat.getRandom().triangle(d1, 3.0D * d4), d2, this.goat.getRandom().triangle(d3, 3.0D * d4));
                                smallfireball.setPos(smallfireball.getX(), this.goat.getY(0.25D) + 0.25D, smallfireball.getZ());
                                this.goat.level.addFreshEntity(smallfireball);
                                this.goat.setFireBreathing(true);
                                this.goat.playSound(SoundEvents.FIRE_AMBIENT);
                            }
                        } else {
                            this.goat.setFireBreathing(false);
                        }
                    }

                    this.goat.getLookControl().setLookAt(livingentity, 10.0F, 10.0F);
                } else if (this.lastSeen < 5) {
                    this.goat.getMoveControl().setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), 1.0D);
                }

                super.tick();
            }
        }

        private double getFollowDistance() {
            return this.goat.getAttributeValue(Attributes.FOLLOW_RANGE);
        }
    }
}
