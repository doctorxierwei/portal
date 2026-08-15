package com.portal.mes.entity;

import lombok.Data;

/**
 * 字典项 (value + label)
 */
@Data
public class DictItem {
    private String value;
    private String label;

    public DictItem() {
    }

    public DictItem(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
