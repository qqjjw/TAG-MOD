package dev.tagmod;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.*;

public final class TagMod implements ModInitializer {
    public static final String ID="tagmod";
    public static final Logger LOG=LoggerFactory.getLogger(ID);
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Config config=new Config();
    public static MinecraftServer server;
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath(ID,path);}
    public static boolean onServerThread(){return server!=null&&server.isSameThread();}
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(s->{server=s;config=Config.load(FabricLoader.getInstance().getConfigDir().resolve("tagmod.json"));Effects.clear();Monsters.clear();Combat.clear();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{Effects.clear();Monsters.clear();Combat.clear();server=null;});
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(entity instanceof Mob m){Monsters.initialize(m);for(EquipmentSlot slot:EquipmentSlot.values())Gear.ensure(m.getItemBySlot(slot));}
            if(entity instanceof ItemEntity item)Gear.ensure(item.getItem());
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{for(ServerPlayer p:s.getPlayerList().getPlayers())scan(p);});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
            Commands.literal("tagmod").requires(s->s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("inspect").executes(ctx->{var p=ctx.getSource().getPlayerOrException();GearData d=Gear.data(p.getMainHandItem());ctx.getSource().sendSuccess(()->Component.literal(d==null?"처리된 장비를 주손에 드세요.":JSON.toJson(d)),false);return 1;}))
            .then(Commands.literal("roll").executes(ctx->{var p=ctx.getSource().getPlayerOrException();ItemStack item=p.getMainHandItem();if(!Gear.eligible(item)){ctx.getSource().sendFailure(Component.literal("대상 장비를 주손에 드세요."));return 0;}Gear.roll(item,Gear.RANDOM.nextLong());p.containerMenu.broadcastChanges();ctx.getSource().sendSuccess(()->Component.literal("테스트용 재추첨 완료"),false);return 1;}))
            .then(Commands.literal("spawn").executes(ctx->{var source=ctx.getSource();Mob m=EntityTypes.ZOMBIE.spawn(source.getLevel(),net.minecraft.core.BlockPos.containing(source.getPosition()),EntitySpawnReason.COMMAND);if(m==null)return 0;m.addTag(Monsters.PREFIX+"speed");m.addTag(Monsters.PREFIX+"invisibility");source.sendSuccess(()->Component.literal("신속·간헐적 투명화 테스트 좀비 생성"),false);return 1;}))
            .then(Commands.literal("mob").then(Commands.argument("target",EntityArgument.entity()).executes(ctx->{Entity e=EntityArgument.getEntity(ctx,"target");ctx.getSource().sendSuccess(()->Component.literal(e.entityTags().toString()),false);return 1;})))
        ));
        LOG.info("TagMod 0.1.2 initialized: authoritative logical-server rolls / Korean tooltips");
    }
    public static void scan(ServerPlayer p) {
        boolean changed=false;
        for(int i=0;i<p.getInventory().getContainerSize();i++)changed|=Gear.ensure(p.getInventory().getItem(i));
        for(EquipmentSlot slot:EquipmentSlot.values())changed|=Gear.ensure(p.getItemBySlot(slot));
        changed|=Gear.ensure(p.containerMenu.getCarried());
        if(changed){p.getInventory().setChanged();p.containerMenu.broadcastChanges();p.inventoryMenu.broadcastChanges();}
    }
}
