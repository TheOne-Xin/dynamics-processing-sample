Android 9.0新增了用于 DynamicsProcessing 的 AudioEffect API，使用此类，我们可以构建基于通道的音频效果，包括各种类型的多个阶段，包括均衡，多频段压缩和限制。频段和活动阶段的数量是可配置的，并且大多数参数可以实时控制，例如增益、启动/释放时间、阈值等。
# 1 基本原理
效果由通道实例化和控制。每个通道具有相同的基本架构，但它们的所有参数都独立于其他通道。
基本通道配置为：

```java

    Channel 0          Channel 1       ....       Channel N-1
      Input              Input                       Input
        |                  |                           |
   +----v----+        +----v----+                 +----v----+
   |inputGain|        |inputGain|                 |inputGain|
   +---------+        +---------+                 +---------+
        |                  |                           |
  +-----v-----+      +-----v-----+               +-----v-----+
  |   PreEQ   |      |   PreEQ   |               |   PreEQ   |
  +-----------+      +-----------+               +-----------+
        |                  |                           |
  +-----v-----+      +-----v-----+               +-----v-----+
  |    MBC    |      |    MBC    |               |    MBC    |
  +-----------+      +-----------+               +-----------+
        |                  |                           |
  +-----v-----+      +-----v-----+               +-----v-----+
  |  PostEQ   |      |  PostEQ   |               |  PostEQ   |
  +-----------+      +-----------+               +-----------+
        |                  |                           |
  +-----v-----+      +-----v-----+               +-----v-----+
  |  Limiter  |      |  Limiter  |               |  Limiter  |
  +-----------+      +-----------+               +-----------+
        |                  |                           |
     Output             Output                      Output
 
```
其中，
- inputGain：输入增益因子，以分贝 (dB) 为单位。0 dB 表示电平没有变化。
- PreEQ：多频段均衡器。
- MBC：多频段压缩器 。
- PostEQ：多频段均衡器。
- Limiter：单频段压缩器/限制器，通常用于保护信号免于过载和失真。 
## 1.1 均衡器
可以理解为单独控制每个频率的音量，调整各频段信号的增益量，衰减多余频率的同时塑造音色。
详细介绍：[EQ 均衡器介绍](https://blog.csdn.net/hello_1995/article/details/125287877)
## 1.2 压缩器
是用来控制电平的效果器，控制电平的同时会影响音色，不单单是人声，混音当中，所有的元素都可以需要压缩器。
常见参数：
- Thress hoid（阈值）:阈值表示声音经过压缩器是，多大的电平才能触发压缩器进行压缩工作，当声音超过你设定的阈值时他开始对音频进行压缩，声音低于阈值压缩器不工作。
- Ratio（压缩比）：压缩器表示当声音超过阈值压缩器开始工作时，压缩器会以多大的比例对声音进行压缩。
- Attack（启动时间）：表示声音超过阈值后需要多长时间才平稳到达使压缩器压缩到规定的压缩比。
- release（释放时间）：表示当压缩器已经开始工作后，当声音电平低于阈值时，压缩器要多长时间平稳的恢复到压缩前的电平（目前电平）。
## 1.3 限制器
用来控制峰值的效果器，限制信号不过载的同时提高响度。
常见参数：
- Thress hold （阈值）：限制器工作时的触发值。
- output（输出上限）：能被输出的最高电平。
- release（释放时间）：指限制器恢复到不限制所需要的时间。
- Dither（抖动）：作用是给声音添加一层白底燥。

应用程序创建一个 DynamicsProcessing 对象以在音频框架中实例化和控制此音频效果。如果需要，可以使用 DynamicsProcessor.Config 和 DynamicsProcessor.Config.Builder 来帮助配置多个阶段和每个频段参数。
如果在创建过程中未指定任何配置，则选择默认配置。
下面介绍 DynamicsProcessing 的使用方法。
# 2 初始化
在使用的时候需要先判断 **Build.VERSION.SDK_INT >= 28**。
## 2.1 创建 DynamicsProcessing 对象
要将 DynamicsProcessing 附加到特定的 AudioTrack 或 MediaPlayer，在实例化时指定此 AudioTrack 或 MediaPlayer 的音频会话 ID。

```java
MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.test_cbr/*音频路径*/);
int audioSessionId = mediaPlayer.getAudioSessionId();
DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                        0,//variant
                        1,//channelCount
                        true,//preEqInUse
                        10,//preEqBandCount
                        true,//mbcInUse
                        10,//mbcBandCount
                        true,//postEqInUse
                        10,//postEqBandCount
                        true//limiterInUse
);
DynamicsProcessing mDynamicsProcessing = new DynamicsProcessing(0, audioSessionId, builder.build());
mDynamicsProcessing.setEnabled(true);
```
## 2.2 创建 DynamicsProcessing.Eq （均衡器）对象

```java
//创建用于调节的10个频段
private static final int[] bandVal = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
private static final int maxBandCount = bandVal.length;

DynamicsProcessing.Eq mEq = new DynamicsProcessing.Eq(true, true, maxBandCount);
mEq.setEnabled(true);

for (int i = 0; i < maxBandCount; i++) {
    mEq.getBand(i).setCutoffFrequency(bandVal[i]);//设置此频段将处理的最高频率数（以 Hz 为单位）
}
//设置压缩前的均衡器给全频道
mDynamicsProcessing.setPreEqAllChannelsTo(mEq);
//设置压缩前的均衡器给指定频道
//public static final int CHANNEL_1 = 0;
//public static final int CHANNEL_2 = 1;
//mDynamicsProcessing.setPreEqByChannelIndex(CHANNEL_1, mEq);

//设置压缩后的均衡器给全频道
//mDynamicsProcessing.setPostEqAllChannelsTo(mEq);
//设置压缩后的均衡器给指定频道
//public static final int CHANNEL_1 = 0;
//public static final int CHANNEL_2 = 1;
//mDynamicsProcessing.setPostEqByChannelIndex(CHANNEL_1, mEq);
```
## 2.3 创建 DynamicsProcessing.Mbc（多频段压缩器）对象

```java
DynamicsProcessing.Mbc mDynamicsProcessingMbc = new DynamicsProcessing.Mbc(true, true, maxBandCount);
mDynamicsProcessingMbc.setEnabled(true);
for (int i = 0; i < maxBandCount; i++) {
    mDynamicsProcessingMbc.getBand(i).setCutoffFrequency(bandVal[i]);//设置此频段将处理的最高频率数（以 Hz 为单位）
}

//设置多频段压缩器给全频道
mDynamicsProcessing.setMbcAllChannelsTo(mDynamicsProcessingMbc);
//设置多频段压缩器给指定频道
//mDynamicsProcessing.getMbcBandByChannelIndex(CHANNEL_1, mDynamicsProcessingMbc);
```
## 2.4 创建 DynamicsProcessing.Limiter（限制器）对象

```java
//Limiter构造参数
private static final boolean LIMITER_DEFAULT_IN_USE = true;//如果将使用 MBC 阶段，则为 true，否则为 false。
private static final boolean LIMITER_DEFAULT_ENABLED = true;//如果启用/禁用 MBC 阶段，则为 true。这可以在效果运行时更改
private static final int LIMITER_DEFAULT_LINK_GROUP = 0;//分配给此限制器的组的索引。只有共享相同 linkGroup 索引的限制器才会一起做出反应。
private static final float LIMITER_DEFAULT_ATTACK_TIME = 1; // 限制器压缩器的启动时间，以毫秒 (ms) 为单位
private static final float LIMITER_DEFAULT_RELEASE_TIME = 60; //限制器压缩器的释放时间，以毫秒 (ms) 为单位
private static final float LIMITER_DEFAULT_RATIO = 10; // 限制器压缩比 （N:1）（输入：输出）
private static final float LIMITER_DEFAULT_THRESHOLD = -2; // 限幅压缩器阈值以分贝 (dB) 为单位，从 0 dB 满量程 (dBFS) 开始测量。
private static final float LIMITER_DEFAULT_POST_GAIN = 0; // 压缩后应用于信号的增益。

DynamicsProcessing.Limiter mDynamicsProcessingLimiter = new DynamicsProcessing.Limiter(LIMITER_DEFAULT_IN_USE, LIMITER_DEFAULT_ENABLED, LIMITER_DEFAULT_LINK_GROUP, LIMITER_DEFAULT_ATTACK_TIME, LIMITER_DEFAULT_RELEASE_TIME, LIMITER_DEFAULT_RATIO, LIMITER_DEFAULT_THRESHOLD, LIMITER_DEFAULT_POST_GAIN);
mDynamicsProcessingLimiter.setEnabled(true);

//设置限制器给全频道
mDynamicsProcessing.setLimiterAllChannelsTo(mDynamicsProcessingLimiter);
//设置限制器给指定频道
//mDynamicsProcessing.setLimiterByChannelIndex(CHANNEL_1, mDynamicsProcessingLimiter);
```
# 3 调节频段
## 3.1 调节输入增益

```java
mDynamicsProcessing.setInputGainAllChannelsTo(value);
```
## 3.2 调节均衡器

```java
//根据调节的频段在bandVal数组中的索引bandIndex和调整后的值gain来调节均衡器
mEq.getBand(bandIndex).setGain(gain);
//设置压缩前的均衡器频段增益给全频道
mDynamicsProcessing.setPreEqBandAllChannelsTo(bandIndex, mEq.getBand(bandIndex));
//设置压缩前的均衡器频段增益给指定频道
//mDynamicsProcessing.setPreEqBandByChannelIndex(CHANNEL_1, bandIndex, mEq.getBand(bandIndex));

//设置压缩后的均衡器频段增益给全频道
//mDynamicsProcessing.setPostEqBandAllChannelsTo(bandIndex, mEq.getBand(bandIndex));
//设置压缩后的均衡器频段增益给指定频道
//mDynamicsProcessing.setPostEqBandByChannelIndex(CHANNEL_1, bandIndex, mEq.getBand(bandIndex));
```
## 3.3 调节多频段压缩器

```java
//设置在压缩之前应用于信号的增益，以分贝 (dB) 为单位测量，其中 0 dB 表示没有电平变化。
mDynamicsProcessingMbc.getBand(bandIndex).setPreGain(gain);
//设置给全频道
mDynamicsProcessing.setMbcBandAllChannelsTo(bandIndex, mDynamicsProcessingMbc.getBand(bandIndex));
//设置给指定频道
//mDynamicsProcessing.setMbcBandByChannelIndex(CHANNEL_1, bandIndex, mDynamicsProcessingMbc.getBand(bandIndex));

//设置在压缩之后应用于信号的增益，以分贝 (dB) 为单位测量，其中 0 dB 表示没有电平变化。
//mDynamicsProcessingMbc.getBand(bandIndex).setPostGain(gain);
//设置给全频道
//mDynamicsProcessing.setMbcBandAllChannelsTo(bandIndex, mDynamicsProcessingMbc.getBand(bandIndex));
//设置给指定频道
//mDynamicsProcessing.setMbcBandByChannelIndex(CHANNEL_1, bandIndex, mDynamicsProcessingMbc.getBand(bandIndex));
```
# 4 销毁

```java
if (mMediaPlayer != null) {
    mMediaPlayer.pause();
    mMediaPlayer.release();
}
if (mDynamicsProcessing != null) {
    mDynamicsProcessing.setEnabled(false);
    mDynamicsProcessing.release();
    mDynamicsProcessing = null;
}
```
