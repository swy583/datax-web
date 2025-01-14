package com.wugui.datax.admin.controller;

import com.wugui.datatx.core.biz.model.ReturnT;
import com.wugui.datax.admin.service.JobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@RestController
@Api(tags = "首页接口")
@RequestMapping("/api")
public class IndexController {

	@Resource
	private JobService jobService;


	@GetMapping("/index")
	@ApiOperation("监控图")
	public ReturnT<Map<String, Object>> index() {
		return new ReturnT<>(jobService.dashboardInfo());
	}

    @RequestMapping(value = "/chartInfo",method  = RequestMethod.POST)
	@ApiOperation("图表信息")
	public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        ReturnT<Map<String, Object>> chartInfo = jobService.chartInfo(startDate, endDate);
        return chartInfo;
    }

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}
	
}
