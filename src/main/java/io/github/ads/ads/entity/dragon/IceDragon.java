package io.github.ads.ads.entity.dragon;

import io.github.ads.ads.Config;
import io.github.ads.ads.client.Keybinds;
import io.github.ads.ads.entity.ai.movement.DragonWander;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;
import static net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;

public class IceDragon extends AbstractDragon implements GeoEntity {
    public int fireTicks = 300;
    public int fireCooldownTimer = 1200;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public IceDragon(EntityType<? extends AbstractDragon> type, Level level) {
        super(type, level);
        if (getDragonStage() < 3) {
            this.maxUpStep = 1.0F;
        } else {
            this.maxUpStep = 3.0F;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(MOVEMENT_SPEED)
                .add(MAX_HEALTH)
                .add(FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(KNOCKBACK_RESISTANCE, BASE_KB_RESISTANCE)
                .add(ATTACK_DAMAGE)
                .add(FLYING_SPEED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        goalSelector.addGoal(2, new DragonWander(this, 1.0D));;
        targetSelector.addGoal(0, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(1, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NonTameRandomTargetGoal<>(this, Animal.class, false, e -> !(e instanceof AbstractDragon)));
    }

    private <E extends GeoEntity> PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("normal"));
        return PlayState.CONTINUE;
    }

    //set animasi
    private <E extends GeoEntity> PlayState movePredicate(software.bernie.geckolib.core.animation.AnimationState<E> event) {
        if (getPassengers() != null) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                if (isFlying()) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("fly"));
                } else if (isInWater() ) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("swim"));
                } else {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("walk"));
                }
            } else {
                if (isFlying()) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("flyidle"));
                } else {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("landidle"));
                }
            }
        } else {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                if (isFlying()) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("fly"));
                } else if (isInWater() ) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("swim"));
                } else {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("walk"));
                }
            } else {
                if (isFlying()) {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("flyidle"));
                } else {
                    event.getController().setAnimationSpeed(1.0F).setAnimation(RawAnimation.begin().thenLoop("landidle"));
                }
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

    private <E extends GeoEntity> PlayState roarPredicate(AnimationState<E> event) {
        if (isRoaring() && event.getController().getAnimationState().equals(AnimationController.State.STOPPED)) {
            event.setAndContinue(RawAnimation.begin().thenPlay("loudroar"));
        }
        event.getController().forceAnimationReset();

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController moveController = new AnimationController(this, "predicate", 0, this::predicate);
        moveController.setAnimationSpeed(1.0f);
        controllers.add(moveController);
        controllers.add(new AnimationController<>(this, "movePredicate", 10, this::movePredicate));
        controllers.add(new AnimationController<>(this, "roarPredicate", 10, this::roarPredicate));
        controllers.add(new AnimationController<>(this, "attackPredicate", 0, this::attackPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void tick() {
        super.tick();

        nearGround = onGround || !getLevel().noCollision(this, new AABB(getX(), getY(), getZ(), getX(), getY() - (GROUND_CLEARENCE_THRESHOLD * getScale()), getZ()));

        boolean flying = shouldFly();
        if (flying != isFlying()) {
            setFlying(flying);
            if (isServer()) {
                setNavigation(flying);
            }
        }

        //umur nambah
        if (ageTicks < 24000) {
            ageTicks++;
        }

        //umur nambah
        if (getAgeInDays() < 100 && ageTicks == 24000 && canAge()) {
            this.setAgeInDays(this.getAgeInDays() + 1);
            ageTicks = 0;
        }

        //tua = kuat
        if (getDragonStage() == 1) {
            this.getAttribute(MOVEMENT_SPEED).setBaseValue(BASE_SPEED_GROUND);
            this.getAttribute(FLYING_SPEED).setBaseValue(BASE_SPEED_FLYING);
            this.getAttribute(ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE);
        } else if (getDragonStage() == 2) {
            this.getAttribute(MOVEMENT_SPEED).setBaseValue(BASE_SPEED_GROUND + 0.25D);
            this.getAttribute(FLYING_SPEED).setBaseValue(BASE_SPEED_FLYING + 0.25D);
            this.getAttribute(ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE + 5.0D);
        } else if (getDragonStage() == 3) {
            this.getAttribute(MOVEMENT_SPEED).setBaseValue(BASE_SPEED_GROUND + 0.5D);
            this.getAttribute(FLYING_SPEED).setBaseValue(BASE_SPEED_FLYING + 0.5D);
            this.getAttribute(ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE + 10.0D);
        } else if (getDragonStage() > 3) {
            this.getAttribute(MOVEMENT_SPEED).setBaseValue(BASE_SPEED_GROUND + 0.75D);
            this.getAttribute(FLYING_SPEED).setBaseValue(BASE_SPEED_FLYING + 0.75D);
            this.getAttribute(ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE + 15.0D);
        }

        //tua = badag
        this.getAttribute(MAX_HEALTH).setBaseValue(BASE_HEALTH + getAgeInDays());

        //keluarin api
        if (this.isControlledByLocalInstance()) {
            if (Keybinds.DRAGON_BREATH_KEY.isDown() && !Keybinds.MOUNT_ROAR_KEY.isDown()) {
                setFireBreath(true);
            }

            if (Keybinds.MOUNT_ROAR_KEY.isDown() && roarTicks == 100) {
                setRoaring(true);
                roarTicks--;
            }
        }

        if (roarTicks == 0) {
            setRoaring(false);
            roarTicks = 100;
        }

        if (this.getTarget() != null && fireTicks == 0 && fireCooldownTimer == 0) {
            setFireBreath(true);
            fireTicks++;
        }

        if (fireTicks == 300) {
            setFireBreath(false);
            fireTicks = 0;
            fireCooldownTimer = 1200;
        }

        if (fireCooldownTimer > 0) {
            fireCooldownTimer--;
        }
    }

    @Override
    public void travel(@NotNull Vec3 vec3) {
        super.travel(vec3);

        boolean isFlying = isFlying();
        float flyspeed = (float) getAttributeValue(isFlying? FLYING_SPEED : MOVEMENT_SPEED) * 0.225f;
        float walkspeed = (float) getAttributeValue(MOVEMENT_SPEED);
        LivingEntity driver = (LivingEntity) getControllingPassenger();

        if (canBeControlledByRider() && driver != null) {
            double moveSideways = vec3.x;
            double moveY = vec3.y;
            double moveForward = Math.min(Math.abs(driver.zza) + Math.abs(driver.xxa), 1);

            //kalau diam nggak bisa muter
            if (moveForward > 0) {
                float yaw = driver.yHeadRot;
                yaw += (float) Mth.atan2(driver.zza, driver.xxa) * (180f / (float) Math.PI) - 90;
                yHeadRot = yaw;
                setXRot(driver.getXRot() * 0.68f);
            }

            setYRot(Mth.rotateIfNecessary(yHeadRot, getYRot(), 4));

            //set kontrol, dll
            if (isControlledByLocalInstance()) {
                if (isFlying) {
                    moveForward = moveForward > 0 ? moveForward : 0;
                    moveY = 0;
                    if (Keybinds.FLIGHT_ASCENT_KEY.isDown() && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                        moveY = 0.35;
                    } else if (Keybinds.FLIGHT_DESCENT_KEY.isDown() && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                        moveY = -0.35;
                    } else if (moveForward > 0 && Config.cameraFlight()) {
                        moveY = -driver.getXRot() * (Math.PI / 180);
                    }
                } else if (isInWater()) {
                    if (Keybinds.FLIGHT_ASCENT_KEY.isDown() && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                        moveY = 1.0;
                    } else if (Keybinds.FLIGHT_DESCENT_KEY.isDown() && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                        moveY = -1.0;
                    } else if (moveForward > 0 && Config.cameraFlight()) {
                        moveY = -driver.getXRot() * (Math.PI / 180);
                    }
                } else if (Keybinds.FLIGHT_ASCENT_KEY.isDown() && canFly()) {
                    setFlying(true);
                    setNoGravity(true);
                }

                if (!Keybinds.FLIGHT_ASCENT_KEY.isDown() && canFly() && isFlying && !isOnGround() && !isInWater()) {
                    setFlying(true);
                    setNoGravity(true);
                }

                if (isFlying && isOnGround()) {
                    setFlying(false);
                    setNoGravity(false);
                }

                if (isFlying && isInWater()) {
                    setFlying(false);
                    setNoGravity(false);
                }

                if (isControlledByLocalInstance()) {
                    if (Keybinds.WALK_FORWARD_KEY.isDown()) {
                        vec3 = new Vec3(moveSideways, moveY, moveForward);
                        if (isFlying) {
                            setSpeed(flyspeed);
                        } else {
                            setSpeed(walkspeed);
                        }
                    } else {
                        this.setSpeed(0F);
                    }
                }
            } else {
                calculateEntityAnimation(this, true);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }

        //set cepat laju
        if (isFlying) {
            moveRelative(flyspeed, vec3);
        } else if (isInWater()) {
            moveRelative(flyspeed, vec3);
        } else if (isOnGround()) {
            moveRelative(walkspeed, vec3);
        } else {
            super.travel(vec3);
        }
    }

    //napas di air
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    //dinaikin di air
    @Override
    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
        return true;
    }

    //nggak bisa tenggelam
    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }
}
