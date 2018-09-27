package com.huanke.iot.base.dao;

import com.huanke.iot.base.po.config.DictPo;

import java.util.List;

/**
 * 描述:
 *
 * @author onlymark
 * @create 2018-09-14 下午1:54
 */
public interface DictMapper extends BaseMapper<DictPo> {
    List<DictPo> selectByType(String type);
}