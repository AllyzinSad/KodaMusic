package com.koda.anime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.*;
import android.view.*;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicBoolean;

final class GoogleLink {
    static void show(Activity activity, Runnable linked) {
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(1);panel.setPadding(34,24,34,20);
        TextView hint=new TextView(activity);hint.setText("Para testar, crie um cliente OAuth do tipo 'TVs e dispositivos com entrada limitada' no Google Cloud. O segredo é usado apenas nesta sessão.");hint.setTextColor(-1);hint.setTextSize(16);panel.addView(hint);
        EditText client=new EditText(activity);client.setSingleLine(true);client.setHint("Client ID do Google");client.setText(activity.getSharedPreferences("auth",0).getString("clientId",""));panel.addView(client);
        EditText secret=new EditText(activity);secret.setSingleLine(true);secret.setHint("Client secret do Google (não salvo)");panel.addView(secret);
        TextView status=new TextView(activity);status.setTextColor(-1);status.setTextSize(17);status.setPadding(0,22,0,0);panel.addView(status);
        AtomicBoolean cancelled=new AtomicBoolean(false);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Vincular conta Google").setView(panel)
            .setNegativeButton("Cancelar",(d,w)->cancelled.set(true)).setPositiveButton("Gerar código",null).create();
        dialog.setOnDismissListener(d->cancelled.set(true));dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String id=client.getText().toString().trim(), s=secret.getText().toString().trim();
            if(id.isEmpty()||s.isEmpty()){status.setText("Informe Client ID e secret do seu projeto Google.");return;}
            activity.getSharedPreferences("auth",0).edit().putString("clientId",id).apply();
            status.setText("Solicitando código...");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            Net.post("https://oauth2.googleapis.com/device/code","client_id="+Net.enc(id)+"&scope="+Net.enc("email profile"),(code,e)->{
                if(cancelled.get())return;if(e!=null||code==null||code.has("error")){status.setText("Erro ao gerar código: "+(e==null?code:e.getMessage()));return;}
                String device=code.optString("device_code"),user=code.optString("user_code"),url=code.optString("verification_url");
                if(device.isEmpty()||user.isEmpty()){status.setText("Resposta inesperada do Google.");return;}
                status.setText("No celular, abra:\n"+url+"\n\nDigite o código: "+user+"\n\nAguardando autorização...");
                long until=System.currentTimeMillis()+Math.max(60,code.optInt("expires_in",900))*1000L;
                int interval=Math.max(5,code.optInt("interval",5));
                poll(activity,id,s,device,interval,until,cancelled,status,dialog,linked);
            });
        });
    }
    private static void poll(Activity a,String id,String secret,String device,int interval,long until,AtomicBoolean cancelled,TextView status,AlertDialog dialog,Runnable linked){
        if(cancelled.get())return;if(System.currentTimeMillis()>until){status.setText("Código expirou. Gere outro.");return;}
        Net.UI.postDelayed(()->{
            if(cancelled.get())return;
            String form="client_id="+Net.enc(id)+"&client_secret="+Net.enc(secret)+"&code="+Net.enc(device)+"&grant_type="+Net.enc("http://oauth.net/grant_type/device/1.0");
            Net.post("https://oauth2.googleapis.com/token",form,(token,e)->{
                if(cancelled.get())return;
                if(e!=null){status.setText("Erro de rede: "+e.getMessage());poll(a,id,secret,device,interval,until,cancelled,status,dialog,linked);return;}
                String problem=token.optString("error");
                if(problem.equals("authorization_pending")||problem.equals("slow_down")){poll(a,id,secret,device,interval+(problem.equals("slow_down")?5:0),until,cancelled,status,dialog,linked);return;}
                if(!problem.isEmpty()){status.setText("Google: "+problem);return;}
                String access=token.optString("access_token");if(access.isEmpty()){status.setText("Token ausente.");return;}
                // Escopo email/profile, leitura do perfil diretamente do Google. Sem sincronização em nuvem.
                Net.jsonBearer("https://www.googleapis.com/oauth2/v3/userinfo",access,(profile,x)->{
                    if(cancelled.get())return;
                    if(x!=null||profile==null||profile.optString("sub").isEmpty()){status.setText("Não foi possível ler o perfil.");return;}
                    SharedPreferences.Editor edit=a.getSharedPreferences("auth",0).edit();
                    edit.putString("sub",profile.optString("sub")).putString("name",profile.optString("name","Conta Google"));edit.apply();
                    cancelled.set(true);dialog.dismiss();linked.run();
                });
            });
        },interval*1000L);
    }
}
