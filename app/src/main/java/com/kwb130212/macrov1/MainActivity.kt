package com.kwb130212.macrov1

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
 private val points=mutableListOf<TapPoint>()
 private val main=Handler(Looper.getMainLooper())

 override fun onCreate(b:Bundle?){super.onCreate(b);ui();load();watchCapture()}
 override fun onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy()}

 private fun ui(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,20,24,20);setBackgroundColor(Color.rgb(15,15,20))}
  fun t(s:String,z:Float=16f)=TextView(this).apply{text=s;textSize=z;setTextColor(Color.WHITE);setPadding(0,8,0,8)}
  root.addView(t("매크로v1.2",28f));root.addView(t("백지헌의 캣히어로 매크로",18f));status=t("● 대기",14f);root.addView(status)
  pkg=EditText(this).apply{hint="대상 게임 패키지";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)}
  interval=EditText(this).apply{hint="기본 간격(ms) — 최소 20";inputType=2;setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)}
  webhook=EditText(this).apply{hint="웹훅 URL (선택, HTTPS만)";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)}
  root.addView(pkg);root.addView(interval);root.addView(webhook)
  webhookStatus=t("웹훅: 사용 안 함 · 로컬 실행 가능",13f);root.addView(webhookStatus)
  root.addView(Button(this).apply{text="웹훅 고급 설정";setOnClickListener{webhookDialog()}})
  root.addView(Button(this).apply{text="웹훅 테스트";setOnClickListener{save();MacroAccessibilityService.instance?.testWebhook()?:toast("접근성 서비스를 먼저 켜세요.")}})
  root.addView(Button(this).apply{text="좌표 직접 입력";setOnClickListener{addPointDialog()}})
  root.addView(Button(this).apply{text="터치로 좌표 설정";setOnClickListener{capturePoint()}})
  root.addView(Button(this).apply{text="전체 삭제";setOnClickListener{points.clear();refresh()}})
  box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(box)
  root.addView(Button(this).apply{text="접근성 권한 열기";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
  root.addView(Button(this).apply{text="설정 저장";setOnClickListener{save();toast("저장됨")}})
  root.addView(Button(this).apply{text="매크로 시작";setOnClickListener{save();if(points.isEmpty()){toast("좌표를 1개 이상 추가하세요.");return@setOnClickListener};MacroAccessibilityService.instance?.let{it.startMacro();status()}?:run{toast("접근성 서비스를 먼저 켜세요.");startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}})
  root.addView(Button(this).apply{text="매크로 정지";setOnClickListener{MacroAccessibilityService.instance?.stopMacro();status()}})
  setContentView(ScrollView(this).apply{addView(root)});updateWebhookStatus()
 }

 private fun webhookDialog(){
  val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,8,32,8)}
  val events=CheckBox(this).apply{text="시작/정지 이벤트";isChecked=getSharedPreferences("macro",0).getBoolean("webhookEvents",true)}
  val errors=CheckBox(this).apply{text="오류 이벤트";isChecked=getSharedPreferences("macro",0).getBoolean("webhookErrors",true)}
  val ticks=CheckBox(this).apply{text="100회마다 진행 이벤트";isChecked=getSharedPreferences("macro",0).getBoolean("webhookTicks",false)}
  l.addView(events);l.addView(errors);l.addView(ticks)
  AlertDialog.Builder(this).setTitle("웹훅 고급 설정").setMessage("URL이 비어 있으면 네트워크 통신 없이 로컬로만 실행됩니다. HTTPS만 허용하고 전송 실패는 매크로 실행을 막지 않습니다.").setView(l).setPositiveButton("저장"){_,_->getSharedPreferences("macro",0).edit().putBoolean("webhookEvents",events.isChecked).putBoolean("webhookErrors",errors.isChecked).putBoolean("webhookTicks",ticks.isChecked).apply();updateWebhookStatus()}.setNegativeButton("취소",null).show()
 }

 private fun updateWebhookStatus(){val u=getSharedPreferences("macro",0).getString("webhook","").orEmpty();webhookStatus.text=if(u.startsWith("https://"))"웹훅: HTTPS 연결 설정됨 · 실패해도 로컬 실행 유지" else "웹훅: 사용 안 함 · 로컬 실행 가능"}

 private fun addPointDialog(){
  val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,8,32,8)}
  val x=EditText(this).apply{hint="X";inputType=2;setSingleLine(true)};val y=EditText(this).apply{hint="Y";inputType=2;setSingleLine(true)};val d=EditText(this).apply{hint="이후 대기(ms), 비우면 기본값";inputType=2;setSingleLine(true)}
  l.addView(x);l.addView(y);l.addView(d)
  AlertDialog.Builder(this).setTitle("좌표 직접 입력").setView(l).setPositiveButton("추가"){_,_->addValidated(x.text.toString().toIntOrNull(),y.text.toString().toIntOrNull(),d.text.toString().toLongOrNull())}.setNegativeButton("취소",null).show()
 }

 private fun capturePoint(){
  val s=MacroAccessibilityService.instance
  if(s==null){toast("접근성 서비스를 먼저 켜세요.");startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return}
  save();s.beginCoordinateCapture();toast("게임 화면에서 원하는 위치를 터치하세요.")
 }

 private fun watchCapture(){main.postDelayed({val p=getSharedPreferences("macro",0);if(p.getBoolean("captureReady",false)){val x=p.getInt("captureX",-1);val y=p.getInt("captureY",-1);p.edit().putBoolean("captureReady",false).apply();if(x>=0&&y>=0)delayDialog(x,y)};if(!isFinishing)watchCapture()},100)}
 private fun delayDialog(x:Int,y:Int){val d=EditText(this).apply{hint="이후 대기(ms), 비우면 기본값";inputType=2;setSingleLine(true)};AlertDialog.Builder(this).setTitle("터치 좌표 확인").setMessage("화면 좌표: ($x, $y)").setView(d).setPositiveButton("추가"){_,_->addValidated(x,y,d.text.toString().toLongOrNull())}.setNegativeButton("취소",null).show()}
 private fun addValidated(x:Int?,y:Int?,delay:Long?){if(x==null||y==null||x<0||y<0){toast("좌표가 올바르지 않습니다.");return};val base=interval.text.toString().toLongOrNull()?.coerceAtLeast(20L)?:100L;points.add(TapPoint(x,y,(delay?:base).coerceAtLeast(20L)));refresh()}
 private fun refresh(){box.removeAllViews();points.forEachIndexed{i,p->box.addView(TextView(this).apply{text=String.format(Locale.US,"%02d. (%d,%d) → %dms",i+1,p.x,p.y,p.delayMs);textSize=15f;setTextColor(Color.WHITE);setPadding(0,10,0,10);setOnClickListener{editPoint(i)};setOnLongClickListener{points.removeAt(i);refresh();true}})}}
 private fun editPoint(i:Int){
  val p=points[i];val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,8,32,8)}
  val x=EditText(this).apply{setText(p.x.toString());inputType=2;setSingleLine(true)};val y=EditText(this).apply{setText(p.y.toString());inputType=2;setSingleLine(true)};val d=EditText(this).apply{setText(p.delayMs.toString());inputType=2;setSingleLine(true)}
  l.addView(x);l.addView(y);l.addView(d)
  val dlg=AlertDialog.Builder(this).setTitle("좌표 편집").setView(l).setPositiveButton("저장"){_,_->val nx=x.text.toString().toIntOrNull();val ny=y.text.toString().toIntOrNull();val nd=d.text.toString().toLongOrNull();if(nx==null||ny==null||nd==null||nx<0||ny<0||nd<20)toast("값이 올바르지 않습니다.")else{points[i]=TapPoint(nx,ny,nd);refresh()}}.setNegativeButton("취소",null).create()
  dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener{}};dlg.setButton(AlertDialog.BUTTON_NEUTRAL,"1회 테스트"){_,_->MacroAccessibilityService.instance?.testPoint(p)?:toast("접근성 서비스를 먼저 켜세요.")};dlg.show()
 }

 private fun save(){getSharedPreferences("macro",0).edit().putString("targetPackage",pkg.text.toString().trim()).putLong("interval",interval.text.toString().toLongOrNull()?.coerceAtLeast(20L)?:100L).putString("webhook",webhook.text.toString().trim()).putString("points",points.joinToString(";"){p->p.x.toString()+","+p.y+","+p.delayMs}).apply();updateWebhookStatus()}
 private fun load(){val p=getSharedPreferences("macro",0);pkg.setText(p.getString("targetPackage",""));interval.setText(p.getLong("interval",100L).toString());webhook.setText(p.getString("webhook",""));points.clear();p.getString("points","").orEmpty().split(";").forEach{a->val q=a.split(",");if(q.size==3){val x=q[0].toIntOrNull();val y=q[1].toIntOrNull();val d=q[2].toLongOrNull();if(x!=null&&y!=null&&d!=null&&x>=0&&y>=0&&d>=20)points.add(TapPoint(x,y,d))}};refresh();updateWebhookStatus()}
 private fun status(){status.text=if(MacroAccessibilityService.instance?.running==true)"● 실행 중" else "● 대기"}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
