package com.mystipixel.royalenchanting.hook;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EcoEnchants descriptions for the GUI lore, via {@code EcoEnchantLike.getRawDescription(int, Player)}
 * (javap, EcoEnchants 2026.38). Best effort: vanilla enchants and any failure give no description.
 */
public final class EnchantDescriptions {

    private static final Method NONE;

    static {
        try {
            NONE = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Map<Class<?>, Method> methods = new ConcurrentHashMap<>();

    public List<String> describe(Enchantment enchantment, int level, Player player) {
        Method method = methods.computeIfAbsent(enchantment.getClass(), EnchantDescriptions::find);
        if (method == NONE) {
            return List.of();
        }
        try {
            Object result = method.invoke(enchantment, level, player);
            if (result instanceof List<?> list) {
                List<String> out = new ArrayList<>(list.size());
                for (Object line : list) {
                    out.add(String.valueOf(line));
                }
                return out;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            methods.put(enchantment.getClass(), NONE);
        }
        return List.of();
    }

    private static Method find(Class<?> type) {
        try {
            Method m = type.getMethod("getRawDescription", int.class, Player.class);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException | RuntimeException e) {
            return NONE;
        }
    }
}
