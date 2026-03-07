package dev.example;

import java.nio.file.Files;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/convert")
public class SvgConverterResource {

    @Inject
    SvgConversionService conversionService;

    @POST
    @Path("/png")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("image/png")
    public Response svgToPng(
            @RestForm("file") FileUpload file,
            @RestForm("scale") Double scale,
            @RestForm("dpi") Integer dpi) throws Exception {

        double scaleValue = scale != null ? scale : 1.0;
        int dpiValue = dpi != null ? dpi : 96;

        byte[] svgBytes = Files.readAllBytes(file.uploadedFile());

        byte[] pngBytes = conversionService.convertToPng(svgBytes, scaleValue, dpiValue);

        String outputName = originalName(file) + ".png";

        return Response.ok(pngBytes)
                .header("Content-Disposition",
                        "attachment; filename=\"" + outputName + "\"")
                .header("Content-Length", pngBytes.length)
                .build();
    }

    private String originalName(FileUpload file) {
        String name = file.fileName();
        if (name != null && name.endsWith(".svg")) {
            return name.substring(0, name.length() - 4);
        }
        return "output";
    }

}
