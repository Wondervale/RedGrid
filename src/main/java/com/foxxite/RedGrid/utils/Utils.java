package com.foxxite.RedGrid.utils;

import java.util.Dictionary;
import java.util.Enumeration;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;

public class Utils {
    static Dictionary<String, SignType> signTypeDictionary = new java.util.Hashtable<>() {{
        put("RGT", SignType.TRANSMITTER);
        put("RGR", SignType.RECEIVER);
        put("RGX", SignType.INVALID);
    }};

    public static SignType getSignType(String firstLine) {
        // Make sure we start with a [ and end with a ]
        if (!firstLine.startsWith("[") || !firstLine.endsWith("]"))
            return SignType.INVALID;

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

    public static boolean isValidSignType(String firstLine) {
        return getSignType(firstLine) != SignType.INVALID;
    }

    public static boolean isWallSign(Sign sing)
    {
        return sing.getBlockData() instanceof org.bukkit.block.data.type.WallSign;
    }

    public static BlockFace getSignFacingDirection(Sign sing)
    {
        return ((Rotatable) sing.getBlockData()).getRotation();
    }
}
