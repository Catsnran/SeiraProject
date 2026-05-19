package com.seira.models;

public class Category {
    private int id;
    private int userId;
    private String name;
    private String type; // INCOME, EXPENSE, or BOTH
    private String color;
    private String icon;

    public Category() {}

    public Category(int id, String name, String type, String color, String icon) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color != null ? color : "#C87941"; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public String toString() { return name; }
}
