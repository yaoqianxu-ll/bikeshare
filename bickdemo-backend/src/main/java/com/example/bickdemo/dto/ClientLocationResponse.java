package com.example.bickdemo.dto;

import lombok.Data;

/**
 * 基于客户端 IP 推断出的静默定位结果。
 * 这不是浏览器 GPS 定位，不会触发权限弹窗，但精度依赖公网 IP 归属地。
 */
@Data
public class ClientLocationResponse {

    /** 客户端 IP */
    private String ip;

    /** 国家/地区名称 */
    private String country;

    /** 国家/地区编码 */
    private String countryCode;

    /** 省级名称 */
    private String province;

    /** 市级名称 */
    private String city;

    /** 区县名称 */
    private String district;

    /** 推断坐标纬度 */
    private Double latitude;

    /** 推断坐标经度 */
    private Double longitude;

    /** 组合后的地区文本 */
    private String locationText;

    /** 数据来源 */
    private String source;
}
