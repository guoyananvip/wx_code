package com.tencent.wxcloudrun.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: 王富贵
 * @description:
 * @createTime: 2024年04月27日 00:05:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Health implements Serializable {
    private String action;
}
