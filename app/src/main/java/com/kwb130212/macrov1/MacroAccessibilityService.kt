package com.kwb130212.macrov1

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.view.accessibility.AccessibilityEvent
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MacroAccessibilityService:AccessibilityService(){
 companion object{@Volatile var instance:MacroAccessibilityService?=null}
 @Volatile var running=false;private set
 private val main=Handler(Looper.getMainLooper());private val network=Executors.newSingleThreadExecutor()
 private var index=0;private var overlay:TextView?=null;private var capture:FrameLayout?=null;private var targetPackage="";private var webhook="";private var points=emptyList<TapPoint>();private var total=0
 override fun onServiceConnected(){super.onServiceConnected();instance=this;loadConfig();showIndicator()}
 override fun onAccessibilityEvent(e:AccessibilityEvent?){if(e?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&running&&targetPackage.isNotBlank()&&e.packageName?.toString()!=targetPackage)stopMacro()}
 override fun onInterrupt()=stopMacro()
 fun startMacro(){loadConfig();if(points.isEmpty()||targetPackage.isBlank()){notifyWebhook("macro_start_rejected");return};running=true;index=0;total=0;updateIndicator(true);notifyWebhook("macro_started");scheduleNext(0)}
 fun stopMacro(){if(!running)return;running=false;main.removeCallbacksAndMessages(null);updateIndicator(false);notifyWebhook("macro_stopped")}
 fun testPoint(p:TapPoint){if(capture!=null||running)return;tap(p)}
 fun beginCoordinateCapture(){
  if(capture!=null)return
  val v=FrameLayout(this).apply{setBackgroundColor(Color.argb(40,0,0,0));isClickable=true
   addView(TextView(this@MacroAccessibilityService).apply{text="원하는 위치를 터치하세요";textSize=16f;setTextColor(Color.WHITE);setBackgroundColor(Color.argb(190,0,0,0));setPadding(24,18,24,18)})
   setOnTouchListener{_,e->if(e.action==MotionEvent.ACTION_UP){val x=e.rawX.toInt();val y=e.rawY.toInt();getSharedPreferences("macro",0).edit().putInt("captureX",x).putInt("captureY",y).putBoolean("captureReady",true).apply();removeCapture()};true}
  }
  val lp=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT)
  try{getSystemService(WindowManager::class.java)?.addView(v,lp);capture=v}catch(_:Exception){}
 }
 private fun removeCapture(){capture?.let{try{getSystemService(WindowManager::class.java)?.removeView(it)}catch(_:Exception){}};capture=null}
 private fun scheduleNext(delay:Long){if(!running)return;main.postDelayed({if(!running||points.isEmpty())return@postDelayed;val p=points[index%points.size];index++;tap(p)},delay.coerceAtLeast(0))}
 private fun tap(p:TapPoint){val path=Path().apply{moveTo(p.x.toFloat(),p.y.toFloat())};val stroke=GestureDescription.StrokeDescription(path,0,35);val ok=try{dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(),null,main)}catch(_:RuntimeException){false};if(!ok){running=false;updateIndicator(false);notifyWebhook("gesture_dispatch_failed");return};total++;if(running)scheduleNext(p.delayMs.coerceAtLeast(20))}
 private fun loadConfig(){val p=getSharedPreferences("macro",Context.MODE_PRIVATE);targetPackage=p.getString("targetPackage","")?.trim().orEmpty();webhook=p.getString("webhook","")?.trim().orEmpty();points=p.getString("points","").orEmpty().split(";").mapNotNull{a->val q=a.split(",");if(q.size!=3)return@mapNotNull null;val x=q[0].toIntOrNull()?:return@mapNotNull null;val y=q[1].toIntOrNull()?:return@mapNotNull null;val d=q[2].toLongOrNull()?:return@mapNotNull null;if(x<0||y<0||d<20)null else TapPoint(x,y,d)}}
 private fun showIndicator(){if(overlay!=null)return;val v=TextView(this).apply{text="Ⅱ";textSize=12f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(Color.argb(180,100,100,100));contentDescription="MacroV1 실행 상태"};val size=(24*resources.displayMetrics.density).toInt();val lp=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=4;y=4};try{getSystemService(WindowManager::class.java)?.addView(v,lp);overlay=v}catch(_:Exception){overlay=null}}
 private fun updateIndicator(a:Boolean){main.post{overlay?.apply{text=if(a)"▶" else "Ⅱ";setBackgroundColor(if(a)Color.argb(220,70,180,100)else Color.argb(180,100,100,100))}}}
 private fun notifyWebhook(e:String){val u=webhook;if(u.isBlank()||!u.startsWith("https://"))return;network.execute{try{val c=(URL(u).openConnection()as HttpURLConnection).apply{requestMethod="POST";connectTimeout=3000;readTimeout=3000;doOutput=true;setRequestProperty("Content-Type","application/json; charset=utf-8")};c.outputStream.use{it.write("""{"event":"$e","app":"MacroV1","version":"1.1","count":$total}""".toByteArray(StandardCharsets.UTF_8))};try{c.inputStream.close()}catch(_:Exception){};c.disconnect()}catch(_:Exception){}}}
 override fun onDestroy(){stopMacro();removeCapture();try{overlay?.let{getSystemService(WindowManager::class.java)?.removeView(it)}}catch(_:Exception){};overlay=null;network.shutdownNow();instance=null;super.onDestroy()}
}