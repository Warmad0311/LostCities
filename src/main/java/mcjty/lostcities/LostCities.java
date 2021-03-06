package mcjty.lostcities;

import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.commands.CommandBuildPart;
import mcjty.lostcities.commands.CommandDebug;
import mcjty.lostcities.commands.CommandExportBuilding;
import mcjty.lostcities.commands.CommandExportPart;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.proxy.CommonProxy;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;

@Mod(modid = LostCities.MODID, name="The Lost Cities",
        dependencies =
                        "after:forge@[" + LostCities.MIN_FORGE11_VER + ",)",
        version = LostCities.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)",
        acceptableRemoteVersions = "*")
public class LostCities {
    public static final String MODID = "lostcities";
    public static final String VERSION = "2.0.12";
    public static final String MIN_FORGE11_VER = "13.19.0.2176";

    @SidedProxy(clientSide="mcjty.lostcities.proxy.ClientProxy", serverSide="mcjty.lostcities.proxy.ServerProxy")
    public static CommonProxy proxy;

    @Mod.Instance("lostcities")
    public static LostCities instance;

    public static LostCitiesImp lostCitiesImp = new LostCitiesImp();

    public static boolean chisel = false;
    public static boolean biomesoplenty = false;
    public static boolean atg = false;

    public static Logger logger;

    /**
     * Run before anything else. Read your config, create blocks, items, etc, and
     * register them with the GameRegistry.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        logger = e.getModLog();
        chisel = Loader.isModLoaded("chisel");
        biomesoplenty = Loader.isModLoaded("biomesoplenty") || Loader.isModLoaded("BiomesOPlenty");
//        atg = Loader.isModLoaded("atg"); // @todo
        this.proxy.preInit(e);
    }

    /**
     * Do your mod setup. Build whatever data structures you care about. Register recipes.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        this.proxy.init(e);
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandDebug());
        event.registerServerCommand(new CommandExportBuilding());
        event.registerServerCommand(new CommandExportPart());
        event.registerServerCommand(new CommandBuildPart());
        cleanCaches();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        cleanCaches();
        WorldTypeTools.cleanChunkGeneratorMap();
    }

    private void cleanCaches() {
        BuildingInfo.cleanCache();
        Highway.cleanCache();
        Railway.cleanCache();
        BiomeInfo.cleanCache();
        City.cleanCache();
        CitySphere.cleanCache();
        WorldTypeTools.cleanCache();
    }

    @Mod.EventHandler
    public void imcCallback(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            if (message.key.equalsIgnoreCase("getLostCities")) {
                Optional<Function<ILostCities, Void>> value = message.getFunctionValue(ILostCities.class, Void.class);
                if (value.isPresent()) {
                    value.get().apply(lostCitiesImp);
                } else {
                    logger.warn("Some mod didn't return a valid result with getLostCities!");
                }
            }
        }
    }


    /**
     * Handle interaction with other mods, complete your setup based on this.
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        this.proxy.postInit(e);
    }
}
