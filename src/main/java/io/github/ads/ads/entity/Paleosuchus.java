package io.github.ads.ads.entity;

import io.github.ads.ads.entity.ai.movement.*;
import io.github.ads.ads.entity.types.GenderedMob;
import io.github.ads.ads.entity.types.ISemiAquatic;
import io.github.ads.ads.registries.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Predicate;

public class Paleosuchus extends GenderedMob implements GeoEntity, ISemiAquatic, NeutralMob {
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(Paleosuchus.class, EntityDataSerializers.INT);
    private boolean isLandNavigator;
    public float baskingProgress = 0;
    public float prevBaskingProgress = 0;
    public int baskingType = 0;
    private int baskingTimer = 0;
    private int swimTimer = -1000;
    private int ticksSinceInWater = 0;
    private Goal landTargetGoal;
    private Goal angryTargetGoal;
    public static final Predicate<LivingEntity> PREY_SELECTOR = (p_248371_) -> {
        EntityType<?> entitytype = p_248371_.getType();
        return entitytype == EntityType.FROG || entitytype == EntityType.TROPICAL_FISH || entitytype == EntityType.COD || entitytype == EntityType.SALMON || entitytype == EntityType.TADPOLE || entitytype == EntityType.RABBIT || entitytype == EntityType.CHICKEN;
    };
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    @javax.annotation.Nullable
    private UUID persistentAngerTarget;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Paleosuchus(EntityType<? extends Animal> animal, Level level) {
        super(animal, level);
        switchNavigator(false);
        this.baskingType = random.nextInt(1);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.maxUpStep = 1f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("BaskingStyle", this.baskingType);
        compound.putInt("BaskingTimer", this.baskingTimer);
        compound.putInt("SwimTimer", this.swimTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.baskingType = compound.getInt("BaskingStyle");
        this.baskingTimer = compound.getInt("BaskingTimer");
        this.swimTimer = compound.getInt("SwimTimer");
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return ModEntityTypes.PALEOSUCHUS.get().create(level);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.landTargetGoal = new NearestAttackableTargetGoal<>(this, Animal.class, 10, false, false, (prey) -> {
            EntityType<?> entitytype = prey.getType();
            return entitytype == EntityType.FROG || entitytype == EntityType.TROPICAL_FISH || entitytype == EntityType.COD || entitytype == EntityType.SALMON || entitytype == EntityType.TADPOLE || entitytype == EntityType.RABBIT || entitytype == EntityType.CHICKEN;
        });
        this.angryTargetGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> this.getLastHurtByMob() != null);
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new BreathAirGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(1, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(1, new AnimalAILeaveWater(this));
        this.goalSelector.addGoal(3, new PanicGoal(this, 1D));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D, 4));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, false, PREY_SELECTOR));

    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> event) {
        if (!this.isInWater()) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                if (this.isSprinting()) {
                    event.getController().setAnimationSpeed(6.0F).setAnimation(RawAnimation.begin().thenLoop("walk"));
                } else {
                    event.getController().setAnimationSpeed(1.5F).setAnimation(RawAnimation.begin().thenLoop("walk"));
                }
            } else {
                if (this.baskingProgress > 0.0F) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("bask"));
                } else {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("landidle"));
                }
            }
        } else {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("swim"));
            } else {
                event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("wateridle"));
            }
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

    private void setTargetGoals() {
        this.targetSelector.addGoal(4, this.landTargetGoal);
        this.targetSelector.addGoal(3, this.angryTargetGoal);
    }

    @Override
    public boolean shouldEnterWater() {
        if (this.isOnGround() && !this.getPassengers().isEmpty()) {
            return true;
        }
        return this.getTarget() == null && this.baskingTimer <= 0 && !shouldLeaveWater() && swimTimer <= -1000;
    }

    @Override
    public boolean shouldLeaveWater() {
        if (!this.getPassengers().isEmpty()) {
            return false;
        }
        if (this.getTarget() != null && !this.getTarget().isInWater()) {
            return true;
        }
        return swimTimer > 600 && this.baskingTimer > 0;
    }

    @Override
    public boolean shouldStopMoving() {
        return this.baskingTimer > 0 && this.isOnGround();
    }

    @Override
    public int getWaterSearchRange() {
        return 100;
    }

    public void aiStep() {
        super.aiStep();
        if (!this.level.isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level, true);
        }

        if (this.getMoveControl().hasWanted()) {
            double speedModifier = this.getMoveControl().getSpeedModifier();
            if (speedModifier < 1.0D && this.isOnGround()) {
                this.setPose(Pose.CROUCHING);
                this.setSprinting(false);
            } else if (speedModifier >= 1.5D && this.isOnGround()) {
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

    public void tick() {
        super.tick();
        this.prevBaskingProgress = baskingProgress;

        final boolean ground = !this.isInWater();
        final boolean groundAnimate = !this.isInWater();
        final boolean basking = groundAnimate;

        if (!ground && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (ground && !this.isLandNavigator) {
            switchNavigator(true);
        }

        if (basking) {
            if (this.baskingProgress < 10F)
                this.baskingProgress++;
        } else {
            if (this.baskingProgress > 0F)
                this.baskingProgress--;
        }

        if (baskingTimer < 0) {
            baskingTimer++;
        }

        if (this.isInLove() && this.getTarget() != null) {
            this.setTarget(null);
        }

        if (!level.isClientSide) {
            if (isInWater()) {
                swimTimer++;
                ticksSinceInWater = 0;
            } else {
                ticksSinceInWater++;
                swimTimer--;
            }
        }
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            PathNavigation prevNav = this.navigation;
            this.navigation = new BetterGroundPathNavigation(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AquaticMoveController(this, 1F);
            PathNavigation prevNav = this.navigation;
            this.navigation = new SemiAquaticPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    public boolean doHurtTarget(Entity p_30372_) {
        boolean flag = p_30372_.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, p_30372_);
            this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundVolume(), this.getVoicePitch());
        }

        return flag;
    }

    public boolean isPushedByFluid() {
        return false;
    }

    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    public void setRemainingPersistentAngerTime(int p_30404_) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, p_30404_);
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Nullable
    @Override
    public LivingEntity getLastHurtByMob() {
        return null;
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity p_21669_) {

    }

    @Override
    public void setLastHurtByPlayer(@Nullable Player p_21680_) {

    }

    @Override
    public void setTarget(@Nullable LivingEntity p_21681_) {

    }

    @Override
    public boolean canAttack(LivingEntity p_181126_) {
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return null;
    }

    @javax.annotation.Nullable
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    public void setPersistentAngerTarget(@javax.annotation.Nullable UUID p_30400_) {
        this.persistentAngerTarget = p_30400_;
    }

    public boolean checkSpawnObstruction(LevelReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.DROWN || source == DamageSource.IN_WALL || super.isInvulnerableTo(source);
    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    public float getWalkTargetValue(BlockPos pos, LevelReader worldIn) {
        return super.getWalkTargetValue(pos, worldIn);
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            if (entity != null && !(entity instanceof Player)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.hurt(source, amount);
        }
    }
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }
}
