package com.huanke.iot.manage.controller.project;

import com.huanke.iot.base.api.ApiResponse;
import com.huanke.iot.base.exception.BusinessException;
import com.huanke.iot.base.request.BaseListRequest;
import com.huanke.iot.base.request.config.DictQueryRequest;
import com.huanke.iot.base.request.project.RuleRequest;
import com.huanke.iot.base.resp.project.RuleRsp;
import com.huanke.iot.manage.service.project.RuleService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 描述:
 * 规则管理
 *
 * @author onlymark
 * @create 2018-11-14 下午4:37
 */
@RestController
@RequestMapping("/api/rule")
@Slf4j
public class RuleController {
    @Autowired
    private RuleService ruleService;

    @ApiOperation("查询客户列表")
    @PostMapping(value = "/selectList")
    public ApiResponse<RuleRsp> selectList(@RequestBody DictQueryRequest request) throws Exception {
        RuleRsp ruleRsp = ruleService.selectList(request);
        return new ApiResponse<>(ruleRsp);
    }

    @ApiOperation("添加规则信息")
    @PostMapping(value = "/addRule")
    public ApiResponse<Boolean> addRule(@RequestBody RuleRequest request) {
        Boolean result = ruleService.addOrUpdate(request);
        return new ApiResponse<>(result);
    }

    @ApiOperation("修改规则信息")
    @PostMapping(value = "/editRule")
    public ApiResponse<Boolean> editRule(@RequestBody RuleRequest request) {
        Boolean result = ruleService.addOrUpdate(request);
        return new ApiResponse<>(result);
    }

    @ApiOperation("批量删除规则")
    @PostMapping(value = "/deleteRule")
    public ApiResponse<Boolean> deleteRule(@RequestBody BaseListRequest<Integer> request) {
        List<Integer> valueList = request.getValueList();
        if(valueList.size() == 0){
            throw new BusinessException("没有要删除规则信息");
        }
        Boolean result = ruleService.deleteRule(valueList);
        return new ApiResponse<>(result);
    }

    @ApiOperation("批量禁用规则")
    @PostMapping(value = "/forbitRule")
    public ApiResponse<Boolean> forbitRule(@RequestBody BaseListRequest<Integer> request) {
        List<Integer> valueList = request.getValueList();
        if(valueList.size() == 0){
            throw new BusinessException("没有要禁用规则信息");
        }
        Boolean result = ruleService.forbitRule(valueList);
        return new ApiResponse<>(result);
    }
}
