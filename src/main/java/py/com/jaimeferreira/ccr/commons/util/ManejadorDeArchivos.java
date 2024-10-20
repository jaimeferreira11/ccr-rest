package py.com.jaimeferreira.ccr.commons.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    public String imagenToBase64(String imagePath) throws Exception {
        File file = new File(directorioServer.concat(directorioServerPathImages).concat(imagePath));
        FileInputStream fileInputStream = null;
        byte[] imageBytes = null;
        try {
            fileInputStream = new FileInputStream(file);
            imageBytes = new byte[(int) file.length()];
            fileInputStream.read(imageBytes);
            fileInputStream.close();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
        catch (FileNotFoundException e) {
            LOGGER.info("Imagen no encontrada: " + imagePath);
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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

        FileOutputStream outputStream = null;

        File outputDir = outputFile.getParentFile();

        // Crear el directorio si no existe
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Renombrar si el archivo ya existe
        outputFile = renameIfExists(outputFile);

        try {
         // Convertir los bytes a un BufferedImage
            InputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new FileNotFoundException("Error al leer la imagen.");
            }

            // Verificar si la imagen necesita ser rotada
            if (image.getWidth() > image.getHeight()) {
                LOGGER.info("Rotar la imagen para que esté en orientación vertical: " + fileName );
                image = rotateImage(image, 90);
            }

            // Guardar la imagen en el archivo
            ImageIO.write(image, "jpg", outputFile);  // Asumiendo que es JPG, cambia según tu formato

            // Añadir la marca de agua
            ManejadorDeArchivos.addTextWatermark(watermarkText, outputFile, outputFile);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private BufferedImage rotateImage(BufferedImage originalImage, int angle) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // Crear una nueva imagen con las dimensiones invertidas
        BufferedImage rotatedImage = new BufferedImage(height, width, originalImage.getType());
        
        Graphics2D g2d = rotatedImage.createGraphics();
        
        // Rotar la imagen
        AffineTransform at = new AffineTransform();
        at.translate(height / 2.0, width / 2.0);  // Mover al centro
        at.rotate(Math.toRadians(angle));         // Rotar
        at.translate(-width / 2.0, -height / 2.0); // Mover al origen
        g2d.drawImage(originalImage, at, null);
        g2d.dispose();
        
        return rotatedImage;
    }
    
    private File renameIfExists(File file) {
        int count = 0;
        String filePath = file.getAbsolutePath();
        String fileName = file.getName();
        String fileExtension = "";

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex);
            fileName = fileName.substring(0, dotIndex);
        }

        while (file.exists()) {
            count++;
            file = new File(filePath.replace(file.getName(), fileName + "(" + count +")" + fileExtension));
        }
        return file;
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
