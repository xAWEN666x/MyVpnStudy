package com.xawen666x.myvpnstudy;

import android.content.Context;
import android.content.SharedPreferences;

import com.xawen666x.myvpnstudy.model.Server;

import static com.xawen666x.myvpnstudy.Utils.getImgURL;

public class SharedPreference {

    private static final String APP_PREFS_NAME = "MyStudyVpnPreference";

    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPrefEditor;
    private Context context;

    private static final String SERVER_COUNTRY = "server_country";
    private static final String SERVER_FLAG = "server_flag";
    private static final String SERVER_OVPN = "server_ovpn";
    private static final String SERVER_OVPN_USER = "server_ovpn_user";
    private static final String SERVER_OVPN_PASSWORD = "server_ovpn_password";

    public SharedPreference(Context context) {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE);
        this.mPrefEditor = mPreference.edit();
        this.context = context;
    }

    /**
     * Данные сервера
     * @param server данные сервера
     */
    public void saveServer(Server server){
        mPrefEditor.putString(SERVER_COUNTRY, server.getCountry());
        mPrefEditor.putString(SERVER_FLAG, server.getFlagUrl());
        mPrefEditor.putString(SERVER_OVPN, server.getOvpn());
        mPrefEditor.putString(SERVER_OVPN_USER, server.getOvpnUserName());
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.getOvpnUserPassword());
        mPrefEditor.commit();
    }

    /**
     * Получение данных сервера с главной
     * @return сервер
     */
    public Server getServer() {

        Server server = new Server(
                mPreference.getString(SERVER_COUNTRY,"Нидерланды"),
                mPreference.getString(SERVER_FLAG,getImgURL(R.drawable.netherland)),
                mPreference.getString(SERVER_OVPN,"awen-aeza-server.ovpn"),
                mPreference.getString(SERVER_OVPN_USER,""),
                mPreference.getString(SERVER_OVPN_PASSWORD,"")
        );

        return server;
    }
}
