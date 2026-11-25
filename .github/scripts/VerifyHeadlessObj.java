import java.io.File;
import javax.imageio.ImageIO;

class VerifyHeadlessObj {
    public static void main(String[] args) throws Exception {
        var image = ImageIO.read(new File(args[0]));
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            throw new AssertionError("Unexpected PNG dimensions");
        }

        int colored = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >>> 16) & 255;
                int g = (rgb >>> 8) & 255;
                int b = rgb & 255;
                if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 20) {
                    colored++;
                }
            }
        }
        // The OBJ with default white materials is grayscale. Its original textures give it color.
        if (colored < width * height / 200) {
            throw new AssertionError("Expected a textured OBJ, found only " + colored + " colored pixels");
        }
        System.out.println("Verified " + width + "x" + height + " PNG with " + colored + " colored pixels.");
    }
}
