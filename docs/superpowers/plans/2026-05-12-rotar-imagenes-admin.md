# Módulo Admin Rotación de Imágenes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una pantalla admin en `d-insights-ccr` que permita listar fotos de boca por brand (jhonson/nestle/shell) + mes/año + filtros opcionales, y rotar individualmente cada foto 90° CW. Reutiliza `ManejadorDeArchivos.rotateImage()` ya existente en el backend.

**Architecture:** Un controller Spring nuevo en `commons/controller/` con 3 endpoints (`/listar`, `/rotar`, `/binario`) que delega a un service por brand. Servicio Shell se crea desde cero (no existía); los services Jhonson y Nestle reciben 2 métodos nuevos cada uno. Frontend Angular standalone que extiende `BaseComponent`, sigue el patrón de `items-admin.component`. **Nota importante:** las imágenes se sirven a través de un endpoint del backend (`/binario`), no de URLs externas — esto evita lidiar con diferencias de URLs entre dev y prod.

**Tech Stack:**
- Backend: Spring Boot 2.7, Java 11, `metadata-extractor` 2.19.0 (ya agregado), Apache POI
- Frontend: Angular (standalone components), `ng-select`, `ngx-spinner`, `ngx-toastr`, sweetalert2
- Sin tests automatizados (el proyecto no los tiene — validación manual)

**Spec de referencia:** `docs/superpowers/specs/2026-05-12-rotar-imagenes-admin-design.md`

**Diferencias clave entre brands** (consultar siempre antes de implementar cada brand):

| Aspecto | Jhonson | Nestle | Shell |
|---|---|---|---|
| Carpeta filesystem | `zoomin-jhonson/{codBoca}/` | `zoomin-nestle/{codDistribuidor}/{codBoca}/` | `zoomin-shell/{codBoca}/` |
| Property carpeta | `path.directory.server_path_images_scj` | `path.directory.server_path_images_nestle` | `path.directory.server_path_images_shell` |
| Service backend | `ImagenesSCJService` (existe) | `ImagenesNestService` (existe) | `ImagenesShellService` (CREAR) |
| Filtros | brand + boca opcional | brand + **distribuidor obligatorio** + boca opcional | brand + boca opcional |
| Convención nombre archivo | `{codBoca}_{anio}_{mes}_{nro}.jpg` | misma (asumir, verificar al implementar) | misma (asumir, verificar al implementar) |

---

## File Structure

### Backend — `ccr-rest`

**Crear:**
- `src/main/java/py/com/jaimeferreira/ccr/commons/dto/ImagenAdminDTO.java`
- `src/main/java/py/com/jaimeferreira/ccr/commons/util/ImagenPathValidator.java`
- `src/main/java/py/com/jaimeferreira/ccr/shell/service/ImagenesShellService.java`
- `src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java`

**Modificar:**
- `src/main/java/py/com/jaimeferreira/ccr/jhonson/service/ImagenesSCJService.java` (sumar 2 métodos)
- `src/main/java/py/com/jaimeferreira/ccr/nestle/service/ImagenesNestService.java` (sumar 2 métodos)
- `src/main/java/py/com/jaimeferreira/ccr/commons/util/ManejadorDeArchivos.java` (exponer un método `getDirectoryMainImages` si no lo está)

### Frontend — `d-insights-ccr`

**Crear:**
- `src/app/pages/admin/imagenes/imagenes-admin.component.ts`
- `src/app/pages/admin/imagenes/imagenes-admin.component.html`
- `src/app/pages/admin/imagenes/imagenes-admin.component.scss`
- `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.ts`
- `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.html`
- `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.scss`
- `src/app/models/imagen-admin.interface.ts`

**Modificar:**
- `src/app/core/services/insights.service.ts` (agregar métodos HTTP del módulo)
- `src/app/pages/admin/admin-routing.module.ts` (ruta nueva)
- `src/app/pages/layouts/sidebar/menu.ts` (item de menú; **opcional** — coordinar con usuario si querés un menú dedicado o no)

---

## Backend Tasks

### Task 1: DTO `ImagenAdminDTO`

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/commons/dto/ImagenAdminDTO.java`

- [ ] **Step 1: Crear el DTO con builder**

```java
package py.com.jaimeferreira.ccr.commons.dto;

public class ImagenAdminDTO {

    private String brand;
    private String codBoca;
    private String codDistribuidor; // solo Nestle, null para los demás
    private String fileName;
    private String pathRelativo;
    private String urlPublica;
    private Integer anio;
    private Integer mes;

    public ImagenAdminDTO() { }

    public ImagenAdminDTO(String brand, String codBoca, String codDistribuidor, String fileName,
                          String pathRelativo, String urlPublica, Integer anio, Integer mes) {
        this.brand = brand;
        this.codBoca = codBoca;
        this.codDistribuidor = codDistribuidor;
        this.fileName = fileName;
        this.pathRelativo = pathRelativo;
        this.urlPublica = urlPublica;
        this.anio = anio;
        this.mes = mes;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCodBoca() { return codBoca; }
    public void setCodBoca(String codBoca) { this.codBoca = codBoca; }
    public String getCodDistribuidor() { return codDistribuidor; }
    public void setCodDistribuidor(String codDistribuidor) { this.codDistribuidor = codDistribuidor; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getPathRelativo() { return pathRelativo; }
    public void setPathRelativo(String pathRelativo) { this.pathRelativo = pathRelativo; }
    public String getUrlPublica() { return urlPublica; }
    public void setUrlPublica(String urlPublica) { this.urlPublica = urlPublica; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/commons/dto/ImagenAdminDTO.java
git commit -m "feat: agregar ImagenAdminDTO para módulo admin de rotación"
```

---

### Task 2: Helper `ImagenPathValidator` (anti path-traversal)

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/commons/util/ImagenPathValidator.java`

- [ ] **Step 1: Crear el validator**

```java
package py.com.jaimeferreira.ccr.commons.util;

import java.util.HashMap;
import java.util.Map;

public final class ImagenPathValidator {

    private static final Map<String, String> BRAND_TO_FOLDER = new HashMap<>();

    static {
        BRAND_TO_FOLDER.put("jhonson", "zoomin-jhonson");
        BRAND_TO_FOLDER.put("nestle", "zoomin-nestle");
        BRAND_TO_FOLDER.put("shell", "zoomin-shell");
    }

    private ImagenPathValidator() { }

    public static boolean isBrandSoportado(String brand) {
        return brand != null && BRAND_TO_FOLDER.containsKey(brand.toLowerCase());
    }

    public static String getFolderForBrand(String brand) {
        if (brand == null) {
            return null;
        }
        return BRAND_TO_FOLDER.get(brand.toLowerCase());
    }

    /**
     * Valida que un path relativo sea seguro:
     * - no contiene .. ni ~ ni caracteres null
     * - no es absoluto
     * - empieza con la carpeta del brand
     * - termina en .jpg
     */
    public static void validarPathRelativo(String brand, String pathRelativo) {
        if (pathRelativo == null || pathRelativo.isEmpty()) {
            throw new IllegalArgumentException("path es obligatorio");
        }
        if (pathRelativo.contains("..") || pathRelativo.contains("~") || pathRelativo.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("path inválido (contiene secuencias prohibidas)");
        }
        if (pathRelativo.startsWith("/") || pathRelativo.startsWith("\\")) {
            throw new IllegalArgumentException("path inválido (debe ser relativo)");
        }
        if (!pathRelativo.toLowerCase().endsWith(".jpg")) {
            throw new IllegalArgumentException("path inválido (debe terminar en .jpg)");
        }
        String folderEsperado = getFolderForBrand(brand);
        if (folderEsperado == null) {
            throw new IllegalArgumentException("brand no soportado: " + brand);
        }
        if (!pathRelativo.startsWith(folderEsperado + "/")) {
            throw new IllegalArgumentException(
                "path inválido (debe arrancar con " + folderEsperado + "/)");
        }
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/commons/util/ImagenPathValidator.java
git commit -m "feat: agregar ImagenPathValidator para validación anti path-traversal"
```

---

### Task 3: `ImagenesShellService` (nuevo)

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/shell/service/ImagenesShellService.java`

**Diferencias entre brands aplicables aquí:** Shell **no tiene** jerarquía de distribuidor. Carpeta plana: `zoomin-shell/{codBoca}/`. Misma convención de nombre de archivo que Jhonson.

- [ ] **Step 1: Crear el service**

```java
package py.com.jaimeferreira.ccr.shell.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;

@Service
public class ImagenesShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagenesShellService.class);
    private static final String BRAND = "shell";

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

    @Value("${path.directory.server_path_images_shell}")
    private String mainPathImages;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codBocaOpcional) {
        Path base = Paths.get(directorioServer + mainPathImages);
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            LOGGER.info("Carpeta base no existe: {}", base);
            return new ArrayList<>();
        }

        List<ImagenAdminDTO> resultado = new ArrayList<>();

        try (Stream<Path> bocaDirs = Files.list(base)) {
            List<Path> bocas = bocaDirs.filter(Files::isDirectory)
                    .filter(p -> codBocaOpcional == null || codBocaOpcional.isEmpty()
                            || p.getFileName().toString().equals(codBocaOpcional))
                    .collect(Collectors.toList());

            for (Path bocaDir : bocas) {
                String codBoca = bocaDir.getFileName().toString();
                try (Stream<Path> files = Files.list(bocaDir)) {
                    files.filter(Files::isRegularFile)
                         .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                         .filter(p -> matchesAnioMes(p.getFileName().toString(), anio, mes))
                         .forEach(p -> resultado.add(buildDto(codBoca, p)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error iterando carpetas de Shell", e);
        }

        return resultado;
    }

    public void rotarImagen(String pathRelativo) {
        Path archivo = Paths.get(directorioServer, pathRelativo);
        if (!Files.exists(archivo)) {
            throw new RuntimeException("Archivo no existe: " + pathRelativo);
        }
        try {
            manejadorDeArchivos.rotateImage(pathRelativo);
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen " + pathRelativo, e);
            throw new RuntimeException("No se pudo rotar la imagen", e);
        }
    }

    private boolean matchesAnioMes(String fileName, int anio, int mes) {
        try {
            String mesStr = mes < 10 ? "0" + mes : String.valueOf(mes);
            // patrón: {codBoca}_{anio}_{mes}_{nro}.jpg
            return fileName.contains("_" + anio + "_" + mesStr + "_");
        } catch (Exception e) {
            return false;
        }
    }

    private ImagenAdminDTO buildDto(String codBoca, Path archivo) {
        String fileName = archivo.getFileName().toString();
        String pathRelativo = mainPathImages + "/" + codBoca + "/" + fileName;
        Integer anio = null;
        Integer mes = null;
        try {
            String[] partes = fileName.replace(".jpg", "").split("_");
            if (partes.length >= 3) {
                anio = Integer.parseInt(partes[1]);
                mes = Integer.parseInt(partes[2]);
            }
        } catch (Exception ignored) { }

        return new ImagenAdminDTO(BRAND, codBoca, null, fileName, pathRelativo,
                buildUrlPublica(pathRelativo), anio, mes);
    }

    private String buildUrlPublica(String pathRelativo) {
        // El frontend pega al endpoint /api/v1/admin/imagenes/binario con el path y un cache-buster.
        // Acá solo armamos un path "interno"; el controller compone la URL final con cache-buster.
        return pathRelativo;
    }
}
```

> **Nota:** `buildUrlPublica` devuelve el path relativo. El controller lo combina con su URL pública y un timestamp. Esto mantiene el service sin conocimiento de URLs HTTP.

- [ ] **Step 2: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/shell/service/ImagenesShellService.java
git commit -m "feat: agregar ImagenesShellService con métodos findAllByMes y rotarImagen"
```

---

### Task 4: Métodos nuevos en `ImagenesSCJService`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/jhonson/service/ImagenesSCJService.java`

**Diferencias entre brands aplicables aquí:** Jhonson tiene jerarquía plana boca → fotos (sin distribuidor).

- [ ] **Step 1: Agregar import del DTO al inicio del archivo**

Buscar la sección de imports y agregar después de `import py.com.jaimeferreira.ccr.jhonson.dto.ImagenBocaMesDTO;`:

```java
import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
```

- [ ] **Step 2: Agregar los métodos al final de la clase (antes del último `}`)**

```java
    public List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codBocaOpcional) {
        Path base = Paths.get(directorioServer + mainPathImages);
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            LOGGER.info("Carpeta base no existe: {}", base);
            return new ArrayList<>();
        }

        List<ImagenAdminDTO> resultado = new ArrayList<>();
        String mesStr = mes < 10 ? "0" + mes : String.valueOf(mes);

        try (java.util.stream.Stream<Path> bocaDirs = Files.list(base)) {
            List<Path> bocas = bocaDirs.filter(Files::isDirectory)
                    .filter(p -> codBocaOpcional == null || codBocaOpcional.isEmpty()
                            || p.getFileName().toString().equals(codBocaOpcional))
                    .collect(Collectors.toList());

            for (Path bocaDir : bocas) {
                String codBoca = bocaDir.getFileName().toString();
                try (java.util.stream.Stream<Path> files = Files.list(bocaDir)) {
                    files.filter(Files::isRegularFile)
                         .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                         .filter(p -> p.getFileName().toString().contains("_" + anio + "_" + mesStr + "_"))
                         .forEach(p -> resultado.add(buildAdminDto(codBoca, p, anio, mes)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error iterando carpetas de Jhonson", e);
        }

        return resultado;
    }

    public void rotarImagen(String pathRelativo) {
        Path archivo = Paths.get(directorioServer, pathRelativo);
        if (!Files.exists(archivo)) {
            throw new UnknownResourceException("Archivo no existe: " + pathRelativo);
        }
        try {
            manejadorDeArchivos.rotateImage(pathRelativo);
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen " + pathRelativo, e);
            throw new RuntimeException("No se pudo rotar la imagen", e);
        }
    }

    private ImagenAdminDTO buildAdminDto(String codBoca, Path archivo, int anio, int mes) {
        String fileName = archivo.getFileName().toString();
        String pathRelativo = mainPathImages + "/" + codBoca + "/" + fileName;
        return new ImagenAdminDTO("jhonson", codBoca, null, fileName, pathRelativo,
                pathRelativo, anio, mes);
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/jhonson/service/ImagenesSCJService.java
git commit -m "feat: agregar findAllByMes y rotarImagen a ImagenesSCJService"
```

---

### Task 5: Métodos nuevos en `ImagenesNestService`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/nestle/service/ImagenesNestService.java`

**Diferencias entre brands aplicables aquí:** Nestle tiene **distribuidor obligatorio** como nivel intermedio. Estructura: `zoomin-nestle/{codDistribuidor}/{codBoca}/{archivo.jpg}`. La firma del método incluye `codDistribuidor` no-null.

- [ ] **Step 1: Agregar import del DTO**

Buscar la sección de imports y agregar:

```java
import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
```

- [ ] **Step 2: Agregar los métodos al final de la clase (antes del último `}`)**

```java
    public List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codDistribuidor, String codBocaOpcional) {
        if (codDistribuidor == null || codDistribuidor.isEmpty()) {
            throw new IllegalArgumentException("codDistribuidor es obligatorio para Nestle");
        }

        Path baseDistribuidor = Paths.get(directorioServer + mainPathImages, codDistribuidor);
        if (!Files.exists(baseDistribuidor) || !Files.isDirectory(baseDistribuidor)) {
            LOGGER.info("Carpeta del distribuidor no existe: {}", baseDistribuidor);
            return new ArrayList<>();
        }

        List<ImagenAdminDTO> resultado = new ArrayList<>();
        String mesStr = mes < 10 ? "0" + mes : String.valueOf(mes);

        try (java.util.stream.Stream<Path> bocaDirs = Files.list(baseDistribuidor)) {
            List<Path> bocas = bocaDirs.filter(Files::isDirectory)
                    .filter(p -> codBocaOpcional == null || codBocaOpcional.isEmpty()
                            || p.getFileName().toString().equals(codBocaOpcional))
                    .collect(Collectors.toList());

            for (Path bocaDir : bocas) {
                String codBoca = bocaDir.getFileName().toString();
                try (java.util.stream.Stream<Path> files = Files.list(bocaDir)) {
                    files.filter(Files::isRegularFile)
                         .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                         .filter(p -> p.getFileName().toString().contains("_" + anio + "_" + mesStr + "_"))
                         .forEach(p -> resultado.add(buildAdminDto(codDistribuidor, codBoca, p, anio, mes)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error iterando carpetas de Nestle", e);
        }

        return resultado;
    }

    public void rotarImagen(String pathRelativo) {
        Path archivo = Paths.get(directorioServer, pathRelativo);
        if (!Files.exists(archivo)) {
            throw new UnknownResourceException("Archivo no existe: " + pathRelativo);
        }
        try {
            manejadorDeArchivos.rotateImage(pathRelativo);
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen " + pathRelativo, e);
            throw new RuntimeException("No se pudo rotar la imagen", e);
        }
    }

    private ImagenAdminDTO buildAdminDto(String codDistribuidor, String codBoca, Path archivo, int anio, int mes) {
        String fileName = archivo.getFileName().toString();
        String pathRelativo = mainPathImages + "/" + codDistribuidor + "/" + codBoca + "/" + fileName;
        return new ImagenAdminDTO("nestle", codBoca, codDistribuidor, fileName, pathRelativo,
                pathRelativo, anio, mes);
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/nestle/service/ImagenesNestService.java
git commit -m "feat: agregar findAllByMes y rotarImagen a ImagenesNestService"
```

---

### Task 6: `AdminImagenesController` — endpoints listar y rotar

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java`

**Diferencias entre brands aplicables aquí:** el switch del controller mapea cada brand a su service. Para Nestle valida `codDistribuidor` obligatorio antes de delegar.

- [ ] **Step 1: Crear el controller con endpoints `/listar` y `/rotar`**

```java
package py.com.jaimeferreira.ccr.commons.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
import py.com.jaimeferreira.ccr.commons.util.ImagenPathValidator;
import py.com.jaimeferreira.ccr.jhonson.service.ImagenesSCJService;
import py.com.jaimeferreira.ccr.nestle.service.ImagenesNestService;
import py.com.jaimeferreira.ccr.shell.service.ImagenesShellService;

@RestController
@RequestMapping("/api/v1/admin/imagenes")
public class AdminImagenesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminImagenesController.class);

    @Autowired
    private ImagenesSCJService imagenesSCJService;

    @Autowired
    private ImagenesNestService imagenesNestService;

    @Autowired
    private ImagenesShellService imagenesShellService;

    @GetMapping(value = "/listar", produces = "application/json")
    public ResponseEntity<?> listar(
            @RequestParam("brand") String brand,
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam(value = "codBoca", required = false) String codBoca,
            @RequestParam(value = "codDistribuidor", required = false) String codDistribuidor) {

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().body(error("brand no soportado: " + brand));
        }
        if (mes < 1 || mes > 12) {
            return ResponseEntity.badRequest().body(error("mes inválido (debe ser 1-12)"));
        }
        if (anio < 2000 || anio > 2100) {
            return ResponseEntity.badRequest().body(error("anio inválido"));
        }

        String brandLower = brand.toLowerCase();
        List<ImagenAdminDTO> resultado;

        switch (brandLower) {
            case "jhonson":
                resultado = imagenesSCJService.findAllByMes(anio, mes, codBoca);
                break;
            case "nestle":
                if (codDistribuidor == null || codDistribuidor.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(error("codDistribuidor es obligatorio para Nestle"));
                }
                resultado = imagenesNestService.findAllByMes(anio, mes, codDistribuidor, codBoca);
                break;
            case "shell":
                resultado = imagenesShellService.findAllByMes(anio, mes, codBoca);
                break;
            default:
                return ResponseEntity.badRequest().body(error("brand no soportado: " + brand));
        }

        // armar urlPublica con cache-buster
        long cb = System.currentTimeMillis();
        resultado.forEach(dto -> dto.setUrlPublica(
                "/api/v1/admin/imagenes/binario?brand=" + brandLower
                + "&path=" + dto.getPathRelativo() + "&v=" + cb));

        return ResponseEntity.ok(resultado);
    }

    @PostMapping(value = "/rotar", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> rotar(@RequestBody Map<String, String> body) {
        String brand = body.get("brand");
        String path = body.get("path");

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().body(error("brand no soportado"));
        }

        try {
            ImagenPathValidator.validarPathRelativo(brand, path);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rechazado intento de rotar path inválido. brand={}, path={}, error={}",
                        brand, path, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }

        try {
            switch (brand.toLowerCase()) {
                case "jhonson":
                    imagenesSCJService.rotarImagen(path);
                    break;
                case "nestle":
                    imagenesNestService.rotarImagen(path);
                    break;
                case "shell":
                    imagenesShellService.rotarImagen(path);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen brand=" + brand + " path=" + path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("No se pudo rotar la imagen: " + e.getMessage()));
        }

        long cb = System.currentTimeMillis();
        Map<String, String> resp = new HashMap<>();
        resp.put("path", path);
        resp.put("urlPublica", "/api/v1/admin/imagenes/binario?brand=" + brand.toLowerCase()
                + "&path=" + path + "&v=" + cb);
        return ResponseEntity.ok(resp);
    }

    private Map<String, String> error(String mensaje) {
        Map<String, String> m = new HashMap<>();
        m.put("mensaje", mensaje);
        return m;
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java
git commit -m "feat: AdminImagenesController con endpoints listar y rotar"
```

---

### Task 7: Endpoint `/binario` para servir imágenes

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java`

- [ ] **Step 1: Agregar import en AdminImagenesController**

Buscar la sección de imports y agregar:

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
```

- [ ] **Step 2: Agregar el campo de la property y el método al controller**

Después de los `@Autowired` existentes:

```java
    @Value("${path.directory.main_imagenes}")
    private String directorioServer;
```

Y agregar el método antes del helper `error()`:

```java
    @GetMapping("/binario")
    public ResponseEntity<Resource> binario(
            @RequestParam("brand") String brand,
            @RequestParam("path") String path) {

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ImagenPathValidator.validarPathRelativo(brand, path);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rechazado intento de leer path inválido. brand={}, path={}, error={}",
                        brand, path, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        Path archivo = Paths.get(directorioServer, path);
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(archivo.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw clean compile -P dev`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java
git commit -m "feat: agregar endpoint /binario para servir imágenes en admin"
```

---

### Task 8: Validación manual del backend con curl

- [ ] **Step 1: Levantar el backend en dev**

Run en terminal aparte: `./mvnw spring-boot:run`
Expected: aplicación corriendo en `http://localhost:8080/ccr-rest-api`

- [ ] **Step 2: Obtener token JWT**

Sustituir `usuario`/`pass` por credenciales válidas:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/ccr-rest-api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario","password":"pass"}' | jq -r '.token')
echo "$TOKEN"
```

Expected: un JWT no vacío.

- [ ] **Step 3: Probar listar para Jhonson**

Reemplazar `2026`/`05` por un mes/año con datos reales en el filesystem dev:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/ccr-rest-api/api/v1/admin/imagenes/listar?brand=jhonson&anio=2026&mes=05" \
  | jq .
```

Expected: array JSON con objetos `ImagenAdminDTO`. Cada uno tiene `urlPublica` apuntando a `/api/v1/admin/imagenes/binario?...`.

- [ ] **Step 4: Probar listar para Nestle sin distribuidor (debe fallar)**

```bash
curl -s -i -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/ccr-rest-api/api/v1/admin/imagenes/listar?brand=nestle&anio=2026&mes=05"
```

Expected: HTTP 400 con `{"mensaje":"codDistribuidor es obligatorio para Nestle"}`.

- [ ] **Step 5: Probar rotar con path inválido (debe fallar)**

```bash
curl -s -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"brand":"jhonson","path":"../../etc/passwd"}' \
  http://localhost:8080/ccr-rest-api/api/v1/admin/imagenes/rotar
```

Expected: HTTP 400 con mensaje de path inválido. Verificar en logs WARN del backend.

- [ ] **Step 6: Probar rotar con path válido**

Tomar un `pathRelativo` del Step 3 y rotarlo:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"brand":"jhonson","path":"zoomin-jhonson/<codBoca>/<archivo>.jpg"}' \
  http://localhost:8080/ccr-rest-api/api/v1/admin/imagenes/rotar | jq .
```

Expected: HTTP 200 con `{path, urlPublica}`. Verificar visualmente en el filesystem que la imagen quedó rotada (abrir con un visor).

- [ ] **Step 7: Probar binario**

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/ccr-rest-api/api/v1/admin/imagenes/binario?brand=jhonson&path=zoomin-jhonson/<codBoca>/<archivo>.jpg" \
  -o /tmp/foto-test.jpg
file /tmp/foto-test.jpg
```

Expected: archivo descargado, `file` reporta `JPEG image data`.

---

## Frontend Tasks

### Task 9: Modelo TypeScript

**Files:**
- Create: `src/app/models/imagen-admin.interface.ts`

- [ ] **Step 1: Crear la interface**

```typescript
export interface IImagenAdmin {
  brand: string;
  codBoca: string;
  codDistribuidor: string | null;
  fileName: string;
  pathRelativo: string;
  urlPublica: string;
  anio: number | null;
  mes: number | null;
}

export interface IRotarResponse {
  path: string;
  urlPublica: string;
}
```

- [ ] **Step 2: Verificar que compila**

Run en `d-insights-ccr`: `npx tsc --noEmit`
Expected: sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/app/models/imagen-admin.interface.ts
git commit -m "feat: agregar interface IImagenAdmin para módulo admin de rotación"
```

---

### Task 10: Métodos HTTP en `InsightsService`

**Files:**
- Modify: `src/app/core/services/insights.service.ts`

- [ ] **Step 1: Agregar imports al inicio del archivo**

Buscar la sección de imports y agregar:

```typescript
import { IImagenAdmin, IRotarResponse } from "../../models/imagen-admin.interface";
```

- [ ] **Step 2: Agregar métodos al final de la clase (antes del último `}`)**

```typescript
  // ── Admin imágenes ─────────────────────────────────────────────────

  listarImagenesAdmin(params: {
    brand: string;
    anio: number;
    mes: number;
    codBoca?: string;
    codDistribuidor?: string;
  }): Observable<IImagenAdmin[]> {
    const httpParams: { [key: string]: string } = {
      brand: params.brand,
      anio: String(params.anio),
      mes: String(params.mes),
    };
    if (params.codBoca) httpParams["codBoca"] = params.codBoca;
    if (params.codDistribuidor) httpParams["codDistribuidor"] = params.codDistribuidor;
    return this.http.get<IImagenAdmin[]>(
      `${this.COMMONS_ADMIN_URL}/imagenes/listar`,
      { params: httpParams },
    );
  }

  rotarImagenAdmin(brand: string, path: string): Observable<IRotarResponse> {
    return this.http.post<IRotarResponse>(
      `${this.COMMONS_ADMIN_URL}/imagenes/rotar`,
      { brand, path },
    );
  }

  buildImagenUrl(brand: string, path: string, cacheBuster: number | string): string {
    return `${this.COMMONS_ADMIN_URL}/imagenes/binario?brand=${brand}&path=${encodeURIComponent(path)}&v=${cacheBuster}`;
  }
```

- [ ] **Step 3: Verificar que compila**

Run: `npx tsc --noEmit`
Expected: sin errores.

- [ ] **Step 4: Commit**

```bash
git add src/app/core/services/insights.service.ts src/app/models/imagen-admin.interface.ts
git commit -m "feat: agregar métodos HTTP de admin de imágenes a InsightsService"
```

---

### Task 11: Componente del modal de previsualización

**Files:**
- Create: `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.ts`
- Create: `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.html`
- Create: `src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.scss`

- [ ] **Step 1: Crear el TypeScript**

```typescript
import { CommonModule } from "@angular/common";
import { Component, EventEmitter, inject, Input, Output } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { ToastrService } from "ngx-toastr";

import { InsightsService } from "../../../../core/services/insights.service";
import { IImagenAdmin } from "../../../../models/imagen-admin.interface";

@Component({
  selector: "app-imagen-preview-modal",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./imagen-preview-modal.component.html",
  styleUrl: "./imagen-preview-modal.component.scss",
})
export class ImagenPreviewModalComponent {
  private apiService = inject(InsightsService);
  private toastService = inject(ToastrService);

  @Input() imagen!: IImagenAdmin;
  @Output() cerrar = new EventEmitter<void>();
  @Output() rotada = new EventEmitter<IImagenAdmin>();

  rotando = false;

  get tituloModal(): string {
    if (!this.imagen) return "";
    const distr = this.imagen.codDistribuidor ? `Dist. ${this.imagen.codDistribuidor} / ` : "";
    return `${distr}Boca ${this.imagen.codBoca} — ${this.imagen.fileName}`;
  }

  async rotar() {
    if (this.rotando || !this.imagen) return;
    this.rotando = true;
    try {
      const resp = await firstValueFrom(
        this.apiService.rotarImagenAdmin(this.imagen.brand, this.imagen.pathRelativo),
      );
      this.imagen = { ...this.imagen, urlPublica: resp.urlPublica };
      this.rotada.emit(this.imagen);
    } catch (error) {
      console.error(error);
      this.toastService.error("No se pudo rotar la imagen. Intentá de nuevo.", "Error", {
        positionClass: "toast-top-center",
      });
    } finally {
      this.rotando = false;
    }
  }

  cerrarModal() {
    this.cerrar.emit();
  }
}
```

- [ ] **Step 2: Crear el HTML**

```html
<div class="modal-backdrop" (click)="cerrarModal()"></div>
<div class="modal-imagen" role="dialog" aria-modal="true">
  <div class="modal-header">
    <h5 class="modal-title">{{ tituloModal }}</h5>
    <button type="button" class="btn-close" aria-label="Cerrar" (click)="cerrarModal()">×</button>
  </div>
  <div class="modal-body">
    <img [src]="imagen.urlPublica" [alt]="imagen.fileName" />
  </div>
  <div class="modal-footer">
    <button type="button" class="btn btn-primary" (click)="rotar()" [disabled]="rotando">
      <ng-container *ngIf="!rotando">↻ Rotar 90° CW</ng-container>
      <ng-container *ngIf="rotando">Rotando...</ng-container>
    </button>
  </div>
</div>
```

- [ ] **Step 3: Crear el SCSS**

```scss
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1040;
}

.modal-imagen {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  z-index: 1050;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #e9ecef;

    .modal-title {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
    }

    .btn-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: #888;
      line-height: 1;
    }
  }

  .modal-body {
    padding: 16px;
    overflow: auto;
    text-align: center;

    img {
      max-width: 100%;
      max-height: 70vh;
      object-fit: contain;
    }
  }

  .modal-footer {
    padding: 12px 16px;
    border-top: 1px solid #e9ecef;
    text-align: right;

    .btn-primary {
      min-width: 160px;
    }
  }
}
```

- [ ] **Step 4: Verificar que compila**

Run: `npx tsc --noEmit`
Expected: sin errores.

- [ ] **Step 5: Commit**

```bash
git add src/app/pages/admin/imagenes/imagen-preview-modal/
git commit -m "feat: ImagenPreviewModalComponent con visualización y rotación"
```

---

### Task 12: Componente principal `ImagenesAdminComponent` — TypeScript

**Files:**
- Create: `src/app/pages/admin/imagenes/imagenes-admin.component.ts`

**Diferencias entre brands aplicables aquí:** el componente expone `esNestle` para mostrar el dropdown de distribuidor solo en ese caso. El listado de bocas depende del brand seleccionado.

- [ ] **Step 1: Crear el TypeScript**

```typescript
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import { NgSelectComponent } from "@ng-select/ng-select";

import { GenericFormComponent } from "../../../core/base/generic-form.component";
import { InsightsService } from "../../../core/services/insights.service";
import { IImagenAdmin } from "../../../models/imagen-admin.interface";
import { LoaderComponent } from "../../../shared/ui/loader/loader.component";
import { PagetitleComponent } from "../../../shared/ui/pagetitle/pagetitle.component";
import { ImagenPreviewModalComponent } from "./imagen-preview-modal/imagen-preview-modal.component";

interface OpcionSelect {
  value: string;
  label: string;
}

@Component({
  selector: "app-imagenes-admin",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NgSelectComponent,
    LoaderComponent,
    PagetitleComponent,
    ImagenPreviewModalComponent,
  ],
  templateUrl: "./imagenes-admin.component.html",
  styleUrl: "./imagenes-admin.component.scss",
})
export class ImagenesAdminComponent extends GenericFormComponent<any> implements OnInit {

  brandSeleccionado: string | null = null;
  anioSeleccionado: number = new Date().getFullYear();
  mesSeleccionado: number = new Date().getMonth() + 1;
  codDistribuidor: string | null = null;
  codBoca: string | null = null;

  distribuidores: OpcionSelect[] = [];
  bocas: OpcionSelect[] = [];

  imagenes: IImagenAdmin[] = [];
  buscaRealizada = false;

  imagenSeleccionada: IImagenAdmin | null = null;

  readonly opcionesBrand: OpcionSelect[] = [
    { value: "jhonson", label: "Jhonson" },
    { value: "nestle", label: "Nestle" },
    { value: "shell", label: "Shell" },
  ];

  readonly opcionesMes: OpcionSelect[] = [
    { value: "1", label: "Enero" }, { value: "2", label: "Febrero" }, { value: "3", label: "Marzo" },
    { value: "4", label: "Abril" }, { value: "5", label: "Mayo" }, { value: "6", label: "Junio" },
    { value: "7", label: "Julio" }, { value: "8", label: "Agosto" }, { value: "9", label: "Septiembre" },
    { value: "10", label: "Octubre" }, { value: "11", label: "Noviembre" }, { value: "12", label: "Diciembre" },
  ];

  get aniosDisponibles(): number[] {
    const actual = new Date().getFullYear();
    return [actual - 2, actual - 1, actual];
  }

  get esNestle(): boolean {
    return this.brandSeleccionado === "nestle";
  }

  constructor(private apiService: InsightsService) {
    super();
  }

  override async ngOnInit() {
    super.ngOnInit();
  }

  async onBrandChange() {
    this.distribuidores = [];
    this.bocas = [];
    this.codDistribuidor = null;
    this.codBoca = null;
    this.imagenes = [];
    this.buscaRealizada = false;

    if (!this.brandSeleccionado) return;

    if (this.esNestle) {
      await this.cargarDistribuidores();
    } else {
      await this.cargarBocas();
    }
  }

  async onDistribuidorChange() {
    this.bocas = [];
    this.codBoca = null;
    this.imagenes = [];
    this.buscaRealizada = false;
    if (this.esNestle && this.codDistribuidor) {
      await this.cargarBocas();
    }
  }

  async cargarDistribuidores() {
    try {
      const data = await firstValueFrom(this.apiService.getDistribuidoresNestle());
      this.distribuidores = (data ?? [])
        .map((d: any) => ({ value: d.codDistribuidor, label: `${d.codDistribuidor} — ${d.nombre}` }))
        .sort((a, b) => a.label.localeCompare(b.label));
    } catch (error) {
      console.error(error);
      this.showToastError("No se pudieron cargar los distribuidores.");
    }
  }

  async cargarBocas() {
    if (!this.brandSeleccionado) return;
    try {
      let data: any[] = [];
      if (this.brandSeleccionado === "jhonson") {
        data = await firstValueFrom(this.apiService.getBocasJhonson()) ?? [];
      } else if (this.brandSeleccionado === "shell") {
        data = await firstValueFrom(this.apiService.getBocasShell()) ?? [];
      } else if (this.brandSeleccionado === "nestle" && this.codDistribuidor) {
        data = await firstValueFrom(this.apiService.getBocasNestle(this.codDistribuidor)) ?? [];
      }
      this.bocas = data
        .map((b: any) => ({ value: b.codBoca, label: `${b.codBoca} — ${b.nombre}` }))
        .sort((a, b) => a.label.localeCompare(b.label));
    } catch (error) {
      console.error(error);
      this.bocas = [];
    }
  }

  async buscar() {
    if (!this.brandSeleccionado) {
      this.showToastWarning("Seleccioná un brand.");
      return;
    }
    if (this.esNestle && !this.codDistribuidor) {
      this.showToastWarning("Seleccioná un distribuidor para Nestle.");
      return;
    }

    this.showLoader();
    this.buscaRealizada = false;
    try {
      this.imagenes = await firstValueFrom(
        this.apiService.listarImagenesAdmin({
          brand: this.brandSeleccionado,
          anio: this.anioSeleccionado,
          mes: this.mesSeleccionado,
          codBoca: this.codBoca || undefined,
          codDistribuidor: this.codDistribuidor || undefined,
        }),
      );
      this.buscaRealizada = true;
    } catch (error) {
      console.error(error);
      this.showToastError("No se pudo cargar el listado de imágenes.");
    } finally {
      this.hideLoader();
    }
  }

  abrirPreview(imagen: IImagenAdmin) {
    this.imagenSeleccionada = imagen;
  }

  cerrarPreview() {
    this.imagenSeleccionada = null;
  }

  onImagenRotada(actualizada: IImagenAdmin) {
    // refrescar solo el tile correspondiente
    const idx = this.imagenes.findIndex((i) => i.pathRelativo === actualizada.pathRelativo);
    if (idx >= 0) {
      this.imagenes[idx] = { ...this.imagenes[idx], urlPublica: actualizada.urlPublica };
    }
    this.showToastSuccess("Imagen rotada.");
  }
}
```

> **Nota:** este componente asume que existen en `InsightsService` los métodos `getDistribuidoresNestle()`, `getBocasJhonson()`, `getBocasNestle(codDistribuidor)`, `getBocasShell()`. Si alguno no existe, verificarlo y agregarlo siguiendo el patrón de los otros métodos del service.

- [ ] **Step 2: Verificar que compila**

Run: `npx tsc --noEmit`
Expected: sin errores. Si falta algún método en `InsightsService`, agregarlo siguiendo el patrón existente (por ejemplo `getBocasShell` puede no existir — chequear primero con grep).

- [ ] **Step 3: Commit**

```bash
git add src/app/pages/admin/imagenes/imagenes-admin.component.ts
git commit -m "feat: ImagenesAdminComponent TS con filtros y búsqueda"
```

---

### Task 13: Componente principal — HTML y SCSS

**Files:**
- Create: `src/app/pages/admin/imagenes/imagenes-admin.component.html`
- Create: `src/app/pages/admin/imagenes/imagenes-admin.component.scss`

- [ ] **Step 1: Crear el HTML**

```html
<app-pagetitle title="Rotar Imágenes" [breadcrumbItems]="[]"></app-pagetitle>

<div class="card">
  <div class="card-body">
    <div class="row gy-3 gx-3 align-items-end">
      <div class="col-12 col-md-3">
        <label class="form-label">Brand</label>
        <ng-select
          [items]="opcionesBrand"
          bindValue="value"
          bindLabel="label"
          [(ngModel)]="brandSeleccionado"
          (ngModelChange)="onBrandChange()"
          placeholder="Seleccionar..."
          [clearable]="false"></ng-select>
      </div>
      <div class="col-6 col-md-2">
        <label class="form-label">Año</label>
        <ng-select
          [items]="aniosDisponibles"
          [(ngModel)]="anioSeleccionado"
          [clearable]="false"></ng-select>
      </div>
      <div class="col-6 col-md-2">
        <label class="form-label">Mes</label>
        <ng-select
          [items]="opcionesMes"
          bindValue="value"
          bindLabel="label"
          [(ngModel)]="mesSeleccionado"
          [clearable]="false"></ng-select>
      </div>
      <div class="col-12 col-md-3" *ngIf="esNestle">
        <label class="form-label">Distribuidor</label>
        <ng-select
          [items]="distribuidores"
          bindValue="value"
          bindLabel="label"
          [(ngModel)]="codDistribuidor"
          (ngModelChange)="onDistribuidorChange()"
          placeholder="Seleccionar..."></ng-select>
      </div>
      <div class="col-12 col-md-3">
        <label class="form-label">Boca (opcional)</label>
        <ng-select
          [items]="bocas"
          bindValue="value"
          bindLabel="label"
          [(ngModel)]="codBoca"
          placeholder="Todas"></ng-select>
      </div>
      <div class="col-12 col-md-2">
        <button class="btn btn-primary w-100" (click)="buscar()">Buscar</button>
      </div>
    </div>
  </div>
</div>

<div class="mt-3" *ngIf="buscaRealizada">
  <div class="mb-2 text-muted">{{ imagenes.length }} imágenes</div>

  <div class="grid-imagenes" *ngIf="imagenes.length > 0; else sinResultados">
    <div class="imagen-tile" *ngFor="let img of imagenes" (click)="abrirPreview(img)">
      <img [src]="img.urlPublica" [alt]="img.fileName" loading="lazy" />
      <div class="caption">{{ img.codBoca }} — {{ img.fileName }}</div>
    </div>
  </div>

  <ng-template #sinResultados>
    <div class="alert alert-info">No hay imágenes con esos filtros.</div>
  </ng-template>
</div>

<div *ngIf="!buscaRealizada && imagenes.length === 0" class="mt-3 text-muted text-center">
  Aplicá filtros para ver imágenes.
</div>

<app-loader></app-loader>

<app-imagen-preview-modal
  *ngIf="imagenSeleccionada"
  [imagen]="imagenSeleccionada"
  (cerrar)="cerrarPreview()"
  (rotada)="onImagenRotada($event)">
</app-imagen-preview-modal>
```

- [ ] **Step 2: Crear el SCSS**

```scss
.grid-imagenes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.imagen-tile {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  img {
    width: 100%;
    aspect-ratio: 4 / 3;
    object-fit: cover;
    display: block;
    background: #f8f9fa;
  }

  .caption {
    padding: 6px 8px;
    font-size: 0.75rem;
    color: #6c757d;
    border-top: 1px solid #f0f0f0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
```

- [ ] **Step 3: Verificar que compila**

Run: `npx tsc --noEmit`
Expected: sin errores.

- [ ] **Step 4: Commit**

```bash
git add src/app/pages/admin/imagenes/imagenes-admin.component.html src/app/pages/admin/imagenes/imagenes-admin.component.scss
git commit -m "feat: HTML y SCSS de ImagenesAdminComponent"
```

---

### Task 14: Registrar la ruta

**Files:**
- Modify: `src/app/pages/admin/admin-routing.module.ts`

- [ ] **Step 1: Agregar import**

Al inicio del archivo, después del último import de componentes:

```typescript
import { ImagenesAdminComponent } from "./imagenes/imagenes-admin.component";
```

- [ ] **Step 2: Agregar la ruta en el array `routes`**

Después de la ruta de items:

```typescript
  { path: "imagenes", component: ImagenesAdminComponent },
```

- [ ] **Step 3: Verificar que compila**

Run: `npx tsc --noEmit`
Expected: sin errores.

- [ ] **Step 4: Commit**

```bash
git add src/app/pages/admin/admin-routing.module.ts
git commit -m "feat: registrar ruta /admin/imagenes en admin-routing.module"
```

---

### Task 15: Validación end-to-end manual

- [ ] **Step 1: Levantar frontend**

Run en `d-insights-ccr`: `npm start` (o `ng serve`)
Expected: app corriendo en `http://localhost:4200`.

- [ ] **Step 2: Login**

Login con un usuario que tenga rol "admin" (recordar que `AdminRoleGuard` redirige si no).

- [ ] **Step 3: Navegar a `/admin/imagenes`**

URL: `http://localhost:4200/admin/imagenes`. Esperado: pantalla con filtros vacíos y mensaje "Aplicá filtros para ver imágenes.".

- [ ] **Step 4: Probar flujo Jhonson**

1. Seleccionar Brand=Jhonson, Año/Mes con datos reales.
2. Click "Buscar" → spinner → aparece el grid.
3. Click en una foto que se ve acostada → modal con foto grande.
4. Click "↻ Rotar 90° CW" → spinner en el botón → foto se actualiza en el modal.
5. Cerrar modal → el tile del grid también se actualizó.

- [ ] **Step 5: Probar flujo Nestle**

1. Seleccionar Brand=Nestle → aparece dropdown Distribuidor.
2. Sin elegir distribuidor, click "Buscar" → toast warning.
3. Elegir distribuidor → bocas se cargan.
4. Click "Buscar" → aparecen fotos del distribuidor.
5. Rotar una foto.

- [ ] **Step 6: Probar flujo Shell**

Idéntico a Jhonson — distribuidor no aparece.

- [ ] **Step 7: Probar caso de error**

Detener el backend; intentar buscar → toast rojo "No se pudo cargar el listado de imágenes.".

- [ ] **Step 8: Commit final**

Si hubo ajustes (típicamente menores), commitear y consolidar:

```bash
git status
git add -A
git commit -m "fix: ajustes UX módulo admin rotación de imágenes"
```

---

### Task 16 (opcional): Item de menú lateral

Si el usuario lo pide, agregar entrada en `src/app/pages/layouts/sidebar/menu.ts`. **Coordinar primero** con el usuario porque el menú usa claves de traducción i18n y la decisión de dónde poner el item es de UX.

---

## Notas finales para el implementador

1. **Compilación del backend** después de cada task: si falla, revisar imports (especialmente `java.util.stream.Stream` y `IOException` en los services).
2. **Compilación del frontend** después de cada task: revisar nombres exactos de los métodos en `InsightsService` (ej. `getBocasShell` puede no existir — hay que crearlo siguiendo el patrón).
3. **Performance**: si un mes tiene muchas fotos (500+), revisar los logs del backend para asegurar que el listado de archivos no demora más de 2-3 segundos. Si tarda, agregar paginación al endpoint es scope aparte.
4. **Edge cases observados durante validación**: si un archivo no matchea el patrón `{codBoca}_{anio}_{mes}_{nro}.jpg`, el filtro por `_anio_mes_` puede dejarlo fuera. Documentar si aparece y decidir si extender el filtro.
5. **Si el frontend pide muchas imágenes en paralelo a `/binario` y el backend lame**, considerar agregar caché HTTP en el endpoint binario (`Cache-Control: max-age=...`) — scope aparte.
