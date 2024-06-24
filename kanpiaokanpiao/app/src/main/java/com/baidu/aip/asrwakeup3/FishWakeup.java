package com.baidu.aip.asrwakeup3;

import android.content.Context;

import com.baidu.aip.asrwakeup3.core.wakeup.WakeUpResult;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.Map;


public class FishWakeup  implements EventListener {
    private static boolean isInited = false;
    private EventManager wp;
    private EventListener localResultLisener = null;
    private static final String TAG = "MyWakeup";
    ActivityMain parent_context;
    private   String json_param ="" ;
    public FishWakeup(Context context,ActivityMain parent) {
        parent_context = parent;
        if (isInited) {
            System.out.println(TAG + "还未调用release()，请勿新建一个新类");
            throw new RuntimeException("还未调用release()，请勿新建一个新类");
        }
        isInited = true;
        wp = EventManagerFactory.create(context, "wp");
        localResultLisener = this;  //new FishWakeupEventAdapter();
        wp.registerListener(localResultLisener);
    }

    //实际推理唤醒服务是C++完成，这里是通过消息发送给C++的推理引擎的。
    public void start(Map<String, Object> params) {
        json_param = new JSONObject(params).toString();
        System.out.println(TAG +  "启动唤醒引擎，wakeup params(反馈请带上此行日志):" + json_param);
        start();


    }
    public void start(){
        if(json_param.length() >0) {
            wp.send(SpeechConstant.WAKEUP_START, json_param, null, 0, 0);
        }
    }

    public void stop() {
        System.out.println(TAG + "唤醒引擎结束");
        wp.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
    }


    public void release() {
        stop();
        wp.unregisterListener(localResultLisener);
        wp = null;
        isInited = false;
    }


    // 基于DEMO唤醒3.1 开始回调事件
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        // android studio日志Monitor 中搜索 WakeupEventAdapter即可看见下面一行的日志
        System.out.println("wakeup name:" + name + "; params:" + params);
        if (SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS.equals(name)) { // 识别唤醒词成功
            WakeUpResult result = WakeUpResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()) { // error不为0依旧有可能是异常情况
                System.out.println(errorCode +  " " +  result);
            } else {
                String word = result.getWord();
//                listener.onSuccess(word, result);
                System.out.println("Fish成功唤醒： 识别的唤醒词为：" + word);

                if(word.equalsIgnoreCase("上鱼上鱼")) {
                    // Network operations should not be done on the UI thread
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            parent_context.udpSend("fishcatching");
                        }
                    }).start();
                }else  if(word.equalsIgnoreCase("看漂看漂")) {
                    // Network operations should not be done on the UI thread
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            parent_context.udpSend("kanpiao");
                        }
                    }).start();
                }




            }
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_ERROR.equals(name)) { // 识别唤醒词报错
            WakeUpResult result = WakeUpResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()) {
                System.out.println(errorCode +  " " +  result);
            }
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED.equals(name)) { // 关闭唤醒词
            System.out.println("关闭唤醒词");
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_AUDIO.equals(name)) { // 音频回调
            //listener.onASrAudio(data, offset, length);
            System.out.println("音频回调");
        }
    }

}
