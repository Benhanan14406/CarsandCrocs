package io.github.ads.ads.entity.types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class GenderedMob extends Animal {
    private static final EntityDataAccessor<Boolean> GENDER = SynchedEntityData.defineId(GenderedMob.class, EntityDataSerializers.BOOLEAN);

    protected GenderedMob(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    public boolean canMateWith(Animal otherAnimal) {
        if (otherAnimal instanceof GenderedMob genderedMob && otherAnimal != this && otherAnimal.getClass() == this.getClass()) {
            return this.isMale() && !genderedMob.isMale() || !this.isMale() && genderedMob.isMale();
        }
        return false;
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.setGender(ThreadLocalRandom.current().nextBoolean());
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GENDER, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Gender", this.isMale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setGender(compound.getBoolean("Gender"));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    public boolean isMale() {
        return this.entityData.get(GENDER);
    }

    public void setGender(boolean male) {
        this.entityData.set(GENDER, male);
    }
}
