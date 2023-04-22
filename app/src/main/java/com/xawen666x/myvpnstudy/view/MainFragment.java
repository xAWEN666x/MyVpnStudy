package com.xawen666x.myvpnstudy.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.xawen666x.myvpnstudy.CheckInternetConnection;
import com.xawen666x.myvpnstudy.R;
import com.xawen666x.myvpnstudy.SharedPreference;
import com.xawen666x.myvpnstudy.databinding.FragmentMainBinding;
import com.xawen666x.myvpnstudy.interfaces.ChangeServer;
import com.xawen666x.myvpnstudy.model.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer {

    private Server server;
    private CheckInternetConnection connection;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();
    boolean vpnStart = false;
    private SharedPreference preference;

    private FragmentMainBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        View view = binding.getRoot();
        initializeAll();

        return view;
    }

    /**
     * Инициализация всех переменных
     */
    private void initializeAll() {
        preference = new SharedPreference(getContext());
        server = preference.getServer();

        // Update current selected server icon
        updateCurrentServerIcon(server.getFlagUrl());

        connection = new CheckInternetConnection();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.vpnBtn.setOnClickListener(this);

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(getActivity().getCacheDir());
    }

    /**
     * @param v: клик
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vpnBtn:
                // Vpn is running, user would like to disconnect current connection.
                if (vpnStart) {
                    confirmDisconnect();
                }else {
                    prepareVpn();
                }
        }
    }

    /**
     * Подтверждение отключения от VPN
     */
    public void confirmDisconnect(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.connection_close_confirm));

        builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopVpn();
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Подготовка к подключению с требуемым разрешением
     */
    private void prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(getContext());

                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();//have already permission

                // Update confection status
                status("connecting");

            } else {

                // No internet connection available
                showToast("Вы не подключены к интернету!");
            }

        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("VPN выключен");
        }
    }

    /**
     * Отключение VPN
     * @return boolean: VPN статус
     */
    public boolean stopVpn() {
        try {
            vpnThread.stop();

            status("connect");
            vpnStart = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Доступ к сетевым настройкам
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Отказано в доступе! ");
        }
    }

    /**
     * Статус подключения к интернету
     */
    public boolean getInternetStatus() {
        return connection.netCheck(getContext());
    }

    /**
     * Статус сервиса
     */
    public void isServiceRunning() {
        setStatus(vpnService.getStatus());
    }

    /**
     * Запуск VPN
     */
    private void startVpn() {
        try {
            // .ovpn file
            InputStream conf = getActivity().getAssets().open(server.getOvpn());
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }

            br.readLine();
            OpenVpnApi.startVpn(getContext(), config, server.getCountry(), server.getOvpnUserName(), server.getOvpnUserPassword());

            // Update log
            binding.logTv.setText("Подключение...");
            vpnStart = true;

        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Изменение статуса соответсвенно статусу VPN подключения
     * @param connectionState
     */
    public void setStatus(String connectionState) {
        if (connectionState!= null)
        switch (connectionState) {
            case "DISCONNECTED":
                status("connect");
                vpnStart = false;
                vpnService.setDefaultStatus();
                binding.logTv.setText("");
                break;
            case "CONNECTED":
                vpnStart = true;// it will use after restart this activity
                status("connected");
                binding.logTv.setText("");
                break;
            case "WAIT":
                binding.logTv.setText("ожидание сервера!");
                break;
            case "AUTH":
                binding.logTv.setText("подключение к серверу!");
                break;
            case "RECONNECTING":
                status("connecting");
                binding.logTv.setText("Переподключение...");
                break;
            case "NONETWORK":
                binding.logTv.setText("Нет интернета");
                break;
        }

    }

    /**
     * @param status: Статус текущего подключения
     */
    public void status(String status) {

        if (status.equals("connect")) {
            binding.vpnBtn.setText(getContext().getString(R.string.connect));
        } else if (status.equals("connecting")) {
            binding.vpnBtn.setText(getContext().getString(R.string.connecting));
        } else if (status.equals("connected")) {

            binding.vpnBtn.setText(getContext().getString(R.string.disconnect));

        } else if (status.equals("tryDifferentServer")) {

            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Попробуйте другой\nСервер");
        } else if (status.equals("loading")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button);
            binding.vpnBtn.setText("Загрузка..");
        } else if (status.equals("invalidDevice")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Девайс не подходит");
        } else if (status.equals("authenticationCheck")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connecting);
            binding.vpnBtn.setText("Подключение \n Проверка...");
        }

    }

    /**
     * Вывод данных
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String duration = intent.getStringExtra("duration");
                String lastPacketReceive = intent.getStringExtra("lastPacketReceive");
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (duration == null) duration = "00:00:00";
                if (lastPacketReceive == null) lastPacketReceive = "0";
                if (byteIn == null) byteIn = " ";
                if (byteOut == null) byteOut = " ";
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    /**
     * Обновление пользовательского интерфейса
     * @param duration: Длительность подключения
     * @param lastPacketReceive: Время получения последних данных
     * @param byteIn: Входящие данные
     * @param byteOut: Исходящие данные
     */
    public void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
        binding.durationTv.setText("Активно в течении: " + duration);
        binding.lastPacketReceiveTv.setText("Данные получены: " + lastPacketReceive + " секунд назад");
        binding.byteInTv.setText("Входящие: " + byteIn);
        binding.byteOutTv.setText("Исходящие: " + byteOut);
    }

    /**
     * Вывод сообщения
     * @param message: сообщение
     */
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Смена иконки впн сервера
     * @param serverIcon: адрес иконки
     */
    public void updateCurrentServerIcon(String serverIcon) {
        Glide.with(getContext())
                .load(serverIcon)
                .into(binding.selectedServerIcon);
    }

    /**
     * Смена сервера, когда юзер выбрал новый сервер
     * @param server данные сервера
     */
    @Override
    public void newServer(Server server) {
        this.server = server;
        updateCurrentServerIcon(server.getFlagUrl());

        // Stop previous connection
        if (vpnStart) {
            stopVpn();
        }

        prepareVpn();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));

        if (server == null) {
            server = preference.getServer();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /**
     * Сохранение текущего сервера на главной странице
     */
    @Override
    public void onStop() {
        if (server != null) {
            preference.saveServer(server);
        }

        super.onStop();
    }
}
