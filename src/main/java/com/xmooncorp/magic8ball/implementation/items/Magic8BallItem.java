package com.xmooncorp.magic8ball.implementation.items;

import com.xmooncorp.magic8ball.Magic8Ball;
import com.xmooncorp.magic8ball.core.ConfigBasedLocalization;
import com.xmooncorp.magic8ball.utils.LocationUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public class Magic8BallItem extends SlimefunItem implements Listener {

    private static final Response FALLBACK = new Response(
        "Ask again later", NamedTextColor.GRAY, Sound.ENTITY_VILLAGER_TRADE, Particle.CRIT);

    private final List<ResponseGroup> responseGroups = new ArrayList<>();
    private final ConcurrentMap<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final long cooldownMillis;

    public Magic8BallItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        ConfigBasedLocalization localization = Magic8Ball.instance().localization();
        addGroup(localization, "responses.affirmative", NamedTextColor.GREEN, Sound.ENTITY_VILLAGER_YES, Particle.TOTEM_OF_UNDYING);
        addGroup(localization, "responses.noncommittal", NamedTextColor.GRAY, Sound.ENTITY_VILLAGER_TRADE, Particle.CRIT);
        addGroup(localization, "responses.negative", NamedTextColor.RED, Sound.ENTITY_VILLAGER_NO, Particle.ENCHANTED_HIT);
        cooldownMillis = Math.max(0L, Magic8Ball.instance().config().getLong("options.cooldown-ms", 500L));
        addItemHandler(onPlayerInteractBlock());
    }

    private void addGroup(ConfigBasedLocalization localization, String path, NamedTextColor color, Sound sound, Particle particle) {
        String[] values = localization.getStringList(path).stream()
            .filter(value -> value != null && !value.isBlank())
            .toArray(String[]::new);
        if (values.length > 0) {
            responseGroups.add(new ResponseGroup(values, color, sound, particle));
        }
    }

    @Override
    public void preRegister() {
        Magic8Ball plugin = Magic8Ball.instance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void sendRandom8BallMessage(@Nonnull Player player, @Nullable Block block) {
        if (!acquireCooldown(player)) {
            return;
        }

        Response response = getRandomResponse();
        player.sendActionBar(Component.text(response.message(), response.color()));
        playSoundAtLocation(player.getWorld(), player.getLocation(), response.sound());

        Location handLocation = LocationUtils.getFrontSide(
            LocationUtils.getRightSide(player.getEyeLocation(), 0.325).subtract(0, 0.7, 0), 0.6);
        if (block != null) {
            createParticleAtLocation(block.getWorld(), block.getLocation().add(0.5, 0.5, 0.5), response.particle());
        } else {
            createParticleAtLocation(player.getWorld(), handLocation, response.particle());
        }
    }

    private boolean acquireCooldown(Player player) {
        if (cooldownMillis <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long previous = cooldownUntil.get(playerId);
        if (previous != null && previous > now) {
            return false;
        }
        cooldownUntil.put(playerId, now + cooldownMillis);
        return true;
    }

    @Nonnull
    private Response getRandomResponse() {
        if (responseGroups.isEmpty()) {
            return FALLBACK;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ResponseGroup group = responseGroups.get(random.nextInt(responseGroups.size()));
        String message = group.values()[random.nextInt(group.values().length)];
        return new Response(message, group.color(), group.sound(), group.particle());
    }

    private void createParticleAtLocation(@Nonnull World world, @Nonnull Location location, @Nonnull Particle particle) {
        world.spawnParticle(particle, location, 20);
    }

    private void playSoundAtLocation(@Nonnull World world, @Nonnull Location location, @Nonnull Sound sound) {
        world.playSound(location, sound, 1, 0.65f);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        SlimefunItem itemHeld = SlimefunItem.getByItem(event.getItem());
        if (itemHeld != null && itemHeld.getId().equals(getId())) {
            sendRandom8BallMessage(event.getPlayer(), null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownUntil.remove(event.getPlayer().getUniqueId());
    }

    private BlockUseHandler onPlayerInteractBlock() {
        return event -> {
            Player player = event.getPlayer();
            Optional<Block> block = event.getClickedBlock();
            block.ifPresent(value -> sendRandom8BallMessage(player, value));
        };
    }

    private record ResponseGroup(String[] values, NamedTextColor color, Sound sound, Particle particle) {}
    private record Response(String message, NamedTextColor color, Sound sound, Particle particle) {}
}
