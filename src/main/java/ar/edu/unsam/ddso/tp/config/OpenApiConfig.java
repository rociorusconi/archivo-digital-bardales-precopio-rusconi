package ar.edu.unsam.ddso.tp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Archivo Digital API",
                version = "1.0",
                description = "APIs de Digitalización, Catalogación y Disponibilización del Archivo Digital. "
                        + "Especificación completa en docs/api.md."),
        tags = {
            @Tag(name = OpenApiConfig.DIGITIZATION, description = "Alta de piezas y sus digitalizaciones"),
            @Tag(name = OpenApiConfig.CATALOGING, description = "Tags y su asignación a digitalizaciones"),
            @Tag(name = OpenApiConfig.AVAILABILITY, description = "Búsqueda de piezas"),
            @Tag(name = OpenApiConfig.IMPORT, description = "Carga masiva de piezas desde un CSV (proceso batch)")
        })
public class OpenApiConfig {

    public static final String DIGITIZATION = "Digitalización";
    public static final String CATALOGING = "Catalogación";
    public static final String AVAILABILITY = "Disponibilización";
    public static final String IMPORT = "Carga masiva";
}
