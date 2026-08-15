package com.portal.user.entity;

import lombok.Data;

/**
 * 字典项 (按类型查询时返回 value + label)
 */
@Data
public class DictItem {
    /** 字典值 */
    private String value;
    /** 字典显示名称 */
    private String label;
    /** 备注 */
    private String remark;

    public DictItem() {
    }

    public DictItem(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public DictItem(String value, String label, String remark) {
        this.value = value;
        this.label = label;
        this.remark = remark;
    }
}
