package com.jz.dev;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.jz.dev.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //google map
        findViewById(R.id.btn_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this,"go Home",1).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage("com.google.android.apps.maps");
                ComponentName cn = new ComponentName("com.google.android.apps.maps", "com.google.android.apps.maps.LauncherShortcutActivity");
                intent.setComponent(cn);
                intent.putExtra("extra_destination_home_key",true);
                startActivity(intent);
            }
        });
        findViewById(R.id.btn_company).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this,"go Company",1).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage("com.google.android.apps.maps");
                ComponentName cn = new ComponentName("com.google.android.apps.maps", "com.google.android.apps.maps.LauncherShortcutActivity");
                intent.setComponent(cn);
                intent.putExtra("extra_destination_work_key",true);
                startActivity(intent);
            }
        });

    }

    private void yandexGoHome(){
        Toast.makeText(MainActivity.this,"go Company",1).show();
        Intent intent = new Intent("ru.yandex.yandexnavi.action.ON_SHORTCUT_HOME");
        intent.setPackage("ru.yandex.yandexnavi");
        ComponentName cn = new ComponentName("ru.yandex.yandexnavi", "ru.yandex.yandexnavi.core.NavigatorActivity");
        intent.setComponent(cn);
        startActivity(intent);
    }
    private void yandexGoCompany(){
        Toast.makeText(MainActivity.this,"go Company",1).show();
        Intent intent = new Intent("ru.yandex.yandexnavi.action.ON_SHORTCUT_WORK");
        intent.setPackage("ru.yandex.yandexnavi");
        ComponentName cn = new ComponentName("ru.yandex.yandexnavi", "ru.yandex.yandexnavi.core.NavigatorActivity");
        intent.setComponent(cn);
        startActivity(intent);
    }
}