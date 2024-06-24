1. 本项目的code base： 是百度开放平台下载(https://ai.baidu.com/sdk)下来的语音安卓SDK：
       baidu_speech_ASR_V3_20210628_cfe8c44_3.4.4.zip (展开后，对比当前的工程代码，就知道我增加/修改的部分了)
2. 本地展开后，用android IDE 加载这个目录， 然后就可以build 成功。
3. IDE 连接安卓真机就可以运行出来 sdk 的demo。
    3.1  https://ai.baidu.com/ ， 注册账号  153。。2863
    3.2  控制台， 产品list， 找到语音技术， 进入语音技术主页， 就可以创建应用， 得到4个东西
        3.2.1 appid
        3.2.2 ak
        3.2.3 sk
        3.2.4 安卓语音包的名称： 这地方务必填写你的安卓代码里的app的 applicationId which is defined in build.gradle
    3.3 修改代码： 把appid, ak, sk 修改到core 模块下的AuthUtil.java 里
4. 去 https://ai.baidu.com/tech/speech/wake，  输入几个要的唤醒词，（多个）， 然后下载到本地： WakeUp.bin （算法参数）
5. 把下载到的 WakeUp.bin， 放置到core模块下的assets 目录下（覆盖）
6. 真机运行这个demo，第一次要联网， SDK会下载一个授权文件到项目中 *.bsg 文件
7. 到此这个安卓app，唤醒功能就OK了 （可以离线工作）
8. 把之前的zoominPiao App 里的摄像头preview， 后台Service 编码器和TCP 发送的代码移植进来。  参考：App模块的 FishCameraService
9. 优化demo里的wakeup SDK 使用的代码， 改为简单的： FsihWakeup
10. 这个app 起名字： 为kanpiao，  唤醒词1： 看漂看漂（识别后， UDP发送"kanpiao" 给server， server 会切换画面），
唤醒词2： 上鱼上鱼， （识别后， UDP发送"fishcatching" 给server， server 会切换画面），