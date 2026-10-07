package com.careerpilot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String settingKey;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String settingValue;

    private String settingGroup; // AI, INTERVIEW, GENERAL

    private String description;

    public SystemSetting() {}

    public SystemSetting(String settingKey, String settingValue, String settingGroup, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingGroup = settingGroup;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getSettingGroup() { return settingGroup; }
    public void setSettingGroup(String settingGroup) { this.settingGroup = settingGroup; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
