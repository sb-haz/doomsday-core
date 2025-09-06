package gg.doomsday.core.utils;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientUtils {
    
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9a-fA-F]{6}(?::#[0-9a-fA-F]{6})*?)>([^<]+)</gradient>");
    
    public static String parseGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String colorCodes = matcher.group(1);
            String text = matcher.group(2);
            String gradientText = applyGradient(text, colorCodes);
            matcher.appendReplacement(result, gradientText);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private static String applyGradient(String text, String colorCodes) {
        String[] hexColors = colorCodes.split(":");
        
        if (hexColors.length < 2) {
            return text;
        }
        
        Color[] colors = new Color[hexColors.length];
        for (int i = 0; i < hexColors.length; i++) {
            try {
                colors[i] = Color.decode(hexColors[i]);
            } catch (NumberFormatException e) {
                return text;
            }
        }
        
        StringBuilder gradientText = new StringBuilder();
        int textLength = text.length();
        
        for (int i = 0; i < textLength; i++) {
            char character = text.charAt(i);
            
            if (character == ' ') {
                gradientText.append(character);
                continue;
            }
            
            double position = (double) i / (textLength - 1);
            Color interpolatedColor = interpolateColors(colors, position);
            
            String hexColor = String.format("#%02x%02x%02x", 
                interpolatedColor.getRed(), 
                interpolatedColor.getGreen(), 
                interpolatedColor.getBlue());
            
            gradientText.append(ChatColor.of(hexColor)).append(character);
        }
        
        return gradientText.toString();
    }
    
    private static Color interpolateColors(Color[] colors, double position) {
        if (colors.length == 1) {
            return colors[0];
        }
        
        if (position <= 0) {
            return colors[0];
        }
        if (position >= 1) {
            return colors[colors.length - 1];
        }
        
        double scaledPosition = position * (colors.length - 1);
        int index = (int) scaledPosition;
        double fraction = scaledPosition - index;
        
        if (index >= colors.length - 1) {
            return colors[colors.length - 1];
        }
        
        Color startColor = colors[index];
        Color endColor = colors[index + 1];
        
        int red = (int) (startColor.getRed() + fraction * (endColor.getRed() - startColor.getRed()));
        int green = (int) (startColor.getGreen() + fraction * (endColor.getGreen() - startColor.getGreen()));
        int blue = (int) (startColor.getBlue() + fraction * (endColor.getBlue() - startColor.getBlue()));
        
        return new Color(red, green, blue);
    }
}