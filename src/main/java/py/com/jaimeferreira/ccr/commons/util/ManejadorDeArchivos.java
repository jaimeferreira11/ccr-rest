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

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

@Component
public class ManejadorDeArchivos {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorDeArchivos.class);

    @Value("${path.directory.server}")
    private String directorioServer;

    @Value("${path.directory.server_path_images}")
    private String directorioServerPathImages;

    @Value("${path.directory.main_imagenes}")
    private String directoryMainImages;

    @Value("${path.directory.server_path_images_scj}")
    private String jhonsonFolder;

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

    public String getDirectoryPathMainImagenes() {

        return directoryMainImages;
    }

    public String getDirectoryPathImagenesJhonson() {

        return directoryMainImages.concat(jhonsonFolder);
    }

    // imagen desde el path a base64
    public String imagenToBase64(String imagePath) throws Exception {
        File file = new File(directorioServer.concat(directorioServerPathImages).concat(imagePath));
        LOGGER.info(file.getAbsolutePath());
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

    public byte[] getImageFromApp(String path) {
        byte[] arr = null;
        try {
            File file = null;
            String sFile = directoryMainImages.concat(path);
            FileInputStream fileStream = new FileInputStream(file = new File(sFile));
            arr = new byte[(int) file.length()];
            fileStream.read(arr, 0, arr.length);

        }
        catch (FileNotFoundException e) {
            System.out.println("No se encontro la imagen: " + directoryMainImages + path);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return arr;
    }

    public void base64ToImagen(String fileName, String base64Img, String watermarkText, boolean onlyVertical) {
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

            // Aplicar rotación según EXIF Orientation (los celulares guardan los píxeles
            // en orientación nativa del sensor + un tag EXIF que indica la rotación lógica).
            // ImageIO descarta el EXIF, por lo que sin este paso las fotos tomadas en
            // vertical quedan acostadas en disco.
            image = applyExifOrientation(image, imageBytes, fileName);

            if (onlyVertical) {
                // Verificar si la imagen necesita ser rotada
                if (image.getWidth() > image.getHeight()) {
                    LOGGER.info("Rotar la imagen para que esté en orientación vertical: " + fileName);
                    image = rotateImage(image, 90);
                }
            }

            // Guardar la imagen en el archivo
            ImageIO.write(image, "jpg", outputFile);  // Asumiendo que es JPG, cambia según tu
                                                      // formato

            // Añadir la marca de agua
            if (watermarkText != null)
                ManejadorDeArchivos.addTextWatermark(watermarkText, outputFile, outputFile);

        }
        catch (Exception e) {
            LOGGER.error("Error procesando imagen " + fileName + ", se guardará el archivo sin transformar", e);
            // Fallback: si falló el pipeline de procesamiento, guardar el archivo crudo
            // para no perder la foto del usuario.
            saveRawImageFallback(outputFile, imageBytes);
        }

    }

    /**
     * Lee el tag EXIF Orientation del JPEG y rota el BufferedImage acorde.
     * Si la lectura del EXIF falla o el tag no está, devuelve la imagen sin tocar.
     * Esto NUNCA debe romper el guardado: cualquier excepción cae en el catch y se
     * conserva la imagen original.
     */
    private BufferedImage applyExifOrientation(BufferedImage image, byte[] imageBytes, String fileName) {
        int orientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        }
        catch (Exception e) {
            LOGGER.warn("No se pudo leer EXIF Orientation de " + fileName + ", se usará orientación nativa: "
                        + e.getMessage());
            return image;
        }

        if (orientation == 1) {
            return image;
        }

        try {
            LOGGER.info("Aplicando rotación EXIF (orientation=" + orientation + ") a " + fileName);
            return rotateByExifOrientation(image, orientation);
        }
        catch (Exception e) {
            LOGGER.warn("Falló la rotación EXIF de " + fileName + ", se usará la imagen sin rotar: " + e.getMessage());
            return image;
        }
    }

    /**
     * Rota/espeja un BufferedImage según el valor del tag EXIF Orientation (1..8).
     * Los valores comunes en celulares son 1, 3, 6, 8. Los flip (2, 4, 5, 7) son raros.
     */
    private BufferedImage rotateByExifOrientation(BufferedImage image, int orientation) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swapDims = orientation >= 5 && orientation <= 8;
        int newW = swapDims ? h : w;
        int newH = swapDims ? w : h;

        int type = image.getType();
        if (type == BufferedImage.TYPE_CUSTOM || type == 0) {
            type = BufferedImage.TYPE_INT_RGB;
        }

        BufferedImage rotated = new BufferedImage(newW, newH, type);
        Graphics2D g2d = rotated.createGraphics();

        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2: // flip horizontal
                t.scale(-1.0, 1.0);
                t.translate(-w, 0);
                break;
            case 3: // 180°
                t.translate(w, h);
                t.rotate(Math.PI);
                break;
            case 4: // flip vertical
                t.scale(1.0, -1.0);
                t.translate(0, -h);
                break;
            case 5: // transpose: 90° CW + flip horizontal
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case 6: // 90° CW
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // transverse: 90° CCW + flip horizontal
                t.scale(-1.0, 1.0);
                t.translate(-h, 0);
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
                break;
            case 8: // 90° CCW (270° CW)
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
                break;
            default:
                g2d.dispose();
                return image;
        }

        g2d.setTransform(t);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return rotated;
    }

    /**
     * Fallback de seguridad: guarda los bytes originales del JPEG sin pasar por ImageIO.
     * Se usa cuando algún paso del pipeline normal falló, para no perder la foto.
     */
    private void saveRawImageFallback(File outputFile, byte[] imageBytes) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(imageBytes);
            LOGGER.warn("Imagen guardada via fallback (sin procesar): " + outputFile.getAbsolutePath());
        }
        catch (Exception ex) {
            LOGGER.error("Fallback de guardado también falló para " + outputFile.getAbsolutePath(), ex);
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
            file = new File(filePath.replace(file.getName(), fileName + "(" + count + ")" + fileExtension));
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

    public void rotateImage(String imgPath) throws IOException {
        // Sentido por defecto (90° CW), usado por el endpoint público /images/rotate.
        rotateImage(imgPath, 90);
    }

    public void rotateImage(String imgPath, int angle) throws IOException {
        String sFile = directoryMainImages.concat(imgPath);

        LOGGER.info(sFile);

        // Cargar la imagen desde el archivo
        BufferedImage originalImage = ImageIO.read(new File(sFile));

        // Guardar la imagen rotada
        ImageIO.write(rotateImage(originalImage, angle), "jpg", new File(sFile));
    }

}
