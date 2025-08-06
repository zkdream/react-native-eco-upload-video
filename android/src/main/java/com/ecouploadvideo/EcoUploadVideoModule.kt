package com.ecouploadvideo

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit

@ReactModule(name = EcoUploadVideoModule.NAME)
class EcoUploadVideoModule(reactContext: ReactApplicationContext) :
    NativeEcoUploadVideoSpec(reactContext) {

    override fun getName(): String {
        return NAME
    }

    private var fileCutUtils: FileCutUtils? = null
    private var littleFileCount: Int = 1
    private var littleFileList: MutableList<File> = ArrayList()
    private var reactContext: ReactApplicationContext = reactContext

    // 存储活跃的上传请求
    private val activeCalls: MutableSet<Call> = Collections.synchronizedSet(HashSet())


    override fun uploadVideoWithUrl(
        url: String,
        fileInfoType: String,
        videoData: ReadableMap,
        promise: Promise
    ) {
        littleFileList = ArrayList()
        littleFileCount = 1

        try {
            // 从videoData中提取filePath
            // 修改前
            // val filePath = videoData.getString("filePath") ?: ""

            // 修改后
            val filePath = videoData.getString("uri") ?: ""

            if (filePath.isEmpty()) {
                promise.reject("INVALID_PARAM", "File path is required")
                return
            }

            var processedFilePath = filePath
            if (processedFilePath.startsWith("file://")) {
                processedFilePath = processedFilePath.substring(7)
            }

            Log.e("FileCutUtils", "getSplitFile: $processedFilePath");
            // 检查文件是否存在和可读
            val file = File(processedFilePath)
            Log.e("FileCutUtils", "file.exists():888" + file.exists());
            if (!file.exists()) {
                Log.e("FileCutUtils", "file.exists():" + file.exists());
                promise.reject("FILE_NOT_FOUND", "File does not exist: $processedFilePath")
                return
            }

            if (!file.canRead()) {
                Log.e("FileCutUtils", "file.canRead(): $file.canRead()");
                promise.reject("PERMISSION_DENIED", "Cannot read file: $processedFilePath")
                return
            }

            val mBufferSize = 1024 * 1024 * 2L // 分片的大小，可自定义
            // 初始化FileCutUtils
            fileCutUtils = FileCutUtils(reactContext)
            // 获取分割后的文件数量
            littleFileCount = fileCutUtils?.getSplitFile(file, mBufferSize) ?: 1
            // 直接访问littlefilelist成员变量
            littleFileList = fileCutUtils?.littlefilelist ?: ArrayList()

            Thread {
                uploadFile(0, url, fileInfoType, promise)
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject("ERROR", e.message)
        }
    }

    override fun cancelUploadVideoTask() {
        // 取消所有活跃的上传请求
        synchronized(activeCalls) {
            for (call in activeCalls) {
                if (!call.isCanceled()) {
                    call.cancel()
                    Log.d("UploadVideoModule", "Upload request canceled")
                }
            }
            activeCalls.clear()
        }
        // 清理临时文件
        deleteLittleList()
    }

    // 自定义方法删除临时文件
    private fun deleteLittleList() {
        if (littleFileList.isNotEmpty()) {
            for (file in littleFileList) {
                if (file.exists()) {
                    file.delete()
                }
            }
            littleFileList.clear()
        }
    }

    private fun uploadFile(fileNum: Int, url: String, fileInfoType: String, promise: Promise) {
        if (fileNum >= littleFileList.size) {
            // 所有文件上传完成
            reactContext.runOnUiQueueThread {
                promise.resolve(null)
            }
            return
        }

        val multiBuilder = MultipartBody.Builder()
        val file = littleFileList[fileNum]

        val name = file.name.substring(file.name.lastIndexOf("_"))
        multiBuilder.setType(MultipartBody.FORM)
        multiBuilder.addFormDataPart("fileInfoType", fileInfoType)
        multiBuilder.addFormDataPart("name", name)
        multiBuilder.addFormDataPart("chunk", fileNum.toString())
        multiBuilder.addFormDataPart("chunks", littleFileCount.toString())
        multiBuilder.addFormDataPart("file", "blob", file.asRequestBody("video/mp4".toMediaType()))

        val multiBody = multiBuilder.build()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).post(multiBody).build()
        val call = okHttpClient.newCall(request)
        // 添加到活跃请求集合
        activeCalls.add(call)

        // 明确指定使用okhttp3.Callback接口
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activeCalls.remove(call)
                promise.reject("UPLOAD_FAILED", "Failed to upload chunk $fileNum: " + e.message)
                // 清理临时文件
                deleteLittleList()
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                activeCalls.remove(call)
                                 val res: String? = response.body?.string()
                 try {
                     val jsonObject = JSONObject(res ?: "")
                     if (jsonObject.optString("StatusCode") == "0") {
                         val obj: JSONObject? = jsonObject.optJSONObject("StatusData")
                         if (obj?.optString("ResultCode") == "0") {
                             val url: String = obj.optJSONObject("ResultData")?.optString("FileUrl") ?: ""
                             deleteLittleList()
                             promise.resolve(url)
                         } else if (obj?.optString("ResultCode") == "8001") {
                             if (littleFileCount > fileNum + 1) {
                                 uploadFile(fileNum + 1, url, fileInfoType, promise)
                             }
                         }else{
                             promise.reject("UPLOAD_FAILED","接口错误")
                         }
                     } else {
                         deleteLittleList()
                         promise.resolve("")
                     }
                } catch (e: JSONException) {
                    promise.reject("UPLOAD_FAILED", "Invalid response format: " + e.message)
                    // 清理临时文件
                    deleteLittleList()
                }
            }
        })
    }

    companion object {
        const val NAME = "EcoUploadVideo"
    }
}
