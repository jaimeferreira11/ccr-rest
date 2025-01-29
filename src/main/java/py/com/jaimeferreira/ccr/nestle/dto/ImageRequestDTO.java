
package py.com.jaimeferreira.ccr.nestle.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class ImageRequestDTO implements Serializable {

    private static final long serialVersionUID = 8121751802472806241L;
    private String image;
    private String extension;

    public String getImage() {
        return image;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setImage(String image) {
        this.image = image;
    }

}
