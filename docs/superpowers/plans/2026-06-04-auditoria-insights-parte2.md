# Auditoría del módulo Insights — Parte 2 (consulta + pantalla) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer la auditoría de Insights con un endpoint paginado/filtrable (por evento y por cliente) y una pantalla de administración **Administración → Auditoría** que la lista, con modal de detalle.

**Architecture:** Backend (ccr-rest): controller dedicado `AuditoriaInsController` + DTOs + métodos de repositorio derivados + métodos de servicio, espejando el patrón server-side de `listarInformes`/`InformePageDTO`. Frontend (d-insights-ccr, Angular 18 standalone): modelo + métodos de servicio + componente de listado server-side (mirando `clientes-admin`) + ruta + ítem de menú en el topbar.

**Tech Stack:** Spring Boot 2.7 / JPA / Java 8 (backend); Angular 18, Bootstrap 5, ng-select, SweetAlert2 (frontend).

> **DOS REPOS:**
> - Backend: `/Users/jaime/development/workspace-sts/ccr-rest` — ya en rama `feature/auditoria-insights-p2`.
> - Frontend: `/Users/jaime/development/workspace-angular/d-insights-ccr` — crear rama de feature antes de commitear (Task 5, Step 0).

> **Pruebas:** sin tests automatizados. Verificación = compilar (`./mvnw -o -q compile` backend; `npx ng build` frontend) + prueba manual al final.

> **Java 8:** sin `var`, sin `Map.of`, sin `records`.

> **Pre-existentes:** en ccr-rest el working tree tiene cambios sin commitear no relacionados (`TemplateInsService.java`, `application-*.properties`). NO tocarlos ni commitearlos.

---

## PARTE A — Backend (ccr-rest)

### Task 1: DTOs de auditoría

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/dto/AuditoriaDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/dto/AuditoriaPageDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/dto/EventoCatalogoDTO.java`

- [ ] **Step 1: Crear `AuditoriaDTO`**

```java
package py.com.jaimeferreira.ccr.insights.dto;

import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;

import java.time.LocalDateTime;

/**
 * Vista de un registro de auditoría para el listado de administración.
 *
 * @author Jaime Ferreira
 */
public class AuditoriaDTO {

    private Long id;
    private String evento;
    private String eventoDescripcion;
    private String resultado;
    private String usuario;
    private String codCliente;
    private String codCategoria;
    private String tipoReporte;
    private LocalDateTime fechaHora;
    private String detalle;

    public static AuditoriaDTO from(AuditoriaIns a) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.id = a.getId();
        dto.evento = a.getEvento() != null ? a.getEvento().name() : null;
        dto.eventoDescripcion = a.getEvento() != null ? a.getEvento().getDescripcion() : null;
        dto.resultado = a.getResultado() != null ? a.getResultado().name() : null;
        dto.usuario = a.getUsuario();
        dto.codCliente = a.getCodCliente();
        dto.codCategoria = a.getCodCategoria();
        dto.tipoReporte = a.getTipoReporte() != null ? a.getTipoReporte().name() : null;
        dto.fechaHora = a.getFechaHora();
        dto.detalle = a.getDetalle();
        return dto;
    }

    public Long getId() { return id; }
    public String getEvento() { return evento; }
    public String getEventoDescripcion() { return eventoDescripcion; }
    public String getResultado() { return resultado; }
    public String getUsuario() { return usuario; }
    public String getCodCliente() { return codCliente; }
    public String getCodCategoria() { return codCategoria; }
    public String getTipoReporte() { return tipoReporte; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getDetalle() { return detalle; }
}
```

- [ ] **Step 2: Crear `AuditoriaPageDTO`** (mismo shape que `InformePageDTO`)

```java
package py.com.jaimeferreira.ccr.insights.dto;

import java.util.List;

public class AuditoriaPageDTO {

    private List<AuditoriaDTO> content;
    private long totalElements;

    public AuditoriaPageDTO(List<AuditoriaDTO> content, long totalElements) {
        this.content = content;
        this.totalElements = totalElements;
    }

    public List<AuditoriaDTO> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
}
```

- [ ] **Step 3: Crear `EventoCatalogoDTO`**

```java
package py.com.jaimeferreira.ccr.insights.dto;

import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;

public class EventoCatalogoDTO {

    private String codigo;
    private String descripcion;

    public EventoCatalogoDTO(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static EventoCatalogoDTO from(EventoAuditoriaIns evento) {
        return new EventoCatalogoDTO(evento.name(), evento.getDescripcion());
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}
```

- [ ] **Step 4: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/dto/AuditoriaDTO.java \
        src/main/java/py/com/jaimeferreira/ccr/insights/dto/AuditoriaPageDTO.java \
        src/main/java/py/com/jaimeferreira/ccr/insights/dto/EventoCatalogoDTO.java
git commit -m "feat(insights): DTOs de auditoría (listado + catálogo)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Métodos de consulta en el repository

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/insights/repository/AuditoriaInsRepository.java`

- [ ] **Step 1: Reemplazar el contenido del repository**

El archivo actual sólo declara la interfaz vacía. Reemplazarlo por (agrega imports + métodos derivados; mismo estilo que `InformeInsRepository`):

```java
package py.com.jaimeferreira.ccr.insights.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;

import java.util.List;

/**
 * @author Jaime Ferreira
 */
public interface AuditoriaInsRepository extends JpaRepository<AuditoriaIns, Long> {

    List<AuditoriaIns> findAllByOrderByFechaHoraDesc(Pageable pageable);

    List<AuditoriaIns> findByEventoOrderByFechaHoraDesc(EventoAuditoriaIns evento, Pageable pageable);

    List<AuditoriaIns> findByCodClienteOrderByFechaHoraDesc(String codCliente, Pageable pageable);

    List<AuditoriaIns> findByEventoAndCodClienteOrderByFechaHoraDesc(
            EventoAuditoriaIns evento, String codCliente, Pageable pageable);

    long countByEvento(EventoAuditoriaIns evento);

    long countByCodCliente(String codCliente);

    long countByEventoAndCodCliente(EventoAuditoriaIns evento, String codCliente);
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0 (Spring Data valida los nombres de métodos derivados en arranque, no en compile; el compile sólo verifica la firma).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/repository/AuditoriaInsRepository.java
git commit -m "feat(insights): consultas paginadas/filtradas en AuditoriaInsRepository

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Métodos de listado en el servicio

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/insights/service/AuditoriaInsService.java`

El servicio ya existe (de la Parte 1, con el método `registrar`). Agregar dos métodos públicos y los imports necesarios.

- [ ] **Step 1: Agregar imports**

Junto a los imports existentes del archivo, agregar:

```java
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import py.com.jaimeferreira.ccr.insights.dto.AuditoriaDTO;
import py.com.jaimeferreira.ccr.insights.dto.AuditoriaPageDTO;
import py.com.jaimeferreira.ccr.insights.dto.EventoCatalogoDTO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
```

> `AuditoriaIns`, `EventoAuditoriaIns` y `AuditoriaInsRepository` ya están importados (los usa `registrar`).

- [ ] **Step 2: Agregar los métodos `listar` y `listarEventos`**

Dentro de la clase `AuditoriaInsService`, después del método `registrar(...)`:

```java
    /**
     * Lista registros de auditoría paginados y filtrados (server-side), ordenados
     * por fecha-hora descendente. Ramifica por combinación de filtros, igual que
     * {@code InformeInsService.findUltimos}.
     *
     * @param evento     filtro por evento (null = todos)
     * @param codCliente filtro por cliente normalizado (null/vacío = todos)
     * @param page       página base 0
     * @param size       tamaño de página
     */
    public AuditoriaPageDTO listar(EventoAuditoriaIns evento, String codCliente, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean tieneCliente = codCliente != null && !codCliente.isEmpty();
        boolean tieneEvento = evento != null;

        long totalElements;
        List<AuditoriaIns> registros;

        if (tieneEvento && tieneCliente) {
            totalElements = auditoriaInsRepository.countByEventoAndCodCliente(evento, codCliente);
            registros = auditoriaInsRepository.findByEventoAndCodClienteOrderByFechaHoraDesc(evento, codCliente, pageable);
        } else if (tieneCliente) {
            totalElements = auditoriaInsRepository.countByCodCliente(codCliente);
            registros = auditoriaInsRepository.findByCodClienteOrderByFechaHoraDesc(codCliente, pageable);
        } else if (tieneEvento) {
            totalElements = auditoriaInsRepository.countByEvento(evento);
            registros = auditoriaInsRepository.findByEventoOrderByFechaHoraDesc(evento, pageable);
        } else {
            totalElements = auditoriaInsRepository.count();
            registros = auditoriaInsRepository.findAllByOrderByFechaHoraDesc(pageable);
        }

        List<AuditoriaDTO> content = registros.stream()
                .map(AuditoriaDTO::from)
                .collect(Collectors.toList());
        return new AuditoriaPageDTO(content, totalElements);
    }

    /**
     * Catálogo de eventos auditables (para poblar el filtro de la pantalla).
     */
    public List<EventoCatalogoDTO> listarEventos() {
        return Arrays.stream(EventoAuditoriaIns.values())
                .map(EventoCatalogoDTO::from)
                .collect(Collectors.toList());
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/service/AuditoriaInsService.java
git commit -m "feat(insights): listar y catálogo de eventos en AuditoriaInsService

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Controller de auditoría

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/admin/controller/AuditoriaInsController.java`

- [ ] **Step 1: Crear el controller**

```java
package py.com.jaimeferreira.ccr.insights.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.AuditoriaPageDTO;
import py.com.jaimeferreira.ccr.insights.dto.EventoCatalogoDTO;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.service.AuditoriaInsService;

import java.util.List;

/**
 * Endpoints de consulta de la auditoría del módulo Insights.
 * Requieren JWT válido (mismo surface admin que AdminPlataformaController).
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/insights/api/v1/admin/auditoria")
public class AuditoriaInsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditoriaInsController.class);

    @Autowired
    private AuditoriaInsService auditoriaInsService;

    /**
     * Lista paginada y filtrable de registros de auditoría.
     *
     * @param evento     filtro por código de evento (opcional)
     * @param codCliente filtro por código de cliente (opcional)
     * @param page       página base 0 (default 0)
     * @param size       tamaño de página (default 10)
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<AuditoriaPageDTO> listar(
            @RequestParam(value = "evento", required = false) String evento,
            @RequestParam(value = "codCliente", required = false) String codCliente,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        LOGGER.info("Listando auditoría insights. evento={}, codCliente={}, page={}, size={}",
                evento, codCliente, page, size);

        EventoAuditoriaIns eventoEnum = parseEvento(evento);
        String codClienteNorm = (codCliente != null && !codCliente.trim().isEmpty())
                ? codCliente.trim().toUpperCase() : null;

        return ResponseEntity.ok(auditoriaInsService.listar(eventoEnum, codClienteNorm, page, size));
    }

    /**
     * Catálogo de eventos auditables (para el filtro de la pantalla).
     */
    @GetMapping(value = "/eventos", produces = "application/json")
    public ResponseEntity<List<EventoCatalogoDTO>> listarEventos() {
        LOGGER.info("Listando catálogo de eventos de auditoría insights.");
        return ResponseEntity.ok(auditoriaInsService.listarEventos());
    }

    private EventoAuditoriaIns parseEvento(String evento) {
        if (evento == null || evento.trim().isEmpty()) {
            return null;
        }
        try {
            return EventoAuditoriaIns.valueOf(evento.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnknownResourceException("Evento de auditoría inválido: " + evento);
        }
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/admin/controller/AuditoriaInsController.java
git commit -m "feat(insights): AuditoriaInsController (listado + catálogo de eventos)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## PARTE B — Frontend (d-insights-ccr)

> Todas las tareas de esta parte trabajan en `/Users/jaime/development/workspace-angular/d-insights-ccr` (repo git distinto).

### Task 5: Rama, modelo y servicio

**Files:**
- Create: `src/app/models/auditoria.interface.ts`
- Modify: `src/app/core/services/insights.service.ts`

- [ ] **Step 0: Crear/posicionar rama de feature en el repo frontend**

```bash
cd /Users/jaime/development/workspace-angular/d-insights-ccr
git checkout -b feature/auditoria-insights 2>/dev/null || git checkout feature/auditoria-insights
git status --short
```
Expected: en rama `feature/auditoria-insights`. (Si hay cambios sin relación previos, no tocarlos.)

- [ ] **Step 1: Crear el modelo**

Crear `src/app/models/auditoria.interface.ts`:

```typescript
export interface IAuditoria {
  id: number;
  evento: string;
  eventoDescripcion: string;
  resultado: "EXITO" | "ERROR";
  usuario: string;
  codCliente: string;
  codCategoria?: string;
  tipoReporte?: string;
  fechaHora: string;
  detalle?: string;
}

export interface IAuditoriaPage {
  content: IAuditoria[];
  totalElements: number;
}

export interface IEventoCatalogo {
  codigo: string;
  descripcion: string;
}
```

- [ ] **Step 2: Agregar import del modelo en `insights.service.ts`**

Junto a los demás imports de modelos (después de `import { IImagenAdmin, IRotarResponse } ...`):

```typescript
import { IAuditoriaPage, IEventoCatalogo } from "../../models/auditoria.interface";
```

- [ ] **Step 3: Agregar los dos métodos al final de la clase `InsightsService`**

Antes de la llave de cierre de la clase:

```typescript
  getAdminAuditoria(filtros: {
    evento?: string;
    codCliente?: string;
    page: number;
    size: number;
  }): Observable<IAuditoriaPage> {
    const params: { [key: string]: string } = {
      page: String(filtros.page),
      size: String(filtros.size),
    };
    if (filtros.evento) {
      params["evento"] = filtros.evento;
    }
    if (filtros.codCliente) {
      params["codCliente"] = filtros.codCliente;
    }
    return this.http.get<IAuditoriaPage>(`${this.ADMIN_URL}/auditoria`, { params });
  }

  getAdminAuditoriaEventos(): Observable<IEventoCatalogo[]> {
    return this.http.get<IEventoCatalogo[]>(`${this.ADMIN_URL}/auditoria/eventos`);
  }
```

- [ ] **Step 4: Commit**

```bash
git add src/app/models/auditoria.interface.ts src/app/core/services/insights.service.ts
git commit -m "feat(auditoria): modelo y métodos de servicio para auditoría insights

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Componente de listado

**Files:**
- Create: `src/app/pages/admin/auditoria/auditoria-admin.component.ts`
- Create: `src/app/pages/admin/auditoria/auditoria-admin.component.html`
- Create: `src/app/pages/admin/auditoria/auditoria-admin.component.scss`

> Extiende `BaseComponent` (pantalla read-only, sin formulario). `BaseComponent` provee `showLoader/hideLoader/showToastError`. Mira `clientes-admin` pero con paginación **server-side**.

- [ ] **Step 1: Crear el componente TypeScript**

Crear `src/app/pages/admin/auditoria/auditoria-admin.component.ts`:

```typescript
import { CommonModule } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import Swal from "sweetalert2";
import { NgSelectComponent } from "@ng-select/ng-select";

import { BaseComponent } from "../../../core/base/base.component";
import { InsightsService } from "../../../core/services/insights.service";
import { ICliente } from "../../../models/cliente.interface";
import { IAuditoria, IEventoCatalogo } from "../../../models/auditoria.interface";
import { LoaderComponent } from "../../../shared/ui/loader/loader.component";
import { PagetitleComponent } from "../../../shared/ui/pagetitle/pagetitle.component";

@Component({
  selector: "app-auditoria-admin",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LoaderComponent,
    PagetitleComponent,
    NgSelectComponent,
  ],
  templateUrl: "./auditoria-admin.component.html",
  styleUrl: "./auditoria-admin.component.scss",
})
export class AuditoriaAdminComponent extends BaseComponent implements OnInit {
  private apiService = inject(InsightsService);

  items: IAuditoria[] = [];
  eventos: IEventoCatalogo[] = [];
  clientes: ICliente[] = [];

  eventoFiltro = "";
  clienteFiltro = "";
  paginaActual = 1;
  tamanioPagina = 10;
  totalElements = 0;

  readonly opcionesPagina = [10, 25, 50];

  async ngOnInit() {
    await this.cargarCatalogos();
    await this.cargarPagina();
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.tamanioPagina));
  }

  get paginas(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(1, this.paginaActual - 2);
    const fin = Math.min(this.totalPaginas, inicio + 4);
    const inicioAjustado = Math.max(1, fin - 4);
    for (let pagina = inicioAjustado; pagina <= fin; pagina += 1) {
      paginas.push(pagina);
    }
    return paginas;
  }

  get mostrandoDesde(): number {
    if (!this.totalElements) {
      return 0;
    }
    return (this.paginaActual - 1) * this.tamanioPagina + 1;
  }

  get mostrandoHasta(): number {
    return Math.min(this.paginaActual * this.tamanioPagina, this.totalElements);
  }

  private async cargarCatalogos() {
    this.showLoader();
    try {
      const [eventos, clientes] = await Promise.all([
        firstValueFrom(this.apiService.getAdminAuditoriaEventos()),
        firstValueFrom(this.apiService.getAdminClientes()),
      ]);
      this.eventos = eventos;
      this.clientes = [...clientes].sort((a, b) =>
        a.descripcion.localeCompare(b.descripcion),
      );
    } catch (error) {
      console.error(error);
      this.showToastError("No se pudieron cargar los filtros de auditoría.");
    } finally {
      this.hideLoader();
    }
  }

  async cargarPagina() {
    this.showLoader();
    try {
      const page = await firstValueFrom(
        this.apiService.getAdminAuditoria({
          evento: this.eventoFiltro || undefined,
          codCliente: this.clienteFiltro || undefined,
          page: this.paginaActual - 1,
          size: this.tamanioPagina,
        }),
      );
      this.items = page.content;
      this.totalElements = page.totalElements;
    } catch (error) {
      console.error(error);
      this.showToastError("No se pudo cargar la auditoría.");
    } finally {
      this.hideLoader();
    }
  }

  async onFiltrosChange() {
    this.paginaActual = 1;
    await this.cargarPagina();
  }

  async onPageSizeChange(size: number | string) {
    this.tamanioPagina = Number(size) || 10;
    this.paginaActual = 1;
    await this.cargarPagina();
  }

  async cambiarPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas || pagina === this.paginaActual) {
      return;
    }
    this.paginaActual = pagina;
    await this.cargarPagina();
  }

  verDetalle(item: IAuditoria) {
    let contenido: string;
    if (item.detalle) {
      try {
        contenido = JSON.stringify(JSON.parse(item.detalle), null, 2);
      } catch {
        contenido = item.detalle;
      }
    } else {
      contenido = "Sin detalle";
    }
    Swal.fire({
      title: "Detalle del evento",
      html: `<pre style="text-align:left;white-space:pre-wrap;word-break:break-word;max-height:50vh;overflow:auto;">${this.escapeHtml(contenido)}</pre>`,
      confirmButtonColor: "#34c38f",
      width: 600,
    });
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }
}
```

- [ ] **Step 2: Crear el template HTML**

Crear `src/app/pages/admin/auditoria/auditoria-admin.component.html`:

```html
<app-loader></app-loader>

<div class="container-fluid">
  <div class="row">
    <div class="col-12">
      <app-page-title
        [title]="'Auditoría'"
        [breadcrumbItems]="[
          { label: 'Admin', active: false },
          { label: 'Auditoría', active: true },
        ]"
      ></app-page-title>
    </div>
  </div>

  <div class="card admin-card">
    <div class="card-body p-4">
      <div class="mb-4">
        <h4 class="mb-1">Auditoría de Insights</h4>
        <p class="text-muted mb-0">
          Registro de acciones sobre archivos base (template, datos y filtros).
        </p>
      </div>

      <div class="row g-3 align-items-end mb-4">
        <div class="col-lg-4">
          <label class="form-label">Evento</label>
          <ng-select
            [items]="eventos"
            bindLabel="descripcion"
            bindValue="codigo"
            placeholder="Todos los eventos"
            [clearable]="true"
            [searchable]="true"
            [(ngModel)]="eventoFiltro"
            (change)="onFiltrosChange()"
            (clear)="onFiltrosChange()"
          ></ng-select>
        </div>

        <div class="col-lg-4">
          <label class="form-label">Cliente</label>
          <ng-select
            [items]="clientes"
            bindLabel="descripcion"
            bindValue="codigo"
            placeholder="Todos los clientes"
            [clearable]="true"
            [searchable]="true"
            [(ngModel)]="clienteFiltro"
            (change)="onFiltrosChange()"
            (clear)="onFiltrosChange()"
          ></ng-select>
        </div>

        <div class="col-lg-2">
          <label class="form-label">Filas por página</label>
          <select
            class="form-select"
            [ngModel]="tamanioPagina"
            (ngModelChange)="onPageSizeChange($event)"
          >
            @for (opcion of opcionesPagina; track opcion) {
              <option [ngValue]="opcion">{{ opcion }}</option>
            }
          </select>
        </div>
      </div>

      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Fecha / Hora</th>
              <th>Evento</th>
              <th>Resultado</th>
              <th>Usuario</th>
              <th>Cliente</th>
              <th>Categoría</th>
              <th>Tipo</th>
              <th class="text-end">Detalle</th>
            </tr>
          </thead>
          <tbody>
            @if (items.length) {
              @for (item of items; track item.id) {
                <tr>
                  <td>{{ item.fechaHora | date: "dd/MM/yyyy HH:mm:ss" }}</td>
                  <td>{{ item.eventoDescripcion }}</td>
                  <td>
                    <span
                      class="badge rounded-pill"
                      [class.bg-success-subtle]="item.resultado === 'EXITO'"
                      [class.text-success]="item.resultado === 'EXITO'"
                      [class.bg-danger-subtle]="item.resultado === 'ERROR'"
                      [class.text-danger]="item.resultado === 'ERROR'"
                    >
                      {{ item.resultado }}
                    </span>
                  </td>
                  <td>{{ item.usuario }}</td>
                  <td>{{ item.codCliente }}</td>
                  <td>{{ item.codCategoria || "-" }}</td>
                  <td>{{ item.tipoReporte || "-" }}</td>
                  <td class="text-end">
                    <button
                      type="button"
                      class="btn btn-sm btn-soft-primary"
                      (click)="verDetalle(item)"
                    >
                      <i class="bx bx-show me-1"></i>
                      Ver
                    </button>
                  </td>
                </tr>
              }
            } @else {
              <tr>
                <td colspan="8" class="text-center py-5 text-muted">
                  No se encontraron registros de auditoría con los filtros seleccionados.
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div
        class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mt-4"
      >
        <p class="text-muted mb-0">
          Mostrando {{ mostrandoDesde }} a {{ mostrandoHasta }} de
          {{ totalElements }} registros
        </p>

        <nav aria-label="Paginación de auditoría">
          <ul class="pagination pagination-rounded mb-0">
            <li class="page-item" [class.disabled]="paginaActual === 1">
              <button class="page-link" type="button" aria-label="Primera página" (click)="cambiarPagina(1)">
                <i class="bx bx-first-page"></i>
              </button>
            </li>
            <li class="page-item" [class.disabled]="paginaActual === 1">
              <button class="page-link" type="button" aria-label="Página anterior" (click)="cambiarPagina(paginaActual - 1)">
                <i class="bx bx-chevron-left"></i>
              </button>
            </li>
            @for (pagina of paginas; track pagina) {
              <li class="page-item" [class.active]="pagina === paginaActual">
                <button class="page-link" type="button" (click)="cambiarPagina(pagina)">
                  {{ pagina }}
                </button>
              </li>
            }
            <li class="page-item" [class.disabled]="paginaActual === totalPaginas">
              <button class="page-link" type="button" aria-label="Página siguiente" (click)="cambiarPagina(paginaActual + 1)">
                <i class="bx bx-chevron-right"></i>
              </button>
            </li>
            <li class="page-item" [class.disabled]="paginaActual === totalPaginas">
              <button class="page-link" type="button" aria-label="Última página" (click)="cambiarPagina(totalPaginas)">
                <i class="bx bx-last-page"></i>
              </button>
            </li>
          </ul>
        </nav>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 3: Crear el SCSS**

Crear `src/app/pages/admin/auditoria/auditoria-admin.component.scss`:

```scss
.admin-card {
  border: none;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/app/pages/admin/auditoria/
git commit -m "feat(auditoria): pantalla de listado de auditoría (server-side)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Ruta y menú

**Files:**
- Modify: `src/app/pages/admin/admin-routing.module.ts`
- Modify: `src/app/pages/layouts/topbar/topbar.component.html`

- [ ] **Step 1: Agregar import y ruta en `admin-routing.module.ts`**

Agregar el import junto a los demás (después de `import { ImagenesAdminComponent } ...`):

```typescript
import { AuditoriaAdminComponent } from "./auditoria/auditoria-admin.component";
```

Y agregar la ruta dentro del array `routes` (después de la línea de `imagenes`):

```typescript
  { path: "auditoria", component: AuditoriaAdminComponent },
```

- [ ] **Step 2: Agregar el ítem en el dropdown "Administración" del topbar**

En `src/app/pages/layouts/topbar/topbar.component.html`, dentro del dropdown de Administración, después del ítem de **Logs** (`routerLink="/admin/logs"` … `</a>`), agregar:

```html
        <a class="dropdown-item" routerLink="/admin/auditoria">
          <i class="bx bx-history font-size-16 align-middle me-2"></i>
          Auditoría
        </a>
```

- [ ] **Step 3: Commit**

```bash
git add src/app/pages/admin/admin-routing.module.ts \
        src/app/pages/layouts/topbar/topbar.component.html
git commit -m "feat(auditoria): ruta /admin/auditoria y entrada en menú Administración

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: Build de verificación del frontend

**Files:** ninguno (verificación).

- [ ] **Step 1: Asegurar dependencias instaladas**

```bash
cd /Users/jaime/development/workspace-angular/d-insights-ccr
[ -d node_modules ] || npm install
```

- [ ] **Step 2: Build de producción (verificación de compilación Angular)**

Run: `npx ng build`
Expected: `Application bundle generation complete` sin errores de TypeScript/template. Puede tardar 1-3 min.

Si falla, leer el error y corregir (causas típicas: import faltante, selector de componente mal escrito, ruta de import del modelo/servicio). No commitear hasta que el build pase.

- [ ] **Step 3: Commit (sólo si hubo correcciones)**

Si el build requirió ajustes, commitearlos:
```bash
git add -A
git commit -m "fix(auditoria): correcciones de build

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```
Si no hubo cambios, no hay nada que commitear.

---

## Task 9: Verificación manual end-to-end

**Files:** ninguno (verificación; la realiza el usuario contra su entorno).

- [ ] **Step 1: Backend arriba**

Levantar `ccr-rest` (`./mvnw spring-boot:run`). La tabla `ccr.auditoria_insights` ya existe (Parte 1) — no requiere DDL nuevo.

- [ ] **Step 2: Probar el endpoint de catálogo**

`GET /ccr-rest-api/insights/api/v1/admin/auditoria/eventos` (con JWT) → 4 eventos con `codigo` + `descripcion`.

- [ ] **Step 3: Probar el listado y filtros**

- `GET .../admin/auditoria` → primera página ordenada por fecha desc + `totalElements`.
- `GET .../admin/auditoria?evento=TEMPLATE_SUBIDO` → sólo ese evento.
- `GET .../admin/auditoria?codCliente=NESTLE` → sólo ese cliente.
- `GET .../admin/auditoria?evento=TEMPLATE_SUBIDO&codCliente=NESTLE` → ambos filtros.
- `GET .../admin/auditoria?evento=XXX` → error (contrato `UnknownResourceException`).

- [ ] **Step 4: Frontend**

Levantar el front (`npm start` / `npx ng serve`), entrar como admin → menú **Administración → Auditoría**:
- La tabla carga y pagina contra el backend.
- Filtrar por evento y por cliente recarga desde el server.
- Botón **Ver** abre el modal con el JSON de `detalle` formateado.

---

## Self-Review (cobertura del spec)

- **GET /auditoria paginado/filtrable (evento, cliente, page, size) server-side** → Tasks 1-4 (DTOs, repo, service, controller). ✓
- **GET /auditoria/eventos (catálogo)** → Task 1 (`EventoCatalogoDTO`), Task 3 (`listarEventos`), Task 4 (endpoint). ✓
- **Orden por fecha_hora desc** → métodos `...OrderByFechaHoraDesc` (Task 2). ✓
- **evento inválido → UnknownResourceException; null/vacío → no filtra; codCliente normalizado** → Task 4 (`parseEvento`, `codClienteNorm`). ✓
- **AuditoriaPageDTO {content, totalElements} igual que InformePageDTO** → Task 1. ✓
- **Modelo TS + servicio (getAdminAuditoria, getAdminAuditoriaEventos)** → Task 5. ✓
- **Componente listado server-side mirando clientes; columnas fecha/evento/resultado(badge)/usuario/cliente/categoría/tipo/Ver; detalle en modal SweetAlert2** → Task 6. ✓
- **Ruta /admin/auditoria** → Task 7. ✓
- **Menú Administración → Auditoría** (dropdown del topbar, texto plano, no i18n — corrige la nota i18n del spec) → Task 7. ✓
- **Sin filtro de fecha (sólo evento + cliente)** → respetado. ✓
- **Verificación: compile backend, ng build frontend, prueba manual** → Tasks 4/8/9. ✓

**Nota de corrección al spec:** el spec mencionaba i18n en `assets/i18n`, pero el menú real (`topbar.component.html`) usa **texto plano en español** para los ítems admin; por lo tanto **no se agregan claves i18n** (el ítem "Auditoría" va como texto directo). El resto del spec se mantiene.

Sin placeholders. Tipos/nombres consistentes entre tareas (`AuditoriaDTO`, `AuditoriaPageDTO`, `EventoCatalogoDTO`, `listar`, `listarEventos`, `getAdminAuditoria`, `getAdminAuditoriaEventos`, `AuditoriaAdminComponent`, ruta `auditoria`).
