package com.foxxite.RedGrid.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import com.foxxite.RedGrid.RedGrid;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

public class Utils {
    static Dictionary<String, SignType> signTypeDictionary = new java.util.Hashtable<>() {{
        put("RGT", SignType.TRANSMITTER);
        put("RGR", SignType.RECEIVER);
        put("RGX", SignType.INVALID);
    }};

    public static SignType getSignType(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return SignType.INVALID;
        return getSignType(componentToString(lines.getFirst()));
    }

    public static SignType getSignType(String firstLine) {
        // Make sure we start with a [ and end with a ]
        if (!firstLine.toUpperCase().startsWith("[RG") || !firstLine.toUpperCase().endsWith("]"))
            return SignType.OTHER_PLUGIN;

        // Remove the [] for checking
        String line = firstLine.replace("[", "").replace("]", "").trim();

        // Check if in the dictionary, ignoring case sensitivity
        for (Enumeration<String> e = signTypeDictionary.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (key.equalsIgnoreCase(line))
                return signTypeDictionary.get(key);
        }
        return SignType.INVALID;
    }

    public static boolean isInvalidSignType(String firstLine) {
        return getSignType(firstLine) == SignType.INVALID || getSignType(firstLine) == SignType.OTHER_PLUGIN;
    }

    public static boolean isWallSign(Sign sing)
    {
        return sing.getBlockData() instanceof org.bukkit.block.data.type.WallSign;
    }

    public static BlockFace getSignFacingDirection(Sign sing)
    {
        return ((Rotatable) sing.getBlockData()).getRotation();
    }

    public static String componentToString(Component component)
    {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static void colorizeSign(Sign sign, SignType type, String channelName)
    {
        sign.setWaxed(true);

        SignSide front = sign.getSide(Side.FRONT);

        front.line(0, switch (type) {
            case TRANSMITTER -> RedGrid.getInstance().getMiniMessage().deserialize("<red>[<bold>RGT</bold>]</red>");
            case RECEIVER -> RedGrid.getInstance().getMiniMessage().deserialize("<red>[<bold>RGR</bold>]</red>");
            default -> RedGrid.getInstance().getMiniMessage().deserialize("<red>[<bold>RGX</bold>]</red>");
        });

        front.line(1, RedGrid.getInstance().getMiniMessage().deserialize(String.format("<blue>%s</blue>", channelName)));

        // Update the  sign state
        sign.update();
    }
}
