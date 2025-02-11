package com.wugui.datax.executor.service.jobhandler;

import cn.hutool.core.io.FileUtil;
import com.wugui.datatx.core.biz.model.HandleProcessCallbackParam;
import com.wugui.datatx.core.biz.model.ReturnT;
import com.wugui.datatx.core.biz.model.TriggerParam;
import com.wugui.datatx.core.handler.IJobHandler;
import com.wugui.datatx.core.handler.annotation.JobHandler;
import com.wugui.datatx.core.log.JobLogger;
import com.wugui.datatx.core.thread.ProcessCallbackThread;
import com.wugui.datatx.core.util.ProcessUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * DataX任务运行
 *
 * @author jingwk 2019-11-16
 */

@JobHandler(value = "executorJobHandler")
@Component
public class ExecutorJobHandler extends IJobHandler {

    @Value("${datax.executor.jsonpath}")
    private String jsonpath;



    @Override
    public ReturnT<String> executeDataX(TriggerParam tgParam) throws Exception {

        int exitValue = -1;
        BufferedReader bufferedReader = null;
        String tmpFilePath = null;
        String line = null;
        //生成Json临时文件
        tmpFilePath = generateTemJsonFile(tgParam.getJobJson());
        try {
            // command process
            Process process = Runtime.getRuntime().exec(new String[]{"python",tgParam.getExecutorParams(), getDataXPyPath(), tmpFilePath});
            String processId = ProcessUtil.getProcessId(process);
            JobLogger.log("------------------DataX运行进程Id: " + processId);
            jobTmpFiles.put(processId, tmpFilePath);
            //更新任务进程Id
            ProcessCallbackThread.pushCallBack(new HandleProcessCallbackParam(tgParam.getLogId(),tgParam.getLogDateTime(),processId));
            InputStreamReader input = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(input);
            while ((line = bufferedReader.readLine()) != null) {
                JobLogger.log(line);
            }

            InputStreamReader error = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(error);
            while ((line = bufferedReader.readLine()) != null) {
                JobLogger.log(line);
            }
            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            JobLogger.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            //  删除临时文件
            if (FileUtil.exist(tmpFilePath)) {
                FileUtil.del(new File(tmpFilePath));
            }
        }

        if (exitValue == 0) {
            return IJobHandler.SUCCESS;
        } else {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "command exit value(" + exitValue + ") is failed");
        }
    }

    private String generateTemJsonFile(String jobJson) {
        String tmpFilePath;
        if (!FileUtil.exist(jsonpath)) {
            FileUtil.mkdir(jsonpath);
        }
        tmpFilePath = jsonpath + "jobTmp-" + System.currentTimeMillis() + ".conf";
        // 根据json写入到临时本地文件
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(jobJson);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            JobLogger.log("JSON 临时文件写入异常：" + e.getMessage());
        }
        return tmpFilePath;
    }


    private String getDataXPyPath() {
        String dataxPyPath;
        String dataXHome = System.getenv("DATAX_HOME");
        if (StringUtils.isBlank(dataXHome)) {
            JobLogger.log("DATAX_HOME 环境变量为NULL");
        }
        String osName = System.getProperty("os.name");
        dataXHome = osName.contains("Windows") ? (!dataXHome.endsWith("\\") ? dataXHome.concat("\\") : dataXHome) : (!dataXHome.endsWith("/") ? dataXHome.concat("/") : dataXHome);
        dataxPyPath = dataXHome + "datax.py";
        return dataxPyPath;
    }
}
