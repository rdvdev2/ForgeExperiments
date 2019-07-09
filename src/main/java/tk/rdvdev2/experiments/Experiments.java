package tk.rdvdev2.experiments;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tk.rdvdev2.experiments.command.RailwayCommand;

import static tk.rdvdev2.experiments.Experiments.MODID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MODID)
public class Experiments {

    public static final String MODID = "experiments";
    private static final Logger LOGGER = LogManager.getLogger();

    public Experiments() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
    }

    private void setup(final FMLCommonSetupEvent event) {

    }

    private void serverStarting(final FMLServerStartingEvent event) {
        RailwayCommand.register(event.getCommandDispatcher());
    }
}
