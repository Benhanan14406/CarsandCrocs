package io.github.ads.ads.entity.chimaera;

import io.github.ads.ads.registries.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class ChimaeraBody extends Animal implements ItemSteerable, GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_SADDLE_ID = SynchedEntityData.defineId(ChimaeraBody.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(ChimaeraBody.class, EntityDataSerializers.INT);
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME, DATA_SADDLE_ID);
    private Goal landTargetGoal;
    private Goal angryTargetGoal;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChimaeraBody(EntityType<? extends Animal> mob, Level level) {
        super(mob, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.5D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @javax.annotation.Nullable SpawnGroupData pSpawnData, @javax.annotation.Nullable CompoundTag pDataTag) {
        if (this.isAddedToWorld() && !this.isVehicle()) {
            ChimaeraGoat goat = ModEntityTypes.GOAT_HEAD.get().create(this.level);
            if (goat != null && !(this.getPassengers() instanceof ChimaeraGoat)) {
                goat.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                goat.finalizeSpawn(pLevel, pDifficulty, MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
                goat.startRiding(this);
                this.addPassenger(goat);
                pLevel.addFreshEntity(goat);
            }
        }

        this.setTargetGoals();

        return super.finalizeSpawn(pLevel, pDifficulty, MobSpawnType.JOCKEY, pSpawnData, pDataTag);
    }

    private boolean canBeControlledBy(Entity entity) {
        return false;
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
        if (DATA_BOOST_TIME.equals(dataAccessor) && this.level.isClientSide) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(dataAccessor);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return null;
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SADDLE_ID, false);
        this.entityData.define(DATA_BOOST_TIME, 0);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        this.steering.addAdditionalSaveData(compound);
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.steering.readAdditionalSaveData(compound);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, true, (Predicate<LivingEntity>)null));
        this.landTargetGoal = new NearestAttackableTargetGoal<>(this, Mob.class, false);
        this.angryTargetGoal = new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (target) -> this.getLastHurtByMob() != null);
        this.goalSelector.addGoal(5, new ChimaeraBody.ChimaeraChasePreyGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Mob.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public double getPassengersRidingOffset() {
        return (double)(this.getBbHeight() * 0.85F);
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_SCREECH;
    }

    private <E extends GeoEntity> PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            if (this.isSprinting()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
                event.getController().setAnimationSpeed(1.5F);
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
                event.getController().setAnimationSpeed(1.0F);
            }
        } else {
            event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    private <E extends GeoEntity> PlayState attackPredicate(AnimationState<E> event) {
        if (swinging && event.getController().getAnimationState().equals(AnimationController.State.STOPPED)) {
            event.setAndContinue(RawAnimation.begin().thenPlay("bite"));
            swinging = false;
        }
        event.getController().forceAnimationReset();

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "predicate", 10, this::predicate));
        controllers.add(new AnimationController<>(this, "attackPredicate", 0, this::attackPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return super.canBeSeenByAnyone();
    }

    public void setTargetGoals() {
        this.targetSelector.addGoal(4, this.landTargetGoal);
        this.targetSelector.addGoal(4, this.angryTargetGoal);
    }

    @Override
    @Nullable
    public Entity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        return this.canBeControlledBy(entity) ? entity : null;
    }

    @Override
    public void customServerAiStep() {
        if (this.getMoveControl().hasWanted()) {
            double speedModifier = this.getMoveControl().getSpeedModifier();
            if (speedModifier >= 1.25D && this.isOnGround()) {
                this.setPose(Pose.STANDING);
                this.setSprinting(true);
            } else {
                this.setPose(Pose.STANDING);
                this.setSprinting(false);
            }
        } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
        }
    }

    @Override
    public void travel(Vec3 vec) {
        this.travel(this, this.steering, vec);
    }

    public float getSteeringSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225F;
    }

    @Override
    public void travelWithInput(Vec3 vec) {
        super.travel(vec);
    }

    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    static class ChimaeraChasePreyGoal extends Goal {
        protected final ChimaeraBody chimaera;
        private double speedModifier = 0.5D;
        private Path path;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;

        public ChimaeraChasePreyGoal (ChimaeraBody chimaera) {
            this.chimaera = chimaera;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.chimaera.isBaby()) {
                return false;
            }

            long gameTime = this.chimaera.level.getGameTime();
            if (gameTime - this.lastCanUseCheck < 20L) {
                return false;
            }

            this.lastCanUseCheck = gameTime;
            LivingEntity livingEntity = this.chimaera.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            this.path = this.chimaera.getNavigation().createPath(livingEntity, 0);
            if (this.path != null) {
                return true;
            }

            return this.getAttackReachSqr(livingEntity) >= this.chimaera.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity livingEntity = this.chimaera.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            return !this.chimaera.getNavigation().isDone();
        }

        @Override
        public void start() {
            LivingEntity target = this.chimaera.getTarget();

            if (target == null) {
                return;
            }

            this.speedModifier = this.chimaera.distanceTo(target) > 15.0D ? 1.0D : 1.5D;
            this.chimaera.getNavigation().moveTo(this.path, this.speedModifier);
            this.chimaera.setAggressive(true);

            if (lastCanUseCheck == 0L) {
                this.chimaera.playSound(SoundEvents.RAVAGER_ROAR);
            }

            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
        }

        @Override
        public void stop() {
            LivingEntity livingEntity = this.chimaera.getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
                this.chimaera.setTarget(null);
            }

            this.chimaera.setAggressive(false);
            this.chimaera.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.chimaera.getTarget();
            if (target == null) {
                return;
            }

            this.speedModifier = this.chimaera.distanceTo(target) > 15.0D ? 1.0D : 1.5D;
            this.chimaera.getLookControl().setLookAt(target, 90.0F, 90.0F);
            double d = this.chimaera.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if (this.chimaera.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0 || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0 || this.chimaera.getRandom().nextFloat() < 0.05f)) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.chimaera.getRandom().nextInt(7);

                if (d > 1024.0) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d > 256.0) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.chimaera.getNavigation().moveTo(target, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            this.checkAndPerformAttack(target, d);
        }

        protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
            double d = this.getAttackReachSqr(enemy);
            if (distToEnemySqr <= d && this.ticksUntilNextAttack <= 0) {
                this.resetAttackCooldown();
                this.chimaera.playSound(SoundEvents.PLAYER_ATTACK_STRONG);
                this.chimaera.swing(InteractionHand.MAIN_HAND);
                this.chimaera.doHurtTarget(enemy);
            }
        }

        protected void resetAttackCooldown() {
            this.ticksUntilNextAttack = this.adjustedTickDelay(20);
        }

        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return this.chimaera.getBbWidth() * 2.0f * (this.chimaera.getBbWidth() * 2.0f) + attackTarget.getBbWidth();
        }
    }
}
