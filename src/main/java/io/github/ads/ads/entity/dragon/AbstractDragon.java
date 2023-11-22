package io.github.ads.ads.entity.dragon;

import io.github.ads.ads.client.Keybinds;
import io.github.ads.ads.entity.ai.*;
import io.github.ads.ads.entity.ai.movement.BetterGroundPathNavigation;
import io.github.ads.ads.entity.ai.movement.DragonMoveController;
import io.github.ads.ads.entity.types.IAgeableMob;
import io.github.ads.ads.entity.types.TamableGenderedMob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;

public class AbstractDragon extends TamableGenderedMob implements FlyingAnimal, PlayerRideable, IAgeableMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_ID_OWNER_UUID = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> AGE_TICKS = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BREATH = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROAR = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_AGE = SynchedEntityData.defineId(AbstractDragon.class, EntityDataSerializers.BOOLEAN);
    public static final double BASE_SPEED_GROUND = 1;
    public static final double BASE_SPEED_FLYING = 1.25;
    public static final double BASE_DAMAGE = 5;
    public static final double BASE_HEALTH = 20;
    public static final double BASE_FOLLOW_RANGE = 50;
    public static final int BASE_KB_RESISTANCE = 10;
    public static final float BASE_WIDTH = 5f;
    public static final float BASE_HEIGHT = 5f;
    public static final String NBT_SADDLED = "Saddle";
    public static final int GROUND_CLEARENCE_THRESHOLD = 3;
    public boolean flying;
    public boolean nearGround;
    public int ageTicks = 24000;
    public int roarTicks = 100;
    public final BetterGroundPathNavigation groundNavigation;
    public final FlyingPathNavigation flyingNavigation;

    public AbstractDragon(EntityType<? extends AbstractDragon> type, Level level) {
        super(type, level);

        noCulling = true;

        moveControl = new DragonMoveController(this);

        //bisa terbang sama jalan
        flyingNavigation = new FlyingPathNavigation(this, level);
        groundNavigation = new BetterGroundPathNavigation(this, level);

        flyingNavigation.setCanFloat(true);
        groundNavigation.setCanFloat(true);

        navigation = groundNavigation;
    }

    @Override
    @NotNull
    public BodyRotationControl createBodyControl()
    {
        return new DragonBodyController(this);
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @org.jetbrains.annotations.Nullable SpawnGroupData spawnDataIn, @org.jetbrains.annotations.Nullable CompoundTag dataTag) {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setAgeInDays(this.getRandom().nextInt(10) + 10);
        this.setGender(ThreadLocalRandom.current().nextBoolean());
        this.randomizeAttributes(worldIn.getRandom());
        return spawnDataIn;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_OWNER_UUID, Optional.empty());
        entityData.define(DATA_SADDLED, false);
        entityData.define(BREATH, false);
        entityData.define(ROAR, false);
        entityData.define(CAN_AGE, false);
        this.entityData.define(AGE_TICKS, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (DATA_FLAGS_ID.equals(data)) refreshDimensions();
        else super.onSyncedDataUpdated(data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(NBT_SADDLED, isSaddled());
        compound.putInt("AgeTicks", this.getAgeInTicks());
        compound.putBoolean("FireBreathing", this.isFireBreathing());
        compound.putBoolean("Roaring", this.isRoaring());
        compound.putBoolean("CanAge", this.canAge());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setSaddled(compound.getBoolean(NBT_SADDLED));
        this.setAgeInTicks(compound.getInt("AgeTicks"));
        this.setFireBreath(compound.getBoolean("FireBreathing"));
        this.setRoaring(compound.getBoolean("Roaring"));
        this.setCanAge(compound.getBoolean("CanAge"));
    }

    protected void randomizeAttributes(RandomSource source) {
    }

    public boolean isSaddled() {
        return entityData.get(DATA_SADDLED);
    }

    public boolean isSaddleable() {
        return isAlive() && !isChild() && isTame();
    }

    public void equipSaddle(@Nullable SoundSource source) {
        setSaddled(true);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.HORSE_SADDLE, getSoundSource(), 1, 1);
    }

    public void setSaddled(boolean saddled) {
        entityData.set(DATA_SADDLED, saddled);
    }

    public boolean canFly() {
        return !isChild();
    }

    public boolean shouldFly() {
        if (isFlying()) {
            return !onGround;
        }
        return canFly() && !isInWater() && !isNearGround();
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isNearGround() {
        return nearGround;
    }

    public void setNavigation(boolean flying) {
        navigation = flying ?
                flyingNavigation :
                groundNavigation;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_ID_OWNER_UUID).orElse((UUID)null);
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(ownerUUID));
    }

    protected void doPlayerRide(Player player) {
        if (!this.level.isClientSide) {
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
        }

    }

    public boolean isImmobile() {
        return super.isImmobile() && this.isVehicle();
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return super.isControlledByLocalInstance();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        var stackResult = stack.interactLivingEntity(player, this, hand);
        if (stackResult.consumesAction()) return stackResult;

        if (!isTame()) {
            if (isServer()) {
                stack.shrink(1);
                tamedFor(player, getRandom().nextInt(5) == 0);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (getHealthRelative() < 1 && isFoodItem(stack)) {
            heal(stack.getItem().getFoodProperties(stack, this).getNutrition());
            stack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (isTamedFor(player) && isSaddleable() && !isSaddled() && stack.getItem() instanceof SaddleItem) {
            stack.shrink(1);
            equipSaddle(getSoundSource());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (isTamedFor(player) && (player.isSecondaryUseActive() || stack.is(Items.BONE))) {
            if (isServer()) {
                navigation.stop();
                setOrderedToSit(!isOrderedToSit());
                if (isOrderedToSit()) setTarget(null);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (isTamedFor(player) && (player.isSecondaryUseActive() || stack.is(Items.POISONOUS_POTATO))) {
            if (isServer()) {
                this.setCanAge(false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (isTamedFor(player) && !isChild() && !isFood(stack)) {
            if (isServer()) {
                setRidingPlayer(player);
                navigation.stop();
                setTarget(null);
            }
            this.doPlayerRide(player);
            setOrderedToSit(false);
            setInSittingPose(false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        final ItemStack itemstack = player.getItemInHand(hand);
        if (isFood(itemstack)) {
            if (getAgeInDays() < 100 && canAge()) {
                this.setAgeInDays(this.getAgeInDays() + 1);
            }
            this.heal(Math.min(this.getHealth(), (int) (this.getMaxHealth() / 2)));
            return InteractionResult.SUCCESS;
        } else if (itemstack.is(Items.NETHER_STAR)) {
            this.setTame(true);
        }

        return super.mobInteract(player, hand);
    }

    public void liftOff() {
        if (canFly()) {
             jumpFromGround();
        }
    }

    @Override
    protected float getJumpPower() {
        return super.getJumpPower() * (canFly()? 3 : 1);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return !canFly() && super.causeFallDamage(pFallDistance, pMultiplier, pSource);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    public SoundEvent getStepSound() {
        return SoundEvents.RAVAGER_STEP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public SoundEvent getEatingSound(ItemStack itemStackIn) {
        return SoundEvents.GENERIC_EAT;
    }

    public SoundEvent getAttackSound() {
        return SoundEvents.EVOKER_FANGS_ATTACK;
    }

    @Override
    protected void playStepSound(BlockPos entityPos, BlockState state) {
        if (isInWater()) {
            return;
        }

        if (isChild()) {
            super.playStepSound(entityPos, state);
            return;
        }

        var soundType = state.getSoundType();
        if (level.getBlockState(entityPos.above()).getBlock() == Blocks.SNOW) {
            soundType = Blocks.SNOW.getSoundType(state, level, entityPos, this);
        }

        playSound(getStepSound(), soundType.getVolume(), soundType.getPitch() * getVoicePitch());
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return getScale();
    }

    @Override
    public float getVoicePitch() {
        return 2 - getScale();
    }

    public boolean isFoodItem(ItemStack stack) {
        var food = stack.getItem().getFoodProperties(stack, this);
        return food != null && food.isMeat();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
    }

    public void tamedFor(Player player, boolean successful) {
        if (successful) {
            setTame(true);
            navigation.stop();
            setTarget(null);
            setOwnerUUID(player.getUUID());
            level.broadcastEntityEvent(this, (byte) 7);
        } else {
            level.broadcastEntityEvent(this, (byte) 6);
        }
    }

    public boolean isTamedFor(Player player) {
        return isTame() && isOwnedBy(player);
    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return sizeIn.height * 1.2f;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() - 3;
    }

    @Override
    public float getScale() {
        return (0.5f + (0.015f * getAgeInDays()));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);

        if (isSaddled()) {
            spawnAtLocation(Items.SADDLE);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
        boolean attacked = entityIn.hurt(DamageSource.mobAttack(this), (float) getAttribute(ATTACK_DAMAGE).getValue());
        if (attacked) {
            doEnchantDamageEffects(this, entityIn);
        }

        return attacked;
    }

    @Override
    public void swing(InteractionHand hand) {
        playSound(getAttackSound(), 1, 0.7f);
        if (this.isControlledByLocalInstance() && Keybinds.MOUNT_ATTACK_KEY.isDown() && !isFireBreathing()) {
            this.swing(InteractionHand.MAIN_HAND, true);
            super.swing(hand);
        } else {
            super.swing(hand);
        }
    }

    @Override
    public boolean hurt(DamageSource src, float par2) {
        if (isInvulnerableTo(src)) {
            return false;
        }

        setOrderedToSit(false);

        return super.hurt(src, par2);
    }

    @Override
    public boolean canMate(Animal mate) {
        if (mate == this) {
            return false;
        }
        else if (!(mate instanceof AbstractDragon)) {
            return false;
        }
        else if (!canReproduce()) {
            return false;
        }

        AbstractDragon dragonMate = (AbstractDragon) mate;

        if (!dragonMate.isTame()) {
            return false;
        }
        else if (!dragonMate.canReproduce()) {
            return false;
        }
        else return isInLove() && dragonMate.isInLove();
    }

    public boolean canReproduce() {
        return isTame();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !(target instanceof TamableAnimal tameable) || !Objects.equals(tameable.getOwner(), owner);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isChild() && !canBeControlledByRider() && super.canAttack(target);
    }

    public boolean canBeControlledByRider() {
        return getControllingPassenger() instanceof LivingEntity driver && isOwnedBy(driver);
    }

    @Override
    public Entity getControllingPassenger() {
        List<Entity> list = getPassengers();
        return list.isEmpty()? null : list.get(0);
    }

    public void setRidingPlayer(Player player) {
        player.setYRot(getYRot());
        player.setXRot(getXRot());
        player.startRiding(this);
    }

    @Override
    protected void addPassenger(Entity pPassenger) {
        super.addPassenger(pPassenger);
        if (!isServer() && isControlledByLocalInstance());
    }

    @Override
    public void positionRider(Entity passenger) {
        Entity riddenByEntity = getControllingPassenger();
        if (riddenByEntity != null) {
            Vec3 pos = new Vec3(0, getPassengersRidingOffset() + riddenByEntity.getMyRidingOffset(), getScale())
                    .yRot((float) Math.toRadians(-yBodyRot))
                    .add(position());
            if (this.getDragonStage() < 4) {
                passenger.setPos(pos.x, pos.y + 0.025f * getAgeInDays(), pos.z);
            } else {
                passenger.setPos(pos.x, pos.y + 0.025f * getAgeInDays(), pos.z);
            }

            if (getFirstPassenger() instanceof LivingEntity) {
                LivingEntity rider = ((LivingEntity) riddenByEntity);
                rider.xRotO = rider.getXRot();
                rider.yRotO = rider.getYRot();
                rider.yBodyRot = yBodyRot;
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        Entity srcEnt = src.getEntity();
        if (srcEnt != null && (srcEnt == this || hasPassenger(srcEnt))) {
            return true;
        }

        if (src == DamageSource.DRAGON_BREATH
                        || src == DamageSource.CACTUS
                        || src == DamageSource.IN_WALL
                        || src == DamageSource.FLY_INTO_WALL
                        || src == DamageSource.STALAGMITE
                        || src == DamageSource.OUT_OF_WORLD
                        || src == DamageSource.MAGIC
                        || src == DamageSource.CRAMMING) {
            return true;
        }
        return super.isInvulnerableTo(src);
    }

    public double getHealthRelative() {
        return getHealth() / (double) getMaxHealth();
    }


    @Override
    public void refreshDimensions() {
        double posXTmp = getX();
        double posYTmp = getY();
        double posZTmp = getZ();
        boolean onGroundTmp = onGround;

        super.refreshDimensions();

        setPos(posXTmp, posYTmp, posZTmp);

        onGround = onGroundTmp;
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        var height = isInSittingPose()? 2.15f : BASE_HEIGHT;
        var scale = getScale();
        return new EntityDimensions(BASE_WIDTH * scale, height * scale, false);
    }

    public boolean isFireBreathing() {
        return this.entityData.get(BREATH).booleanValue();
    }

    public void setFireBreath(boolean breath) {
        this.entityData.set(BREATH, breath);
    }

    public boolean isRoaring() {
        return this.entityData.get(ROAR).booleanValue();
    }

    public void setRoaring(boolean roar) {
        this.entityData.set(ROAR, roar);
    }

    public boolean canAge() {
        return this.entityData.get(CAN_AGE).booleanValue();
    }

    public void setCanAge(boolean age) {
        this.entityData.set(CAN_AGE, age);
    }

    public int getAgeInDays() {
        return this.entityData.get(AGE_TICKS) / 24000;
    }

    public void setAgeInDays(int age) {
        this.entityData.set(AGE_TICKS, age * 24000);
    }

    public int getAgeInTicks() {
        return this.entityData.get(AGE_TICKS);
    }

    public void setAgeInTicks(int age) {
        this.entityData.set(AGE_TICKS, age);
    }

    public int getDragonStage() {
        final int age = this.getAgeInDays();
        if (age >= 100) {
            return 5;
        } else if (age >= 75) {
            return 4;
        } else if (age >= 50) {
            return 3;
        } else if (age >= 25) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public boolean isTeen() {
        return getDragonStage() < 4 && getDragonStage() > 2;
    }

    @Override
    public boolean isAdult() {
        return getDragonStage() >= 4;
    }

    @Override
    public boolean isChild() {
        return getDragonStage() < 2;
    }

    public boolean isServer() {
        return !level.isClientSide;
    }

    @Override
    public boolean isInWall() {
        if (noPhysics) {
            return false;
        } else {
            var collider = getBoundingBox().deflate(getBbWidth() * 0.2f);
            return BlockPos.betweenClosedStream(collider).anyMatch((pos) -> {
                BlockState state = level.getBlockState(pos);
                return !state.isAir() && state.isSuffocating(level, pos) && Shapes.joinIsNotEmpty(state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(collider), BooleanOp.AND);
            });
        }
    }

    @Override
    public Vec3 getLightProbePosition(float p_20309_) {
        return new Vec3(getX(), getY() + getBbHeight(), getZ());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BetterGroundPathNavigation(this, level);
    }

    @Override
    public boolean isPushable() {
        if (this.getDragonStage() < 3) {
            return true;
        } else {
            return false;
        }
    }
}