package com.instakill.media.processing;

import com.instakill.common.error.BadRequestException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


@Service
public class ImageProcessingService {

    private static final int MAX_WIDTH = 300;
    private static final String WATERMARK = "Instakill Demo";

    public byte[] process(InputStream in, String extension) throws IOException {
        BufferedImage original = ImageIO.read(in);
        if (original == null) {
            throw new BadRequestException("Unsupported or invalid image");
        }

        int width = Math.min(original.getWidth(), MAX_WIDTH);
        int height = Math.max(1, original.getHeight() * width / original.getWidth());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, width, height, null);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, width / 20)));
        FontMetrics metrics = g.getFontMetrics();
        int x = width - metrics.stringWidth(WATERMARK) - 6;
        int y = height - 6;
        g.setColor(Color.BLACK);
        g.drawString(WATERMARK, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(WATERMARK, x, y);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(result, extension, out);
        return out.toByteArray();
    }
}