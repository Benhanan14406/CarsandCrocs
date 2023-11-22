package io.github.ads.ads.entity;

import io.github.ads.ads.entity.ai.movement.BetterGroundPathNavigation;
import io.github.ads.ads.entity.types.GenderedMob;
import io.github.ads.ads.registries.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;
import static net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;

public class Basilisk extends GenderedMob implements GeoEntity, NeutralMob {
    private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Basilisk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Basilisk.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ID_ATTACK_TARGET = SynchedEntityData.defineId(Basilisk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(Basilisk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> COURT = SynchedEntityData.defineId(Basilisk.class, EntityDataSerializers.BOOLEAN);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    protected static final int ATTACK_TIME = 80;
    public static final int FLAG_CROUCHING = 4;
    public static final int FLAG_POUNCING = 16;
    private static final int FLAG_SLEEPING = 32;
    private static final int FLAG_PERCHING = 64;
    public static final double BASE_SPEED = 0.2D;
    public static final double BASE_DAMAGE = 5.0D;
    private Goal landTargetGoal;
    private Goal angryTargetGoal;
    float crouchAmount;
    float crouchAmountO;
    private LivingEntity clientSideCachedAttackTarget;
    private int clientSideAttackTime;
    @javax.annotation.Nullable
    private UUID persistentAngerTarget;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Basilisk(EntityType<? extends GenderedMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return ModEntityTypes.BASILISK.get().create(serverLevel);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @javax.annotation.Nullable SpawnGroupData pSpawnData, @javax.annotation.Nullable CompoundTag pDataTag) {
        if (pSpawnData == null) {
            pSpawnData = new AgeableMobGroupData(1.0F);
        }
        Holder<Biome> holder = pLevel.getBiome(this.blockPosition());
        Basilisk.Type basilisk$type = Basilisk.Type.byBiome(holder);
        if (pSpawnData instanceof Basilisk.BasiliskGroupData basilisk$basiliskgroupdata) {
            basilisk$type = basilisk$basiliskgroupdata.type;
        } else {
            pSpawnData = new Basilisk.BasiliskGroupData(basilisk$type);
        }
        if (pLevel instanceof ServerLevel) {
            this.setTargetGoals();
        }
        this.setVariant(basilisk$type);
        this.setGender(ThreadLocalRandom.current().nextBoolean());
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE_ID, 0);
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
        this.entityData.define(DATA_ID_ATTACK_TARGET, 0);
        this.entityData.define(COURT, false);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Sleeping", this.isSleeping());
        compound.putBoolean("Crouching", this.isCrouching());
        compound.putBoolean("Courting", this.isCourting());
        compound.putString("Type", this.getVariant().getSerializedName());
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setSleeping(compound.getBoolean("Sleeping"));
        this.setIsCrouching(compound.getBoolean("Crouching"));
        this.setCourting(compound.getBoolean("Courting"));
        this.setVariant(Basilisk.Type.byName(compound.getString("Type")));
        if (this.level instanceof ServerLevel) {
            this.setTargetGoals();
        }
    }

    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> dataAccessor) {
        super.onSyncedDataUpdated(dataAccessor);
        if (DATA_ID_ATTACK_TARGET.equals(dataAccessor)) {
            this.clientSideAttackTime = 0;
            this.clientSideCachedAttackTarget = null;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(4, new Basilisk.BasiliskStareGoal(this));
        this.landTargetGoal = new NearestAttackableTargetGoal<>(this, Animal.class, 10, false, false, (prey) -> {
            if (getVariant() == Type.JUNGLEFOWL) {
                if (prey.isBaby()) {
                    return prey instanceof Cow || prey instanceof Sheep || prey instanceof Pig || prey instanceof Goat;
                } else {
                    return prey instanceof Chicken || prey instanceof Rabbit || prey instanceof Frog;
                }
            } else {
                if (prey.isBaby()) {
                    return prey instanceof Pig || prey instanceof Goat;
                } else {
                    return prey instanceof Chicken || prey instanceof Rabbit || prey instanceof Frog;
                }
            }
        });
        this.angryTargetGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> this.getLastHurtByMob() != null);
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> this.isLookingAtMe(target) && !(target instanceof Basilisk)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.goalSelector.addGoal(5, new Basilisk.BasiliskStalkPreyGoal(this));
        this.goalSelector.addGoal(5, new Basilisk.BasiliskChasePreyGoal(this));
        this.goalSelector.addGoal(6, new Basilisk.BasiliskPounceGoal());
        this.targetSelector.addGoal(6, new ResetUniversalAngerTargetGoal<>(this, true));
        this.goalSelector.addGoal(7, new Basilisk.SleepGoal());
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new Basilisk.BasiliskCourtGoal(this, 1.0D, 3.0F, 7.0F));
        this.goalSelector.addGoal(9, new Basilisk.PerchAndSearchGoal());
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public Basilisk.Type getVariant() {
        return Basilisk.Type.byId(this.entityData.get(DATA_TYPE_ID));
    }

    public void setVariant(Basilisk.Type type) {
        this.entityData.set(DATA_TYPE_ID, type.getId());
    }

    @Override
    public void customServerAiStep() {

        if (this.getMoveControl().hasWanted()) {
            double speedModifier = this.getMoveControl().getSpeedModifier();
            if (speedModifier < 1.0D && this.isOnGround()) {
                this.setPose(Pose.CROUCHING);
                this.setSprinting(false);
            } else if (speedModifier >= 1.25D && this.isOnGround()) {
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

        if (!this.level.isClientSide && this.isAlive() && this.isEffectiveAi()) {
            LivingEntity livingentity = this.getTarget();
            if (livingentity == null || !livingentity.isAlive()) {
                this.setIsCrouching(false);
            }
        }

        if (this.isSleeping() || this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }

        if (this.level.isClientSide) {
            if (this.hasActiveAttackTarget()) {
                if (this.clientSideAttackTime < this.getAttackDuration()) {
                    ++this.clientSideAttackTime;
                }

                LivingEntity livingentity = this.getActiveAttackTarget();
                if (livingentity != null) {
                    this.getLookControl().setLookAt(livingentity, 90.0F, 90.0F);
                    this.getLookControl().tick();
                    double d0 = livingentity.getX() - this.getX();
                    double d1 = livingentity.getY(0.5D) - this.getEyeY();
                    double d2 = livingentity.getZ() - this.getZ();
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    d0 /= d3;
                    d1 /= d3;
                    d2 /= d3;
                    double d4 = this.random.nextDouble();

                    while(d4 < d3) {
                        d4 += 1.8D - this.random.nextDouble() * (1.7D);
                        this.level.addParticle(ParticleTypes.ASH, this.getX() + d0 * d4, this.getEyeY() + d1 * d4, this.getZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }

        if (this.hasActiveAttackTarget()) {
            this.setYRot(this.yHeadRot);
        }

        super.customServerAiStep();
    }

    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public void tick() {
        super.tick();

        //variasi antarwarna
        if (getVariant() == Type.JUNGLEFOWL) {
            Objects.requireNonNull(this.getAttribute(MOVEMENT_SPEED)).setBaseValue(BASE_SPEED);
            Objects.requireNonNull(this.getAttribute(ATTACK_DAMAGE)).setBaseValue(BASE_DAMAGE + 2.5D);
        } else if (getVariant() == Type.GUINEAFOWL){
            Objects.requireNonNull(this.getAttribute(MOVEMENT_SPEED)).setBaseValue(BASE_SPEED + 0.025D);
            Objects.requireNonNull(this.getAttribute(ATTACK_DAMAGE)).setBaseValue(BASE_DAMAGE);
        }

        //bangun pas basah
        if (this.isEffectiveAi()) {
            boolean flag = this.isInWater();
            if (flag || this.getTarget() != null) {
                this.wakeUp();
            }
        }

        this.crouchAmountO = this.crouchAmount;
        if (this.isCrouching()) {
            this.crouchAmount += 0.2F;
            if (this.crouchAmount > 3.0F) {
                this.crouchAmount = 3.0F;
            }
        } else {
            this.crouchAmount = 0.0F;
        }
    }

    public boolean shouldStare(Entity entity) {
        return this.distanceTo(entity) > 10.0D;
    }

    public @NotNull SoundEvent getEatingSound(@NotNull ItemStack itemStack) {
        return SoundEvents.FROG_EAT;
    }

    private boolean canEat(ItemStack item) {
        return item.getItem().isEdible() && this.getTarget() == null && this.onGround && !this.isSleeping();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new BetterGroundPathNavigation(this, level);
    }

    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    public void setRemainingPersistentAngerTime(int angerTime) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, angerTime);
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @javax.annotation.Nullable
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    public void setPersistentAngerTarget(@javax.annotation.Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    public int getAttackDuration() {
        return 50;
    }

    void setActiveAttackTarget(int attackTarget) {
        this.entityData.set(DATA_ID_ATTACK_TARGET, attackTarget);
    }

    public boolean hasActiveAttackTarget() {
        return this.entityData.get(DATA_ID_ATTACK_TARGET) != 0;
    }

    @javax.annotation.Nullable
    public LivingEntity getActiveAttackTarget() {
        if (!this.hasActiveAttackTarget()) {
            return null;
        } else if (this.level.isClientSide) {
            if (this.clientSideCachedAttackTarget != null) {
                return this.clientSideCachedAttackTarget;
            } else {
                Entity entity = this.level.getEntity(this.entityData.get(DATA_ID_ATTACK_TARGET));
                if (entity instanceof LivingEntity) {
                    this.clientSideCachedAttackTarget = (LivingEntity)entity;
                    return this.clientSideCachedAttackTarget;
                } else {
                    return null;
                }
            }
        } else {
            return this.getTarget();
        }
    }

    protected float getStandingEyeHeight(@NotNull Pose pose, EntityDimensions dimensions) {
        return dimensions.height;
    }

    public int getAmbientSoundInterval() {
        return 250;
    }

    protected SoundEvent getAmbientSound() {
        return this.isInWaterOrRain() ? SoundEvents.FROG_AMBIENT : SoundEvents.PHANTOM_AMBIENT;
    }

    private <E extends GeoEntity> PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (!this.isDeadOrDying()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("normal"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("normal"));
        }

        if (getAmbientSoundInterval() == 0) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("openmouth"));
            event.getController().setAnimationSpeed(1.0F);
        }

        return PlayState.CONTINUE;
    }

    private <E extends GeoEntity> PlayState courtPredicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (isCourting()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("court"));
        }
        return PlayState.CONTINUE;
    }

    private <E extends GeoEntity> PlayState movePredicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (this.isPerched()) {
            event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("perch"));
        } else if (this.isCourting()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("court"));
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            if(getVariant() == Type.JUNGLEFOWL) {
                if (this.isSprinting()) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
                    event.getController().setAnimationSpeed(1.5F);
                } else if (this.getPose() == Pose.CROUCHING) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
                    event.getController().setAnimationSpeed(0.5F);
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
                    event.getController().setAnimationSpeed(1.0F);
                }
            } else {
                if (this.isSprinting()) {
                    event.getController().setAnimationSpeed(2.0F).setAnimation(RawAnimation.begin().thenLoop("run"));
                } else if (this.getPose() == Pose.CROUCHING) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("run"));
                } else {
                    event.getController().setAnimationSpeed(1.25F).setAnimation(RawAnimation.begin().thenLoop("walk"));
                }
            }
        } else {
            event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("landidle"));
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
        controllers.add(new AnimationController<>(this, "movePredicate", 10, this::movePredicate));
        controllers.add(new AnimationController<>(this, "courtPredicate", 10, this::courtPredicate));
        controllers.add(new AnimationController<>(this, "attackPredicate", 0, this::attackPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void setTargetGoals() {
        this.targetSelector.addGoal(4, this.landTargetGoal);
        this.targetSelector.addGoal(3, this.angryTargetGoal);
    }

    public boolean isCourting() {
        return this.entityData.get(COURT);
    }

    public void setCourting(boolean courting) {
        this.entityData.set(COURT, courting);
    }

    public boolean isPouncing() {
        return this.getFlag(16);
    }

    public void setIsPouncing(boolean pouncing) {
        this.setFlag(16, pouncing);
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public boolean isSleeping() {
        return this.getFlag(32);
    }

    void setSleeping(boolean sleep) {
        this.setFlag(32, sleep);
    }

    public boolean isPerched() {
        return this.getFlag(64);
    }

    void setPerched(boolean perched) {
        this.setFlag(64, perched);
    }

    public boolean isFullyCrouched() {
        return this.crouchAmount == 3.0F;
    }

    public void setIsCrouching(boolean p_28615_) {
        this.setFlag(4, p_28615_);
    }

    public boolean isCrouching() {
        return this.getFlag(4);
    }
    
    private void setFlag(int set, boolean flag) {
        if (flag) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | set));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~set));
        }
    }

    private boolean getFlag(int flag) {
        return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
    }

    void wakeUp() {
        this.setSleeping(false);
    }

    void clearStates() {
        this.setIsCrouching(false);
        this.setSleeping(false);
    }

    public static boolean isPathClear(Basilisk basilisk, LivingEntity entity) {
        double d0 = entity.getZ() - basilisk.getZ();
        double d1 = entity.getX() - basilisk.getX();
        double d2 = d0 / d1;
        int i = 6;

        for(int j = 0; j < 6; ++j) {
            double d3 = d2 == 0.0D ? 0.0D : d0 * (double)((float)j / 6.0F);
            double d4 = d2 == 0.0D ? d1 * (double)((float)j / 6.0F) : d3 / d2;

            for(int k = 1; k < 4; ++k) {
                if (!basilisk.level.getBlockState(new BlockPos(basilisk.getX() + d4, basilisk.getY() + (double)k, basilisk.getZ() + d3)).canBeReplaced()) {
                    return false;
                }
            }
        }

        return true;
    }

    boolean isLookingAtMe(LivingEntity entity) {
        Vec3 vec3 = entity.getViewVector(1.0F).normalize();
        Vec3 vec31 = new Vec3(this.getX() - entity.getX(), this.getEyeY() - entity.getEyeY(), this.getZ() - entity.getZ());
        double d0 = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dot(vec31);
        return d1 > 1.0D - 0.025D / d0 && entity.hasLineOfSight(this);
    }

    public static class BasiliskAlertableEntitiesSelector implements Predicate<LivingEntity> {
        public boolean test(LivingEntity entity) {
            if (entity instanceof Basilisk) {
                return false;
            } else if (!(entity instanceof Chicken) && !(entity instanceof Pig) && !(entity instanceof Goat) && !(entity instanceof Frog) && !(entity instanceof Rabbit) && !(entity instanceof Monster)) {
                if (entity instanceof TamableAnimal) {
                    return !((TamableAnimal)entity).isTame();
                } else if (!(entity instanceof Player) || !entity.isSpectator() && !((Player)entity).isCreative()) {
                    return !entity.isSleeping() && !entity.isDiscrete();
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    //bisa tidur
    abstract class BasiliskBehaviorGoal extends Goal {
        private final TargetingConditions alertableTargeting = TargetingConditions.forCombat().range(20.0D).ignoreLineOfSight().selector(new BasiliskAlertableEntitiesSelector());

        protected boolean hasShelter() {
            BlockPos blockpos = new BlockPos(Basilisk.this.getX(), Basilisk.this.getBoundingBox().maxY, Basilisk.this.getZ());
            return !Basilisk.this.level.canSeeSky(blockpos) && Basilisk.this.getWalkTargetValue(blockpos) >= 0.0F;
        }

        protected boolean alertable() {
            return !Basilisk.this.level.getNearbyEntities(LivingEntity.class, this.alertableTargeting, Basilisk.this, Basilisk.this.getBoundingBox().inflate(20.0D, 10.0D, 20.0D)).isEmpty();
        }
    }

    //tendangan maut
    public class BasiliskPounceGoal extends JumpGoal {
        public boolean canUse() {
            LivingEntity livingentity = Basilisk.this.getTarget();
            if (livingentity != null && Basilisk.this.distanceTo(livingentity) > 3.0F) {
                return false;
            }
            if (Basilisk.this.getVariant() == Type.GUINEAFOWL) {
                return false;
            } else {
                if (!Basilisk.this.isFullyCrouched()) {
                    return false;
                } else {
                    if (livingentity != null && livingentity.isAlive()) {
                        if (livingentity.getMotionDirection() != livingentity.getDirection()) {
                            return false;
                        } else {
                            boolean flag = Basilisk.isPathClear(Basilisk.this, livingentity);
                            if (!flag) {
                                Basilisk.this.getNavigation().createPath(livingentity, 0);
                                Basilisk.this.setIsCrouching(false);
                            }

                            return flag;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        public boolean canContinueToUse() {
            LivingEntity livingentity = Basilisk.this.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                double d0 = Basilisk.this.getDeltaMovement().y;
                return (!(d0 * d0 < (double)0.1F) || !Basilisk.this.onGround);
            } else {
                return false;
            }
        }

        public boolean isInterruptable() {
            return false;
        }

        public void start() {
            Basilisk.this.setJumping(true);
            Basilisk.this.setIsPouncing(true);
            LivingEntity livingentity = Basilisk.this.getTarget();
            if (livingentity != null) {
                Basilisk.this.getLookControl().setLookAt(livingentity, 60.0F, 30.0F);
                Vec3 vec3 = (new Vec3(livingentity.getX() - Basilisk.this.getX(), livingentity.getY() - Basilisk.this.getY(), livingentity.getZ() - Basilisk.this.getZ())).normalize();
                Basilisk.this.setDeltaMovement(Basilisk.this.getDeltaMovement().add(vec3.x * 0.8D, vec3.y, vec3.z * 0.8D));
            }

            Basilisk.this.getNavigation().stop();
        }

        public void stop() {
            Basilisk.this.setIsPouncing(false);
        }

        public void tick() {
            LivingEntity livingentity = Basilisk.this.getTarget();
            if (livingentity != null) {
                Basilisk.this.getLookControl().setLookAt(livingentity, 60.0F, 30.0F);
            }

            if (livingentity != null && Basilisk.this.distanceTo(livingentity) <= 3.0F) {
                Basilisk.this.doHurtTarget(livingentity);
            } else if (Basilisk.this.getXRot() > 0.0F && Basilisk.this.onGround && (float)Basilisk.this.getDeltaMovement().y != 0.0F && Basilisk.this.level.getBlockState(Basilisk.this.blockPosition()).is(Blocks.SNOW)) {
                Basilisk.this.setTarget(null);
            }

        }
    }

    //nengak-nengok
    class PerchAndSearchGoal extends Basilisk.BasiliskBehaviorGoal {
        private double relX;
        private double relZ;
        private int lookTime;
        private int looksRemaining;

        public PerchAndSearchGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            return Basilisk.this.getLastHurtByMob() == null && Basilisk.this.getRandom().nextFloat() < 0.02F && !Basilisk.this.isSleeping() && Basilisk.this.getTarget() == null && Basilisk.this.getNavigation().isDone() && !this.alertable();
        }

        public boolean canContinueToUse() {
            return this.looksRemaining > 0;
        }

        public void start() {
            Basilisk.this.setPerched(true);
            this.resetLook();
            this.looksRemaining = Basilisk.this.getRandom().nextInt(3) + Basilisk.this.getRandom().nextInt( 2) + Basilisk.this.getRandom().nextInt( 1);
            Basilisk.this.getNavigation().stop();
        }

        public void stop() {
            Basilisk.this.setPerched(false);
        }

        public void tick() {
            --this.lookTime;
            if (this.lookTime <= 0) {
                --this.looksRemaining;
                this.resetLook();
            }

            Basilisk.this.getLookControl().setLookAt(Basilisk.this.getX() + this.relX, Basilisk.this.getEyeY(), Basilisk.this.getZ() + this.relZ, (float)Basilisk.this.getMaxHeadYRot(), (float)Basilisk.this.getMaxHeadXRot());
        }

        private void resetLook() {
            double d0 = (Math.PI * 2D) * Basilisk.this.getRandom().nextDouble();
            this.relX = Math.cos(d0);
            this.relZ = Math.sin(d0);
            this.lookTime = this.adjustedTickDelay(100 + Basilisk.this.getRandom().nextInt(50) + Basilisk.this.getRandom().nextInt(25));
        }
    }

    //turu
    class SleepGoal extends Basilisk.BasiliskBehaviorGoal {
        private static final int WAIT_TIME_BEFORE_SLEEP = reducedTickDelay(140);
        private int countdown = Basilisk.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);

        public SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        public boolean canUse() {
            if (Basilisk.this.xxa == 0.0F && Basilisk.this.yya == 0.0F && Basilisk.this.zza == 0.0F) {
                return this.canSleep() || Basilisk.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return Basilisk.this.level.isDay() && this.hasShelter() && !this.alertable() && !Basilisk.this.isInPowderSnow && !Basilisk.this.isInWater();
            }
        }

        public void stop() {
            this.countdown = Basilisk.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
            Basilisk.this.clearStates();
        }

        public void start() {
            Basilisk.this.setPerched(false);
            Basilisk.this.setIsCrouching(false);
            Basilisk.this.setJumping(false);
            Basilisk.this.setSleeping(true);
            Basilisk.this.getNavigation().stop();
            Basilisk.this.getMoveControl().setWantedPosition(Basilisk.this.getX(), Basilisk.this.getY(), Basilisk.this.getZ(), 0.0D);
        }
    }

    //variasi warna
    public enum Type implements StringRepresentable {
        JUNGLEFOWL(0, "jungle"),
        GUINEAFOWL(1, "guinea");

        public static final StringRepresentable.EnumCodec<Basilisk.Type> CODEC = StringRepresentable.fromEnum(Basilisk.Type::values);
        private static final IntFunction<Basilisk.Type> BY_ID = ByIdMap.continuous(Basilisk.Type::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private final int id;
        private final String name;

        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public @NotNull String getSerializedName() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public static Basilisk.Type byName(String name) {
            return CODEC.byName(name, JUNGLEFOWL);
        }

        public static Basilisk.Type byId(int id) {
            return BY_ID.apply(id);
        }

        public static Basilisk.Type byBiome(Holder<Biome> biomeHolder) {
            if (biomeHolder.value().getPrecipitation() == Biome.Precipitation.NONE) {
                return GUINEAFOWL;
            } else {
                return JUNGLEFOWL;
            }
        }
    }

    public static class BasiliskGroupData extends AgeableMob.AgeableMobGroupData {
        public final Basilisk.Type type;

        public BasiliskGroupData(Basilisk.Type type) {
            super(false);
            this.type = type;
        }
    }

     static class BasiliskStalkPreyGoal extends Goal {
        protected final Basilisk basilisk;
        private double speedModifier = 0.5D;
        private Path path;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;

        public BasiliskStalkPreyGoal(Basilisk basilisk) {
            this.basilisk = basilisk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.basilisk.isBaby()) {
                return false;
            }

            if (this.basilisk.getVariant() == Type.GUINEAFOWL) {
                return false;
            }

            if (this.basilisk.getTarget() instanceof Basilisk) {
                return false;
            }

            long gameTime = this.basilisk.level.getGameTime();
            if (gameTime - this.lastCanUseCheck < 20L) {
                return false;
            }

            this.lastCanUseCheck = gameTime;
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            this.path = this.basilisk.getNavigation().createPath(livingEntity, 0);
            if (this.path != null) {
                return true;
            }

            return this.getAttackReachSqr(livingEntity) >= this.basilisk.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            return !this.basilisk.getNavigation().isDone();
        }

        @Override
        public void start() {
            LivingEntity target = this.basilisk.getTarget();

            if (target == null) {
                return;
            }

            this.speedModifier = this.basilisk.distanceTo(target) > 10.0D ? 0.5D : 1.7D;
            this.basilisk.getNavigation().moveTo(this.path, this.speedModifier);
            this.basilisk.setAggressive(true);

            if (lastCanUseCheck == 0L) {
                this.basilisk.playSound(SoundEvents.CAT_HISS);
            }

            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
        }

        @Override
        public void stop() {
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
                this.basilisk.setTarget(null);
            }

            this.basilisk.setAggressive(false);
            this.basilisk.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.basilisk.getTarget();
            if (target == null) {
                return;
            }

            this.speedModifier = this.basilisk.distanceTo(target) > 10.0D ? 0.5D : 1.7D;
            this.basilisk.getLookControl().setLookAt(target, 90.0F, 90.0F);
            double d = this.basilisk.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if (this.basilisk.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0 || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0 || this.basilisk.getRandom().nextFloat() < 0.05f)) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.basilisk.getRandom().nextInt(7);

                if (d > 1024.0) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d > 256.0) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.basilisk.getNavigation().moveTo(target, this.speedModifier)) {
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
                this.basilisk.playSound(SoundEvents.FOX_BITE);
                this.basilisk.swing(InteractionHand.MAIN_HAND);
                this.basilisk.doHurtTarget(enemy);
            }
        }

        protected void resetAttackCooldown() {
            this.ticksUntilNextAttack = this.adjustedTickDelay(20);
        }

        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return this.basilisk.getBbWidth() * 2.0f * (this.basilisk.getBbWidth() * 2.0f) + attackTarget.getBbWidth();
        }
    }

    static class BasiliskChasePreyGoal extends Goal {
        protected final Basilisk basilisk;
        private double speedModifier = 0.5D;
        private Path path;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;

        public BasiliskChasePreyGoal (Basilisk basilisk) {
            this.basilisk = basilisk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.basilisk.isBaby()) {
                return false;
            }

            if (this.basilisk.getVariant() == Type.JUNGLEFOWL) {
                return false;
            }

            if (this.basilisk.getTarget() instanceof Basilisk) {
                return false;
            }

            long gameTime = this.basilisk.level.getGameTime();
            if (gameTime - this.lastCanUseCheck < 20L) {
                return false;
            }

            this.lastCanUseCheck = gameTime;
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            this.path = this.basilisk.getNavigation().createPath(livingEntity, 0);
            if (this.path != null) {
                return true;
            }

            return this.getAttackReachSqr(livingEntity) >= this.basilisk.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (livingEntity == null) {
                return false;
            }

            if (!livingEntity.isAlive()) {
                return false;
            }

            return !this.basilisk.getNavigation().isDone();
        }

        @Override
        public void start() {
            LivingEntity target = this.basilisk.getTarget();

            if (target == null) {
                return;
            }

            this.speedModifier = this.basilisk.distanceTo(target) > 15.0D ? 1.0D : 1.7D;
            this.basilisk.getNavigation().moveTo(this.path, this.speedModifier);
            this.basilisk.setAggressive(true);

            if (lastCanUseCheck == 0L) {
                this.basilisk.playSound(SoundEvents.CAT_HISS);
            }

            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
        }

        @Override
        public void stop() {
            LivingEntity livingEntity = this.basilisk.getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
                this.basilisk.setTarget(null);
            }

            this.basilisk.setAggressive(false);
            this.basilisk.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.basilisk.getTarget();
            if (target == null) {
                return;
            }

            this.speedModifier = this.basilisk.distanceTo(target) > 15.0D ? 1.0D : 1.7D;
            this.basilisk.getLookControl().setLookAt(target, 90.0F, 90.0F);
            double d = this.basilisk.distanceToSqr(target.getX(), target.getY(), target.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if (this.basilisk.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0 || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0 || this.basilisk.getRandom().nextFloat() < 0.05f)) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.basilisk.getRandom().nextInt(7);

                if (d > 1024.0) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d > 256.0) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.basilisk.getNavigation().moveTo(target, this.speedModifier)) {
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
                this.basilisk.playSound(SoundEvents.FOX_BITE);
                this.basilisk.swing(InteractionHand.MAIN_HAND);
                this.basilisk.doHurtTarget(enemy);
            }
        }

        protected void resetAttackCooldown() {
            this.ticksUntilNextAttack = this.adjustedTickDelay(20);
        }

        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return this.basilisk.getBbWidth() * 2.0f * (this.basilisk.getBbWidth() * 2.0f) + attackTarget.getBbWidth();
        }
    }

    static class BasiliskStareGoal extends Goal {
        private final Basilisk basilisk;
        private int attackTime;

        public BasiliskStareGoal(Basilisk basilisk) {
            this.basilisk = basilisk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            LivingEntity livingentity = this.basilisk.getTarget();
            return livingentity != null && !(livingentity instanceof Basilisk) && livingentity.isAlive() && this.basilisk.shouldStare(livingentity) && this.basilisk.isLookingAtMe(livingentity);
        }

        public boolean canContinueToUse() {
            if (this.basilisk.getVariant() == Type.GUINEAFOWL) {
                return super.canContinueToUse() && (this.basilisk.getTarget() != null && this.basilisk.distanceToSqr(this.basilisk.getTarget()) > 30.0D && this.basilisk.isLookingAtMe(this.basilisk.getTarget()));
            } else {
                return super.canContinueToUse() && (this.basilisk.getTarget() != null && this.basilisk.distanceToSqr(this.basilisk.getTarget()) > 20.0D && this.basilisk.isLookingAtMe(this.basilisk.getTarget()));
            }
        }

        public void start() {
            this.attackTime = 0;
            this.basilisk.getNavigation().stop();
            LivingEntity livingentity = this.basilisk.getTarget();

            if (livingentity != null) {
                this.basilisk.getLookControl().setLookAt(livingentity, 90.0F, 90.0F);
            }

            this.basilisk.hasImpulse = true;
        }

        public void stop() {
            this.basilisk.setActiveAttackTarget(0);
            this.basilisk.setTarget(null);
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            LivingEntity livingentity = this.basilisk.getTarget();

            if (livingentity != null) {
                this.basilisk.getNavigation().stop();
                this.basilisk.getLookControl().setLookAt(livingentity, 90.0F, 90.0F);

                if (!this.basilisk.hasLineOfSight(livingentity)) {
                    this.basilisk.setTarget(null);
                } else {
                    ++this.attackTime;
                    if (this.attackTime == 0) {
                        this.basilisk.setActiveAttackTarget(livingentity.getId());

                        if (!this.basilisk.isSilent()) {
                            this.basilisk.level.broadcastEntityEvent(this.basilisk, (byte)21);
                        }
                    } else if (this.attackTime >= this.basilisk.getAttackDuration()) {
                        float f = 1.0F;

                        if (this.basilisk.getVariant() == Type.GUINEAFOWL) {
                            f += 1.5F;
                        } else {
                            f += 0.0F;
                        }

                        livingentity.hurt(DamageSource.indirectMagic(this.basilisk, this.basilisk), f);
                        this.basilisk.setTarget(livingentity);
                    }

                    super.tick();
                }
            }
        }
    }

    static class BasiliskCourtGoal extends Goal {
        private final Basilisk basilisk;
        private final Predicate<Mob> followPredicate;
        private Basilisk followingMob;
        private final double speedModifier;
        private final PathNavigation navigation;
        private int timeToRecalcPath;
        private final float stopDistance;
        private float oldWaterCost;
        private final float areaSize;
        private int courtTimer;
        private int courtCooldown;

        public BasiliskCourtGoal(Basilisk basilisk, double speedModifier, float stopDistance, float areaSize) {
            this.basilisk = basilisk;
            this.followPredicate = (follow) -> follow != null && basilisk.getClass() != follow.getClass();
            this.speedModifier = speedModifier;
            this.navigation = basilisk.getNavigation();
            this.stopDistance = stopDistance;
            this.areaSize = areaSize;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            if (!(basilisk.getNavigation() instanceof GroundPathNavigation) && !(basilisk.getNavigation() instanceof FlyingPathNavigation)) {
                throw new IllegalArgumentException("Unsupported mob type for FollowMobGoal");
            }
        }

        @Override
        public boolean canUse() {
            List<Basilisk> list = this.basilisk.level.getEntitiesOfClass(Basilisk.class, this.basilisk.getBoundingBox().inflate(this.areaSize), this.followPredicate);
            if (!list.isEmpty()) {
                for(Basilisk mob : list) {
                    if (!mob.isInvisible() && !mob.isMale() && courtCooldown == 0 && this.basilisk.isMale() && this.basilisk.getTarget() == null && !this.basilisk.isSleeping()) {
                        this.followingMob = mob;
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean canContinueToUse() {
            return courtTimer > 0 && this.followingMob != null && !this.navigation.isDone() && this.basilisk.distanceToSqr(this.followingMob) > (double)(this.stopDistance * this.stopDistance);
        }

        public void start() {
            courtTimer = 100;
            this.basilisk.setCourting(true);
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.basilisk.getPathfindingMalus(BlockPathTypes.WATER);
            this.basilisk.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        }

        public void stop() {
            courtCooldown = 500;
            this.basilisk.setCourting(false);
            this.followingMob = null;
            this.navigation.stop();
            this.basilisk.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
        }

        public void tick() {
            if (courtTimer > 0) {
                courtTimer--;
            }

            if (courtCooldown > 0) {
                courtCooldown--;
            }

            if (this.followingMob != null && !this.basilisk.isLeashed()) {
                this.basilisk.getLookControl().setLookAt(this.followingMob, 10.0F, (float)this.basilisk.getMaxHeadXRot());
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay(10);
                    double d0 = this.basilisk.getX() - this.followingMob.getX();
                    double d1 = this.basilisk.getY() - this.followingMob.getY();
                    double d2 = this.basilisk.getZ() - this.followingMob.getZ();
                    double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                    if (!(d3 <= (double)(this.stopDistance * this.stopDistance))) {
                        this.navigation.moveTo(this.followingMob, this.speedModifier);
                    } else {
                        this.navigation.stop();
                        LookControl lookcontrol = this.followingMob.getLookControl();
                        if (d3 <= (double)this.stopDistance || lookcontrol.getWantedX() == this.basilisk.getX() && lookcontrol.getWantedY() == this.basilisk.getY() && lookcontrol.getWantedZ() == this.basilisk.getZ()) {
                            double d4 = this.followingMob.getX() - this.basilisk.getX();
                            double d5 = this.followingMob.getZ() - this.basilisk.getZ();
                            this.navigation.moveTo(this.basilisk.getX() - d4, this.basilisk.getY(), this.basilisk.getZ() - d5, this.speedModifier);
                        }

                    }
                }
            }
        }
    }
}

