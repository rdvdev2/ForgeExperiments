package tk.rdvdev2.experiments.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
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

            for(BlockPos next: getBlockNeighbours(current, world)) {
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
    }

    private static void placeRail(World world, BlockPos pos) {
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = Blocks.RAIL.getDefaultState();
        world.setBlockState(pos, newState);
        Blocks.RAIL.onBlockAdded(newState, world, pos, oldState, true);
    }

    private static int heuristic(BlockPos a, BlockPos b) {
        return a.manhattanDistance(b);
    }

    private static int calculateCost(BlockPos current, BlockPos next, World world) {
        return 1;
    }

    private static Iterable<? extends BlockPos> getBlockNeighbours(BlockPos pos, World world) {
        HashSet<BlockPos> ret = new HashSet<>();
        for(BlockPos testedPos: new BlockPos[]{pos.north(), pos.east(), pos.south(), pos.west(), pos.north().up(), pos.east().up(), pos.south().up(), pos.west().up(), pos.north().down(), pos.east().down(), pos.south().down(), pos.west().down()}) {
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
