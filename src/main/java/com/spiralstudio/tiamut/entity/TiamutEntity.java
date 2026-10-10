package com.spiralstudio.tiamut.entity;

import com.spiralstudio.tiamut.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Tiamut - a colossal, very fast sea kaiju (pre-evolution Monsterverse Tiamat-style).
 * Neutral until provoked. Plays GeckoLib swim / fast-swim / bite / roar animations.
 */
public class TiamutEntity extends PathfinderMob implements GeoEntity {

    // ---------------------------------------------------------------- tuning
    public static final double MAX_HEALTH = 600.0D;
    /** Water speed ~ (attribute * 0.02)^... see README. Raise for an even faster swimmer. */
    public static final double SWIM_SPEED_ATTRIBUTE = 1.6D;
    public static final double CHASE_SPEED_MODIFIER = 1.35D;
    public static final double BITE_REACH_SQR = 72.0D; // head is ~9 blocks in front of the body centre

    // ---------------------------------------------------------------- animations
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.tiamut.idle");
    public static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.tiamut.swim");
    public static final RawAnimation SWIM_FAST = RawAnimation.begin().thenLoop("animation.tiamut.swim_fast");
    public static final RawAnimation BEACHED = RawAnimation.begin().thenLoop("animation.tiamut.beached");
    public static final RawAnimation BITE = RawAnimation.begin().then("animation.tiamut.bite", Animation.LoopType.PLAY_ONCE);

    public enum RoarType {
        SHORT("roar_short", "animation.tiamut.roar_short", 48),
        LONG("roar_long", "animation.tiamut.roar_long", 80),
        CHALLENGE("roar_challenge", "animation.tiamut.roar_challenge", 64);

        public final String trigger;
        public final String animation;
        public final int ticks;

        RoarType(String trigger, String animation, int ticks) {
            this.trigger = trigger;
            this.animation = animation;
            this.ticks = ticks;
        }

        public SoundEvent sound() {
            return switch (this) {
                case SHORT -> ModSounds.ROAR_SHORT.get();
                case LONG -> ModSounds.ROAR_LONG.get();
                case CHALLENGE -> ModSounds.ROAR_CHALLENGE.get();
            };
        }
    }

    // ---------------------------------------------------------------- state
    /** 0 = hovering, 1 = cruising, 2 = fast swim. Synced so the client animates without guessing velocity. */
    private static final EntityDataAccessor<Integer> SPEED_LEVEL = SynchedEntityData.defineId(TiamutEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ROARING = SynchedEntityData.defineId(TiamutEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.tiamut.tiamut"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private double smoothedSpeed = 0;
    private int roarTicks = 0;
    private int roarCooldown = 0;
    private int ambientRoarTimer = 600;
    private int biteTicks = 0;
    @Nullable
    private LivingEntity biteTarget;

    public TiamutEntity(EntityType<? extends TiamutEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SWIM_SPEED_ATTRIBUTE)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    public static boolean checkTiamutSpawnRules(EntityType<TiamutEntity> type, ServerLevelAccessor level,
                                                MobSpawnType reason, BlockPos pos, RandomSource random) {
        return pos.getY() < level.getSeaLevel() - 14
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above(8)).is(FluidTags.WATER)
                && random.nextInt(3) == 0;
    }

    // ---------------------------------------------------------------- setup
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPEED_LEVEL, 0);
        this.entityData.define(ROARING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RoarGoal());
        this.goalSelector.addGoal(1, new BiteGoal(this, CHASE_SPEED_MODIFIER));
        this.goalSelector.addGoal(3, new KaijuSwimGoal(this, 0.7D, 60));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 40.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Guardian.class, true));
    }

    // ---------------------------------------------------------------- roars
    public boolean isRoaring() {
        return this.entityData.get(ROARING);
    }

    public void roar(RoarType type, boolean force) {
        if (this.level().isClientSide) return;
        if (!force && (this.roarTicks > 0 || this.roarCooldown > 0)) return;
        this.roarTicks = type.ticks;
        this.roarCooldown = type.ticks + 100;
        this.entityData.set(ROARING, true);
        this.triggerAnim("action", type.trigger);
        this.level().playSound(null, this.getX(), this.getY() + 2.0D, this.getZ(), type.sound(), SoundSource.HOSTILE,
                10.0F, 0.95F + this.random.nextFloat() * 0.1F);
        if (this.level() instanceof ServerLevel server) {
            Vec3 head = this.position().add(Vec3.directionFromRotation(0, this.yBodyRot).scale(8.0D)).add(0, 2.2D, 0);
            server.sendParticles(ParticleTypes.BUBBLE, head.x, head.y, head.z, 80, 1.5, 1.0, 1.5, 0.25);
            server.sendParticles(ParticleTypes.SPLASH, head.x, head.y, head.z, 40, 2.0, 1.0, 2.0, 0.1);
        }
    }

    private class RoarGoal extends Goal {
        RoarGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return roarTicks > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return roarTicks > 0;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }
    }

    // ---------------------------------------------------------------- biting
    private void queueBite(LivingEntity target) {
        if (this.level().isClientSide) return;
        this.triggerAnim("action", "bite");
        this.biteTarget = target;
        this.biteTicks = 9; // jaws close ~0.45s into the animation
    }

    private static class BiteGoal extends MeleeAttackGoal {
        private final TiamutEntity kaiju;

        BiteGoal(TiamutEntity kaiju, double speed) {
            super(kaiju, speed, true);
            this.kaiju = kaiju;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && kaiju.isInWaterOrBubble() && !kaiju.isRoaring();
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return BITE_REACH_SQR + target.getBbWidth();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distSqr) {
            if (distSqr <= getAttackReachSqr(target) && isTimeToAttack() && kaiju.biteTicks <= 0) {
                resetAttackCooldown();
                kaiju.queueBite(target);
            }
        }
    }

    // ---------------------------------------------------------------- roaming
    private static class KaijuSwimGoal extends RandomStrollGoal {
        KaijuSwimGoal(PathfinderMob mob, double speed, int interval) {
            super(mob, speed, interval, false);
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            RandomSource r = this.mob.getRandom();
            for (int i = 0; i < 12; i++) {
                Vec3 d = new Vec3(r.nextGaussian() * 28, r.nextGaussian() * 7, r.nextGaussian() * 28);
                BlockPos p = BlockPos.containing(this.mob.position().add(d));
                if (this.mob.level().getFluidState(p).is(FluidTags.WATER)
                        && this.mob.level().getFluidState(p.above(3)).is(FluidTags.WATER)) {
                    return Vec3.atCenterOf(p);
                }
            }
            return null;
        }
    }

    // ---------------------------------------------------------------- tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        double v = this.getDeltaMovement().length();
        this.smoothedSpeed = this.smoothedSpeed * 0.85D + v * 0.15D;
        int level = this.smoothedSpeed > 0.38D ? 2 : this.smoothedSpeed > 0.07D ? 1 : 0;
        if (level != this.entityData.get(SPEED_LEVEL)) this.entityData.set(SPEED_LEVEL, level);

        if (this.roarTicks > 0 && --this.roarTicks == 0) this.entityData.set(ROARING, false);
        if (this.roarCooldown > 0) this.roarCooldown--;

        if (this.biteTicks > 0 && --this.biteTicks == 0 && this.biteTarget != null) {
            LivingEntity t = this.biteTarget;
            this.biteTarget = null;
            if (t.isAlive() && this.distanceToSqr(t) <= BITE_REACH_SQR * 1.5D) {
                this.doHurtTarget(t);
                this.level().playSound(null, t.getX(), t.getY(), t.getZ(), ModSounds.BITE.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null);
        if (this.getTarget() == null && --this.ambientRoarTimer <= 0) {
            this.ambientRoarTimer = 900 + this.random.nextInt(1800);
            this.roar(RoarType.SHORT, false);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean newlyAngry = target != null && this.getTarget() == null;
        super.setTarget(target);
        if (newlyAngry) this.roar(RoarType.CHALLENGE, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && amount >= 8.0F && this.random.nextFloat() < 0.3F) {
            this.roar(RoarType.LONG, false);
        }
        return hurt;
    }

    // ---------------------------------------------------------------- aquatic behaviour
    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(input);
        }
    }

    @Override
    public void baseTick() {
        int air = this.getAirSupply();
        super.baseTick();
        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirSupply(air - 1);
            if (this.getAirSupply() <= -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAirSupply(this.getMaxAirSupply());
        }
    }

    @Override public int getMaxAirSupply() { return 12000; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double dist) { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean checkSpawnObstruction(LevelReader level) { return level.isUnobstructed(this); }
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(14.0D, 4.0D, 14.0D); }
    @Override public int getMaxHeadYRot() { return 10; }

    // ---------------------------------------------------------------- sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.DEATH.get(); }
    @Override protected float getSoundVolume() { return 4.0F; }
    @Override public int getAmbientSoundInterval() { return 400; }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    // ---------------------------------------------------------------- GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 8, this::movementPredicate));

        AnimationController<TiamutEntity> action = new AnimationController<>(this, "action", 4, state -> PlayState.STOP);
        action.triggerableAnim("bite", BITE);
        for (RoarType type : RoarType.values()) {
            action.triggerableAnim(type.trigger, RawAnimation.begin().then(type.animation, Animation.LoopType.PLAY_ONCE));
        }
        controllers.add(action);
    }

    private PlayState movementPredicate(AnimationState<TiamutEntity> state) {
        TiamutEntity e = state.getAnimatable();
        if (!e.isInWaterOrBubble()) return state.setAndContinue(BEACHED);
        return switch (e.entityData.get(SPEED_LEVEL)) {
            case 2 -> state.setAndContinue(SWIM_FAST);
            case 1 -> state.setAndContinue(SWIM);
            default -> state.setAndContinue(IDLE);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
