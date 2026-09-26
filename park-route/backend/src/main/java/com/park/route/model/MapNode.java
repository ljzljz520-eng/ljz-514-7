package com.park.route.model;

import jakarta.persistence.*;

@Entity
@Table(name = "map_node")
public class MapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NodeType type;

    /** SVG 地图坐标 */
    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;

    @Column(nullable = false)
    private Boolean active = true;

    public MapNode() {}

    public MapNode(String code, String name, NodeType type, Integer x, Integer y) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
