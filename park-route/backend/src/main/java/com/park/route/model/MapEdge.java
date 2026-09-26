package com.park.route.model;

import jakarta.persistence.*;

/** 道路边（无向，寻路时双向通行） */
@Entity
@Table(name = "map_edge")
public class MapEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "from_code", nullable = false, length = 32)
    private String fromCode;

    @Column(name = "to_code", nullable = false, length = 32)
    private String toCode;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    /** 是否允许步行 */
    @Column(name = "walk_allowed", nullable = false)
    private Boolean walkAllowed = true;

    /** 是否允许摆渡车通行 */
    @Column(name = "shuttle_allowed", nullable = false)
    private Boolean shuttleAllowed = false;

    /** 是否为货车通道（访客路线一律避开） */
    @Column(name = "truck_road", nullable = false)
    private Boolean truckRoad = false;

    @Column(nullable = false)
    private Boolean active = true;

    public MapEdge() {}

    public MapEdge(String code, String fromCode, String toCode, Integer distanceMeters,
                   Boolean walkAllowed, Boolean shuttleAllowed, Boolean truckRoad) {
        this.code = code;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.distanceMeters = distanceMeters;
        this.walkAllowed = walkAllowed;
        this.shuttleAllowed = shuttleAllowed;
        this.truckRoad = truckRoad;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFromCode() { return fromCode; }
    public void setFromCode(String fromCode) { this.fromCode = fromCode; }
    public String getToCode() { return toCode; }
    public void setToCode(String toCode) { this.toCode = toCode; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public Boolean getWalkAllowed() { return walkAllowed; }
    public void setWalkAllowed(Boolean walkAllowed) { this.walkAllowed = walkAllowed; }
    public Boolean getShuttleAllowed() { return shuttleAllowed; }
    public void setShuttleAllowed(Boolean shuttleAllowed) { this.shuttleAllowed = shuttleAllowed; }
    public Boolean getTruckRoad() { return truckRoad; }
    public void setTruckRoad(Boolean truckRoad) { this.truckRoad = truckRoad; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
