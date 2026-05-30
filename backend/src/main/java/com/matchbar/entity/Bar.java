package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bars")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bar {

    @Id
    private String id;

    private String userId;

    private String name;

    private String description;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String ownerPhone;

    private String licenseDocFileId;

    private String licenseDocFilename;

    /** Fotos del establecimiento (ids de ficheros en GridFS). */
    @Builder.Default
    private List<String> photoFileIds = new ArrayList<>();

    /** Carta del bar: una o varias imágenes (ids de ficheros en GridFS). */
    @Builder.Default
    private List<String> menuFileIds = new ArrayList<>();

    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    private Instant createdAt;

    public enum Status { PENDING, APPROVED, REJECTED }
}
