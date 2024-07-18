package py.com.jaimeferreira.ccr.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ManejadorDeArchivos {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorDeArchivos.class);

    @Value("${path.directory.server}")
    private String directorioServer;

    @Value("${path.directory.server_path_images}")
    private String directorioServerPathImages;

    public ManejadorDeArchivos() {
        super();
    }

    public String getDirectorioServer() {
        return directorioServer;
    }

    public void setDirectorioServer(String directorioServer) {
        this.directorioServer = directorioServer;
    }

    public String getDirectorioServerPathImages() {
        return directorioServerPathImages;
    }

    public void setDirectorioServerPathImages(String directorioServerPathImages) {
        this.directorioServerPathImages = directorioServerPathImages;
    }

    // imagen desde el path a base64
    public String imagenToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        FileInputStream fileInputStream = null;
        byte[] imageBytes = null;
        try {
            fileInputStream = new FileInputStream(file);
            imageBytes = new byte[(int) file.length()];
            fileInputStream.read(imageBytes);
            fileInputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // imagen desde el path a array de byte
    public byte[] getIMGByPath(String imagePath) {
        File file = new File(imagePath);
        FileInputStream fileInputStream = null;
        byte[] imageBytes = null;
        try {
            fileInputStream = new FileInputStream(file);
            imageBytes = new byte[(int) file.length()];
            fileInputStream.read(imageBytes);
            fileInputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return imageBytes;
    }

    public void base64ToImagen(String fileName, String base64Img, String watermarkText) {
        byte[] imageBytes = Base64.getDecoder().decode(base64Img);
        File outputFile = new File(directorioServer.concat(directorioServerPathImages).concat(fileName));

        // if (pathImg.contains("/")) {
        // outputFile = new File(pathImg);
        // }
        // else {
        // outputFile = new
        // File(directorioServer.concat(directorioServerPathImages).concat(fileName));
        // }
        FileOutputStream outputStream = null;
        
        
        
        
        File outputDir = outputFile.getParentFile();

        // Crear el directorio si no existe
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        
        
        try {
            outputStream = new FileOutputStream(outputFile);

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            outputStream.write(imageBytes);
            outputStream.close();

            ManejadorDeArchivos.addTextWatermark(watermarkText, outputFile,outputFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String removerStringPath(String path) {
        return path.replaceAll(directorioServer + directorioServerPathImages, "");
    }

    public byte[] getImage(String id) throws Exception {
        byte[] arr = null;
        try {

            File file = null;
            String sFile = directorioServer + directorioServerPathImages + id;
            System.out.println("getImage --> " + sFile);
            @SuppressWarnings("resource")
            FileInputStream fileStream = new FileInputStream(file = new File(sFile));
            arr = new byte[(int) file.length()];
            fileStream.read(arr, 0, arr.length);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public static void addTextWatermark(String text, File sourceImageFile, File destImageFile) {
        BufferedImage img = null;

        // Read image
        try {
            img = ImageIO.read(sourceImageFile);
        }
        catch (IOException e) {
            System.out.println(e);
        }

        // create BufferedImage object of same width and
        // height as of input image
        BufferedImage temp = new BufferedImage(
                                               img.getWidth(), img.getHeight(),
                                               BufferedImage.TYPE_INT_RGB);

        // Create graphics object and add original
        // image to it
        Graphics graphics = temp.getGraphics();
        graphics.drawImage(img, 0, 0, null);

        // Set font for the watermark text
        graphics.setFont(new Font("Arial", Font.PLAIN, 100));
        graphics.setColor(Color.RED);

        // Add the watermark text at (width/5, height/3)
        // location
        graphics.drawString(text, img.getWidth() / 20,
                            img.getHeight() / 15);

        graphics.dispose();

        try {
            ImageIO.write(temp, "jpg", destImageFile);
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

}
