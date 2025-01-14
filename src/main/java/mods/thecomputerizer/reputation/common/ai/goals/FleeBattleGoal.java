package mods.thecomputerizer.reputation.common.ai.goals;

import mods.thecomputerizer.reputation.api.Faction;
import mods.thecomputerizer.reputation.api.ReputationHandler;
import mods.thecomputerizer.reputation.common.ai.ChatTracker;
import mods.thecomputerizer.reputation.common.ai.ReputationAIPackages;
import mods.thecomputerizer.reputation.common.ai.ServerTrackers;
import mods.thecomputerizer.reputation.common.event.WorldEvents;
import mods.thecomputerizer.reputation.common.network.FleeIconMessage;
import mods.thecomputerizer.reputation.common.network.PacketHandler;
import mods.thecomputerizer.reputation.util.HelperMethods;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FleeBattleGoal extends Goal {

    private final PathfinderMob mob;
    private final float fleeFactor;
    private final PathNavigation pathNav;
    private Path path;
    private Player fleeFrom;
    public boolean isFleeing;

    public FleeBattleGoal(PathfinderMob mob, float fleeFactor) {
        this.mob = mob;
        this.pathNav = mob.getNavigation();
        this.fleeFactor = fleeFactor;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.isFleeing = false;
    }

    private boolean escaped() {
        if(this.fleeFrom!=null && this.mob.distanceTo(this.fleeFrom)>=28) {
            for (Faction f : ReputationHandler.getEntityFactions(this.mob)) ReputationHandler.changeReputation(this.fleeFrom, f, -1 * f.getActionWeighting("fleeing"));
            this.mob.discard();
            this.isFleeing = false;
            return true;
        }
        return false;
    }

    private boolean ensureCorrectConditions(Player player) {
        return (this.mob.getHealth() / this.mob.getMaxHealth()) <= .5f
                && this.mob.distanceTo(player)<=32
                && HelperMethods.ensureSeparateFactions(this.mob,player)
                && HelperMethods.isPlayerInCustomStanding(this.mob,player, ReputationAIPackages.standings.getInjured(this.mob.getType()));
    }

    @Override
    public boolean canUse() {
        if(this.mob.getLastHurtByMob() instanceof Player player) {
            if(ensureCorrectConditions(player)) {
                this.isFleeing = true;
                this.fleeFrom = player;
                PacketHandler.sendTo(new FleeIconMessage(mob.getUUID(), true), (ServerPlayer) this.fleeFrom);
                Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 32, 16, this.fleeFrom.position());
                if (vec3 == null) {
                    return false;
                } else if (this.fleeFrom.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.fleeFrom.distanceToSqr(this.mob)) {
                    return false;
                } else {
                    this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
                    return this.path != null;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if(!this.isFleeing) return false;
        if(!ensureCorrectConditions(this.fleeFrom)) this.isFleeing = false;
        return this.isFleeing;
    }

    @Override
    public void start() {
        this.pathNav.moveTo(this.path, this.fleeFactor);
    }

    @Override
    public void stop() {
        if(!this.mob.isRemoved() && !ensureCorrectConditions(this.fleeFrom)) PacketHandler.sendTo(new FleeIconMessage(mob.getUUID(), false), (ServerPlayer) this.fleeFrom);
        this.fleeFrom = null;
    }

    @Override
    public void tick() {
        if(!escaped()) {
            if (this.pathNav.isDone() || this.path == null) {
                Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 32, 16, this.fleeFrom.position());
                if (vec3 != null && this.fleeFrom.distanceToSqr(vec3.x, vec3.y, vec3.z) >= this.fleeFrom.distanceToSqr(this.mob))
                    this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
                this.pathNav.moveTo(this.path, this.fleeFactor);
            } else this.mob.getNavigation().setSpeedModifier(this.fleeFactor);
            synchronized (WorldEvents.TRACKER_MAP) {
                if (WorldEvents.TRACKER_MAP.containsKey(mob)) {
                    ChatTracker tracker = WorldEvents.TRACKER_MAP.get(mob);
                    if (!tracker.getRecent() && !tracker.getFlee() && ServerTrackers.hasIconsForEvent(tracker.getEntityType(), "flee")) {
                        tracker.setFlee(true);
                        tracker.setChanged(true);
                        tracker.setRecent(true);
                    }
                }
            }
        }
    }
}
