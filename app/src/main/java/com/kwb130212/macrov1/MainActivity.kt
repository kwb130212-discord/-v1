package com.kwb130212.macrov1

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import java.util.Locale

data class TapPoint(val x:Int,val y:Int,val delayMs:Long)

class MainActivity:Activity(){
 private lateinit var box:LinearLayout
 private lateinit var status:TextView
 private lateinit var webhookStatus:TextView
 private lateinit var pkg:EditText
 private lateinit var interval:EditText
 private lateinit var webhook:EditText
 private lateinit var web:WebView
 private lateinit var progress:ProgressBar
 private lateinit var loadText:TextView
 private val points=mutableListOf<TapPoint>()
 private val main=Handler(Looper.getMainLooper())
 private var loadTimeout=false
 private var customView:View?=null
 private var customCallback:WebChromeClient.CustomViewCallback?=null
 private var immersive=false

 override fun onCreate(b:Bundle?){super.onCreate(b);ui();load();watchCapture()}
 override fun onDestroy(){exitFullscreen();main.removeCallbacksAndMessages(null);web.stopLoading();web.destroy();super.onDestroy()}

 private fun ui(){
  window.statusBarColor=Color.rgb(9,10,13);window.navigationBarColor=Color.rgb(9,10,13)
  fun bg(color:Int,r:Float=18f)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
  fun text(s:String,size:Float,color:Int=Color.WHITE,bold:Boolean=false)=TextView(this).apply{
   this.text=s;textSize=size;setTextColor(color);setPadding(0,dp(2),0,dp(2));if(bold)typeface=Typeface.DEFAULT_BOLD
  }
  fun button(s:String,action:()->Unit)=Button(this).apply{
   this.text=s;isAllCaps=false;textSize=14f;setTextColor(Color.rgb(239,242,247));typeface=Typeface.DEFAULT_BOLD
   background=bg(Color.rgb(30,33,42),14f);setPadding(dp(12),0,dp(12),0);minHeight=dp(48);stateListAnimator=null
   setOnClickListener{action()}
  }
  fun section(s:String)=text(s,12f,Color.rgb(128,136,153),true).apply{setPadding(dp(2),dp(18),0,dp(8))}
  fun card():LinearLayout=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=bg(Color.rgb(18,20,27),18f)
   layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)}
  }
  val scroll=ScrollView(this).apply{setBackgroundColor(Color.rgb(9,10,13));isFillViewport=true}
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(28))}
  val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  brand.addView(text("MACRO V1",25f,Color.WHITE,true))
  brand.addView(text("빠르고 단순하게 · 필요한 것만",12f,Color.rgb(133,140,155)))
  head.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
  status=text("● 대기",12f,Color.rgb(148,157,174),true).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(8),dp(12),dp(8));background=bg(Color.rgb(28,31,40),30f)}
  head.addView(status)
  root.addView(head);root.addView(text("좌표 기반 자동화 도구",13f,Color.rgb(111,119,136)).apply{setPadding(0,dp(2),0,dp(12))})

  root.addView(section("게임"))
  val game=card()
  val gameTitle=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  gameTitle.addView(text("Cat Hero",19f,Color.WHITE,true))
  gameTitle.addView(text("게임 화면을 여기서 열고 바로 설정할 수 있습니다.",12f,Color.rgb(137,144,158)))
  game.addView(gameTitle)
  web=WebView(this).apply{
   settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.databaseEnabled=true
   settings.cacheMode=WebSettings.LOAD_DEFAULT;settings.mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
   settings.userAgentString=settings.userAgentString+" MacroV1/1.3"
   webViewClient=object:WebViewClient(){
    override fun onPageStarted(v:WebView?,url:String?,favicon:android.graphics.Bitmap?){super.onPageStarted(v,url,favicon);showLoading("페이지 불러오는 중…");loadTimeout=false;main.removeCallbacksAndMessages("web_timeout");main.postAtTime({if(!isFinishing&&web.progress<95){loadTimeout=true;showLoading("응답이 지연되고 있습니다.");toast("페이지 응답이 오래 걸립니다. 새로고침을 눌러주세요.")}},"web_timeout",18000)}
    override fun onPageFinished(v:WebView?,url:String?){super.onPageFinished(v,url);main.removeCallbacksAndMessages("web_timeout");loadTimeout=false;hideLoading()}
    override fun onReceivedError(v:WebView?,request:WebResourceRequest?,error:WebResourceError?){super.onReceivedError(v,request,error);if(request?.isForMainFrame==true){main.removeCallbacksAndMessages("web_timeout");showLoading("페이지를 불러오지 못했습니다.")}}
   }
   setOnTouchListener(object:View.OnTouchListener{
    private val detector=GestureDetector(this@MainActivity,object:GestureDetector.SimpleOnGestureListener(){
     override fun onDoubleTap(e:MotionEvent):Boolean{toggleImmersive();return true}
    })
    override fun onTouch(v:View?,event:MotionEvent?):Boolean{if(event!=null)detector.onTouchEvent(event);return false}
   })
   webChromeClient=object:WebChromeClient(){
    override fun onProgressChanged(v:WebView?,newProgress:Int){super.onProgressChanged(v,newProgress);this@MainActivity.progress.progress=newProgress;if(newProgress>=95&&!loadTimeout)hideLoading()}
    override fun onShowCustomView(view:View?,callback:CustomViewCallback?){if(view==null)return;if(customView!=null){callback?.onCustomViewHidden();return};customView=view;customCallback=callback;(web.parent as? ViewGroup)?.removeView(web);addContentView(view,ViewGroup.LayoutParams(-1,-1));enterImmersive()}
    override fun onHideCustomView(){exitFullscreen()}
   }
   layoutParams=LinearLayout.LayoutParams(-1,dp(330)).apply{topMargin=dp(12)}
  }
  game.addView(web)
  val loading=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(4))}
  progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=0}
  loadText=text("준비 중…",11f,Color.rgb(146,153,168));loading.addView(progress,LinearLayout.LayoutParams(0,dp(4),1f));loading.addView(loadText,LinearLayout.LayoutParams(dp(125),-2).apply{leftMargin=dp(10)})
  game.addView(loading);loading.visibility=View.GONE
  val gameRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  gameRow.addView(button("게임 열기"){openCatHero()},LinearLayout.LayoutParams(0,dp(48),1f))
  gameRow.addView(Space(this),LinearLayout.LayoutParams(dp(8),1))
  gameRow.addView(button("새로고침"){web.reload()},LinearLayout.LayoutParams(0,dp(48),1f));game.addView(gameRow)
  game.addView(button("전체화면으로 보기"){enterImmersive()}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(8)}})
  root.addView(game)

  root.addView(section("자동화 설정"))
  val config=card()
  config.addView(text("실행 대상",16f,Color.WHITE,true))
  pkg=field("대상 패키지");config.addView(pkg)
  interval=field("기본 간격(ms) · 1ms까지").apply{inputType=2};config.addView(interval)
  config.addView(text("웹훅 주소 · 선택사항",13f,Color.rgb(153,160,173)).apply{setPadding(0,dp(8),0,dp(4))})
  webhook=field("HTTPS 주소를 입력하세요");config.addView(webhook)
  webhookStatus=text("",11f,Color.rgb(137,144,158));config.addView(webhookStatus)
  root.addView(config)

  root.addView(section("좌표 목록"))
  val pointsCard=card()
  pointsCard.addView(text("터치 순서를 그대로 재생합니다.",12f,Color.rgb(137,144,158)))
  val pointButtons=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  pointButtons.addView(button("＋ 좌표 추가"){addPointDialog()},LinearLayout.LayoutParams(0,dp(46),1f))
  pointButtons.addView(Space(this),LinearLayout.LayoutParams(dp(7),1))
  pointButtons.addView(button("화면에서 선택"){capturePoint()},LinearLayout.LayoutParams(0,dp(46),1f))
  pointsCard.addView(pointButtons)
  pointsCard.addView(button("모든 좌표 지우기"){points.clear();refresh()}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(7)}})
  box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(8),0,0)};pointsCard.addView(box)
  root.addView(pointsCard)

  root.addView(section("실행"))
  val control=card()
  val runRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  val start=button("▶  시작"){save();if(points.isEmpty()){toast("좌표를 1개 이상 추가하세요.");return@button};MacroAccessibilityService.instance?.let{it.startMacro();status()}?:run{toast("접근성 서비스를 먼저 켜세요.");startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}
  start.background=bg(Color.rgb(46,76,59),14f)
  val stop=button("■  정지"){MacroAccessibilityService.instance?.stopMacro();status()}
  stop.background=bg(Color.rgb(65,40,43),14f)
  runRow.addView(start,LinearLayout.LayoutParams(0,dp(52),1f));runRow.addView(Space(this),LinearLayout.LayoutParams(dp(8),1));runRow.addView(stop,LinearLayout.LayoutParams(0,dp(52),1f));control.addView(runRow)
  control.addView(button("접근성 설정 열기"){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(8)}})
  control.addView(button("설정 저장"){save();toast("설정을 저장했습니다.")}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(7)}})
  root.addView(control)

  root.addView(section("웹훅"))
  val hook=card()
  hook.addView(text("실행 기록을 외부 서버로 보낼 수 있습니다.",12f,Color.rgb(137,144,158)))
  hook.addView(button("웹훅 세부 설정"){webhookDialog()}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(8)}})
  hook.addView(button("연결 테스트"){save();MacroAccessibilityService.instance?.testWebhook()?:toast("접근성 서비스를 먼저 켜세요.")}.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(7)}})
  root.addView(hook)

  root.addView(text("Macro V1  ·  설정은 이 기기에 저장됩니다.",11f,Color.rgb(82,89,104)).apply{gravity=Gravity.CENTER;setPadding(0,dp(12),0,0)})
  scroll.addView(root);setContentView(scroll);updateWebhookStatus()
 } private fun field(h:String)=EditText(this).apply{hint=h;setTextColor(Color.WHITE);setHintTextColor(Color.rgb(100,108,122));setSingleLine(true);textSize=14f;setPadding(dp(12),0,dp(12),0);background=GradientDrawable().apply{setColor(Color.rgb(24,27,35));cornerRadius=dp(12f).toFloat()};layoutParams=LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(7)}}
 private fun openCatHero(){pkg.setText(packageName);save();showLoading("Cat Hero 연결 중…");web.loadUrl("https://cathero.gv.gameduo.net/mobile/index.html")}
 private fun toggleImmersive(){if(immersive)exitImmersive()else enterImmersive()}
 private fun enterImmersive(){immersive=true;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE}
 private fun exitImmersive(){immersive=false;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_STABLE}
 private fun exitFullscreen(){customCallback?.onCustomViewHidden();customCallback=null;customView?.let{v->(v.parent as? ViewGroup)?.removeView(v)};customView=null;exitImmersive()}
 override fun onBackPressed(){if(customView!=null){exitFullscreen();return};if(immersive){exitImmersive();return};if(web.canGoBack()){web.goBack();return};super.onBackPressed()}
 private fun showLoading(s:String){if(!::loadText.isInitialized)return;loadText.text=s;progress.visibility=View.VISIBLE;loadText.visibility=View.VISIBLE}
 private fun hideLoading(){if(!::loadText.isInitialized)return;progress.visibility=View.GONE;loadText.text="연결됨"}
 private fun webhookDialog(){
  val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(28),dp(8),dp(28),dp(8))};val p=getSharedPreferences("macro",0)
  val events=CheckBox(this).apply{text="시작/정지 이벤트";isChecked=p.getBoolean("webhookEvents",true)}
  val errors=CheckBox(this).apply{text="오류 이벤트";isChecked=p.getBoolean("webhookErrors",true)}
  val ticks=CheckBox(this).apply{text="100회마다 진행 이벤트";isChecked=p.getBoolean("webhookTicks",false)}
  l.addView(events);l.addView(errors);l.addView(ticks)
  AlertDialog.Builder(this).setTitle("웹훅 설정").setMessage("HTTPS만 사용합니다. 전송 실패가 매크로 실행을 막지는 않습니다.").setView(l).setPositiveButton("저장"){_,_->p.edit().putBoolean("webhookEvents",events.isChecked).putBoolean("webhookErrors",errors.isChecked).putBoolean("webhookTicks",ticks.isChecked).apply();updateWebhookStatus()}.setNegativeButton("취소",null).show()
 }
 private fun updateWebhookStatus(){if(!::webhookStatus.isInitialized)return;val u=getSharedPreferences("macro",0).getString("webhook","").orEmpty();webhookStatus.text=if(u.startsWith("https://"))"웹훅 활성 · 실패 시에도 로컬 매크로 유지" else "웹훅 비활성 · 로컬 실행"}
 private fun addPointDialog(){
  val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(28),dp(8),dp(28),dp(8))}
  val x=fieldInDialog("X");val y=fieldInDialog("Y");val d=fieldInDialog("이후 대기(ms)");l.addView(x);l.addView(y);l.addView(d)
  AlertDialog.Builder(this).setTitle("좌표 추가").setView(l).setPositiveButton("추가"){_,_->addValidated(x.text.toString().toIntOrNull(),y.text.toString().toIntOrNull(),d.text.toString().toLongOrNull())}.setNegativeButton("취소",null).show()
 }
 private fun capturePoint(){val s=MacroAccessibilityService.instance;if(s==null){toast("접근성 서비스를 먼저 켜세요.");startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return};save();s.beginCoordinateCapture();toast("게임 화면에서 원하는 위치를 터치하세요.")}
 private fun watchCapture(){main.postDelayed({val p=getSharedPreferences("macro",0);if(p.getBoolean("captureReady",false)){val x=p.getInt("captureX",-1);val y=p.getInt("captureY",-1);p.edit().putBoolean("captureReady",false).apply();if(x>=0&&y>=0)delayDialog(x,y)};if(!isFinishing)watchCapture()},100)}
 private fun delayDialog(x:Int,y:Int){val d=fieldInDialog("이후 대기(ms)");AlertDialog.Builder(this).setTitle("터치 좌표 확인").setMessage("화면 좌표: ($x, $y)").setView(d).setPositiveButton("추가"){_,_->addValidated(x,y,d.text.toString().toLongOrNull())}.setNegativeButton("취소",null).show()}
 private fun fieldInDialog(h:String)=EditText(this).apply{hint=h;inputType=2;setSingleLine(true)}
 private fun addValidated(x:Int?,y:Int?,delay:Long?){if(x==null||y==null||x<0||y<0){toast("좌표가 올바르지 않습니다.");return};val base=interval.text.toString().toLongOrNull()?.coerceAtLeast(1L)?:10L;points.add(TapPoint(x,y,(delay?:base).coerceAtLeast(1L)));refresh()}
 private fun refresh(){box.removeAllViews();points.forEachIndexed{i,p->box.addView(TextView(this).apply{text=String.format(Locale.US,"%02d  (%d, %d)   %d ms",i+1,p.x,p.y,p.delayMs);textSize=14f;setTextColor(Color.WHITE);setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(Color.rgb(23,25,32));setOnClickListener{editPoint(i)};setOnLongClickListener{points.removeAt(i);refresh();true}})}}
 private fun editPoint(i:Int){
  val p=points[i];val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(28),dp(8),dp(28),dp(8))}
  val x=fieldInDialog("X").apply{setText(p.x.toString())};val y=fieldInDialog("Y").apply{setText(p.y.toString())};val d=fieldInDialog("대기(ms)").apply{setText(p.delayMs.toString())};l.addView(x);l.addView(y);l.addView(d)
  AlertDialog.Builder(this).setTitle("좌표 편집").setView(l).setPositiveButton("저장"){_,_->val nx=x.text.toString().toIntOrNull();val ny=y.text.toString().toIntOrNull();val nd=d.text.toString().toLongOrNull();if(nx==null||ny==null||nd==null||nx<0||ny<0||nd<20)toast("값이 올바르지 않습니다.")else{points[i]=TapPoint(nx,ny,nd);refresh()}}.setNegativeButton("취소",null).setNeutralButton("1회 테스트"){_,_->MacroAccessibilityService.instance?.testPoint(p)?:toast("접근성 서비스를 먼저 켜세요.")}.show()
 }
 private fun save(){getSharedPreferences("macro",0).edit().putString("targetPackage",pkg.text.toString().trim()).putLong("interval",interval.text.toString().toLongOrNull()?.coerceAtLeast(20L)?:100L).putString("webhook",webhook.text.toString().trim()).putString("points",points.joinToString(";"){p -> "${p.x},${p.y},${p.delayMs}"}).apply();updateWebhookStatus()}
 private fun load(){val p=getSharedPreferences("macro",0);pkg.setText(p.getString("targetPackage",""));interval.setText(p.getLong("interval",100L).toString());webhook.setText(p.getString("webhook",""));points.clear();p.getString("points","").orEmpty().split(";").forEach{a->val q=a.split(",");if(q.size==3){val x=q[0].toIntOrNull();val y=q[1].toIntOrNull();val d=q[2].toLongOrNull();if(x!=null&&y!=null&&d!=null&&x>=0&&y>=0&&d>=1)points.add(TapPoint(x,y,d))}};refresh();updateWebhookStatus()}
 private fun status(){status.text=if(MacroAccessibilityService.instance?.running==true)"● 실행 중" else "● 대기"}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun dp(v:Float)=(v*resources.displayMetrics.density).toInt()
}