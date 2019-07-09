package tk.rdvdev2.experiments.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.*;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.state.properties.RailShape;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import tk.rdvdev2.experiments.common.item.MarkerItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class RailwayCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("railway").executes(command -> {
            generateRailway(command.getSource().getWorld());
            return 1;
        }));
    }

    private static void generateRailway(World world) {
        BlockPos start = MarkerItem.pos1;
        BlockPos end = MarkerItem.pos2;

        // Ensure we're on air blocks
        while (!world.getBlockState(start).isAir()) {
            start = start.up();
        }
        while (!world.getBlockState(end).isAir()) {
            end = end.up();
        }

        PriorityQueue<PrioritizedBlockPos> frontier = new PriorityQueue<>();
        frontier.add(new PrioritizedBlockPos(start, 0));
        HashMap<BlockPos, BlockPos> cameFrom = new HashMap<>();
        cameFrom.put(start, null);
        HashMap<BlockPos, Integer> costSoFar = new HashMap<>();
        costSoFar.put(start, 0);

        // Generate data
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.remove().getPos();

            if (current.toLong() == end.toLong()) {
                break;
            }

            for(BlockPos next: getBlockNeighbours(current, cameFrom.get(current), world)) {
                int newCost = costSoFar.get(current) + calculateCost(current, next, world);
                if (!costSoFar.containsKey(next) || newCost < costSoFar.get(next)) {
                    costSoFar.put(next, newCost);
                    int priority = newCost + heuristic(end, next);
                    frontier.add(new PrioritizedBlockPos(next, priority));
                    cameFrom.put(next, current);
                }
            }
        }

        BlockPos current = end;
        LinkedList<BlockPos> path = new LinkedList<>();
        while (current != start) {
            path.add(current);
            current = cameFrom.get(current);
        }
        path.add(start);

        // Draw on world
        for (BlockPos currentPos : path) {
            placeRail(world, currentPos);
        }
        
        powerUp(path, world);
    }

    private static void powerUp(LinkedList<BlockPos> path, World world) {
        powerUp(path, world, 0, 8);
    }

    private static void powerUp(LinkedList<BlockPos> path, World world, int index, int unpoweredCount) {
        BlockPos pos = path.get(index);
        RailShape shape = ((RailBlock)Blocks.RAIL).getRailDirection(world.getBlockState(pos), world, pos, null);
        if (index == path.size() - 1) {
            placePoweredRail(world, pos);
            return;
        } else if (unpoweredCount >= 8) {
            if (shape.getMeta() < 6) {
                placePoweredRail(world, pos);
                unpoweredCount = 0;
            }
        } else if (shape.isAscending()) {
            placePoweredRail(world, pos);
            unpoweredCount = 0;
        } else {
            unpoweredCount++;
        }
        index++;
        powerUp(path, world, index, unpoweredCount);
    }

    private static void placeRail(World world, BlockPos pos) {
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = Blocks.RAIL.getDefaultState();
        world.setBlockState(pos, newState);
        Blocks.RAIL.onBlockAdded(newState, world, pos, oldState, true);
    }

    private static void placePoweredRail(World world, BlockPos pos) {
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = Blocks.POWERED_RAIL.getDefaultState();
        world.setBlockState(pos, newState);
        world.setBlockState(pos.down().down(), Blocks.REDSTONE_TORCH.getDefaultState());
        Blocks.POWERED_RAIL.onBlockAdded(newState, world, pos, oldState, true);
    }

    private static int heuristic(BlockPos a, BlockPos b) {
        return a.manhattanDistance(b);
    }

    private static int calculateCost(BlockPos current, BlockPos next, World world) {
        return 1;
    }

    private static Iterable<? extends BlockPos> getBlockNeighbours(BlockPos pos, BlockPos previous, World world) {
        // Setup vars
        HashSet<BlockPos> toCheck = new HashSet<>();
        HashSet<BlockPos> ret = new HashSet<>();
        Direction originFacing;
        Direction originHeight;
        BlockPos difference;

        // Determine origin
        if (previous == null) { // If it's start pos
            originHeight = Direction.DOWN; // Allow turning
            difference = pos.subtract(pos.down()); // Little trick, this way all directions will be valid
        } else if (pos.getY() == previous.getY()) { // Same Y
            originHeight = Direction.DOWN; // Also works with same Y
            difference = previous.subtract(pos);
        } else if (pos.up().getY() == previous.getY()) { // Rail is going down
            originHeight = Direction.UP;
            difference = previous.subtract(pos.up());
        } else if (pos.down().getY() == previous.getY()) { // Rail was going up
            originHeight = Direction.DOWN;
            difference = previous.subtract(pos.down());
        } else throw new RuntimeException();
        originFacing = Direction.getFacingFromVector(difference.getX(), difference.getY(), difference.getZ());

        // Get possible neighbours depending on previous rail
        for(Direction direction: new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            if (direction == originFacing) continue;
            switch (originHeight) {
                case UP:
                    if (direction.getOpposite() == originFacing){
                        toCheck.add(pos.offset(direction));
                        toCheck.add(pos.offset(direction).down());
                    }
                    break;
                case DOWN:
                    toCheck.add(pos.offset(direction));
                    toCheck.add(pos.offset(direction).down());
                    if (direction.getOpposite() == originFacing) toCheck.add(pos.offset(direction).up());
                    break;
            }
        }

        // Check the possible neighbours to see if the rail can be placed
        for(BlockPos testedPos: toCheck) {
            if ((world.getBlockState(testedPos).isAir() || world.getBlockState(testedPos).getBlock() instanceof BushBlock) && Block.func_220064_c(world, testedPos.down())) {
                ret.add(testedPos);
            }
        }

        return ret;
    }

    private static class PrioritizedBlockPos implements Comparable<PrioritizedBlockPos>{

        private BlockPos pos;
        private int priority;

        private PrioritizedBlockPos(BlockPos pos, int priority) {
            this.pos = pos;
            this.priority = priority;
        }

        public BlockPos getPos() {
            return pos;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(PrioritizedBlockPos other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
}
